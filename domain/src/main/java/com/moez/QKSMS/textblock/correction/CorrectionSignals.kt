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

import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class CorrectionSignals(
    val bodyFingerprint: CorrectionKey,
    val tokenSignature: CorrectionKey,
    val senderFingerprint: CorrectionKey?
) {
    val keys: List<CorrectionKey>
        get() = listOfNotNull(bodyFingerprint, tokenSignature, senderFingerprint)

    fun keyFor(action: CorrectionAction): CorrectionKey {
        return when (action) {
            CorrectionAction.NOT_SPAM -> bodyFingerprint
            CorrectionAction.BLOCK_SIMILAR -> tokenSignature
        }
    }

    companion object {
        fun from(message: InboundMessageForClassification): CorrectionSignals {
            return from(
                address = message.address,
                body = message.body
            )
        }

        fun from(address: String, body: String): CorrectionSignals {
            val normalizedBody = normalizeBody(body)
            val normalizedAddress = normalizeAddress(address)
            return CorrectionSignals(
                bodyFingerprint = CorrectionKey(
                    type = CorrectionKeyType.BODY_SHA256,
                    sha256 = sha256("textblock:correction:body:v1:$normalizedBody")
                ),
                tokenSignature = CorrectionKey(
                    type = CorrectionKeyType.TOKEN_SIGNATURE_SHA256,
                    sha256 = sha256("textblock:correction:tokens:v1:${tokenSignature(normalizedBody)}")
                ),
                senderFingerprint = normalizedAddress.takeIf { it.isNotBlank() }?.let {
                    CorrectionKey(
                        type = CorrectionKeyType.SENDER_SHA256,
                        sha256 = sha256("textblock:correction:sender:v1:$it")
                    )
                }
            )
        }

        private fun normalizeBody(body: String): String {
            return Normalizer.normalize(body, Normalizer.Form.NFKC)
                .replace(Regex("\\[(image|video|audio)/[^]]+]"), " ")
                .replace(Regex("(?m)^\\s*null\\s*$"), " ")
                .lowercase(Locale.US)
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun normalizeAddress(address: String): String {
            return Normalizer.normalize(address, Normalizer.Form.NFKC)
                .lowercase(Locale.US)
                .trim()
        }

        private fun tokenSignature(normalizedBody: String): String {
            return tokenPattern.findAll(normalizedBody)
                .map { it.value }
                .toSet()
                .sorted()
                .joinToString(separator = " ")
        }

        private fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
        }

        private val tokenPattern = Regex("[a-z0-9]{3,}")
    }
}
