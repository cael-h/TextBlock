/*
 * Copyright (C) 2026
 *
 * This file is part of TextBlock.
 */
package dev.octoshrimpy.quik.textblock.nn

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileInputStream

class PoliticalNeuralModelTest {

    private val model by lazy {
        FileInputStream("src/main/assets/textblock_political_nn_v1.bin")
            .use(PoliticalNeuralModel::read)
    }

    @Test
    fun catchesPreviouslyMissedPoliticalMessages() {
        assertPolitical(
            "Kyle, you've been selected for a 1-min survey. Your input directly shapes " +
                "Democratic strategy. Will you take it? https://s.alchemer.com/s3/example Stop to End"
        )
        assertPolitical(
            "Barack Obama just met with House Democrats and made an AMAZING statement. " +
                "READ MORE NOW! https://fwd-blue.com/l/example -ForwardBlue Stop2Quit"
        )
        assertPolitical(
            "Protect our freedoms. Tell Congress that Maryland families are watching. " +
                "Sign the petition now https://takeit-back.org/a"
        )
    }

    @Test
    fun allowsSensitiveOrdinaryMessages() {
        assertOrdinary("Mariah: Can you pick up oat milk on your way home?")
        assertOrdinary("Chase alert: your $358.72 external transfer was completed")
        assertOrdinary("Your pharmacy order is ready. Reply STOP to end reminders")
        assertOrdinary("Vote for the restaurant for Friday's team lunch in the office poll")
        assertOrdinary("Dentistry for Children: your appointment is tomorrow at 9:30 AM")
    }

    private fun assertPolitical(text: String) {
        val score = model.score(text)
        assertTrue("Expected political score >= ${model.threshold}, was $score", score >= model.threshold)
    }

    private fun assertOrdinary(text: String) {
        val score = model.score(text)
        assertTrue("Expected ordinary score < ${model.threshold}, was $score", score < model.threshold)
    }
}
