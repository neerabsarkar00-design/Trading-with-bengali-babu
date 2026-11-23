package com.example.domain

import com.example.data.local.DisciplineProfileEntity
import com.example.data.local.TradeEntity
import com.example.data.local.TradeResult
import com.example.data.local.UserEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class DayPnlPoint(
    val dateKey: String,
    val label: String,
    val netPnl: Double,
    val tradeCount: Int
)

data class MonthPnlPoint(
    val monthKey: String,
    val label: String,
    val netPnl: Double,
    val tradeCount: Int
)

data class EquityPoint(
    val tradeCode: String,
    val dateLabel: String,
    val cumulativePnl: Double
)

data class CategoryBreakdown(
    val category: String,
    val totalTrades: Int,
    val winCount: Int,
    val winRatePercent: Double,
    val netPnl: Double
)

data class MistakeStat(
    val mistake: String,
    val count: Int
)

data class DashboardMetrics(
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val breakEvenTrades: Int = 0,
    val winRatePercent: Double = 0.0,
    val lossRatePercent: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalLoss: Double = 0.0,
    val netProfitLoss: Double = 0.0,
    val averageWin: Double = 0.0,
    val averageLoss: Double = 0.0,
    val profitFactor: Double = 0.0,
    val isInfiniteProfitFactor: Boolean = false,
    val averageRiskReward: Double = 0.0,
    val bestTradingDayLabel: String = "—",
    val bestTradingDayPnl: Double = 0.0,
    val worstTradingDayLabel: String = "—",
    val worstTradingDayPnl: Double = 0.0,
    val currentStreakLabel: String = "No Streak",
    val currentStreakPositive: Boolean = true,
    val dailySeries: List<DayPnlPoint> = emptyList(),
    val monthlySeries: List<MonthPnlPoint> = emptyList(),
    val equityCurve: List<EquityPoint> = emptyList(),
    val byInstrument: List<CategoryBreakdown> = emptyList(),
    val byTimeframe: List<CategoryBreakdown> = emptyList()
)

data class DisciplineSummary(
    val todayTradesCount: Int = 0,
    val todayNetPnl: Double = 0.0,
    val isOvertradingToday: Boolean = false,
    val overtradingDaysCount: Int = 0,
    val isDailyMaxLossBreached: Boolean = false,
    val dailyGoalProgressPercent: Float = 0f,
    val stopLossDisciplinePercent: Double = 0.0,
    val riskCompliancePercent: Double = 0.0,
    val topMistakes: List<MistakeStat> = emptyList()
)

data class StudentPerformanceSummary(
    val student: UserEntity,
    val metrics: DashboardMetrics,
    val discipline: DisciplineSummary,
    val screenshotCount: Int
)

object AnalyticsEngine {

    private val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayLabelFormatter = SimpleDateFormat("dd MMM", Locale.US)
    private val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthLabelFormatter = SimpleDateFormat("MMM yy", Locale.US)

    fun computeMetrics(trades: List<TradeEntity>): DashboardMetrics {
        if (trades.isEmpty()) return DashboardMetrics()

        val sortedChronological = trades.sortedWith(
            compareBy<TradeEntity> { it.tradeDateMillis }.thenBy { it.id }
        )

        var wins = 0
        var losses = 0
        var breakEvens = 0
        var totalProfit = 0.0
        var totalLoss = 0.0
        var rrSum = 0.0
        var rrCount = 0

        for (t in sortedChronological) {
            when (t.resultEnum) {
                TradeResult.WIN -> {
                    wins++
                    if (t.profitLoss > 0) totalProfit += t.profitLoss
                }
                TradeResult.LOSS -> {
                    losses++
                    if (t.profitLoss < 0) totalLoss += abs(t.profitLoss)
                }
                TradeResult.BREAK_EVEN -> {
                    breakEvens++
                    if (t.profitLoss > 0) totalProfit += t.profitLoss
                    else if (t.profitLoss < 0) totalLoss += abs(t.profitLoss)
                }
            }
            val rr = t.riskRewardRatio
            if (rr > 0.0) {
                rrSum += rr
                rrCount++
            }
        }

        val total = sortedChronological.size
        val winRate = (wins.toDouble() / total) * 100.0
        val lossRate = (losses.toDouble() / total) * 100.0
        val netPnl = sortedChronological.sumOf { it.profitLoss }
        val avgWin = if (wins > 0) totalProfit / wins else 0.0
        val avgLoss = if (losses > 0) totalLoss / losses else 0.0
        val isInfinitePf = totalLoss == 0.0 && totalProfit > 0.0
        val profitFactor = if (totalLoss > 0.0) totalProfit / totalLoss else 0.0
        val avgRr = if (rrCount > 0) rrSum / rrCount else 0.0

        // Current Streak (ignoring break-even or stopping at break-even)
        var streakCount = 0
        var streakType: TradeResult? = null
        for (t in sortedChronological.asReversed()) {
            val res = t.resultEnum
            if (res == TradeResult.BREAK_EVEN) {
                if (streakType == null) continue else break
            }
            if (streakType == null) {
                streakType = res
                streakCount = 1
            } else if (streakType == res) {
                streakCount++
            } else {
                break
            }
        }
        val streakLabel = when {
            streakCount == 0 || streakType == null -> "No Streak"
            streakType == TradeResult.WIN -> "$streakCount Win Streak"
            else -> "$streakCount Loss Streak"
        }

        // Daily series
        val dailyGrouped = sortedChronological.groupBy { dayKeyFormatter.format(Date(it.tradeDateMillis)) }
        val dailySeries = dailyGrouped.entries.sortedBy { it.key }.map { (key, list) ->
            val firstDate = Date(list.first().tradeDateMillis)
            DayPnlPoint(
                dateKey = key,
                label = dayLabelFormatter.format(firstDate),
                netPnl = list.sumOf { it.profitLoss },
                tradeCount = list.size
            )
        }

        val bestDay = dailySeries.maxByOrNull { it.netPnl }
        val worstDay = dailySeries.minByOrNull { it.netPnl }

        // Monthly series
        val monthlyGrouped = sortedChronological.groupBy { monthKeyFormatter.format(Date(it.tradeDateMillis)) }
        val monthlySeries = monthlyGrouped.entries.sortedBy { it.key }.map { (key, list) ->
            val firstDate = Date(list.first().tradeDateMillis)
            MonthPnlPoint(
                monthKey = key,
                label = monthLabelFormatter.format(firstDate),
                netPnl = list.sumOf { it.profitLoss },
                tradeCount = list.size
            )
        }

        // Equity curve
        var running = 0.0
        val equityCurve = sortedChronological.map { t ->
            running += t.profitLoss
            EquityPoint(
                tradeCode = t.tradeCode,
                dateLabel = dayLabelFormatter.format(Date(t.tradeDateMillis)),
                cumulativePnl = running
            )
        }

        // Breakdown by Instrument
        val byInstrument = sortedChronological
            .groupBy { it.instrument.trim().uppercase(Locale.US).ifBlank { "OTHER" } }
            .map { (instrument, list) ->
                val instWins = list.count { it.resultEnum == TradeResult.WIN }
                CategoryBreakdown(
                    category = instrument,
                    totalTrades = list.size,
                    winCount = instWins,
                    winRatePercent = (instWins.toDouble() / list.size) * 100.0,
                    netPnl = list.sumOf { it.profitLoss }
                )
            }
            .sortedByDescending { it.netPnl }

        // Breakdown by Timeframe
        val tfOrder = listOf("M1", "M5", "M15", "M30", "H1", "H4", "D1")
        val byTimeframe = sortedChronological
            .groupBy { it.timeframe.trim().uppercase(Locale.US).ifBlank { "H1" } }
            .map { (tf, list) ->
                val tfWins = list.count { it.resultEnum == TradeResult.WIN }
                CategoryBreakdown(
                    category = tf,
                    totalTrades = list.size,
                    winCount = tfWins,
                    winRatePercent = (tfWins.toDouble() / list.size) * 100.0,
                    netPnl = list.sumOf { it.profitLoss }
                )
            }
            .sortedBy { tfOrder.indexOf(it.category).let { idx -> if (idx < 0) 99 else idx } }

        return DashboardMetrics(
            totalTrades = total,
            winningTrades = wins,
            losingTrades = losses,
            breakEvenTrades = breakEvens,
            winRatePercent = winRate,
            lossRatePercent = lossRate,
            totalProfit = totalProfit,
            totalLoss = totalLoss,
            netProfitLoss = netPnl,
            averageWin = avgWin,
            averageLoss = avgLoss,
            profitFactor = profitFactor,
            isInfiniteProfitFactor = isInfinitePf,
            averageRiskReward = avgRr,
            bestTradingDayLabel = bestDay?.label ?: "—",
            bestTradingDayPnl = bestDay?.netPnl ?: 0.0,
            worstTradingDayLabel = worstDay?.label ?: "—",
            worstTradingDayPnl = worstDay?.netPnl ?: 0.0,
            currentStreakLabel = streakLabel,
            currentStreakPositive = streakType != TradeResult.LOSS,
            dailySeries = dailySeries,
            monthlySeries = monthlySeries,
            equityCurve = equityCurve,
            byInstrument = byInstrument,
            byTimeframe = byTimeframe
        )
    }

    fun computeDiscipline(
        trades: List<TradeEntity>,
        profile: DisciplineProfileEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): DisciplineSummary {
        val todayKey = dayKeyFormatter.format(Date(nowMillis))
        val todayTrades = trades.filter { dayKeyFormatter.format(Date(it.tradeDateMillis)) == todayKey }
        val todayPnl = todayTrades.sumOf { it.profitLoss }

        val dailyCounts = trades.groupBy { dayKeyFormatter.format(Date(it.tradeDateMillis)) }
        val overtradingDays = dailyCounts.count { it.value.size > profile.maxTradesPerDay }

        val slCount = trades.count { it.stopLoss > 0.0 }
        val slDiscipline = if (trades.isNotEmpty()) (slCount.toDouble() / trades.size) * 100.0 else 100.0

        val riskCompliantCount = trades.count { it.riskAmount <= profile.maxRiskPerTrade }
        val riskCompliance = if (trades.isNotEmpty()) (riskCompliantCount.toDouble() / trades.size) * 100.0 else 100.0

        val goalProgress = if (profile.dailyProfitGoal > 0.0) {
            (todayPnl / profile.dailyProfitGoal).toFloat().coerceIn(0f, 1f)
        } else 0f

        val mistakeMap = mutableMapOf<String, Int>()
        for (trade in trades) {
            val raw = trade.mistakes.trim()
            if (raw.isNotEmpty()) {
                raw.split(",", ";", "\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { tag ->
                        mistakeMap[tag] = (mistakeMap[tag] ?: 0) + 1
                    }
            }
        }
        val topMistakes = mistakeMap.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { MistakeStat(it.key, it.value) }

        return DisciplineSummary(
            todayTradesCount = todayTrades.size,
            todayNetPnl = todayPnl,
            isOvertradingToday = todayTrades.size > profile.maxTradesPerDay,
            overtradingDaysCount = overtradingDays,
            isDailyMaxLossBreached = profile.dailyMaxLossLimit > 0.0 && todayPnl <= -profile.dailyMaxLossLimit,
            dailyGoalProgressPercent = goalProgress,
            stopLossDisciplinePercent = slDiscipline,
            riskCompliancePercent = riskCompliance,
            topMistakes = topMistakes
        )
    }

    fun startOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
