/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.android.AndroidInjection
import dev.octoshrimpy.quik.repository.ContactRepository
import dev.octoshrimpy.quik.repository.ConversationRepository
import dev.octoshrimpy.quik.repository.MessageRepository
import dev.octoshrimpy.quik.textblock.InboundMessageClassifier
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import dev.octoshrimpy.quik.textblock.TextBlockReceiveEffect
import dev.octoshrimpy.quik.textblock.TextBlockReceivePolicy
import dev.octoshrimpy.quik.textblock.toTextBlockBlockReason
import dev.octoshrimpy.quik.util.Preferences
import dev.octoshrimpy.quik.worker.ReceiveSmsWorker
import dev.octoshrimpy.quik.worker.ReceiveSmsWorker.Companion.INPUT_DATA_KEY_MESSAGE_ID
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SmsReceivedReceiver : BroadcastReceiver() {
    @Inject lateinit var messageRepo: MessageRepository
    @Inject lateinit var conversationRepo: ConversationRepository
    @Inject lateinit var contactsRepo: ContactRepository
    @Inject lateinit var prefs: Preferences
    @Inject lateinit var inboundMessageClassifier: InboundMessageClassifier

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        Sms.Intents.getMessagesFromIntent(intent)?.let { messages ->
            val pendingResult = goAsync()
            // reduce list of messages to single message and save in db
            Single.just(messages)
                .observeOn(Schedulers.io())
                .map { smsMessages ->
                    persistAndMaybeFilterSms(intent, smsMessages)
                }
                .subscribe({ result ->
                    result.workerMessageId?.let { messageId ->
                        WorkManager.getInstance(context).enqueue(
                            OneTimeWorkRequestBuilder<ReceiveSmsWorker>()
                                .setInputData(workDataOf(INPUT_DATA_KEY_MESSAGE_ID to messageId))
                                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .build()
                        )
                    }
                    pendingResult.finish()
                }, { error ->
                    Timber.e(error, "error receiving new sms")
                    pendingResult.finish()
                })
        }
    }

    private fun persistAndMaybeFilterSms(
        intent: Intent,
        messages: Array<SmsMessage>
    ): ReceiveSmsResult {
        Timber.v("onReceive() new sms")  // here so runs on io thread

        val subscriptionId = intent.extras?.getInt("subscription", -1) ?: -1
        val address = messages[0].displayOriginatingAddress
        val body = messages.mapNotNull { it.displayMessageBody }.joinToString(separator = "")
        val sentTime = messages[0].timestampMillis
        val message = messageRepo.insertReceivedSms(subscriptionId, address, body, sentTime)

        return try {
            val senderIsContact = contactsRepo.isContact(address)
            val textBlockDecision = TextBlockReceivePolicy.evaluate(
                filteringEnabled = prefs.textBlockFiltering.get(),
                allowContacts = prefs.textBlockAllowContacts.get(),
                isFromContact = senderIsContact,
                dropMode = isTextBlockDropMode()
            ) { isFromContactForClassifier ->
                inboundMessageClassifier.classify(
                    InboundMessageForClassification(
                        address = address,
                        body = body,
                        timestampMillis = sentTime,
                        isMms = false,
                        isFromContact = isFromContactForClassifier,
                        subscriptionId = subscriptionId
                    )
                )
            }

            when (textBlockDecision.effect) {
                TextBlockReceiveEffect.ALLOW -> ReceiveSmsResult(workerMessageId = message.id)

                TextBlockReceiveEffect.QUARANTINE -> {
                    val textBlockResult = textBlockDecision.requireClassificationResult()
                    Timber.v("TextBlock quarantined SMS before worker as ${textBlockResult.action}")
                    conversationRepo.updateConversations(listOf(message.threadId))
                    messageRepo.markRead(listOf(message.threadId))
                    conversationRepo.markBlocked(
                        listOf(message.threadId),
                        prefs.blockingManager.get(),
                        textBlockResult.toTextBlockBlockReason()
                    )
                    ReceiveSmsResult()
                }

                TextBlockReceiveEffect.DROP -> {
                    val textBlockResult = textBlockDecision.requireClassificationResult()
                    Timber.v("TextBlock dropped SMS before worker as ${textBlockResult.action}")
                    messageRepo.deleteMessages(listOf(message.id))
                    ReceiveSmsResult()
                }
            }
        } catch (error: Exception) {
            Timber.e(error, "TextBlock pre-worker SMS filter failed")
            ReceiveSmsResult(workerMessageId = message.id)
        }
    }

    private fun isTextBlockDropMode(): Boolean {
        return prefs.textBlockFilterMode.get() == Preferences.TEXTBLOCK_FILTER_MODE_DROP
    }

    private data class ReceiveSmsResult(
        val workerMessageId: Long? = null
    )

}
