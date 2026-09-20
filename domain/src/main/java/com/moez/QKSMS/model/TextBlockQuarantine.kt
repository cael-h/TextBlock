/*
 * Copyright (C) 2026
 *
 * This file is part of TextBlock.
 */
package dev.octoshrimpy.quik.model

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class TextBlockQuarantine : RealmObject() {
    @PrimaryKey var messageId: Long = 0
    @Index var quarantinedAtMillis: Long = 0
}
