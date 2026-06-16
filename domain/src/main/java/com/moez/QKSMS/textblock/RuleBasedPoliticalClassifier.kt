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

import java.text.Normalizer

class RuleBasedPoliticalClassifier : InboundMessageClassifier {
    override fun classify(message: InboundMessageForClassification): ClassificationResult {
        val text = message.body.normalizedForClassification()
        if (text.isBlank()) {
            return ClassificationResult.Allow
        }

        val matches = mutableListOf<String>()
        var score = 0

        if (campaignDomains.any { text.contains(it) }) {
            score += 4
            matches += "known campaign domain"
        }

        if (campaignLinkTerms.any { text.contains(it) }) {
            score += 2
            matches += "campaign link pattern"
        }

        if (dollarAskPattern.containsMatchIn(text)) {
            score += 2
            matches += "small-dollar ask"
        }

        if (raceCodePattern.containsMatchIn(text)) {
            score += 1
            matches += "race code"
        }

        if (text.contains("end2end")) {
            score += 2
            matches += "campaign text footer"
        }

        val complianceMatches = complianceTerms.count { text.contains(it) }
        if (complianceMatches > 0) {
            score += complianceMatches.coerceAtMost(2)
            matches += "campaign compliance language"
        }

        val fundraisingMatches = fundraisingTerms.count { text.containsWordOrPhrase(it) }
        if (fundraisingMatches > 0) {
            score += fundraisingMatches.coerceAtMost(2)
            matches += "fundraising language"
        }

        val politicalMatches = politicalTerms.count { text.containsWordOrPhrase(it) }
        if (politicalMatches > 0) {
            score += politicalMatches.coerceAtMost(3)
            matches += "political language"
        }

        val localRaceMatches = localRaceTerms.count { text.containsWordOrPhrase(it) }
        if (localRaceMatches > 0) {
            score += localRaceMatches.coerceAtMost(2)
            matches += "local race language"
        }

        val petitionMatches = petitionTerms.count { text.containsWordOrPhrase(it) }
        if (petitionMatches > 0) {
            score += petitionMatches.coerceAtMost(2)
            matches += "petition language"
        }

        val urgencyMatches = urgencyTerms.count { text.containsWordOrPhrase(it) }
        if (urgencyMatches > 0) {
            score += urgencyMatches.coerceAtMost(2)
            matches += "urgency language"
        }

        if (message.address.isShortCode()) {
            score += 1
            matches += "short code sender"
        }

        if (message.isFromContact && score < 5) {
            return ClassificationResult.Allow
        }

        return when {
            score >= 5 -> ClassificationResult(
                action = FilterAction.QUARANTINE,
                category = if (fundraisingMatches > 0 || text.contains("actblue_secure")) {
                    FilterCategory.FUNDRAISING
                } else {
                    FilterCategory.POLITICAL
                },
                confidence = 0.9f,
                reason = matches.distinct().joinToString()
            )

            score >= 3 -> ClassificationResult(
                action = FilterAction.QUARANTINE,
                category = FilterCategory.POLITICAL,
                confidence = 0.7f,
                reason = matches.distinct().joinToString()
            )

            else -> ClassificationResult.Allow
        }
    }

    private fun String.normalizedForClassification(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFKC)
            .replace(Regex("\\[(image|video|audio)/[^]]+]"), " ")
            .replace(Regex("(?m)^null$"), " ")
            .lowercase()
    }

    private fun String.containsWordOrPhrase(value: String): Boolean {
        val escaped = Regex.escape(value)
        return Regex("(^|[^a-z0-9])$escaped([^a-z0-9]|$)").containsMatchIn(this)
    }

    private fun String.isShortCode(): Boolean {
        val digits = filter { it.isDigit() }
        return digits.length in 5..6 && digits.length == count { it.isDigit() }
    }

    private companion object {
        val campaignDomains = listOf(
            "actblue.com",
            "ak4m.org",
            "bldg-blue.org",
            "brooks4pa.com",
            "dasspac.org",
            "demconservationalliance.org",
            "fight-fascism.org",
            "got-pac.org",
            "jg4co.org",
            "jp4tn.org",
            "letsgv.net",
            "mm4ca.org",
            "nl4sc.com",
            "prochoicemaj.com",
            "ro4congress.org",
            "senmajority.com",
            "takeit-back.org",
            "w-2l.org",
            "winred.com",
            "secure.actblue.com",
            "secure.winred.com",
        )

        val campaignLinkTerms = listOf(
            "/actblue_secure_",
            "donate.",
            "/donate",
            "/l/"
        )

        val complianceTerms = listOf(
            "reply stop",
            "stop to end",
            "stop2end",
            "msg&data",
            "msg & data",
            "end2end",
            "paid for by",
            "not authorized by any candidate",
            "political committee"
        )

        val fundraisingTerms = listOf(
            "donate",
            "donation",
            "chip in",
            "contribute",
            "contribution",
            "match",
            "triple match",
            "pitch in",
            "rush even just",
            "fundraising",
            "deadline"
        )

        val politicalTerms = listOf(
            "ballot",
            "campaign",
            "candidate",
            "canvass",
            "congress",
            "congressional district",
            "democrat",
            "democratic primary",
            "democratic nominee",
            "democratic secretaries of state",
            "election",
            "election day",
            "endorse",
            "flip",
            "gop",
            "grassroots campaign",
            "house majority",
            "midterm",
            "pac",
            "poll",
            "primary election",
            "republican",
            "senate",
            "special election",
            "tossup",
            "vote",
            "voter",
            "voting",
            "voting rights"
        )

        val localRaceTerms = listOf(
            "county council",
            "district 1",
            "early voting",
            "dark money",
            "opponent"
        )

        val petitionTerms = listOf(
            "add your name",
            "petition",
            "sign your name",
            "signatures",
            "take action",
            "urge"
        )

        val urgencyTerms = listOf(
            "11:59 pm",
            "24 hours",
            "before the polls open",
            "deadline",
            "emergency",
            "last chance",
            "right now",
            "tomorrow"
        )

        val dollarAskPattern = Regex("\\$\\s?(1|7|15|25|50|100)\\b")
        val raceCodePattern = Regex("\\b[a-z]{2}-\\d{1,2}\\b")
    }
}
