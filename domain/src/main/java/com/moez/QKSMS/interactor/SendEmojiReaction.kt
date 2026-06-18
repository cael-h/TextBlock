/*
 * Copyright (C) 2026
 *
 * This file is part of TextBlock.
 *
 * TextBlock is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.octoshrimpy.quik.interactor

import dev.octoshrimpy.quik.manager.ShortcutManager
import dev.octoshrimpy.quik.repository.ConversationRepository
import dev.octoshrimpy.quik.repository.MessageRepository
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

class SendEmojiReaction @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val updateBadge: UpdateBadge,
    private val shortcutManager: ShortcutManager
) : Interactor<SendEmojiReaction.Params>() {

    data class Params(
        val subId: Int,
        val addresses: Collection<String>,
        val targetMessageId: Long,
        val emoji: String,
        val sendAsGroup: Boolean
    )

    data class Result(
        val threadIds: List<Long>
    ) {
        val created: Boolean = threadIds.isNotEmpty()
    }

    override fun buildObservable(params: Params): Flowable<Result> = Flowable.fromCallable {
        val conversation = conversationRepo.getOrCreateConversation(params.addresses)
        if (conversation == null) {
            Timber.e("unable to get or create a conversation for emoji reaction")
            Result(emptyList())
        } else {
            val messages = messageRepo.sendEmojiReaction(
                subId = params.subId,
                toAddresses = conversation.recipients.map { it.address },
                targetMessageId = params.targetMessageId,
                emoji = params.emoji,
                sendAsGroup = params.sendAsGroup
            )

            Result(messages.map { it.threadId }.distinct())
        }
    }
        .doOnNext { result ->
            if (!result.created) {
                Timber.w("emoji reaction send did not create any message records")
                return@doOnNext
            }

            conversationRepo.updateConversations(result.threadIds)
            conversationRepo.markUnarchived(result.threadIds)

            AndroidSchedulers.mainThread().scheduleDirect {
                result.threadIds.forEach { shortcutManager.getOrCreateShortcut(it) }
            }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap { result ->
            updateBadge.buildObservable(Unit)
                .map { result }
                .defaultIfEmpty(result)
        }
}
