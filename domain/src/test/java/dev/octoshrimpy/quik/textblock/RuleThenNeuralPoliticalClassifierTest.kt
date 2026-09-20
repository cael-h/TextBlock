/*
 * Copyright (C) 2026
 *
 * This file is part of TextBlock.
 */
package dev.octoshrimpy.quik.textblock

import org.junit.Assert.assertEquals
import org.junit.Test

class RuleThenNeuralPoliticalClassifierTest {

    @Test
    fun ruleDecisionWinsWithoutCallingNeuralClassifier() {
        var neuralCalls = 0
        val classifier = RuleThenNeuralPoliticalClassifier(
            ruleClassifier = classifierReturning(political),
            neuralClassifier = object : InboundMessageClassifier {
                override fun classify(message: InboundMessageForClassification): ClassificationResult {
                    neuralCalls++
                    return ClassificationResult.Allow
                }
            }
        )

        assertEquals(political, classifier.classify(message))
        assertEquals(0, neuralCalls)
    }

    @Test
    fun neuralClassifierHandlesMessagesAllowedByRules() {
        val classifier = RuleThenNeuralPoliticalClassifier(
            ruleClassifier = classifierReturning(ClassificationResult.Allow),
            neuralClassifier = classifierReturning(political)
        )

        assertEquals(political, classifier.classify(message))
    }

    private val message = InboundMessageForClassification(
        address = "+12025550123",
        body = "message",
        timestampMillis = 1L,
        isMms = false,
        isFromContact = false,
        subscriptionId = 1
    )

    private val political = ClassificationResult(
        action = FilterAction.QUARANTINE,
        category = FilterCategory.POLITICAL,
        confidence = 0.95f,
        reason = "test"
    )

    private fun classifierReturning(result: ClassificationResult) =
        object : InboundMessageClassifier {
            override fun classify(message: InboundMessageForClassification): ClassificationResult = result
        }
}
