package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SessionContext
import com.example.data.local.TradeEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.domain.AnalyticsEngine
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LuxuryCard
import com.example.ui.components.formatSignedMoney
import com.example.ui.components.pnlColor
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RoyalGold
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom Range")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndSettingsScreen(
    session: SessionContext,
    personalTrades: List<TradeEntity>,
    allAcademyTrades: List<TradeEntity>,
    students: List<UserEntity>,
    isDarkMode: Boolean,
    customLogoPath: String?,
    onToggleTheme: () -> Unit,
    onUploadLogo: (Uri) -> Unit,
    onResetLogo: () -> Unit,
    onExportPdf: (Uri, String, String, String, List<TradeEntity>) -> Unit,
    onExportCsv: (Uri, String, String, String, List<TradeEntity>) -> Unit,
    onSharePdf: (String, String, String, List<TradeEntity>) -> Unit,
    onShareCsv: (String, String, String, List<TradeEntity>) -> Unit,
    onBackupJson: (Uri) -> Unit,
    onRestoreJson: (Uri) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    // Admin scope: -1L = Personal, -2L = All Academy Combined, >0L = Specific Student ID
    var adminScopeTargetId by remember { mutableLongStateOf(-1L) }

    val now = remember { System.currentTimeMillis() }
    var customStartMillis by remember { mutableLongStateOf(now - 30L * 24 * 3600 * 1000) }
    var customEndMillis by remember { mutableLongStateOf(now) }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    val (rangeStart, rangeEnd, periodLabel) = remember(selectedPeriod, customStartMillis, customEndMillis) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            ReportPeriod.DAILY -> {
                val s = AnalyticsEngine.startOfTodayMillis(now)
                val e = AnalyticsEngine.endOfTodayMillis(now)
                Triple(s, e, "Today (${dateFormatter.format(Date(now))})")
            }
            ReportPeriod.WEEKLY -> {
                cal.timeInMillis = AnalyticsEngine.startOfTodayMillis(now)
                cal.add(Calendar.DAY_OF_YEAR, -6)
                val s = cal.timeInMillis
                val e = AnalyticsEngine.endOfTodayMillis(now)
                Triple(s, e, "${dateFormatter.format(Date(s))} - ${dateFormatter.format(Date(e))}")
            }
            ReportPeriod.MONTHLY -> {
                cal.timeInMillis = AnalyticsEngine.startOfTodayMillis(now)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val s = cal.timeInMillis
                val e = AnalyticsEngine.endOfTodayMillis(now)
                Triple(s, e, "${dateFormatter.format(Date(s))} - ${dateFormatter.format(Date(e))}")
            }
            ReportPeriod.CUSTOM -> {
                val s = AnalyticsEngine.startOfTodayMillis(minOf(customStartMillis, customEndMillis))
                val e = AnalyticsEngine.endOfTodayMillis(maxOf(customStartMillis, customEndMillis))
                Triple(s, e, "${dateFormatter.format(Date(s))} - ${dateFormatter.format(Date(e))}")
            }
        }
    }

    val (scopedTrades, scopeLabel) = remember(
        session,
        personalTrades,
        allAcademyTrades,
        students,
        adminScopeTargetId,
        rangeStart,
        rangeEnd
    ) {
        val baseTrades = if (session.role != UserRole.ADMIN) {
            personalTrades to "Student: ${session.fullName}"
        } else {
            when (adminScopeTargetId) {
                -1L -> personalTrades to "Admin Personal: ${session.fullName}"
                -2L -> allAcademyTrades to "All Academy Combined"
                else -> {
                    val stu = students.firstOrNull { it.id == adminScopeTargetId }
                    allAcademyTrades.filter { it.userId == adminScopeTargetId } to "Student: ${stu?.fullName ?: "Selected"}"
                }
            }
        }
        baseTrades.first.filter { it.tradeDateMillis in rangeStart..rangeEnd } to baseTrades.second
    }

    val previewMetrics = remember(scopedTrades) { AnalyticsEngine.computeMetrics(scopedTrades) }
    val reportTitle = "${selectedPeriod.label} Trading Report"

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            onExportPdf(uri, reportTitle, scopeLabel, periodLabel, scopedTrades)
        }
    }

    val csvSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportCsv(uri, reportTitle, scopeLabel, periodLabel, scopedTrades)
        }
    }

    val backupSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onBackupJson(uri)
    }

    val backupRestorer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreJson(uri)
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUploadLogo(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Reports Generator Card
        item {
            LuxuryCard(
                highlightBorder = true,
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()
            ) {
                Text("Reports & Exports", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Generate Daily, Weekly, Monthly, or Custom Date Range PDF & Excel/CSV reports from saved records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                Text("Report Period", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label) }
                        )
                    }
                }

                if (selectedPeriod == ReportPeriod.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickingStartDate = true }, modifier = Modifier.weight(1f)) {
                            Text("From: ${dateFormatter.format(Date(customStartMillis))}")
                        }
                        OutlinedButton(onClick = { pickingEndDate = true }, modifier = Modifier.weight(1f)) {
                            Text("To: ${dateFormatter.format(Date(customEndMillis))}")
                        }
                    }
                }

                if (session.role == UserRole.ADMIN) {
                    Spacer(Modifier.height(10.dp))
                    Text("Report Scope (Admin)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = adminScopeTargetId == -1L,
                            onClick = { adminScopeTargetId = -1L },
                            label = { Text("My Trades") }
                        )
                        FilterChip(
                            selected = adminScopeTargetId == -2L,
                            onClick = { adminScopeTargetId = -2L },
                            label = { Text("All Students Combined") }
                        )
                        students.forEach { stu ->
                            FilterChip(
                                selected = adminScopeTargetId == stu.id,
                                onClick = { adminScopeTargetId = stu.id },
                                label = { Text(stu.fullName) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Preview • $scopeLabel ($periodLabel)",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoyalGold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        label = "Matching Trades",
                        value = "${previewMetrics.totalTrades}",
                        subtitle = "${previewMetrics.winningTrades}W / ${previewMetrics.losingTrades}L",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Period Net P&L",
                        value = formatSignedMoney(previewMetrics.netProfitLoss),
                        subtitle = String.format(Locale.US, "%.1f%% WR", previewMetrics.winRatePercent),
                        valueColor = pnlColor(previewMetrics.netProfitLoss, MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { pdfSaver.launch("BangaliBabu_${selectedPeriod.name}.pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy),
                        modifier = Modifier.weight(1f).testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save PDF", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { csvSaver.launch("BangaliBabu_${selectedPeriod.name}.csv") },
                        modifier = Modifier.weight(1f).testTag("export_csv_button")
                    ) {
                        Icon(Icons.Filled.TableView, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save CSV/Excel")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSharePdf(reportTitle, scopeLabel, periodLabel, scopedTrades) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                    OutlinedButton(
                        onClick = { onShareCsv(reportTitle, scopeLabel, periodLabel, scopedTrades) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Share CSV")
                    }
                }
            }
        }

        // Appearance & Brand Customization
        item {
            LuxuryCard(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                Text("Appearance & Brand Identity", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = null,
                            tint = RoyalGold
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (isDarkMode) "Midnight Navy & Gold (Dark)" else "Ivory White & Navy (Light)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { onToggleTheme() })
                }

                if (session.role == UserRole.ADMIN) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BrandLogoAvatar(customLogoPath = customLogoPath, size = 54.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Brand Logo", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Upload your exact original PNG/JPG logo from your device to display across the app and PDF reports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Upload Exact Logo")
                        }
                        if (!customLogoPath.isNullOrBlank()) {
                            TextButton(onClick = onResetLogo) { Text("Reset Default") }
                        }
                    }
                }
            }
        }

        // Database Backup & Restore (Admin)
        if (session.role == UserRole.ADMIN) {
            item {
                LuxuryCard(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                    Text("Database Ownership & Backup", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "All accounts, trades, and reviews are permanently saved in on-device SQLite (Room). Export a full JSON backup anytime to safeguard or transfer data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { backupSaver.launch("BangaliBabu_Backup_${System.currentTimeMillis()}.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Backup, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Backup JSON", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { backupRestorer.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                }
            }
        }

        // Account & Sign Out
        item {
            LuxuryCard(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                Text("Signed In Account", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${session.fullName} • ${session.email} (${session.role.name})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = LossCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = LossCrimson, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (pickingStartDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = customStartMillis)
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { customStartMillis = it }
                    pickingStartDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    if (pickingEndDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = customEndMillis)
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { customEndMillis = it }
                    pickingEndDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }
}
