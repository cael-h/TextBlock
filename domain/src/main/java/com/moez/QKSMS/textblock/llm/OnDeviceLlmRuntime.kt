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

interface OnDeviceLlmRuntime {
    fun classify(request: LlmClassificationRequest): OnDeviceLlmRuntimeResult
}

sealed class OnDeviceLlmRuntimeResult {
    data class Classified(
        val result: ClassificationResult
    ) : OnDeviceLlmRuntimeResult()

    data class ModelUnavailable(
        val reason: String? = null
    ) : OnDeviceLlmRuntimeResult()

    data class Deferred(
        val reason: String? = null
    ) : OnDeviceLlmRuntimeResult()
}
