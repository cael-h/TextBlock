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
package dev.octoshrimpy.quik.model

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class TextBlockCorrection : RealmObject() {
    @PrimaryKey var id: String = ""
    @Index var action: String = ""
    @Index var keyType: String = ""
    @Index var keySha256: String = ""
    var createdAtMillis: Long = 0L
}
