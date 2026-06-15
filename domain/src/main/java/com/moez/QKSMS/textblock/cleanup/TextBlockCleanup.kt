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
package dev.octoshrimpy.quik.textblock.cleanup

import dev.octoshrimpy.quik.manager.NotificationManager
import dev.octoshrimpy.quik.manager.ShortcutManager
import dev.octoshrimpy.quik.manager.WidgetManager
import dev.octoshrimpy.quik.repository.ContactRepository
import dev.octoshrimpy.quik.repository.ConversationRepository
import dev.octoshrimpy.quik.repository.MessageRepository
import dev.octoshrimpy.quik.textblock.ClassificationResult
import dev.octoshrimpy.quik.textblock.InboundMessageClassifier
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import dev.octoshrimpy.quik.textblock.TextBlockReceiveEffect
import dev.octoshrimpy.quik.textblock.TextBlockReceivePolicy
import dev.octoshrimpy.quik.textblock.toTextBlockBlockReason
import io.reactivex.Flowable
import javax.inject.Inject

class TextBlockCleanup @Inject constructor(
    private val contactRepo: ContactRepository,
    private val conversationRepo: ConversationRepository,
    private val inboundMessageClassifier: InboundMessageClassifier,
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val shortcutManager: ShortcutManager,
    private val widgetManager: WidgetManager
) {

    data class Params(
        val sinceMillis: Long,
        val action: TextBlockCleanupAction,
        val allowContacts: Boolean,
        val blockingClient: Int
    )

    data class Result(
        val scanned: Int,
        val matched: Int,
        val quarantined: Int,
        val deleted: Int,
        val skippedContacts: Int
    )

    fun buildObservable(params: Params): Flowable<Result> {
        return Flowable.fromCallable { run(params) }
    }

    private fun run(params: Params): Result {
        val candidates = messageRepo.getTextBlockCleanupCandidates(params.sinceMillis)
        val contactCache = mutableMapOf<String, Boolean>()
        val plan = TextBlockCleanupPlanner.plan(
            candidates = candidates,
            params = params,
            isContact = { address -> contactCache.getOrPut(address) { contactRepo.isContact(address) } },
            classify = inboundMessageClassifier::classify
        )

        val matchedMessageIds = plan.matches.map { match -> match.candidate.messageId }
        val matchedThreadIds = plan.matches
            .map { match -> match.candidate.threadId }
            .distinct()

        when (params.action) {
            TextBlockCleanupAction.QUARANTINE -> quarantineMatches(
                matches = plan.matches,
                threadIds = matchedThreadIds,
                blockingClient = params.blockingClient
            )

            TextBlockCleanupAction.DELETE -> deleteMatches(
                messageIds = matchedMessageIds,
                threadIds = matchedThreadIds
            )
        }

        matchedThreadIds.forEach(notificationManager::update)
        if (matchedThreadIds.isNotEmpty()) {
            shortcutManager.updateBadge()
            widgetManager.sendDatasetChanged()
        }

        return Result(
            scanned = plan.scanned,
            matched = plan.matches.size,
            quarantined = when (params.action) {
                TextBlockCleanupAction.QUARANTINE -> plan.matches.size
                TextBlockCleanupAction.DELETE -> 0
            },
            deleted = when (params.action) {
                TextBlockCleanupAction.QUARANTINE -> 0
                TextBlockCleanupAction.DELETE -> plan.matches.size
            },
            skippedContacts = plan.skippedContacts
        )
    }

    private fun quarantineMatches(
        matches: List<TextBlockCleanupMatch>,
        threadIds: List<Long>,
        blockingClient: Int
    ) {
        if (matches.isEmpty()) return

        threadIds.forEach { threadId -> conversationRepo.getOrCreateConversation(threadId) }
        conversationRepo.updateConversations(threadIds)
        messageRepo.markRead(threadIds)

        matches
            .groupBy { match -> match.candidate.threadId }
            .forEach { (threadId, threadMatches) ->
                conversationRepo.markBlocked(
                    listOf(threadId),
                    blockingClient,
                    threadMatches.first().classificationResult.toTextBlockBlockReason()
                )
            }
    }

    private fun deleteMatches(
        messageIds: List<Long>,
        threadIds: List<Long>
    ) {
        if (messageIds.isEmpty()) return

        messageRepo.deleteMessages(messageIds)
        conversationRepo.updateConversations(threadIds)
    }

}

enum class TextBlockCleanupAction {
    QUARANTINE,
    DELETE
}

data class TextBlockCleanupCandidate(
    val messageId: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestampMillis: Long,
    val isMms: Boolean,
    val subscriptionId: Int
)

data class TextBlockCleanupPlan(
    val scanned: Int,
    val skippedContacts: Int,
    val matches: List<TextBlockCleanupMatch>
)

data class TextBlockCleanupMatch(
    val candidate: TextBlockCleanupCandidate,
    val classificationResult: ClassificationResult,
    val effect: TextBlockReceiveEffect
)

object TextBlockCleanupPlanner {

    fun plan(
        candidates: List<TextBlockCleanupCandidate>,
        params: TextBlockCleanup.Params,
        isContact: (String) -> Boolean,
        classify: (InboundMessageForClassification) -> ClassificationResult
    ): TextBlockCleanupPlan {
        var skippedContacts = 0
        val matches = candidates.mapNotNull { candidate ->
            val isFromContact = isContact(candidate.address)
            if (params.allowContacts && isFromContact) {
                skippedContacts++
                return@mapNotNull null
            }

            val decision = TextBlockReceivePolicy.evaluate(
                filteringEnabled = true,
                allowContacts = params.allowContacts,
                isFromContact = isFromContact,
                dropMode = params.action == TextBlockCleanupAction.DELETE
            ) { isFromContactForClassifier ->
                classify(candidate.toInboundMessage(isFromContactForClassifier))
            }

            when (decision.effect) {
                TextBlockReceiveEffect.ALLOW -> null
                TextBlockReceiveEffect.QUARANTINE,
                TextBlockReceiveEffect.DROP -> TextBlockCleanupMatch(
                    candidate = candidate,
                    classificationResult = decision.requireClassificationResult(),
                    effect = decision.effect
                )
            }
        }

        return TextBlockCleanupPlan(
            scanned = candidates.size,
            skippedContacts = skippedContacts,
            matches = matches
        )
    }

    private fun TextBlockCleanupCandidate.toInboundMessage(
        isFromContactForClassifier: Boolean
    ): InboundMessageForClassification {
        return InboundMessageForClassification(
            address = address,
            body = body,
            timestampMillis = timestampMillis,
            isMms = isMms,
            isFromContact = isFromContactForClassifier,
            subscriptionId = subscriptionId
        )
    }
}
