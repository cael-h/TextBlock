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

import dev.octoshrimpy.quik.textblock.ClassificationResult
import dev.octoshrimpy.quik.textblock.FilterAction
import dev.octoshrimpy.quik.textblock.FilterCategory
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import dev.octoshrimpy.quik.textblock.TextBlockReceiveEffect
import org.junit.Assert.assertEquals
import org.junit.Test

class TextBlockCleanupPlannerTest {

    @Test
    fun quarantinePlanSkipsContactsWhenAllowContactsIsEnabled() {
        val plan = TextBlockCleanupPlanner.plan(
            candidates = listOf(
                candidate(messageId = 1, address = "+contact", body = "donate today"),
                candidate(messageId = 2, address = "+unknown", body = "donate today"),
                candidate(messageId = 3, address = "+unknown", body = "family dinner")
            ),
            params = TextBlockCleanup.Params(
                sinceMillis = 0L,
                action = TextBlockCleanupAction.QUARANTINE,
                allowContacts = true,
                blockingClient = 0
            ),
            isContact = { address -> address == "+contact" },
            classify = ::classifyDonateMessages
        )

        assertEquals(3, plan.scanned)
        assertEquals(1, plan.skippedContacts)
        assertEquals(listOf(2L), plan.matches.map { match -> match.candidate.messageId })
        assertEquals(TextBlockReceiveEffect.QUARANTINE, plan.matches.single().effect)
    }

    @Test
    fun deletePlanTurnsMatchedMessagesIntoDropEffects() {
        val plan = TextBlockCleanupPlanner.plan(
            candidates = listOf(candidate(messageId = 4, body = "donate today")),
            params = TextBlockCleanup.Params(
                sinceMillis = 0L,
                action = TextBlockCleanupAction.DELETE,
                allowContacts = false,
                blockingClient = 0
            ),
            isContact = { false },
            classify = ::classifyDonateMessages
        )

        assertEquals(1, plan.scanned)
        assertEquals(0, plan.skippedContacts)
        assertEquals(listOf(4L), plan.matches.map { match -> match.candidate.messageId })
        assertEquals(TextBlockReceiveEffect.DROP, plan.matches.single().effect)
    }

    private fun classifyDonateMessages(message: InboundMessageForClassification): ClassificationResult {
        return when {
            message.body.contains("donate") -> ClassificationResult(
                action = FilterAction.QUARANTINE,
                category = FilterCategory.POLITICAL,
                confidence = 0.9f,
                reason = "fundraising"
            )

            else -> ClassificationResult.Allow
        }
    }

    private fun candidate(
        messageId: Long,
        address: String = "+unknown",
        body: String
    ): TextBlockCleanupCandidate {
        return TextBlockCleanupCandidate(
            messageId = messageId,
            threadId = messageId * 10,
            address = address,
            body = body,
            timestampMillis = 1_700_000_000_000L,
            isMms = false,
            subscriptionId = 1
        )
    }
}
