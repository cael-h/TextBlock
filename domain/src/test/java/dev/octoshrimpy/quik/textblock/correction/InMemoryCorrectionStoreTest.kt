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
package dev.octoshrimpy.quik.textblock.correction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCorrectionStoreTest {

    @Test
    fun resolvesNotSpamAsForceAllowForExactBodyFingerprint() {
        val store = InMemoryCorrectionStore()
        val signals = CorrectionSignals.from(
            address = "30330",
            body = syntheticBody()
        )
        store.save(CorrectionAction.NOT_SPAM, signals, createdAtMillis = 42L)

        val decision = store.resolve(
            CorrectionSignals.from(
                address = "99999",
                body = syntheticBody()
            )
        )

        assertEquals(CorrectionDecisionOutcome.FORCE_ALLOW, decision.outcome)
        assertEquals(CorrectionAction.NOT_SPAM, decision.matchedAction)
        assertEquals(signals.bodyFingerprint, decision.matchedKey)
    }

    @Test
    fun resolvesBlockSimilarByTokenSignature() {
        val store = InMemoryCorrectionStore()
        val original = CorrectionSignals.from(
            address = "30330",
            body = "synthetic campaign donate update"
        )
        val similar = CorrectionSignals.from(
            address = "40404",
            body = "update donate synthetic campaign"
        )
        store.save(CorrectionAction.BLOCK_SIMILAR, original)

        val decision = store.resolve(similar)

        assertEquals(CorrectionDecisionOutcome.BLOCK_SIMILAR, decision.outcome)
        assertEquals(CorrectionAction.BLOCK_SIMILAR, decision.matchedAction)
        assertEquals(original.tokenSignature, decision.matchedKey)
    }

    @Test
    fun notSpamPrecedesBlockSimilarForSameSignals() {
        val store = InMemoryCorrectionStore()
        val signals = CorrectionSignals.from(
            address = "30330",
            body = syntheticBody()
        )
        store.save(CorrectionAction.BLOCK_SIMILAR, signals)
        store.save(CorrectionAction.NOT_SPAM, signals)

        val decision = store.resolve(signals)

        assertEquals(CorrectionDecisionOutcome.FORCE_ALLOW, decision.outcome)
        assertEquals(CorrectionAction.NOT_SPAM, decision.matchedAction)
    }

    @Test
    fun resolveReturnsNoMatchWhenNoCorrectionKeyMatches() {
        val store = InMemoryCorrectionStore()
        store.save(
            action = CorrectionAction.BLOCK_SIMILAR,
            signals = CorrectionSignals.from(address = "30330", body = syntheticBody())
        )

        val decision = store.resolve(
            CorrectionSignals.from(
                address = "40404",
                body = "synthetic appointment reminder"
            )
        )

        assertEquals(CorrectionDecision.NoMatch, decision)
    }

    @Test
    fun storedRecordsDoNotExposeRawMessageBodies() {
        val store = InMemoryCorrectionStore()
        val body = syntheticBody()
        store.save(
            action = CorrectionAction.BLOCK_SIMILAR,
            signals = CorrectionSignals.from(address = "30330", body = body)
        )

        val recordText = store.records().joinToString()

        assertFalse(recordText.contains(body))
        assertFalse(recordText.contains("Synthetic campaign"))
        assertTrue(store.records().all { it.key.sha256.matches(Regex("[a-f0-9]{64}")) })
    }

    private fun syntheticBody(): String {
        return "Synthetic campaign update: donate today."
    }
}
