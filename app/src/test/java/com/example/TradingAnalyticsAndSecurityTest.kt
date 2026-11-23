package com.example

import com.example.data.local.DisciplineProfileEntity
import com.example.data.local.TradeDirection
import com.example.data.local.TradeEntity
import com.example.data.local.TradeResult
import com.example.domain.AnalyticsEngine
import com.example.export.ReportExporter
import com.example.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingAnalyticsAndSecurityTest {

    private fun sampleTrade(
        id: Long,
        code: String,
        dateMillis: Long,
        instrument: String,
        timeframe: String,
        direction: TradeDirection,
        entry: Double,
        exit: Double,
        sl: Double,
        tp: Double,
        risk: Double,
        pnl: Double,
        result: TradeResult,
        mistakes: String = ""
    ) = TradeEntity(
        id = id,
        tradeCode = code,
        userId = 1L,
        userName = "Bangali Babu",
        tradeDateMillis = dateMillis,
        instrument = instrument,
        direction = direction.name,
        timeframe = timeframe,
        entryPrice = entry,
        exitPrice = exit,
        stopLoss = sl,
        takeProfit = tp,
        lotSize = 0.1,
        riskAmount = risk,
        profitLoss = pnl,
        result = result.name,
        strategy = "SMC",
        entryReason = "OB Sweep",
        exitReason = "Target",
        mistakes = mistakes,
        lessonsLearned = "Stay patient"
    )

    @Test
    fun analyticsEngine_computesAll16MetricsAccurately() {
        val day1 = 1795400000000L
        val day2 = day1 + 86_400_000L
        val trades = listOf(
            sampleTrade(1, "T1", day1, "XAUUSD", "M15", TradeDirection.BUY, 2600.0, 2620.0, 2590.0, 2620.0, 100.0, 200.0, TradeResult.WIN),
            sampleTrade(2, "T2", day1, "EURUSD", "H1", TradeDirection.SELL, 1.1000, 1.1050, 1.1050, 1.0900, 50.0, -50.0, TradeResult.LOSS, "FOMO"),
            sampleTrade(3, "T3", day2, "XAUUSD", "M15", TradeDirection.BUY, 2610.0, 2640.0, 2600.0, 2640.0, 100.0, 300.0, TradeResult.WIN),
            sampleTrade(4, "T4", day2, "NIFTY", "M5", TradeDirection.BUY, 24000.0, 24000.0, 23950.0, 24100.0, 50.0, 0.0, TradeResult.BREAK_EVEN)
        )

        val metrics = AnalyticsEngine.computeMetrics(trades)

        assertEquals(4, metrics.totalTrades)
        assertEquals(2, metrics.winningTrades)
        assertEquals(1, metrics.losingTrades)
        assertEquals(1, metrics.breakEvenTrades)
        assertEquals(50.0, metrics.winRatePercent, 0.01)
        assertEquals(25.0, metrics.lossRatePercent, 0.01)
        assertEquals(500.0, metrics.totalProfit, 0.01)
        assertEquals(50.0, metrics.totalLoss, 0.01)
        assertEquals(450.0, metrics.netProfitLoss, 0.01)
        assertEquals(250.0, metrics.averageWin, 0.01)
        assertEquals(50.0, metrics.averageLoss, 0.01)
        assertEquals(10.0, metrics.profitFactor, 0.01)
        assertFalse(metrics.isInfiniteProfitFactor)
        // Streak: T4 is BREAK_EVEN (skipped at tail), T3 is WIN -> 1 Win Streak
        assertEquals("1 Win Streak", metrics.currentStreakLabel)
        assertTrue(metrics.currentStreakPositive)
        assertEquals(3, metrics.byInstrument.size)
        assertEquals(3, metrics.byTimeframe.size)
    }

    @Test
    fun analyticsEngine_detectsOvertradingAndMaxLossBreach() {
        val now = 1795400000000L
        val trades = listOf(
            sampleTrade(1, "T1", now, "XAUUSD", "M15", TradeDirection.BUY, 2600.0, 2590.0, 0.0, 2620.0, 120.0, -120.0, TradeResult.LOSS, "No SL, Revenge"),
            sampleTrade(2, "T2", now, "XAUUSD", "M15", TradeDirection.SELL, 2590.0, 2595.0, 2600.0, 2570.0, 50.0, -50.0, TradeResult.LOSS, "Revenge")
        )
        val profile = DisciplineProfileEntity(
            userId = 1L,
            dailyProfitGoal = 200.0,
            dailyMaxLossLimit = 150.0,
            maxTradesPerDay = 1,
            maxRiskPerTrade = 100.0
        )

        val disc = AnalyticsEngine.computeDiscipline(trades, profile, now)
        assertTrue(disc.isOvertradingToday)
        assertTrue(disc.isDailyMaxLossBreached)
        assertEquals(50.0, disc.stopLossDisciplinePercent, 0.01)
        assertEquals(50.0, disc.riskCompliancePercent, 0.01)
        assertEquals("Revenge", disc.topMistakes.first().mistake)
        assertEquals(2, disc.topMistakes.first().count)
    }

    @Test
    fun securityUtils_hashesAndVerifiesPasswordsWithSalt() {
        val salt = SecurityUtils.generateSaltBase64()
        val hash = SecurityUtils.hashPassword("Bangali@2026", salt)
        assertTrue(SecurityUtils.verifyPassword("Bangali@2026", salt, hash))
        assertFalse(SecurityUtils.verifyPassword("WrongPass", salt, hash))
        assertEquals("admin@bangalibabu.com", SecurityUtils.normalizeEmail("  Admin@BangaliBabu.COM "))
    }

    @Test
    fun reportExporter_generatesValidBomCsv() {
        val trades = listOf(
            sampleTrade(1, "TBB-001", 1795400000000L, "XAUUSD", "H1", TradeDirection.BUY, 2600.0, 2615.0, 2595.0, 2615.0, 50.0, 150.0, TradeResult.WIN)
        )
        val metrics = AnalyticsEngine.computeMetrics(trades)
        val csv = ReportExporter.buildCsvContent("Monthly Report", "Admin", "Nov 2026", trades, metrics)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("TRADING WITH BANGALI BABU"))
        assertTrue(csv.contains("TBB-001"))
        assertTrue(csv.contains("XAUUSD"))
    }
}
