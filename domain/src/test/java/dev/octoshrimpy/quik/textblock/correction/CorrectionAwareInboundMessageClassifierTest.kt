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

import dev.octoshrimpy.quik.textblock.ClassificationResult
import dev.octoshrimpy.quik.textblock.FilterAction
import dev.octoshrimpy.quik.textblock.FilterCategory
import dev.octoshrimpy.quik.textblock.InboundMessageClassifier
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionAwareInboundMessageClassifierTest {

    @Test
    fun noCorrectionMatchDelegatesToBaseClassifier() {
        val baseResult = ClassificationResult(
            action = FilterAction.QUARANTINE,
            category = FilterCategory.FUNDRAISING,
            confidence = 0.7f,
            reason = "base classifier"
        )
        val baseClassifier = RecordingClassifier(baseResult)
        val classifier = CorrectionAwareInboundMessageClassifier(
            correctionStore = InMemoryCorrectionStore(),
            delegate = baseClassifier
        )

        val result = classifier.classify(syntheticMessage())

        assertEquals(baseResult, result)
        assertEquals(1, baseClassifier.callCount)
    }

    @Test
    fun notSpamCorrectionForcesAllowBeforeBaseClassifier() {
        val message = syntheticMessage()
        val store = InMemoryCorrectionStore()
        store.save(
            action = CorrectionAction.NOT_SPAM,
            signals = CorrectionSignals.from(message)
        )
        val baseClassifier = RecordingClassifier(
            ClassificationResult(
                action = FilterAction.QUARANTINE,
                category = FilterCategory.POLITICAL,
                confidence = 0.9f,
                reason = "base classifier"
            )
        )
        val classifier = CorrectionAwareInboundMessageClassifier(
            correctionStore = store,
            delegate = baseClassifier
        )

        val result = classifier.classify(message)

        assertEquals(FilterAction.ALLOW, result.action)
        assertEquals(FilterCategory.UNKNOWN, result.category)
        assertEquals(1f, result.confidence)
        assertTrue(result.reason?.contains("not spam") == true)
        assertEquals(0, baseClassifier.callCount)
    }

    @Test
    fun blockSimilarCorrectionQuarantinesBeforeBaseClassifier() {
        val original = syntheticMessage(
            body = "Synthetic campaign donate update today"
        )
        val similar = syntheticMessage(
            address = "40404",
            body = "today update donate campaign synthetic"
        )
        val store = InMemoryCorrectionStore()
        store.save(
            action = CorrectionAction.BLOCK_SIMILAR,
            signals = CorrectionSignals.from(original)
        )
        val baseClassifier = RecordingClassifier(ClassificationResult.Allow)
        val classifier = CorrectionAwareInboundMessageClassifier(
            correctionStore = store,
            delegate = baseClassifier
        )

        val result = classifier.classify(similar)

        assertEquals(FilterAction.QUARANTINE, result.action)
        assertEquals(FilterCategory.UNKNOWN, result.category)
        assertEquals(1f, result.confidence)
        assertTrue(result.reason?.contains("block similar") == true)
        assertEquals(0, baseClassifier.callCount)
    }

    @Test
    fun correctionRecordsDoNotIncludeRawMessageBody() {
        val body = "Synthetic campaign donate update today"
        val store = InMemoryCorrectionStore()
        store.save(
            action = CorrectionAction.BLOCK_SIMILAR,
            signals = CorrectionSignals.from(syntheticMessage(body = body))
        )

        val storedRecordText = store.records().joinToString()

        assertFalse(storedRecordText.contains(body))
        assertFalse(storedRecordText.contains("Synthetic campaign"))
        assertTrue(store.records().all { it.key.sha256.matches(Regex("[a-f0-9]{64}")) })
    }

    private fun syntheticMessage(
        address: String = "30330",
        body: String = "Synthetic campaign update: donate today."
    ): InboundMessageForClassification {
        return InboundMessageForClassification(
            address = address,
            body = body
        )
    }

    private class RecordingClassifier(
        private val result: ClassificationResult
    ) : InboundMessageClassifier {
        var callCount = 0
            private set

        override fun classify(message: InboundMessageForClassification): ClassificationResult {
            callCount += 1
            return result
        }
    }
}
