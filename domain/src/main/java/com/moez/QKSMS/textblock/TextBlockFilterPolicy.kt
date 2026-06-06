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

object TextBlockFilterPolicy {

    fun shouldClassify(
        filteringEnabled: Boolean,
        allowContacts: Boolean,
        isFromContact: Boolean
    ): Boolean {
        return filteringEnabled && !(allowContacts && isFromContact)
    }

    fun isFromContactForClassifier(
        allowContacts: Boolean,
        isFromContact: Boolean
    ): Boolean {
        return allowContacts && isFromContact
    }

    fun actionFor(
        result: ClassificationResult,
        dropMode: Boolean
    ): TextBlockFilterDecision {
        return when (result.action) {
            FilterAction.ALLOW -> TextBlockFilterDecision.ALLOW
            FilterAction.QUARANTINE,
            FilterAction.BLOCK_CONVERSATION,
            FilterAction.DROP -> when (dropMode) {
                true -> TextBlockFilterDecision.DROP
                false -> TextBlockFilterDecision.QUARANTINE
            }
        }
    }
}

enum class TextBlockFilterDecision {
    ALLOW,
    QUARANTINE,
    DROP
}
