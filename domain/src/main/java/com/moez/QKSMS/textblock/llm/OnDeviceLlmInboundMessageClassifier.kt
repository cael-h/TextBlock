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
import dev.octoshrimpy.quik.textblock.InboundMessageClassifier
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification

class OnDeviceLlmInboundMessageClassifier(
    private val runtime: OnDeviceLlmRuntime = NoModelOnDeviceLlmRuntime,
    private val noModelFallback: InboundMessageClassifier = NoModelFallbackClassifier
) : InboundMessageClassifier {

    override fun classify(message: InboundMessageForClassification): ClassificationResult {
        return when (val result = runtime.classify(LlmClassificationRequest.from(message))) {
            is OnDeviceLlmRuntimeResult.Classified -> result.result
            is OnDeviceLlmRuntimeResult.ModelUnavailable -> noModelFallback.classify(message)
            is OnDeviceLlmRuntimeResult.Deferred -> noModelFallback.classify(message)
        }
    }
}
