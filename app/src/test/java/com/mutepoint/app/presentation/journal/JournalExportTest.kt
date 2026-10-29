package com.mutepoint.app.presentation.journal

import com.mutepoint.app.domain.model.RealityAnchorEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalExportTest {

    private val instant = Instant.ofEpochMilli(1_700_000_000_000L)

    private fun entry(
        trigger: String = "They cut me off mid-sentence",
        reality: String = "I was speaking for 40 seconds when they interrupted.",
    ) = RealityAnchorEntry(
        id = 1L,
        timestamp = instant,
        triggerText = trigger,
        realityText = reality,
    )

    @Test
    fun `export contains header and recorded timestamp`() {
        val text = formatJournalExport(entry())
        assertTrue(text.startsWith("Mute Point journal export"))
        val expectedDate = instant
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm"))
        assertTrue(text.contains("Recorded: $expectedDate"))
    }

    @Test
    fun `export contains trigger and anchor lines`() {
        val text = formatJournalExport(entry())
        assertTrue(text.contains("Trigger: They cut me off mid-sentence"))
        assertTrue(
            text.contains(
                "Anchor: I was speaking for 40 seconds when they interrupted.",
            ),
        )
    }

    @Test
    fun `export preserves multi-line content`() {
        val text = formatJournalExport(
            entry(trigger = "Line one\nLine two", reality = "Anchor text"),
        )
        assertTrue(text.contains("Trigger: Line one\nLine two"))
        assertTrue(text.contains("Anchor: Anchor text"))
    }
}
