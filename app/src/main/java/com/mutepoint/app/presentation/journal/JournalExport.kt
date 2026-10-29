package com.mutepoint.app.presentation.journal

import com.mutepoint.app.domain.model.RealityAnchorEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatJournalExport(entry: RealityAnchorEntry): String = buildString {
    appendLine("Mute Point journal export")
    appendLine(
        "Recorded: ${
            entry.timestamp
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm"))
        }",
    )
    appendLine()
    appendLine("Trigger: ${entry.triggerText}")
    append("Anchor: ${entry.realityText}")
}
