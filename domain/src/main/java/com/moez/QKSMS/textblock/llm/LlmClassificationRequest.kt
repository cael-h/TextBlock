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

import dev.octoshrimpy.quik.textblock.InboundMessageForClassification

data class LlmClassificationRequest(
    val address: String,
    val body: String,
    val timestampMillis: Long,
    val isMms: Boolean,
    val isFromContact: Boolean,
    val subscriptionId: Int
) {
    companion object {
        fun from(message: InboundMessageForClassification): LlmClassificationRequest {
            return LlmClassificationRequest(
                address = message.address,
                body = message.body,
                timestampMillis = message.timestampMillis,
                isMms = message.isMms,
                isFromContact = message.isFromContact,
                subscriptionId = message.subscriptionId
            )
        }
    }
}
