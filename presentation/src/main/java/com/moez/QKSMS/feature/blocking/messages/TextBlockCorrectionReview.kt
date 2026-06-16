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
package dev.octoshrimpy.quik.feature.blocking.messages

import dev.octoshrimpy.quik.repository.MessageRepository
import dev.octoshrimpy.quik.model.Message
import dev.octoshrimpy.quik.textblock.correction.CorrectionAction
import dev.octoshrimpy.quik.textblock.correction.CorrectionSignals
import dev.octoshrimpy.quik.textblock.correction.CorrectionStore
import javax.inject.Inject

data class TextBlockCorrectionReviewResult(
    val saved: Int,
    val skipped: Int
)

class TextBlockCorrectionReview @Inject constructor(
    private val messageRepo: MessageRepository,
    private val correctionStore: CorrectionStore
) {

    fun record(action: CorrectionAction, threadIds: Collection<Long>): TextBlockCorrectionReviewResult {
        val messages = mutableListOf<Message>()
        var skipped = 0

        threadIds.forEach { threadId ->
            val message = messageRepo.getLastIncomingMessage(threadId)
                .firstOrNull { it.hasNonWhitespaceText() }

            if (message == null) {
                skipped += 1
            } else {
                messages += message
            }
        }

        return recordMessages(
            action,
            messages,
            skipped
        )
    }

    fun recordSelectedMessages(
        action: CorrectionAction,
        messageIds: Collection<Long>
    ): TextBlockCorrectionReviewResult {
        val messages = mutableListOf<Message>()
        var skipped = 0

        messageIds.forEach { messageId ->
            val message = messageRepo.getMessage(messageId)
            if (message == null) {
                skipped += 1
            } else {
                messages += message
            }
        }

        return recordMessages(
            action,
            messages,
            skipped
        )
    }

    private fun recordMessages(
        action: CorrectionAction,
        messages: Collection<Message>,
        initialSkipped: Int = 0
    ): TextBlockCorrectionReviewResult {
        var saved = 0
        var skipped = initialSkipped

        messages.forEach { message ->
            val body = message.getText().takeIf { it.isNotBlank() }
            if (body == null) {
                skipped += 1
            } else {
                correctionStore.save(
                    action = action,
                    signals = CorrectionSignals.from(
                        address = message.address,
                        body = body
                    ),
                    createdAtMillis = System.currentTimeMillis()
                )
                saved += 1
            }
        }

        return TextBlockCorrectionReviewResult(saved = saved, skipped = skipped)
    }
}
