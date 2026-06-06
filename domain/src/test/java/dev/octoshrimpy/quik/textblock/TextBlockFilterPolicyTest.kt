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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextBlockFilterPolicyTest {

    @Test
    fun disabledFilteringSkipsClassification() {
        assertFalse(
            TextBlockFilterPolicy.shouldClassify(
                filteringEnabled = false,
                allowContacts = false,
                isFromContact = false
            )
        )
    }

    @Test
    fun allowContactsSkipsClassificationForContacts() {
        assertFalse(
            TextBlockFilterPolicy.shouldClassify(
                filteringEnabled = true,
                allowContacts = true,
                isFromContact = true
            )
        )
    }

    @Test
    fun disallowContactsClassifiesContactsAsNonContacts() {
        assertTrue(
            TextBlockFilterPolicy.shouldClassify(
                filteringEnabled = true,
                allowContacts = false,
                isFromContact = true
            )
        )
        assertFalse(
            TextBlockFilterPolicy.isFromContactForClassifier(
                allowContacts = false,
                isFromContact = true
            )
        )
    }

    @Test
    fun quarantineModeQuarantinesSuppressingResults() {
        assertEquals(
            TextBlockFilterDecision.QUARANTINE,
            TextBlockFilterPolicy.actionFor(quarantineResult, dropMode = false)
        )
    }

    @Test
    fun dropModeDropsSuppressingResults() {
        assertEquals(
            TextBlockFilterDecision.DROP,
            TextBlockFilterPolicy.actionFor(quarantineResult, dropMode = true)
        )
    }

    @Test
    fun allowResultAlwaysAllows() {
        assertEquals(
            TextBlockFilterDecision.ALLOW,
            TextBlockFilterPolicy.actionFor(ClassificationResult.Allow, dropMode = true)
        )
    }

    private val quarantineResult = ClassificationResult(
        action = FilterAction.QUARANTINE,
        category = FilterCategory.POLITICAL,
        confidence = 0.9f
    )
}
