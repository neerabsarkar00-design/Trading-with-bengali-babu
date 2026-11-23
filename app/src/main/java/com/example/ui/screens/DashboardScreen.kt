package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SessionContext
import com.example.data.local.UserRole
import com.example.domain.DashboardMetrics
import com.example.ui.DashboardScope
import com.example.ui.components.BarDatum
import com.example.ui.components.CategoryBreakdownCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.EquityCurveChart
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LuxuryCard
import com.example.ui.components.StatusPill
import com.example.ui.components.WinLossPieChart
import com.example.ui.components.formatMoney
import com.example.ui.components.formatSignedMoney
import com.example.ui.components.pnlColor
import com.example.ui.theme.BreakEvenAmber
import com.example.ui.theme.GoldContainerDark
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RoyalGold
import java.util.Locale

@Composable
fun DashboardScreen(
    session: SessionContext,
    scope: DashboardScope,
    onScopeChange: (DashboardScope) -> Unit,
    personalMetrics: DashboardMetrics,
    combinedMetrics: DashboardMetrics,
    studentCount: Int,
    onQuickAddTrade: () -> Unit
) {
    val metrics = if (session.role == UserRole.ADMIN && scope == DashboardScope.COMBINED_ACADEMY) {
        combinedMetrics
    } else {
        personalMetrics
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                LuxuryCard(highlightBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(
                                text = if (session.role == UserRole.ADMIN) "ADMIN COMMAND CENTER" else "STUDENT TRADER",
                                containerColor = GoldContainerDark,
                                contentColor = RoyalGold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Welcome, ${session.fullName}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = if (session.role == UserRole.ADMIN) {
                                    "Monitoring $studentCount registered student(s) • Live calculated metrics"
                                } else {
                                    "Your private performance analytics • Live calculated from saved trades"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (session.role == UserRole.ADMIN) {
                        Spacer(Modifier.height(14.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = scope == DashboardScope.PERSONAL,
                                onClick = { onScopeChange(DashboardScope.PERSONAL) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.testTag("scope_personal")
                            ) {
                                Text("My Trades")
                            }
                            SegmentedButton(
                                selected = scope == DashboardScope.COMBINED_ACADEMY,
                                onClick = { onScopeChange(DashboardScope.COMBINED_ACADEMY) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.testTag("scope_academy")
                            ) {
                                Text("All Academy")
                            }
                        }
                    }
                }
            }
        }

        if (metrics.totalTrades == 0) {
            item {
                Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        title = "No Trades Recorded Yet",
                        subtitle = "Tap + Log Trade in the Journal tab to record your first real setup. All 16 metrics and charts calculate automatically."
                    )
                }
            }
        }

        // Row 1: Net P&L & Win Rate
        item {
            Column(
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = "Net Profit / Loss",
                        value = formatSignedMoney(metrics.netProfitLoss),
                        subtitle = "${metrics.totalTrades} total trades",
                        icon = Icons.Filled.AccountBalanceWallet,
                        valueColor = pnlColor(metrics.netProfitLoss, MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Win Rate / Loss Rate",
                        value = String.format(Locale.US, "%.1f%%", metrics.winRatePercent),
                        subtitle = String.format(Locale.US, "Loss Rate: %.1f%%", metrics.lossRatePercent),
                        icon = Icons.Filled.Analytics,
                        valueColor = if (metrics.winRatePercent >= 50.0) ProfitEmerald else RoyalGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Total Trades Breakdown (Wins / Losses / Break Even)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = "Winning Trades",
                        value = "${metrics.winningTrades}",
                        subtitle = "of ${metrics.totalTrades} trades",
                        icon = Icons.Filled.TrendingUp,
                        valueColor = ProfitEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Losing Trades",
                        value = "${metrics.losingTrades}",
                        subtitle = "${metrics.breakEvenTrades} Break Even",
                        icon = Icons.Filled.TrendingDown,
                        valueColor = LossCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Total Profit & Total Loss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = "Total Profit",
                        value = "+${formatMoney(metrics.totalProfit)}",
                        subtitle = "Avg Win: +${formatMoney(metrics.averageWin)}",
                        icon = Icons.Filled.ArrowUpward,
                        valueColor = ProfitEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Total Loss",
                        value = "-${formatMoney(metrics.totalLoss)}",
                        subtitle = "Avg Loss: -${formatMoney(metrics.averageLoss)}",
                        icon = Icons.Filled.ArrowDownward,
                        valueColor = LossCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Profit Factor & Avg Risk-to-Reward
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = "Profit Factor",
                        value = when {
                            metrics.totalTrades == 0 -> "0.00"
                            metrics.isInfiniteProfitFactor -> "∞"
                            else -> String.format(Locale.US, "%.2f", metrics.profitFactor)
                        },
                        subtitle = "Gross Profit / Gross Loss",
                        icon = Icons.Filled.Equalizer,
                        valueColor = RoyalGold,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Avg Risk-to-Reward",
                        value = String.format(Locale.US, "1 : %.2f", metrics.averageRiskReward),
                        subtitle = "Realized R:R ratio",
                        icon = Icons.Filled.Bolt,
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 5: Best Day, Worst Day, Current Streak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = "Best Trading Day",
                        value = formatSignedMoney(metrics.bestTradingDayPnl),
                        subtitle = metrics.bestTradingDayLabel,
                        icon = Icons.Filled.EmojiEvents,
                        valueColor = ProfitEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Worst Trading Day",
                        value = formatSignedMoney(metrics.worstTradingDayPnl),
                        subtitle = metrics.worstTradingDayLabel,
                        valueColor = LossCrimson,
                        modifier = Modifier.weight(1f)
                    )
                }

                LuxuryCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusPill(
                            text = metrics.currentStreakLabel,
                            containerColor = when {
                                metrics.totalTrades == 0 -> BreakEvenAmber.copy(alpha = 0.16f)
                                metrics.currentStreakPositive -> ProfitEmerald.copy(alpha = 0.16f)
                                else -> LossCrimson.copy(alpha = 0.16f)
                            },
                            contentColor = when {
                                metrics.totalTrades == 0 -> BreakEvenAmber
                                metrics.currentStreakPositive -> ProfitEmerald
                                else -> LossCrimson
                            }
                        )
                    }
                }
            }
        }

        // Charts Section
        item {
            Column(
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EquityCurveChart(points = metrics.equityCurve)

                InteractiveBarChart(
                    title = "Daily Profit & Loss Chart",
                    data = metrics.dailySeries.map { BarDatum(it.label, it.netPnl, it.tradeCount) },
                    emptyMessage = "Daily P&L bars appear once trades are saved."
                )

                InteractiveBarChart(
                    title = "Monthly Performance Chart",
                    data = metrics.monthlySeries.map { BarDatum(it.label, it.netPnl, it.tradeCount) },
                    emptyMessage = "Monthly performance appears as you log trades."
                )

                WinLossPieChart(
                    wins = metrics.winningTrades,
                    losses = metrics.losingTrades,
                    breakEven = metrics.breakEvenTrades,
                    winRatePercent = metrics.winRatePercent
                )

                CategoryBreakdownCard(
                    title = "Performance by Instrument",
                    items = metrics.byInstrument,
                    emptyMessage = "Trade instruments (e.g., XAUUSD, NIFTY, EURUSD) will rank here."
                )

                CategoryBreakdownCard(
                    title = "Performance by Timeframe",
                    items = metrics.byTimeframe,
                    emptyMessage = "Timeframes (M1 to D1) performance will rank here."
                )
            }
        }
    }
}
