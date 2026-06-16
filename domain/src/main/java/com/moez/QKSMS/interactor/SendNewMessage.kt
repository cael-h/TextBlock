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
package dev.octoshrimpy.quik.interactor

import dev.octoshrimpy.quik.manager.ShortcutManager
import dev.octoshrimpy.quik.model.Attachment
import dev.octoshrimpy.quik.repository.ConversationRepository
import dev.octoshrimpy.quik.repository.MessageRepository
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

class SendNewMessage @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val updateBadge: UpdateBadge,
    private val shortcutManager: ShortcutManager
) : Interactor<SendNewMessage.Params>() {

    data class Params(
        val subId: Int,
        val threadId: Long,
        val addresses: Collection<String>,
        val body: String,
        val sendAsGroup: Boolean,
        val attachments: Collection<Attachment> = listOf(),
        val delay: Int = 0
    )

    data class Result(
        val threadIds: List<Long>
    ) {
        val created: Boolean = threadIds.isNotEmpty()
    }

    override fun buildObservable(params: Params): Flowable<Result> = Flowable.fromCallable {
            // if addresses are provided, prefer them over the thread id because from a user
            // perspective it is more important that the intended recipients are messaged rather
            // than that messages go to a thread id
            val conversation = when {
                params.addresses.isNotEmpty() ->
                    conversationRepo.getOrCreateConversation(params.addresses)

                (params.threadId > 0) ->
                    conversationRepo.getOrCreateConversation(params.threadId)

                else -> null
            }

            if (conversation == null) {
                Timber.e("unable to get or create a conversation record")
                Result(emptyList())
            } else {
                val messages = messageRepo.sendNewMessages(
                    params.subId,
                    conversation.recipients.map { it.address },
                    params.body,
                    params.attachments,
                    params.sendAsGroup,
                    params.delay
                )

                Result(messages.map { it.threadId })
            }
        }
        .doOnNext { result ->
            if (!result.created) {
                Timber.w("send did not create any message records")
                return@doOnNext
            }

            conversationRepo.updateConversations(result.threadIds)
            conversationRepo.markUnarchived(result.threadIds)

            AndroidSchedulers.mainThread().scheduleDirect {
                result.threadIds.forEach { shortcutManager.getOrCreateShortcut(it) }
            }

            // delete attachment local files, if any, because they're saved to mms db by now
            params.attachments.forEach { it.removeCacheFile() }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap { result ->
            updateBadge.buildObservable(Unit)
                .map { result }
                .defaultIfEmpty(result)
        } // Update the widget

}
