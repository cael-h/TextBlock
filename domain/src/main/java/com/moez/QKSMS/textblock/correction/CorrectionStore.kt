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

interface CorrectionStore {
    fun save(
        action: CorrectionAction,
        signals: CorrectionSignals,
        createdAtMillis: Long = 0L
    ): CorrectionRecord

    fun resolve(signals: CorrectionSignals): CorrectionDecision
}

class InMemoryCorrectionStore : CorrectionStore {
    private val recordsByKey = linkedMapOf<CorrectionKey, CorrectionRecord>()

    override fun save(
        action: CorrectionAction,
        signals: CorrectionSignals,
        createdAtMillis: Long
    ): CorrectionRecord {
        val record = CorrectionRecord(
            action = action,
            key = signals.keyFor(action),
            createdAtMillis = createdAtMillis
        )
        recordsByKey[record.key] = record
        return record
    }

    override fun resolve(signals: CorrectionSignals): CorrectionDecision {
        val notSpamRecord = recordsByKey[signals.keyFor(CorrectionAction.NOT_SPAM)]
            ?.takeIf { it.action == CorrectionAction.NOT_SPAM }
        if (notSpamRecord != null) {
            return CorrectionDecision.forceAllow(notSpamRecord)
        }

        val blockSimilarRecord = recordsByKey[signals.keyFor(CorrectionAction.BLOCK_SIMILAR)]
            ?.takeIf { it.action == CorrectionAction.BLOCK_SIMILAR }
        if (blockSimilarRecord != null) {
            return CorrectionDecision.blockSimilar(blockSimilarRecord)
        }

        return CorrectionDecision.NoMatch
    }

    fun records(): List<CorrectionRecord> {
        return recordsByKey.values.toList()
    }
}
