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
package dev.octoshrimpy.quik.textblock.llm

import dev.octoshrimpy.quik.textblock.ClassificationResult
import dev.octoshrimpy.quik.textblock.FilterAction
import dev.octoshrimpy.quik.textblock.FilterCategory
import dev.octoshrimpy.quik.textblock.InboundMessageClassifier
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification

object NoModelFallbackClassifier : InboundMessageClassifier {
    const val REASON = "on-device LLM classification unavailable"

    override fun classify(message: InboundMessageForClassification): ClassificationResult {
        return ClassificationResult(
            action = FilterAction.ALLOW,
            category = FilterCategory.UNKNOWN,
            confidence = 0f,
            reason = REASON
        )
    }
}
