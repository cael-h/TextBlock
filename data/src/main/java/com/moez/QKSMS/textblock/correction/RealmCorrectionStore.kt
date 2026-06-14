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

import dev.octoshrimpy.quik.model.TextBlockCorrection
import io.realm.Realm
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealmCorrectionStore @Inject constructor() : CorrectionStore {

    override fun save(
        action: CorrectionAction,
        signals: CorrectionSignals,
        createdAtMillis: Long
    ): CorrectionRecord {
        val record = CorrectionRecord(
            action = action,
            key = signals.keyFor(action),
            createdAtMillis = createdAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        )

        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.copyToRealmOrUpdate(TextBlockCorrection().apply {
                    id = record.key.id
                    this.action = record.action.name
                    keyType = record.key.type.name
                    keySha256 = record.key.sha256
                    this.createdAtMillis = record.createdAtMillis
                })
            }
        }

        return record
    }

    override fun resolve(signals: CorrectionSignals): CorrectionDecision {
        Realm.getDefaultInstance().use { realm ->
            findRecord(realm, CorrectionAction.NOT_SPAM, signals.keyFor(CorrectionAction.NOT_SPAM))
                ?.let { return CorrectionDecision.forceAllow(it) }

            findRecord(realm, CorrectionAction.BLOCK_SIMILAR, signals.keyFor(CorrectionAction.BLOCK_SIMILAR))
                ?.let { return CorrectionDecision.blockSimilar(it) }
        }

        return CorrectionDecision.NoMatch
    }

    private fun findRecord(
        realm: Realm,
        action: CorrectionAction,
        key: CorrectionKey
    ): CorrectionRecord? {
        return realm.where(TextBlockCorrection::class.java)
            .equalTo("id", key.id)
            .equalTo("action", action.name)
            .findFirst()
            ?.toCorrectionRecord()
    }

    private fun TextBlockCorrection.toCorrectionRecord(): CorrectionRecord? {
        val action = runCatching { CorrectionAction.valueOf(action) }.getOrNull()
            ?: return null
        val keyType = runCatching { CorrectionKeyType.valueOf(keyType) }.getOrNull()
            ?: return null

        return CorrectionRecord(
            action = action,
            key = CorrectionKey(
                type = keyType,
                sha256 = keySha256
            ),
            createdAtMillis = createdAtMillis
        )
    }

    private val CorrectionKey.id: String
        get() = "${type.name}:$sha256"
}
