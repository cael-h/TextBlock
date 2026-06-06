/*
 * Copyright (C) 2025
 *
 * This file is part of QUIK.
 *
 * QUIK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QUIK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QUIK.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.worker

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.octoshrimpy.quik.blocking.BlockingClient
import dev.octoshrimpy.quik.interactor.UpdateBadge
import dev.octoshrimpy.quik.manager.NotificationManager
import dev.octoshrimpy.quik.manager.ShortcutManager
import dev.octoshrimpy.quik.model.Message
import dev.octoshrimpy.quik.repository.ContactRepository
import dev.octoshrimpy.quik.repository.ConversationRepository
import dev.octoshrimpy.quik.repository.MessageContentFilterRepository
import dev.octoshrimpy.quik.repository.MessageRepository
import dev.octoshrimpy.quik.textblock.ClassificationResult
import dev.octoshrimpy.quik.textblock.InboundMessageClassifier
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import dev.octoshrimpy.quik.textblock.TextBlockFilterDecision
import dev.octoshrimpy.quik.textblock.TextBlockFilterPolicy
import dev.octoshrimpy.quik.util.Preferences
import timber.log.Timber
import javax.inject.Inject

class ReceiveSmsWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {
    companion object {
        const val INPUT_DATA_KEY_MESSAGE_ID = "messageId"
    }

    @Inject lateinit var conversationRepo: ConversationRepository
    @Inject lateinit var blockingClient: BlockingClient
    @Inject lateinit var prefs: Preferences
    @Inject lateinit var messageRepo: MessageRepository
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var updateBadge: UpdateBadge
    @Inject lateinit var shortcutManager: ShortcutManager
    @Inject lateinit var filterRepo: MessageContentFilterRepository
    @Inject lateinit var contactsRepo: ContactRepository
    @Inject lateinit var inboundMessageClassifier: InboundMessageClassifier

    override fun doWork(): Result {
        Timber.v("started")

        val messageId = inputData.getLong(INPUT_DATA_KEY_MESSAGE_ID, -1)
        if (messageId < 0) {
            Timber.v("failed. message id was {messageId}")
            return Result.failure(inputData)
        }

        val message = messageRepo.getMessage(messageId) ?: return Result.failure(inputData)

        val action = blockingClient.shouldBlock(message.address).blockingGet()

        when {
            ((action is BlockingClient.Action.Block) && prefs.drop.get()) -> {
                // blocked and 'drop blocked' remove from db and don't continue
                Timber.v("address is blocked and drop blocked is on. dropped")
                messageRepo.deleteMessages(listOf(message.id))
                return Result.failure(inputData)
            }

            action is BlockingClient.Action.Block -> {
                // blocked
                Timber.v("address is blocked")
                messageRepo.markRead(listOf(message.threadId))
                conversationRepo.markBlocked(
                    listOf(message.threadId),
                    prefs.blockingManager.get(),
                    action.reason
                )
            }

            action is BlockingClient.Action.Unblock -> {
                // unblock
                Timber.v("unblock conversation if blocked")
                conversationRepo.markUnblocked(message.threadId)
            }
        }

        val messageFilterAction = filterRepo.isBlocked(message.getText(), message.address, contactsRepo)
        if (messageFilterAction) {
            Timber.v("message dropped based on content filters")
            messageRepo.deleteMessages(listOf(message.id))
            return Result.failure(inputData)
        }

        // update and fetch conversation
        conversationRepo.updateConversations(listOf(message.threadId))

        val senderIsContact = contactsRepo.isContact(message.address)
        if (TextBlockFilterPolicy.shouldClassify(
                filteringEnabled = prefs.textBlockFiltering.get(),
                allowContacts = prefs.textBlockAllowContacts.get(),
                isFromContact = senderIsContact
            )
        ) {
            val textBlockResult = classifyTextBlock(
                message,
                TextBlockFilterPolicy.isFromContactForClassifier(
                    allowContacts = prefs.textBlockAllowContacts.get(),
                    isFromContact = senderIsContact
                )
            )
            when (TextBlockFilterPolicy.actionFor(textBlockResult, isTextBlockDropMode())) {
                TextBlockFilterDecision.ALLOW -> Unit

                TextBlockFilterDecision.QUARANTINE -> {
                    Timber.v("TextBlock quarantined SMS as ${textBlockResult.action}")
                    messageRepo.markRead(listOf(message.threadId))
                    conversationRepo.markBlocked(
                        listOf(message.threadId),
                        prefs.blockingManager.get(),
                        textBlockResult.toBlockReason()
                    )
                    return Result.failure(inputData)
                }

                TextBlockFilterDecision.DROP -> {
                    Timber.v("TextBlock dropped SMS as ${textBlockResult.action}")
                    messageRepo.deleteMessages(listOf(message.id))
                    return Result.failure(inputData)
                }
            }
        }

        val conversation = conversationRepo.getOrCreateConversation(message.threadId)
            ?: return Result.failure(inputData)

        // don't notify (continue) for blocked conversations
        if (conversation.blocked) {
            Timber.v("no notifications for blocked")
            return Result.failure(inputData)
        }

        // unarchive conversation if necessary
        if (conversation.archived) {
            Timber.v("conversation unarchived")
            conversationRepo.markUnarchived(listOf(conversation.id))
        }

        // update/create notification
        Timber.v("update/create notification")
        notificationManager.update(conversation.id)

        // update shortcuts
        Timber.v("update shortcuts")
        shortcutManager.updateShortcuts()
        shortcutManager.getOrCreateShortcut(conversation.id)

        // update the badge and widget
        Timber.v("update badge and widget")
        updateBadge.execute(Unit)

        Timber.v("finished")

        return Result.success()
    }

    private fun classifyTextBlock(message: Message, isFromContact: Boolean): ClassificationResult {
        return inboundMessageClassifier.classify(
            InboundMessageForClassification(
                address = message.address,
                body = message.getText(),
                timestampMillis = message.date,
                isMms = message.isMms(),
                isFromContact = isFromContact,
                subscriptionId = message.subId
            )
        )
    }

    private fun isTextBlockDropMode(): Boolean {
        return prefs.textBlockFilterMode.get() == Preferences.TEXTBLOCK_FILTER_MODE_DROP
    }

    private fun ClassificationResult.toBlockReason(): String {
        return buildString {
            append("TextBlock ")
            append(category.name)
            append(" confidence=")
            append(confidence)
            reason?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
        }
    }

    override fun getForegroundInfo() = ForegroundInfo(
        0,
        notificationManager.getForegroundNotificationForWorkersOnOlderAndroids()
    )

}
