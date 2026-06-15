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

data class ClassificationResult(
    val action: FilterAction,
    val category: FilterCategory,
    val confidence: Float,
    val reason: String? = null
) {
    companion object {
        val Allow = ClassificationResult(
            action = FilterAction.ALLOW,
            category = FilterCategory.UNKNOWN,
            confidence = 0f
        )
    }
}

enum class FilterAction {
    ALLOW,
    QUARANTINE,
    BLOCK_CONVERSATION,
    DROP
}

enum class FilterCategory {
    POLITICAL,
    FUNDRAISING,
    SCAM,
    UNKNOWN
}

fun ClassificationResult.toTextBlockBlockReason(): String {
    return buildString {
        append("TextBlock ")
        append(category.name)
        append(" confidence=")
        append(confidence)
        reason?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
    }
}
