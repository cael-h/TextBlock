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
package dev.octoshrimpy.quik.textblock

data class InboundMessageForClassification(
    val address: String,
    val body: String,
    val timestampMillis: Long = 0L,
    val isMms: Boolean = false,
    val isFromContact: Boolean = false,
    val subscriptionId: Int = -1
)
