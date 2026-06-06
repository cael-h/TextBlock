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

enum class CorrectionAction {
    NOT_SPAM,
    BLOCK_SIMILAR
}

enum class CorrectionKeyType {
    BODY_SHA256,
    TOKEN_SIGNATURE_SHA256,
    SENDER_SHA256
}

data class CorrectionKey(
    val type: CorrectionKeyType,
    val sha256: String
) {
    init {
        require(sha256.matches(sha256Pattern)) {
            "Correction keys must be lowercase SHA-256 hex."
        }
    }

    companion object {
        private val sha256Pattern = Regex("[a-f0-9]{64}")
    }
}

data class CorrectionRecord(
    val action: CorrectionAction,
    val key: CorrectionKey,
    val createdAtMillis: Long = 0L
)

enum class CorrectionDecisionOutcome {
    NO_MATCH,
    FORCE_ALLOW,
    BLOCK_SIMILAR
}

data class CorrectionDecision(
    val outcome: CorrectionDecisionOutcome,
    val matchedAction: CorrectionAction? = null,
    val matchedKey: CorrectionKey? = null
) {
    companion object {
        val NoMatch = CorrectionDecision(CorrectionDecisionOutcome.NO_MATCH)

        fun forceAllow(record: CorrectionRecord): CorrectionDecision {
            return CorrectionDecision(
                outcome = CorrectionDecisionOutcome.FORCE_ALLOW,
                matchedAction = record.action,
                matchedKey = record.key
            )
        }

        fun blockSimilar(record: CorrectionRecord): CorrectionDecision {
            return CorrectionDecision(
                outcome = CorrectionDecisionOutcome.BLOCK_SIMILAR,
                matchedAction = record.action,
                matchedKey = record.key
            )
        }
    }
}
