package com.mutepoint.app.presentation.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.mutepoint.app.domain.model.SessionAnalytics
import com.mutepoint.app.domain.usecase.PatternAnalyzer
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {

    fun generateSessionReport(
        context: Context,
        sessions: List<SessionAnalytics>,
        title: String = "Mute Point Session Report",
    ): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { textSize = 12f }
            val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }

            var y = 40f
            canvas.drawText(title, 40f, y, titlePaint)
            y += 24f
            canvas.drawText(
                "Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}",
                40f, y, paint,
            )
            y += 30f

            val insights = PatternAnalyzer().analyze(sessions)
            canvas.drawText("Summary", 40f, y, titlePaint.apply { textSize = 14f })
            y += 20f
            paint.textSize = 11f
            val summaryLines = wrapText(insights.summary, 75)
            for (line in summaryLines) {
                canvas.drawText(line, 40f, y, paint)
                y += 16f
            }
            y += 10f
            canvas.drawText("Sessions: ${insights.totalSessions}", 40f, y, paint); y += 16f
            canvas.drawText("Total interruptions: ${insights.totalInterruptions}", 40f, y, paint); y += 16f
            canvas.drawText("Avg speaking: ${insights.averageSpeakingPercent.toInt()}%", 40f, y, paint); y += 16f
            canvas.drawText("Trend: ${insights.trend}", 40f, y, paint); y += 24f

            canvas.drawText("Recent Sessions", 40f, y, Paint().apply { textSize = 14f; isFakeBoldText = true })
            y += 20f
            paint.textSize = 10f
            val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            for (session in sessions.take(20)) {
                if (y > 800f) break
                val date = dateFormat.format(Date(session.startedAtMs))
                val line = "$date  |  Interruptions: ${session.interruptionCount}  |  Speaking: ${session.speakingPercent.toInt()}%"
                canvas.drawText(line, 40f, y, paint)
                y += 14f
            }

            document.finishPage(page)
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, "session_report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (current.length + word.length + 1 > maxChars && current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(word)
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
