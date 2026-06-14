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
package dev.octoshrimpy.quik.feature.settings

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.databinding.TextBlockCleanupDialogBinding
import dev.octoshrimpy.quik.textblock.cleanup.TextBlockCleanupAction

class TextBlockCleanupDialog(
    context: Activity,
    private val listener: (TextBlockCleanupDialogRequest) -> Unit
) : AlertDialog(context) {

    private val layout = TextBlockCleanupDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setTitle(R.string.settings_textblock_cleanup_dialog_title)
        setView(layout.root)
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.button_cancel)) { _, _ -> }
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.settings_textblock_cleanup_run)) { _, _ ->
            listener(
                TextBlockCleanupDialogRequest(
                    action = when (layout.deleteMatches.isChecked) {
                        true -> TextBlockCleanupAction.DELETE
                        false -> TextBlockCleanupAction.QUARANTINE
                    },
                    lookbackDaysText = layout.lookbackDays.text?.toString().orEmpty(),
                    sinceDateText = layout.sinceDate.text?.toString().orEmpty()
                )
            )
        }
    }

    fun reset(): TextBlockCleanupDialog {
        layout.quarantineMatches.isChecked = true
        layout.lookbackDays.setText("30")
        layout.sinceDate.text = null
        return this
    }
}

data class TextBlockCleanupDialogRequest(
    val action: TextBlockCleanupAction,
    val lookbackDaysText: String,
    val sinceDateText: String
)
