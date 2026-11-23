package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryBreakdown
import com.example.domain.EquityPoint
import com.example.ui.theme.BreakEvenAmber
import com.example.ui.theme.FinancialMonoFont
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RoyalGold
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class BarDatum(
    val label: String,
    val value: Double,
    val count: Int
)

@Composable
fun InteractiveBarChart(
    title: String,
    data: List<BarDatum>,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(data) { mutableIntStateOf(-1) }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val selectedDatum = data.getOrNull(selectedIdx) ?: data.lastOrNull()

    LuxuryCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (selectedDatum != null) {
                Text(
                    text = "${selectedDatum.label}: ${formatSignedMoney(selectedDatum.value)} (${selectedDatum.count})",
                    style = MaterialTheme.typography.labelSmall,
                    color = pnlColor(selectedDatum.value, MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val displayData = data.takeLast(14)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(displayData) {
                        detectTapGestures { offset ->
                            val step = size.width.toFloat() / displayData.size.coerceAtLeast(1)
                            val tapped = (offset.x / step).toInt().coerceIn(0, displayData.lastIndex)
                            val originalOffset = data.size - displayData.size
                            selectedIdx = originalOffset + tapped
                        }
                    }
            ) {
                val maxAbs = displayData.maxOfOrNull { abs(it.value) }?.coerceAtLeast(1.0) ?: 1.0
                val midY = size.height / 2f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 2f
                )

                val slotWidth = size.width / displayData.size
                val barWidth = (slotWidth * 0.6f).coerceAtLeast(8f)
                displayData.forEachIndexed { idx, item ->
                    val ratio = (abs(item.value) / maxAbs).toFloat().coerceIn(0f, 1f)
                    val barHeight = (ratio * (size.height * 0.42f)).coerceAtLeast(4f)
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val top = if (item.value >= 0.0) midY - barHeight else midY
                    val isSelected = (data.size - displayData.size + idx) == selectedIdx
                    val baseColor = when {
                        item.value > 0.0 -> ProfitEmerald
                        item.value < 0.0 -> LossCrimson
                        else -> RoyalGold
                    }
                    drawRoundRect(
                        color = if (isSelected) RoyalGold else baseColor.copy(alpha = 0.88f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayData.first().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap any bar to inspect",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayData.last().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EquityCurveChart(
    points: List<EquityPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val activePoint = points.getOrNull(selectedIdx) ?: points.lastOrNull()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    LuxuryCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trading History Equity Curve",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (activePoint != null) {
                Text(
                    text = "${activePoint.dateLabel}: ${formatSignedMoney(activePoint.cumulativePnl)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = pnlColor(activePoint.cumulativePnl, RoyalGold)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log trades to generate your cumulative equity curve.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.size > 1) {
                                val step = size.width.toFloat() / (points.size - 1)
                                selectedIdx = (offset.x / step).roundToInt().coerceIn(0, points.lastIndex)
                            } else {
                                selectedIdx = 0
                            }
                        }
                    }
            ) {
                val values = points.map { it.cumulativePnl }
                val minVal = minOf(0.0, values.minOrNull() ?: 0.0)
                val maxVal = maxOf(0.0, values.maxOrNull() ?: 0.0)
                val range = (maxVal - minVal).coerceAtLeast(1.0)

                val padY = 14f
                val usableHeight = size.height - padY * 2
                fun yFor(v: Double): Float {
                    val norm = ((v - minVal) / range).toFloat()
                    return size.height - padY - (norm * usableHeight)
                }

                val zeroY = yFor(0.0)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 2f
                )

                val lineColor = if ((points.lastOrNull()?.cumulativePnl ?: 0.0) >= 0.0) {
                    ProfitEmerald
                } else {
                    LossCrimson
                }

                val path = Path()
                val fillPath = Path()
                val coords = points.mapIndexed { idx, p ->
                    val x = if (points.size == 1) size.width / 2f else (idx.toFloat() / (points.size - 1)) * size.width
                    Offset(x, yFor(p.cumulativePnl))
                }

                coords.forEachIndexed { idx, pt ->
                    if (idx == 0) {
                        path.moveTo(pt.x, pt.y)
                        fillPath.moveTo(pt.x, zeroY)
                        fillPath.lineTo(pt.x, pt.y)
                    } else {
                        path.lineTo(pt.x, pt.y)
                        fillPath.lineTo(pt.x, pt.y)
                    }
                }
                fillPath.lineTo(coords.last().x, zeroY)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                coords.forEachIndexed { idx, pt ->
                    val highlight = idx == selectedIdx || (selectedIdx < 0 && idx == coords.lastIndex)
                    drawCircle(
                        color = if (highlight) RoyalGold else lineColor,
                        radius = if (highlight) 8f else 4.5f,
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
fun WinLossPieChart(
    wins: Int,
    losses: Int,
    breakEven: Int,
    winRatePercent: Double,
    modifier: Modifier = Modifier
) {
    val total = wins + losses + breakEven
    LuxuryCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Win vs Loss Distribution",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(14.dp))
        if (total == 0) {
            Text(
                text = "No closed trades recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.size(134.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 22.dp.toPx()
                        val winSweep = (wins.toFloat() / total) * 360f
                        val lossSweep = (losses.toFloat() / total) * 360f
                        val beSweep = max(0f, 360f - winSweep - lossSweep)

                        var startAngle = -90f
                        if (winSweep > 0f) {
                            drawArc(
                                color = ProfitEmerald,
                                startAngle = startAngle,
                                sweepAngle = winSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += winSweep
                        }
                        if (lossSweep > 0f) {
                            drawArc(
                                color = LossCrimson,
                                startAngle = startAngle,
                                sweepAngle = lossSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += lossSweep
                        }
                        if (beSweep > 0f) {
                            drawArc(
                                color = BreakEvenAmber,
                                startAngle = startAngle,
                                sweepAngle = beSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", winRatePercent),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FinancialMonoFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Win Rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PieLegendRow("Wins", wins, total, ProfitEmerald)
                    PieLegendRow("Losses", losses, total, LossCrimson)
                    PieLegendRow("Break Even", breakEven, total, BreakEvenAmber)
                }
            }
        }
    }
}

@Composable
private fun PieLegendRow(label: String, count: Int, total: Int, color: Color) {
    val pct = if (total > 0) (count * 100.0 / total) else 0.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: $count (${String.format(Locale.US, "%.0f%%", pct)})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CategoryBreakdownCard(
    title: String,
    items: List<CategoryBreakdown>,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    LuxuryCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        if (items.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxAbsPnl = items.maxOfOrNull { abs(it.netPnl) }?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.category} (${item.totalTrades} trades • ${String.format(Locale.US, "%.0f%%", item.winRatePercent)} WR)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatSignedMoney(item.netPnl),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = pnlColor(item.netPnl, MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = { (abs(item.netPnl) / maxAbsPnl).toFloat().coerceIn(0.04f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp),
                            color = pnlColor(item.netPnl, RoyalGold),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
