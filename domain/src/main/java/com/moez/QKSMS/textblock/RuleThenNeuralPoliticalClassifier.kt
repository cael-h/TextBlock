/*
 * Copyright (C) 2026
 *
 * This file is part of TextBlock.
 */
package dev.octoshrimpy.quik.textblock

class RuleThenNeuralPoliticalClassifier(
    private val ruleClassifier: InboundMessageClassifier,
    private val neuralClassifier: InboundMessageClassifier
) : InboundMessageClassifier {

    override fun classify(message: InboundMessageForClassification): ClassificationResult {
        val ruleResult = ruleClassifier.classify(message)
        return if (ruleResult.action == FilterAction.ALLOW) {
            neuralClassifier.classify(message)
        } else {
            ruleResult
        }
    }
}
