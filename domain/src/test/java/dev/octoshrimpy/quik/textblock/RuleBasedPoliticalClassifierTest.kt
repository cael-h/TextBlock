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
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedPoliticalClassifierTest {

    private val classifier = RuleBasedPoliticalClassifier()

    @Test
    fun knownPoliticalDonationSpamQuarantinesAsFundraising() {
        val result = classify(
            address = "30330",
            body = """
                Please donate $25 before 11:59 PM to help our senate campaign.
                secure.actblue.com/donate/synthetic-campaign
                Reply STOP to end.
            """.trimIndent()
        )

        assertQuarantined(result, FilterCategory.FUNDRAISING)
        assertReasonIncludes(result, "known campaign domain")
        assertReasonIncludes(result, "fundraising language")
    }

    @Test
    fun politicalPetitionSpamQuarantines() {
        val result = classify(
            body = """
                Add your name to the petition to protect voting rights.
                Take action right now: https://example.org/l/synthetic-petition
                Reply STOP.
            """.trimIndent()
        )

        assertQuarantined(result, FilterCategory.POLITICAL)
        assertReasonIncludes(result, "petition language")
        assertReasonIncludes(result, "political language")
    }

    @Test
    fun stylizedUnicodePoliticalSpamNormalizesAndQuarantines() {
        val result = classify(
            body = """
                ＲＥＰＬＹ ＳＴＯＰ.
                ＤＯＮＡＴＥ to the ＣＡＭＰＡＩＧＮ: https://example.org/l/synthetic
            """.trimIndent()
        )

        assertQuarantined(result, FilterCategory.FUNDRAISING)
        assertReasonIncludes(result, "campaign link pattern")
        assertReasonIncludes(result, "campaign compliance language")
    }

    @Test
    fun mmsExportNoiseDoesNotPreventDetection() {
        val result = classify(
            address = "40404",
            isMms = true,
            body = """
                [image/jpeg]
                null
                Chip in $15 before the polls open for this congressional campaign.
                Reply STOP: https://example.org/l/synthetic-mms
            """.trimIndent()
        )

        assertQuarantined(result, FilterCategory.FUNDRAISING)
        assertReasonIncludes(result, "fundraising language")
        assertReasonIncludes(result, "political language")
    }

    @Test
    fun benignPoliticalConversationFromContactIsAllowedWhenScoreIsLow() {
        val result = classify(
            address = "+15551234567",
            isFromContact = true,
            body = "Can you vote in the neighborhood poll tomorrow?"
        )

        assertAllowed(result)
    }

    @Test
    fun normalMessagesWithMoneyAmountsAreAllowed() {
        val result = classify(
            body = "Can you send $25 for dinner tonight?"
        )

        assertAllowed(result)
    }

    @Test
    fun normalMessagesWithSlashLLinksAreAllowedUnlessCombinedWithCampaignSignals() {
        val normalLink = classify(
            body = "The photo album is at https://example.org/l/family-weekend"
        )
        val campaignLink = classify(
            body = "Campaign update: https://example.org/l/synthetic-campaign"
        )

        assertAllowed(normalLink)
        assertQuarantined(campaignLink, FilterCategory.POLITICAL)
    }

    private fun classify(
        address: String = "+15550001000",
        body: String,
        isMms: Boolean = false,
        isFromContact: Boolean = false
    ): ClassificationResult {
        return classifier.classify(
            InboundMessageForClassification(
                address = address,
                body = body,
                isMms = isMms,
                isFromContact = isFromContact
            )
        )
    }

    private fun assertQuarantined(
        result: ClassificationResult,
        expectedCategory: FilterCategory
    ) {
        assertEquals(FilterAction.QUARANTINE, result.action)
        assertEquals(expectedCategory, result.category)
    }

    private fun assertAllowed(result: ClassificationResult) {
        assertEquals(ClassificationResult.Allow, result)
    }

    private fun assertReasonIncludes(result: ClassificationResult, expected: String) {
        assertTrue(
            "Expected reason to include '$expected' but was '${result.reason}'",
            result.reason?.contains(expected) == true
        )
    }
}
