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
package dev.octoshrimpy.quik.textblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextBlockReceivePolicyTest {

    @Test
    fun filteringDisabledAllowsWithoutClassifying() {
        var classifyCalls = 0

        val decision = TextBlockReceivePolicy.evaluate(
            filteringEnabled = false,
            allowContacts = false,
            isFromContact = false,
            dropMode = false
        ) {
            classifyCalls++
            quarantineResult
        }

        assertEquals(TextBlockReceiveEffect.ALLOW, decision.effect)
        assertFalse(decision.suppressesNotification)
        assertEquals(0, classifyCalls)
    }

    @Test
    fun contactAllowlistAllowsContactWithoutClassifying() {
        var classifyCalls = 0

        val decision = TextBlockReceivePolicy.evaluate(
            filteringEnabled = true,
            allowContacts = true,
            isFromContact = true,
            dropMode = false
        ) {
            classifyCalls++
            quarantineResult
        }

        assertEquals(TextBlockReceiveEffect.ALLOW, decision.effect)
        assertFalse(decision.suppressesNotification)
        assertEquals(0, classifyCalls)
    }

    @Test
    fun quarantineModeQuarantinesSuppressingClassificationResult() {
        val decision = TextBlockReceivePolicy.evaluate(
            filteringEnabled = true,
            allowContacts = false,
            isFromContact = false,
            dropMode = false
        ) { isFromContactForClassifier ->
            assertFalse(isFromContactForClassifier)
            quarantineResult
        }

        assertEquals(TextBlockReceiveEffect.QUARANTINE, decision.effect)
        assertEquals(quarantineResult, decision.classificationResult)
        assertTrue(decision.suppressesNotification)
    }

    @Test
    fun dropModeDropsSuppressingClassificationResult() {
        val decision = TextBlockReceivePolicy.evaluate(
            filteringEnabled = true,
            allowContacts = false,
            isFromContact = false,
            dropMode = true
        ) {
            quarantineResult
        }

        assertEquals(TextBlockReceiveEffect.DROP, decision.effect)
        assertEquals(quarantineResult, decision.classificationResult)
        assertTrue(decision.suppressesNotification)
    }

    @Test
    fun allowClassificationResultAllowsEvenInDropMode() {
        val decision = TextBlockReceivePolicy.evaluate(
            filteringEnabled = true,
            allowContacts = false,
            isFromContact = false,
            dropMode = true
        ) {
            ClassificationResult.Allow
        }

        assertEquals(TextBlockReceiveEffect.ALLOW, decision.effect)
        assertFalse(decision.suppressesNotification)
    }

    @Test
    fun mmsQuarantineStillPlansProtocolResponsesAfterPersistence() {
        val plan = TextBlockReceivePolicy.mmsPostPersistencePlan(
            TextBlockReceiveDecision(
                effect = TextBlockReceiveEffect.QUARANTINE,
                classificationResult = quarantineResult
            )
        )

        assertFalse(plan.shouldNotifyUser)
        assertTrue(plan.shouldSendAcknowledgeInd)
        assertTrue(plan.shouldSendNotifyRespInd)
    }

    @Test
    fun mmsDropStillPlansProtocolResponsesAfterPersistence() {
        val plan = TextBlockReceivePolicy.mmsPostPersistencePlan(
            TextBlockReceiveDecision(
                effect = TextBlockReceiveEffect.DROP,
                classificationResult = quarantineResult
            )
        )

        assertFalse(plan.shouldNotifyUser)
        assertTrue(plan.shouldSendAcknowledgeInd)
        assertTrue(plan.shouldSendNotifyRespInd)
    }

    private val quarantineResult = ClassificationResult(
        action = FilterAction.QUARANTINE,
        category = FilterCategory.POLITICAL,
        confidence = 0.9f,
        reason = "campaign fundraising"
    )
}
