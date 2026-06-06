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

import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionSignalsTest {

    @Test
    fun sameMessageYieldsSameKeys() {
        val first = CorrectionSignals.from(
            InboundMessageForClassification(
                address = "30330",
                body = syntheticFundraisingBody()
            )
        )
        val second = CorrectionSignals.from(
            InboundMessageForClassification(
                address = "30330",
                body = syntheticFundraisingBody()
            )
        )

        assertEquals(first.bodyFingerprint, second.bodyFingerprint)
        assertEquals(first.tokenSignature, second.tokenSignature)
        assertEquals(first.senderFingerprint, second.senderFingerprint)
    }

    @Test
    fun unicodeCompatibilityAndSpacingNormalizeToSameBodyFingerprint() {
        val plain = CorrectionSignals.from(
            address = "30330",
            body = "Synthetic campaign update donate today."
        )
        val stylized = CorrectionSignals.from(
            address = "30330",
            body = "Ｓｙｎｔｈｅｔｉｃ   ＣＡＭＰＡＩＧＮ\nupdate donate today."
        )

        assertEquals(plain.bodyFingerprint, stylized.bodyFingerprint)
        assertEquals(plain.tokenSignature, stylized.tokenSignature)
    }

    @Test
    fun tokenSignatureIgnoresTokenOrderAndDuplicateTokens() {
        val original = CorrectionSignals.from(
            address = "30330",
            body = "campaign donate synthetic synthetic update"
        )
        val reordered = CorrectionSignals.from(
            address = "30330",
            body = "update synthetic donate campaign"
        )

        assertNotEquals(original.bodyFingerprint, reordered.bodyFingerprint)
        assertEquals(original.tokenSignature, reordered.tokenSignature)
    }

    @Test
    fun keysAreTypedSha256Values() {
        val signals = CorrectionSignals.from(
            address = "+15551234567",
            body = syntheticFundraisingBody()
        )

        assertKey(signals.bodyFingerprint, CorrectionKeyType.BODY_SHA256)
        assertKey(signals.tokenSignature, CorrectionKeyType.TOKEN_SIGNATURE_SHA256)
        assertNotNull(signals.senderFingerprint)
        assertKey(signals.senderFingerprint!!, CorrectionKeyType.SENDER_SHA256)
    }

    private fun syntheticFundraisingBody(): String {
        return "Synthetic campaign update: donate today."
    }

    private fun assertKey(key: CorrectionKey, expectedType: CorrectionKeyType) {
        assertEquals(expectedType, key.type)
        assertTrue(
            "Expected SHA-256 hex but was ${key.sha256}",
            Regex("[a-f0-9]{64}").matches(key.sha256)
        )
    }
}
