package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.SessionContext
import com.example.data.local.DisciplineProfileEntity
import com.example.data.local.DisciplineReviewEntity
import com.example.data.local.ReviewType
import com.example.domain.DisciplineSummary
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LuxuryCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatSignedMoney
import com.example.ui.theme.BreakEvenAmber
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RoyalGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisciplineScreen(
    session: SessionContext,
    profile: DisciplineProfileEntity,
    summary: DisciplineSummary,
    reviews: List<DisciplineReviewEntity>,
    onSaveProfile: (DisciplineProfileEntity) -> Unit,
    onSaveReview: (DisciplineReviewEntity, () -> Unit) -> Unit,
    onDeleteReview: (Long) -> Unit
) {
    var editingRules by remember { mutableStateOf(false) }
    var showAddReview by remember { mutableStateOf(false) }
    var filterReviewType by remember { mutableStateOf<ReviewType?>(null) }

    var goalInput by remember(profile) { mutableStateOf(profile.dailyProfitGoal.toString()) }
    var maxLossInput by remember(profile) { mutableStateOf(profile.dailyMaxLossLimit.toString()) }
    var maxTradesInput by remember(profile) { mutableStateOf(profile.maxTradesPerDay.toString()) }
    var maxRiskInput by remember(profile) { mutableStateOf(profile.maxRiskPerTrade.toString()) }

    val filteredReviews = remember(reviews, filterReviewType) {
        if (filterReviewType == null) reviews else reviews.filter { it.reviewTypeEnum == filterReviewType }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddReview = true },
                containerColor = RoyalGold,
                contentColor = DeepNavy,
                modifier = Modifier.testTag("add_review_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Psychology or Periodic Review")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Alerts Banner
            if (summary.isOvertradingToday || summary.isDailyMaxLossBreached) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Discipline Alert",
                                tint = LossCrimson
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DISCIPLINE GUARDRAIL ALERT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LossCrimson
                                )
                                if (summary.isOvertradingToday) {
                                    Text(
                                        text = "Overtrading detected today: ${summary.todayTradesCount} trades (limit ${profile.maxTradesPerDay}). Step away from charts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                if (summary.isDailyMaxLossBreached) {
                                    Text(
                                        text = "Daily max loss limit breached (${formatSignedMoney(summary.todayNetPnl)} vs -$${profile.dailyMaxLossLimit}). Stop trading for today.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Discipline Scorecards
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
                            label = "Stop-Loss Discipline",
                            value = String.format(Locale.US, "%.0f%%", summary.stopLossDisciplinePercent),
                            subtitle = "Trades with SL set",
                            icon = Icons.Filled.Shield,
                            valueColor = if (summary.stopLossDisciplinePercent >= 90.0) ProfitEmerald else BreakEvenAmber,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            label = "Risk Compliance",
                            value = String.format(Locale.US, "%.0f%%", summary.riskCompliancePercent),
                            subtitle = "Within $${profile.maxRiskPerTrade}/trade",
                            icon = Icons.Filled.CheckCircle,
                            valueColor = if (summary.riskCompliancePercent >= 85.0) ProfitEmerald else BreakEvenAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    LuxuryCard(highlightBorder = true) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Goal & Risk Guardrails", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Today: ${summary.todayTradesCount}/${profile.maxTradesPerDay} trades • ${formatSignedMoney(summary.todayNetPnl)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { editingRules = !editingRules }) {
                                Text(if (editingRules) "Close" else "Edit Rules")
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Daily Profit Goal Progress ($${profile.dailyProfitGoal})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { summary.dailyGoalProgressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = ProfitEmerald,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Overtrading Days Logged: ${summary.overtradingDaysCount}   •   Max Daily Loss Limit: $${profile.dailyMaxLossLimit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (editingRules) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = goalInput,
                                    onValueChange = { goalInput = it },
                                    label = { Text("Daily Goal ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = maxLossInput,
                                    onValueChange = { maxLossInput = it },
                                    label = { Text("Daily Max Loss ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = maxTradesInput,
                                    onValueChange = { maxTradesInput = it },
                                    label = { Text("Max Trades/Day") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = maxRiskInput,
                                    onValueChange = { maxRiskInput = it },
                                    label = { Text("Max Risk/Trade ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onSaveProfile(
                                        DisciplineProfileEntity(
                                            userId = session.userId,
                                            dailyProfitGoal = goalInput.toDoubleOrNull() ?: 200.0,
                                            dailyMaxLossLimit = maxLossInput.toDoubleOrNull() ?: 100.0,
                                            maxTradesPerDay = (maxTradesInput.toIntOrNull() ?: 3).coerceAtLeast(1),
                                            maxRiskPerTrade = maxRiskInput.toDoubleOrNull() ?: 50.0
                                        )
                                    )
                                    editingRules = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Guardrails", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Mistake Frequency Tracker
            item {
                LuxuryCard(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                    Text("Recurring Mistake Tracker", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (summary.topMistakes.isEmpty()) {
                        Text(
                            text = "No mistakes tagged in your trades yet. Keep tagging execution errors in your journal to spot patterns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            summary.topMistakes.forEach { stat ->
                                StatusPill(
                                    text = "${stat.mistake} (${stat.count}x)",
                                    containerColor = LossCrimson.copy(alpha = 0.14f),
                                    contentColor = LossCrimson
                                )
                            }
                        }
                    }
                }
            }

            // Periodic Reviews Filter
            item {
                Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                    Text(
                        text = "Psychology Notes & Periodic Reviews",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filterReviewType == null,
                            onClick = { filterReviewType = null },
                            label = { Text("All Reviews") }
                        )
                        ReviewType.entries.forEach { type ->
                            FilterChip(
                                selected = filterReviewType == type,
                                onClick = { filterReviewType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }
            }

            if (filteredReviews.isEmpty()) {
                item {
                    Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                        EmptyStateCard(
                            icon = Icons.Filled.Psychology,
                            title = "No Reviews Logged Yet",
                            subtitle = "Tap + to log your Daily Psychology Note, Weekly Self-Review, or Monthly Review."
                        )
                    }
                }
            } else {
                items(filteredReviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        onDelete = { onDeleteReview(review.id) },
                        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showAddReview) {
        AddReviewSheet(
            userId = session.userId,
            onDismiss = { showAddReview = false },
            onSave = { rev ->
                onSaveReview(rev) { showAddReview = false }
            }
        )
    }
}

@Composable
fun ReviewCard(
    review: DisciplineReviewEntity,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(review.reviewDateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(review.reviewDateMillis))
    }
    LuxuryCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = review.reviewTypeEnum.label,
                    containerColor = RoyalGold.copy(alpha = 0.16f),
                    contentColor = RoyalGold
                )
                StatusPill(
                    text = review.moodTag,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Review", tint = LossCrimson)
                }
            }
        }
        Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Plan: ${if (review.followedPlan) "Followed" else "Broken"}  •  SL: ${if (review.usedStopLoss) "Yes" else "Missed"}  •  Max Loss Respected: ${if (review.respectedMaxLoss) "Yes" else "Breached"}",
            style = MaterialTheme.typography.labelMedium,
            color = if (review.followedPlan && review.usedStopLoss && review.respectedMaxLoss) ProfitEmerald else BreakEvenAmber
        )
        if (review.psychologyNotes.isNotBlank()) {
            Text(
                text = "Psychology: ${review.psychologyNotes}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (review.mistakesSummary.isNotBlank()) {
            Text(
                text = "Mistakes: ${review.mistakesSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = LossCrimson,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (review.actionPlan.isNotBlank()) {
            Text(
                text = "Action Plan: ${review.actionPlan}",
                style = MaterialTheme.typography.bodySmall,
                color = RoyalGold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddReviewSheet(
    userId: Long,
    onDismiss: () -> Unit,
    onSave: (DisciplineReviewEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reviewType by remember { mutableStateOf(ReviewType.DAILY_PSYCHOLOGY) }
    var moodTag by remember { mutableStateOf("Focused") }
    var followedPlan by remember { mutableStateOf(true) }
    var usedStopLoss by remember { mutableStateOf(true) }
    var respectedMaxLoss by remember { mutableStateOf(true) }
    var mistakesSummary by remember { mutableStateOf("") }
    var psychologyNotes by remember { mutableStateOf("") }
    var actionPlan by remember { mutableStateOf("") }

    val moods = listOf("Calm", "Focused", "Confident", "FOMO", "Revenge", "Anxious", "Overconfident")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Log Psychology & Discipline Review", style = MaterialTheme.typography.headlineSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReviewType.entries.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = reviewType == type,
                        onClick = { reviewType = type },
                        shape = SegmentedButtonDefaults.itemShape(idx, ReviewType.entries.size)
                    ) {
                        Text(
                            when (type) {
                                ReviewType.DAILY_PSYCHOLOGY -> "Daily"
                                ReviewType.WEEKLY_REVIEW -> "Weekly"
                                ReviewType.MONTHLY_REVIEW -> "Monthly"
                            }
                        )
                    }
                }
            }

            Text("Emotional State", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                moods.forEach { mood ->
                    FilterChip(
                        selected = moodTag == mood,
                        onClick = { moodTag = mood },
                        label = { Text(mood) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = followedPlan, onCheckedChange = { followedPlan = it })
                Text("Followed trading plan & setup rules")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = usedStopLoss, onCheckedChange = { usedStopLoss = it })
                Text("Placed Stop Loss on every trade")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = respectedMaxLoss, onCheckedChange = { respectedMaxLoss = it })
                Text("Respected daily maximum loss limit")
            }

            OutlinedTextField(
                value = psychologyNotes,
                onValueChange = { psychologyNotes = it },
                label = { Text("Trading Psychology / Mindset Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mistakesSummary,
                onValueChange = { mistakesSummary = it },
                label = { Text("Key Mistakes Identified") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = actionPlan,
                onValueChange = { actionPlan = it },
                label = { Text("Improvement Plan for Next Session") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        DisciplineReviewEntity(
                            userId = userId,
                            reviewType = reviewType.name,
                            reviewDateMillis = System.currentTimeMillis(),
                            moodTag = moodTag,
                            followedPlan = followedPlan,
                            usedStopLoss = usedStopLoss,
                            respectedMaxLoss = respectedMaxLoss,
                            mistakesSummary = mistakesSummary.trim(),
                            psychologyNotes = psychologyNotes.trim(),
                            actionPlan = actionPlan.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Review", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
