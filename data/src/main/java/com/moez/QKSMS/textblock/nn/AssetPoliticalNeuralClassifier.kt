/*
 * Copyright (C) 2026
 *
 * This file is part of TextBlock.
 */
package dev.octoshrimpy.quik.textblock.nn

import android.content.Context
import dev.octoshrimpy.quik.textblock.ClassificationResult
import dev.octoshrimpy.quik.textblock.FilterAction
import dev.octoshrimpy.quik.textblock.FilterCategory
import dev.octoshrimpy.quik.textblock.InboundMessageClassifier
import dev.octoshrimpy.quik.textblock.InboundMessageForClassification
import timber.log.Timber
import java.io.DataInputStream
import java.io.InputStream
import java.text.Normalizer
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.math.tanh

class AssetPoliticalNeuralClassifier(
    context: Context
) : InboundMessageClassifier {

    private val model by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.applicationContext.assets.open(MODEL_ASSET).use(PoliticalNeuralModel::read)
    }

    override fun classify(message: InboundMessageForClassification): ClassificationResult {
        if (message.isFromContact || message.body.isBlank()) {
            return ClassificationResult.Allow
        }

        val score = runCatching { model.score(message.body) }
            .onFailure { error -> Timber.e(error, "TextBlock neural classifier unavailable") }
            .getOrElse { return ClassificationResult.Allow }

        return if (score >= model.threshold) {
            ClassificationResult(
                action = FilterAction.QUARANTINE,
                category = FilterCategory.POLITICAL,
                confidence = score,
                reason = "on-device political neural classifier"
            )
        } else {
            ClassificationResult.Allow
        }
    }

    private companion object {
        const val MODEL_ASSET = "textblock_political_nn_v1.bin"
    }
}

internal class PoliticalNeuralModel private constructor(
    private val inputSize: Int,
    private val hiddenSize: Int,
    val threshold: Float,
    private val inputWeights: Array<FloatArray>,
    private val hiddenBias: FloatArray,
    private val outputWeights: FloatArray,
    private val outputBias: Float
) {

    fun score(body: String): Float {
        val input = vectorize(body)
        val hidden = FloatArray(hiddenSize)
        for (hiddenIndex in 0 until hiddenSize) {
            var sum = hiddenBias[hiddenIndex]
            val weights = inputWeights[hiddenIndex]
            for (inputIndex in input.indices) {
                val value = input[inputIndex]
                if (value != 0f) sum += weights[inputIndex] * value
            }
            hidden[hiddenIndex] = tanh(sum.toDouble()).toFloat()
        }

        var output = outputBias
        for (index in hidden.indices) output += hidden[index] * outputWeights[index]
        return (1.0 / (1.0 + exp(-output.toDouble()))).toFloat()
    }

    private fun vectorize(body: String): FloatArray {
        val text = Normalizer.normalize(body, Normalizer.Form.NFKC)
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(600)
        val counts = FloatArray(inputSize)
        val words = WORD.findAll(text).map { match -> match.value }.toList()
        words.forEach { word -> addFeature(counts, "w:$word") }
        words.zipWithNext().forEach { (first, second) ->
            addFeature(counts, "b:${first}_$second")
        }
        val padded = "  $text  "
        for (size in 3..5) {
            for (index in 0..padded.length - size) {
                addFeature(counts, "c:${padded.substring(index, index + size)}")
            }
        }

        var squaredNorm = 0.0
        counts.forEach { value -> squaredNorm += value * value }
        if (squaredNorm > 0.0) {
            val norm = sqrt(squaredNorm).toFloat()
            for (index in counts.indices) counts[index] /= norm
        }
        return counts
    }

    private fun addFeature(counts: FloatArray, feature: String) {
        val hash = fnv1a(feature)
        val index = hash and (inputSize - 1)
        counts[index] += if (hash < 0) -1f else 1f
    }

    private fun fnv1a(value: String): Int {
        var hash = 0x811c9dc5.toInt()
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = hash xor (byte.toInt() and 0xff)
            hash *= 0x01000193
        }
        return hash
    }

    companion object {
        private val WORD = Regex("[a-z0-9$]+")

        fun read(input: InputStream): PoliticalNeuralModel {
            DataInputStream(input.buffered()).use { data ->
                require(ByteArray(4).also(data::readFully).contentEquals("TBNN".toByteArray())) {
                    "Invalid TextBlock neural model"
                }
                require(data.readInt() == 1) { "Unsupported TextBlock neural model version" }
                val inputSize = data.readInt()
                val hiddenSize = data.readInt()
                require(inputSize > 0 && inputSize and (inputSize - 1) == 0)
                require(hiddenSize > 0)
                val threshold = data.readFloat()
                val inputWeights = Array(hiddenSize) {
                    FloatArray(inputSize) { data.readFloat() }
                }
                val hiddenBias = FloatArray(hiddenSize) { data.readFloat() }
                val outputWeights = FloatArray(hiddenSize) { data.readFloat() }
                val outputBias = data.readFloat()
                return PoliticalNeuralModel(
                    inputSize,
                    hiddenSize,
                    threshold,
                    inputWeights,
                    hiddenBias,
                    outputWeights,
                    outputBias
                )
            }
        }
    }
}
