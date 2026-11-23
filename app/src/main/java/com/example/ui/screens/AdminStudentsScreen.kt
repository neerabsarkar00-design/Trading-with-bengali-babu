package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountStatus
import com.example.data.local.DisciplineReviewEntity
import com.example.data.local.TradeEntity
import com.example.data.local.UserEntity
import com.example.domain.StudentPerformanceSummary
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.EquityCurveChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LuxuryCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatSignedMoney
import com.example.ui.components.pnlColor
import com.example.ui.theme.BreakEvenAmber
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.FinancialMonoFont
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RoyalGold
import java.util.Locale
import kotlin.math.abs

@Composable
fun AdminStudentsScreen(
    summaries: List<StudentPerformanceSummary>,
    allTrades: List<TradeEntity>,
    allReviews: List<DisciplineReviewEntity>,
    onAddStudent: (String, String, String, String, String, String, () -> Unit) -> Unit,
    onEditStudent: (Long, String, String, String, String, String, String?, () -> Unit) -> Unit,
    onToggleStatus: (Long) -> Unit,
    onDeleteStudent: (Long) -> Unit,
    onSaveTradeFeedback: (Long, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<AccountStatus?>(null) }
    var selectedStudentId by remember { mutableStateOf<Long?>(null) }

    var showStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<UserEntity?>(null) }
    var confirmDeleteStudent by remember { mutableStateOf<UserEntity?>(null) }

    val selectedSummary = summaries.firstOrNull { it.student.id == selectedStudentId }
    if (selectedSummary != null) {
        BackHandler { selectedStudentId = null }
        val studentTrades = remember(allTrades, selectedSummary.student.id) {
            allTrades.filter { it.userId == selectedSummary.student.id }
        }
        val studentReviews = remember(allReviews, selectedSummary.student.id) {
            allReviews.filter { it.userId == selectedSummary.student.id }
        }
        StudentDetailScreen(
            summary = selectedSummary,
            trades = studentTrades,
            reviews = studentReviews,
            onBack = { selectedStudentId = null },
            onToggleStatus = { onToggleStatus(selectedSummary.student.id) },
            onEditProfile = {
                editingStudent = selectedSummary.student
                showStudentDialog = true
            },
            onSaveFeedback = onSaveTradeFeedback
        )
    } else {
        val filtered = remember(summaries, searchQuery, statusFilter) {
            val q = searchQuery.trim().lowercase(Locale.US)
            summaries.filter { s ->
                val matchesQuery = q.isEmpty() ||
                    s.student.fullName.lowercase(Locale.US).contains(q) ||
                    s.student.email.lowercase(Locale.US).contains(q)
                val matchesStatus = statusFilter == null || s.student.statusEnum == statusFilter
                matchesQuery && matchesStatus
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingStudent = null
                        showStudentDialog = true
                    },
                    containerColor = RoyalGold,
                    contentColor = DeepNavy,
                    modifier = Modifier.testTag("admin_add_student_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Student Account")
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
                item {
                    Column(
                        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val activeCount = summaries.count { it.student.statusEnum == AccountStatus.ACTIVE }
                        val suspendedCount = summaries.size - activeCount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                label = "Total Students",
                                value = "${summaries.size}",
                                subtitle = "$activeCount Active • $suspendedCount Suspended",
                                icon = Icons.Filled.Group,
                                valueColor = RoyalGold,
                                modifier = Modifier.weight(1f)
                            )
                            val totalStudentPnl = summaries.sumOf { it.metrics.netProfitLoss }
                            KpiMetricCard(
                                label = "Students Net P&L",
                                value = formatSignedMoney(totalStudentPnl),
                                subtitle = "${summaries.sumOf { it.metrics.totalTrades }} student trades",
                                valueColor = pnlColor(totalStudentPnl, MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Student Performance Comparison Leaderboard
                        if (summaries.isNotEmpty()) {
                            LuxuryCard(highlightBorder = true) {
                                Text("Student Performance Comparison", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(10.dp))
                                val maxAbs = summaries.maxOfOrNull { abs(it.metrics.netProfitLoss) }?.coerceAtLeast(1.0) ?: 1.0
                                summaries.sortedByDescending { it.metrics.netProfitLoss }.take(6).forEach { item ->
                                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${item.student.fullName} (${String.format(Locale.US, "%.0f%%", item.metrics.winRatePercent)} WR)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = formatSignedMoney(item.metrics.netProfitLoss),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = pnlColor(item.metrics.netProfitLoss, MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (abs(item.metrics.netProfitLoss) / maxAbs).toFloat().coerceIn(0.05f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = pnlColor(item.metrics.netProfitLoss, RoyalGold),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search students by name or email...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("student_search_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = statusFilter == null,
                                onClick = { statusFilter = null },
                                label = { Text("All (${summaries.size})") }
                            )
                            FilterChip(
                                selected = statusFilter == AccountStatus.ACTIVE,
                                onClick = { statusFilter = AccountStatus.ACTIVE },
                                label = { Text("Active") }
                            )
                            FilterChip(
                                selected = statusFilter == AccountStatus.SUSPENDED,
                                onClick = { statusFilter = AccountStatus.SUSPENDED },
                                label = { Text("Suspended") }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                            EmptyStateCard(
                                icon = Icons.Filled.Group,
                                title = "No Students Found",
                                subtitle = "Tap + to create a student account, or students can sign up on the login screen."
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.student.id }) { item ->
                        val isActive = item.student.statusEnum == AccountStatus.ACTIVE
                        LuxuryCard(
                            modifier = Modifier
                                .widthIn(max = 1000.dp)
                                .fillMaxWidth()
                                .clickable { selectedStudentId = item.student.id }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = item.student.fullName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        StatusPill(
                                            text = item.student.status,
                                            containerColor = if (isActive) ProfitEmerald.copy(alpha = 0.16f) else LossCrimson.copy(alpha = 0.16f),
                                            contentColor = if (isActive) ProfitEmerald else LossCrimson
                                        )
                                    }
                                    Text(
                                        text = item.student.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatSignedMoney(item.metrics.netProfitLoss),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FinancialMonoFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = pnlColor(item.metrics.netProfitLoss, MaterialTheme.colorScheme.onSurface)
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.metrics.totalTrades} trades • ${String.format(Locale.US, "%.0f%%", item.metrics.winRatePercent)} WR • SL ${String.format(Locale.US, "%.0f%%", item.discipline.stopLossDisciplinePercent)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingStudent = item.student
                                            showStudentDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit Student", tint = RoyalGold, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onToggleStatus(item.student.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isActive) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                            contentDescription = if (isActive) "Suspend Student" else "Activate Student",
                                            tint = if (isActive) BreakEvenAmber else ProfitEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { confirmDeleteStudent = item.student },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Student", tint = LossCrimson, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStudentDialog) {
        StudentEditorDialog(
            initial = editingStudent,
            onDismiss = { showStudentDialog = false },
            onSave = { name, email, pass, phone, style, notes ->
                val target = editingStudent
                if (target == null) {
                    onAddStudent(name, email, pass, phone, style, notes) {
                        showStudentDialog = false
                    }
                } else {
                    onEditStudent(target.id, name, email, phone, style, notes, pass.ifBlank { null }) {
                        showStudentDialog = false
                    }
                }
            }
        )
    }

    confirmDeleteStudent?.let { stu ->
        AlertDialog(
            onDismissRequest = { confirmDeleteStudent = null },
            title = { Text("Delete ${stu.fullName}?") },
            text = { Text("This permanently deletes the student account and all their trade records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStudent(stu.id)
                        confirmDeleteStudent = null
                    }
                ) { Text("Delete", color = LossCrimson) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteStudent = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StudentDetailScreen(
    summary: StudentPerformanceSummary,
    trades: List<TradeEntity>,
    reviews: List<DisciplineReviewEntity>,
    onBack: () -> Unit,
    onToggleStatus: () -> Unit,
    onEditProfile: () -> Unit,
    onSaveFeedback: (Long, String) -> Unit
) {
    var inspectingTrade by remember { mutableStateOf<TradeEntity?>(null) }
    var feedbackTrade by remember { mutableStateOf<TradeEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Students")
                        }
                        Column {
                            Text(summary.student.fullName, style = MaterialTheme.typography.headlineSmall)
                            Text(summary.student.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    StatusPill(
                        text = summary.student.status,
                        containerColor = if (summary.student.statusEnum == AccountStatus.ACTIVE) {
                            ProfitEmerald.copy(alpha = 0.16f)
                        } else {
                            LossCrimson.copy(alpha = 0.16f)
                        },
                        contentColor = if (summary.student.statusEnum == AccountStatus.ACTIVE) ProfitEmerald else LossCrimson
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEditProfile, modifier = Modifier.weight(1f)) {
                        Text("Edit Student")
                    }
                    OutlinedButton(onClick = onToggleStatus, modifier = Modifier.weight(1f)) {
                        Text(if (summary.student.statusEnum == AccountStatus.ACTIVE) "Suspend Account" else "Activate Account")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        label = "Net P&L",
                        value = formatSignedMoney(summary.metrics.netProfitLoss),
                        subtitle = "${summary.metrics.totalTrades} trades",
                        valueColor = pnlColor(summary.metrics.netProfitLoss, MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Win Rate",
                        value = String.format(Locale.US, "%.1f%%", summary.metrics.winRatePercent),
                        subtitle = "PF: ${if (summary.metrics.isInfiniteProfitFactor) "∞" else String.format(Locale.US, "%.2f", summary.metrics.profitFactor)}",
                        valueColor = RoyalGold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        label = "SL Discipline",
                        value = String.format(Locale.US, "%.0f%%", summary.discipline.stopLossDisciplinePercent),
                        subtitle = "Overtrading days: ${summary.discipline.overtradingDaysCount}",
                        valueColor = ProfitEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Screenshots",
                        value = "${summary.screenshotCount}",
                        subtitle = summary.metrics.currentStreakLabel,
                        icon = Icons.Filled.Image,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                EquityCurveChart(points = summary.metrics.equityCurve)
            }
        }

        if (summary.discipline.topMistakes.isNotEmpty()) {
            item {
                LuxuryCard(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                    Text("Top Recurring Mistakes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    summary.discipline.topMistakes.forEach { m ->
                        Text("• ${m.mistake}: ${m.count} occurrence(s)", style = MaterialTheme.typography.bodySmall, color = LossCrimson)
                    }
                }
            }
        }

        item {
            Text(
                text = "Student Trade History (${trades.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()
            )
        }

        if (trades.isEmpty()) {
            item {
                LuxuryCard(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                    Text("This student has not logged any trades yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(trades, key = { it.id }) { trade ->
                LuxuryCard(
                    modifier = Modifier
                        .widthIn(max = 1000.dp)
                        .fillMaxWidth()
                        .clickable { inspectingTrade = trade }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${trade.instrument} • ${trade.direction} (${trade.timeframe})", style = MaterialTheme.typography.titleMedium)
                            Text("${trade.tradeCode} • Strategy: ${trade.strategy.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = formatSignedMoney(trade.profitLoss),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FinancialMonoFont, fontWeight = FontWeight.Bold),
                            color = pnlColor(trade.profitLoss, BreakEvenAmber)
                        )
                    }
                    if (trade.adminFeedback.isNotBlank()) {
                        Text(
                            text = "Your Feedback: ${trade.adminFeedback}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoyalGold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { inspectingTrade = trade }) {
                            Text("Inspect Trade & Chart")
                        }
                        TextButton(onClick = { feedbackTrade = trade }) {
                            Text(if (trade.adminFeedback.isBlank()) "Add Feedback" else "Edit Feedback", color = RoyalGold)
                        }
                    }
                }
            }
        }

        if (reviews.isNotEmpty()) {
            item {
                Text(
                    text = "Student Psychology & Periodic Reviews (${reviews.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()
                )
            }
            items(reviews, key = { it.id }) { rev ->
                ReviewCard(
                    review = rev,
                    onDelete = null,
                    modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()
                )
            }
        }
    }

    inspectingTrade?.let { trade ->
        TradeDetailDialog(
            trade = trade,
            isAdmin = true,
            primaryActionLabel = "Mentor Feedback",
            onDismiss = { inspectingTrade = null },
            onEdit = {
                inspectingTrade = null
                feedbackTrade = trade
            }
        )
    }

    feedbackTrade?.let { trade ->
        var feedback by remember(trade.id) { mutableStateOf(trade.adminFeedback) }
        AlertDialog(
            onDismissRequest = { feedbackTrade = null },
            title = { Text("Mentor Feedback • ${trade.tradeCode}") },
            text = {
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Guidance / Corrections for ${summary.student.fullName}") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveFeedback(trade.id, feedback)
                        feedbackTrade = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy)
                ) { Text("Save Feedback") }
            },
            dismissButton = {
                TextButton(onClick = { feedbackTrade = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StudentEditorDialog(
    initial: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.fullName ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var style by remember { mutableStateOf(initial?.tradingStyle ?: "Price Action") }
    var notes by remember { mutableStateOf(initial?.adminNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Student Account" else "Edit Student Account") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (initial == null) "Initial Password (min 6 chars)" else "Reset Password (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / WhatsApp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = style,
                    onValueChange = { style = it },
                    label = { Text("Trading Style") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Admin Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, email, password, phone, style, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
