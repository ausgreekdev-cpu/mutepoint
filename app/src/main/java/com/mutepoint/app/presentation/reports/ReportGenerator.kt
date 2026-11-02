package com.mutepoint.app.presentation.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.mutepoint.app.domain.model.SessionAnalytics
import java.io.FileOutputStream

class ReportGenerator {
    fun generateReport(context: Context, sessions: List<SessionAnalytics>, uri: android.net.Uri) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 18f
        canvas.drawText("Mute Point - Interruption Report", 50f, 50f, paint)
        paint.textSize = 12f
        canvas.drawText("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", 50f, 70f, paint)

        var y = 110f
        paint.textSize = 14f
        canvas.drawText("Summary", 50f, y, paint)
        y += 20f
        paint.textSize = 11f
        val totalSessions = sessions.size
        val totalInterruptions = sessions.sumOf { it.interruptionCount }
        val totalSpeakingMs = sessions.sumOf { it.speakingTimeMs }
        canvas.drawText("Total Sessions: $totalSessions", 50f, y, paint)
        y += 15f
        canvas.drawText("Total Interruptions: $totalInterruptions", 50f, y, paint)
        y += 15f
        canvas.drawText("Total Speaking Time: ${totalSpeakingMs / 1000}s", 50f, y, paint)
        y += 30f

        paint.textSize = 14f
        canvas.drawText("Session Details", 50f, y, paint)
        y += 20f
        paint.textSize = 10f
        sessions.take(20).forEach { session ->
            canvas.drawText(
                "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(session.startedAtMs))} - " +
                    "Interruptions: ${session.interruptionCount}, Speaking: ${session.speakingTimeMs / 1000}s",
                50f,
                y,
                paint,
            )
            y += 15f
        }

        pdfDocument.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
    }
}