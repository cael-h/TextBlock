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

object TextBlockReceivePolicy {

    fun evaluate(
        filteringEnabled: Boolean,
        allowContacts: Boolean,
        isFromContact: Boolean,
        dropMode: Boolean,
        classify: (isFromContactForClassifier: Boolean) -> ClassificationResult
    ): TextBlockReceiveDecision {
        if (!TextBlockFilterPolicy.shouldClassify(filteringEnabled, allowContacts, isFromContact)) {
            return TextBlockReceiveDecision.Allow
        }

        val classificationResult = classify(
            TextBlockFilterPolicy.isFromContactForClassifier(allowContacts, isFromContact)
        )

        return when (TextBlockFilterPolicy.actionFor(classificationResult, dropMode)) {
            TextBlockFilterDecision.ALLOW -> TextBlockReceiveDecision.Allow
            TextBlockFilterDecision.QUARANTINE -> TextBlockReceiveDecision(
                effect = TextBlockReceiveEffect.QUARANTINE,
                classificationResult = classificationResult
            )
            TextBlockFilterDecision.DROP -> TextBlockReceiveDecision(
                effect = TextBlockReceiveEffect.DROP,
                classificationResult = classificationResult
            )
        }
    }

    fun mmsPostPersistencePlan(
        decision: TextBlockReceiveDecision
    ): TextBlockMmsPostPersistencePlan {
        return TextBlockMmsPostPersistencePlan(
            shouldNotifyUser = !decision.suppressesNotification,
            shouldSendAcknowledgeInd = true,
            shouldSendNotifyRespInd = true
        )
    }
}

data class TextBlockReceiveDecision(
    val effect: TextBlockReceiveEffect,
    val classificationResult: ClassificationResult? = null
) {
    val suppressesNotification: Boolean
        get() = effect != TextBlockReceiveEffect.ALLOW

    fun requireClassificationResult(): ClassificationResult {
        return requireNotNull(classificationResult) {
            "TextBlock receive suppression requires a classification result"
        }
    }

    companion object {
        val Allow = TextBlockReceiveDecision(TextBlockReceiveEffect.ALLOW)
    }
}

enum class TextBlockReceiveEffect {
    ALLOW,
    QUARANTINE,
    DROP
}

data class TextBlockMmsPostPersistencePlan(
    val shouldNotifyUser: Boolean,
    val shouldSendAcknowledgeInd: Boolean,
    val shouldSendNotifyRespInd: Boolean
) {
    companion object {
        val NotifyUser = TextBlockMmsPostPersistencePlan(
            shouldNotifyUser = true,
            shouldSendAcknowledgeInd = true,
            shouldSendNotifyRespInd = true
        )
    }
}
