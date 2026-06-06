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

import dev.octoshrimpy.quik.textblock.FilterAction
import dev.octoshrimpy.quik.textblock.FilterCategory
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class NoModelFallbackClassifierTest {

    @Test
    fun noModelFallbackAllowsWithUnknownCategoryAndZeroConfidence() {
        val result = NoModelFallbackClassifier.classify(
            InboundMessageForClassification(
                address = "30330",
                body = "Synthetic local classification candidate.",
                timestampMillis = 1234L,
                isMms = true,
                isFromContact = false,
                subscriptionId = 7
            )
        )

        assertEquals(FilterAction.ALLOW, result.action)
        assertEquals(FilterCategory.UNKNOWN, result.category)
        assertEquals(0f, result.confidence)
        assertEquals(NoModelFallbackClassifier.REASON, result.reason)
    }
}
