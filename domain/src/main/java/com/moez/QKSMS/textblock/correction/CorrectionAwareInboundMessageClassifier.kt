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

class CorrectionAwareInboundMessageClassifier(
    private val correctionStore: CorrectionStore,
    private val delegate: InboundMessageClassifier
) : InboundMessageClassifier {

    override fun classify(message: InboundMessageForClassification): ClassificationResult {
        return when (correctionStore.resolve(CorrectionSignals.from(message)).outcome) {
            CorrectionDecisionOutcome.FORCE_ALLOW -> ClassificationResult(
                action = FilterAction.ALLOW,
                category = FilterCategory.UNKNOWN,
                confidence = 1f,
                reason = "matched local correction: not spam"
            )

            CorrectionDecisionOutcome.BLOCK_SIMILAR -> ClassificationResult(
                action = FilterAction.QUARANTINE,
                category = FilterCategory.UNKNOWN,
                confidence = 1f,
                reason = "matched local correction: block similar"
            )

            CorrectionDecisionOutcome.NO_MATCH -> delegate.classify(message)
        }
    }
}
