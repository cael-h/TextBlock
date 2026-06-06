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
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class OnDeviceLlmInboundMessageClassifierTest {

    @Test
    fun defaultAdapterAllowsWhenNoModelIsConfigured() {
        val result = OnDeviceLlmInboundMessageClassifier().classify(
            message(body = "Synthetic urgent campaign-looking candidate text.")
        )

        assertEquals(FilterAction.ALLOW, result.action)
        assertEquals(FilterCategory.UNKNOWN, result.category)
        assertEquals(0f, result.confidence)
        assertEquals(NoModelFallbackClassifier.REASON, result.reason)
    }

    @Test
    fun deferredRuntimeResultUsesSafeFallback() {
        val classifier = OnDeviceLlmInboundMessageClassifier(
            runtime = StaticRuntime(OnDeviceLlmRuntimeResult.Deferred("runtime warming"))
        )

        val result = classifier.classify(message(body = "Synthetic deferred candidate text."))

        assertEquals(FilterAction.ALLOW, result.action)
        assertEquals(FilterCategory.UNKNOWN, result.category)
        assertEquals(0f, result.confidence)
        assertEquals(NoModelFallbackClassifier.REASON, result.reason)
    }

    @Test
    fun classifiedRuntimeResultPassesThroughUnchanged() {
        val modelResult = ClassificationResult(
            action = FilterAction.QUARANTINE,
            category = FilterCategory.POLITICAL,
            confidence = 0.82f,
            reason = "synthetic model result"
        )
        val classifier = OnDeviceLlmInboundMessageClassifier(
            runtime = StaticRuntime(OnDeviceLlmRuntimeResult.Classified(modelResult))
        )

        val result = classifier.classify(message(body = "Synthetic classified candidate text."))

        assertEquals(modelResult, result)
    }

    @Test
    fun runtimeReceivesInboundMessageMetadata() {
        val runtime = CapturingRuntime()
        val classifier = OnDeviceLlmInboundMessageClassifier(runtime = runtime)

        classifier.classify(
            message(
                address = "+15551234567",
                body = "Synthetic metadata candidate text.",
                timestampMillis = 99L,
                isMms = true,
                isFromContact = true,
                subscriptionId = 3
            )
        )

        assertEquals("+15551234567", runtime.request.address)
        assertEquals("Synthetic metadata candidate text.", runtime.request.body)
        assertEquals(99L, runtime.request.timestampMillis)
        assertEquals(true, runtime.request.isMms)
        assertEquals(true, runtime.request.isFromContact)
        assertEquals(3, runtime.request.subscriptionId)
    }

    private fun message(
        address: String = "30330",
        body: String,
        timestampMillis: Long = 0L,
        isMms: Boolean = false,
        isFromContact: Boolean = false,
        subscriptionId: Int = -1
    ): InboundMessageForClassification {
        return InboundMessageForClassification(
            address = address,
            body = body,
            timestampMillis = timestampMillis,
            isMms = isMms,
            isFromContact = isFromContact,
            subscriptionId = subscriptionId
        )
    }

    private class StaticRuntime(
        private val result: OnDeviceLlmRuntimeResult
    ) : OnDeviceLlmRuntime {
        override fun classify(request: LlmClassificationRequest): OnDeviceLlmRuntimeResult {
            return result
        }
    }

    private class CapturingRuntime : OnDeviceLlmRuntime {
        lateinit var request: LlmClassificationRequest

        override fun classify(request: LlmClassificationRequest): OnDeviceLlmRuntimeResult {
            this.request = request
            return OnDeviceLlmRuntimeResult.Classified(ClassificationResult.Allow)
        }
    }
}
