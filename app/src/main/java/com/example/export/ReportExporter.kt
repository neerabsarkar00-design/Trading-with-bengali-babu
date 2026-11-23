package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.TradeEntity
import com.example.domain.DashboardMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.US)

    fun buildCsvContent(
        reportTitle: String,
        scopeLabel: String,
        periodLabel: String,
        trades: List<TradeEntity>,
        metrics: DashboardMetrics
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel detects UTF-8 properly
        sb.append('\uFEFF')
        sb.appendLine("TRADING WITH BANGALI BABU - PERFORMANCE REPORT")
        sb.appendLine("Report,${csvCell(reportTitle)}")
        sb.appendLine("Scope,${csvCell(scopeLabel)}")
        sb.appendLine("Period,${csvCell(periodLabel)}")
        sb.appendLine("Generated,${csvCell(displayDateFormatter.format(Date()))}")
        sb.appendLine()
        sb.appendLine("SUMMARY METRICS")
        sb.appendLine("Total Trades,${metrics.totalTrades}")
        sb.appendLine("Winning Trades,${metrics.winningTrades}")
        sb.appendLine("Losing Trades,${metrics.losingTrades}")
        sb.appendLine("Break Even Trades,${metrics.breakEvenTrades}")
        sb.appendLine("Win Rate (%),${String.format(Locale.US, "%.2f", metrics.winRatePercent)}")
        sb.appendLine("Loss Rate (%),${String.format(Locale.US, "%.2f", metrics.lossRatePercent)}")
        sb.appendLine("Total Profit,${String.format(Locale.US, "%.2f", metrics.totalProfit)}")
        sb.appendLine("Total Loss,${String.format(Locale.US, "%.2f", metrics.totalLoss)}")
        sb.appendLine("Net Profit/Loss,${String.format(Locale.US, "%.2f", metrics.netProfitLoss)}")
        sb.appendLine("Profit Factor,${if (metrics.isInfiniteProfitFactor) "Infinite" else String.format(Locale.US, "%.2f", metrics.profitFactor)}")
        sb.appendLine("Average R:R,${String.format(Locale.US, "%.2f", metrics.averageRiskReward)}")
        sb.appendLine()
        sb.appendLine(
            "Trade ID,Trader Name,Date,Instrument,Direction,Timeframe,Entry Price,Exit Price," +
                "Stop Loss,Take Profit,Lot Size,Risk Amount,Actual P&L,Result,Strategy," +
                "Entry Reason,Exit Reason,Mistakes,Lessons Learned,Additional Notes,Admin Feedback"
        )
        for (t in trades) {
            sb.appendLine(
                listOf(
                    t.tradeCode,
                    t.userName,
                    dateFormatter.format(Date(t.tradeDateMillis)),
                    t.instrument,
                    t.direction,
                    t.timeframe,
                    t.entryPrice.toString(),
                    t.exitPrice.toString(),
                    t.stopLoss.toString(),
                    t.takeProfit.toString(),
                    t.lotSize.toString(),
                    String.format(Locale.US, "%.2f", t.riskAmount),
                    String.format(Locale.US, "%.2f", t.profitLoss),
                    t.result,
                    t.strategy,
                    t.entryReason,
                    t.exitReason,
                    t.mistakes,
                    t.lessonsLearned,
                    t.notes,
                    t.adminFeedback
                ).joinToString(",") { csvCell(it) }
            )
        }
        return sb.toString()
    }

    suspend fun writePdfReport(
        context: Context,
        outputStream: OutputStream,
        customLogoPath: String?,
        reportTitle: String,
        scopeLabel: String,
        periodLabel: String,
        trades: List<TradeEntity>,
        metrics: DashboardMetrics
    ) = withContext(Dispatchers.IO) {
        val pageWidth = 595 // A4 points at 72 dpi
        val pageHeight = 842
        val document = PdfDocument()

        val navyPaint = Paint().apply { color = Color.rgb(11, 19, 43) }
        val goldPaint = Paint().apply { color = Color.rgb(212, 175, 55) }
        val whiteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val goldSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(243, 211, 122)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(11, 19, 43)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(11, 19, 43)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val profitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val lossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 38, 38)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cardBgPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        val logoBitmap = loadBrandBitmap(context, customLogoPath)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(c: Canvas) {
            c.drawRect(0f, 0f, pageWidth.toFloat(), 86f, navyPaint)
            c.drawRect(0f, 83f, pageWidth.toFloat(), 86f, goldPaint)
            if (logoBitmap != null) {
                val dest = RectF(24f, 15f, 78f, 69f)
                c.drawBitmap(logoBitmap, null, dest, null)
            }
            val titleX = if (logoBitmap != null) 90f else 24f
            c.drawText("TRADING WITH BANGALI BABU", titleX, 34f, whiteTitlePaint)
            c.drawText("$reportTitle • $scopeLabel", titleX, 52f, goldSubPaint)
            c.drawText("Period: $periodLabel   |   Generated: ${displayDateFormatter.format(Date())}", titleX, 68f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                textSize = 9f
            })
        }

        drawHeader(canvas)

        var y = 108f
        canvas.drawText("PERFORMANCE SUMMARY", 24f, y, sectionPaint)
        y += 10f

        // KPI Grid (3 columns x 2 rows)
        val kpis = listOf(
            "Total Trades" to "${metrics.totalTrades} (${metrics.winningTrades}W / ${metrics.losingTrades}L)",
            "Win Rate" to String.format(Locale.US, "%.1f%%", metrics.winRatePercent),
            "Net P&L" to formatSignedMoney(metrics.netProfitLoss),
            "Profit Factor" to if (metrics.isInfiniteProfitFactor) "Infinite" else String.format(Locale.US, "%.2f", metrics.profitFactor),
            "Avg Win / Loss" to "${formatSignedMoney(metrics.averageWin)} / -${String.format(Locale.US, "$%.2f", metrics.averageLoss)}",
            "Best / Worst Day" to "${metrics.bestTradingDayLabel} (${formatSignedMoney(metrics.bestTradingDayPnl)})"
        )
        val colWidth = (pageWidth - 48f - 16f) / 3f
        for (row in 0 until 2) {
            for (col in 0 until 3) {
                val idx = row * 3 + col
                val left = 24f + col * (colWidth + 8f)
                val top = y + row * 48f
                val rect = RectF(left, top, left + colWidth, top + 40f)
                canvas.drawRoundRect(rect, 6f, 6f, cardBgPaint)
                canvas.drawText(kpis[idx].first, left + 8f, top + 14f, Paint(bodyPaint).apply { textSize = 8f })
                canvas.drawText(kpis[idx].second, left + 8f, top + 30f, boldBodyPaint)
            }
        }

        y += 116f
        canvas.drawText("TRADE LOG (${trades.size} RECORDS)", 24f, y, sectionPaint)
        y += 12f

        fun drawTableHeader(c: Canvas, topY: Float): Float {
            c.drawRect(24f, topY, pageWidth - 24f, topY + 20f, navyPaint)
            val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            c.drawText("Date", 30f, topY + 13f, hPaint)
            c.drawText("Trader", 92f, topY + 13f, hPaint)
            c.drawText("Pair / TF", 180f, topY + 13f, hPaint)
            c.drawText("Side", 252f, topY + 13f, hPaint)
            c.drawText("Entry -> Exit", 295f, topY + 13f, hPaint)
            c.drawText("Strategy", 392f, topY + 13f, hPaint)
            c.drawText("P&L", 500f, topY + 13f, hPaint)
            return topY + 24f
        }

        y = drawTableHeader(canvas, y)

        for (t in trades) {
            if (y > pageHeight - 50f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                y = drawTableHeader(canvas, 106f)
            }

            canvas.drawText(dateFormatter.format(Date(t.tradeDateMillis)), 30f, y + 10f, bodyPaint)
            canvas.drawText(t.userName.take(14), 92f, y + 10f, bodyPaint)
            canvas.drawText("${t.instrument.take(9)} ${t.timeframe}", 180f, y + 10f, boldBodyPaint)
            canvas.drawText(t.direction, 252f, y + 10f, bodyPaint)
            canvas.drawText("${t.entryPrice} -> ${t.exitPrice}".take(17), 295f, y + 10f, bodyPaint)
            canvas.drawText(t.strategy.take(16), 392f, y + 10f, bodyPaint)
            val pnlPaint = if (t.profitLoss > 0) profitPaint else if (t.profitLoss < 0) lossPaint else bodyPaint
            canvas.drawText(formatSignedMoney(t.profitLoss), 500f, y + 10f, pnlPaint)

            y += 16f
            canvas.drawLine(24f, y, pageWidth - 24f, y, linePaint)
            y += 4f
        }

        document.finishPage(page)
        outputStream.use { document.writeTo(it) }
        document.close()
        logoBitmap?.recycle()
    }

    suspend fun createShareFileUri(
        context: Context,
        fileName: String,
        writer: suspend (OutputStream) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { out ->
                writer(out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    fun launchShareIntent(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun loadBrandBitmap(context: Context, customLogoPath: String?): Bitmap? {
        return runCatching {
            if (!customLogoPath.isNullOrBlank() && File(customLogoPath).exists()) {
                BitmapFactory.decodeFile(customLogoPath)
            } else {
                BitmapFactory.decodeResource(context.resources, R.drawable.img_brand_logo)
            }
        }.getOrNull()
    }

    private fun csvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun formatSignedMoney(value: Double): String {
        return if (value >= 0.0) {
            String.format(Locale.US, "+$%.2f", value)
        } else {
            String.format(Locale.US, "-$%.2f", kotlin.math.abs(value))
        }
    }
}
