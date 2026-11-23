package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.SessionContext
import com.example.data.local.TradeDirection
import com.example.data.local.TradeEntity
import com.example.data.local.TradeResult
import com.example.data.local.TradeTimeframe
import com.example.data.local.UserRole
import com.example.ui.components.EmptyStateCard
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
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    session: SessionContext,
    trades: List<TradeEntity>,
    onSaveTrade: (TradeEntity, () -> Unit) -> Unit,
    onDeleteTrade: (Long) -> Unit,
    onImportScreenshot: suspend (android.net.Uri) -> Result<String>,
    onShowMessage: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterResult by remember { mutableStateOf<TradeResult?>(null) }
    var filterDirection by remember { mutableStateOf<TradeDirection?>(null) }
    var filterTimeframe by remember { mutableStateOf<String?>(null) }

    var editingTrade by remember { mutableStateOf<TradeEntity?>(null) }
    var showFormSheet by remember { mutableStateOf(false) }
    var inspectingTrade by remember { mutableStateOf<TradeEntity?>(null) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    val filteredTrades = remember(trades, searchQuery, filterResult, filterDirection, filterTimeframe) {
        val q = searchQuery.trim().lowercase(Locale.US)
        trades.filter { t ->
            val matchesQuery = q.isEmpty() ||
                t.instrument.lowercase(Locale.US).contains(q) ||
                t.strategy.lowercase(Locale.US).contains(q) ||
                t.tradeCode.lowercase(Locale.US).contains(q) ||
                t.notes.lowercase(Locale.US).contains(q) ||
                t.mistakes.lowercase(Locale.US).contains(q)
            val matchesResult = filterResult == null || t.resultEnum == filterResult
            val matchesDir = filterDirection == null || t.directionEnum == filterDirection
            val matchesTf = filterTimeframe == null || t.timeframe.equals(filterTimeframe, ignoreCase = true)
            matchesQuery && matchesResult && matchesDir && matchesTf
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTrade = null
                    showFormSheet = true
                },
                containerColor = RoyalGold,
                contentColor = DeepNavy,
                modifier = Modifier.testTag("add_trade_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Log New Trade")
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
                Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search instrument, strategy, Trade ID, mistakes...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_search_input")
                    )

                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterResult == TradeResult.WIN,
                            onClick = { filterResult = if (filterResult == TradeResult.WIN) null else TradeResult.WIN },
                            label = { Text("Wins") }
                        )
                        FilterChip(
                            selected = filterResult == TradeResult.LOSS,
                            onClick = { filterResult = if (filterResult == TradeResult.LOSS) null else TradeResult.LOSS },
                            label = { Text("Losses") }
                        )
                        FilterChip(
                            selected = filterResult == TradeResult.BREAK_EVEN,
                            onClick = {
                                filterResult = if (filterResult == TradeResult.BREAK_EVEN) null else TradeResult.BREAK_EVEN
                            },
                            label = { Text("Break Even") }
                        )
                        FilterChip(
                            selected = filterDirection == TradeDirection.BUY,
                            onClick = {
                                filterDirection = if (filterDirection == TradeDirection.BUY) null else TradeDirection.BUY
                            },
                            label = { Text("BUY") }
                        )
                        FilterChip(
                            selected = filterDirection == TradeDirection.SELL,
                            onClick = {
                                filterDirection = if (filterDirection == TradeDirection.SELL) null else TradeDirection.SELL
                            },
                            label = { Text("SELL") }
                        )
                        TradeTimeframe.entries.forEach { tf ->
                            FilterChip(
                                selected = filterTimeframe == tf.label,
                                onClick = {
                                    filterTimeframe = if (filterTimeframe == tf.label) null else tf.label
                                },
                                label = { Text(tf.label) }
                            )
                        }
                    }
                }
            }

            if (filteredTrades.isEmpty()) {
                item {
                    Column(modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                        EmptyStateCard(
                            icon = Icons.Filled.ReceiptLong,
                            title = if (trades.isEmpty()) "Journal is Empty" else "No Matching Trades",
                            subtitle = if (trades.isEmpty()) {
                                "Tap the + button to log your first trade with all 21 institutional fields & chart screenshot."
                            } else {
                                "Try clearing search or filter chips."
                            }
                        )
                    }
                }
            } else {
                items(filteredTrades, key = { it.id }) { trade ->
                    TradeListCard(
                        trade = trade,
                        onClick = { inspectingTrade = trade },
                        onEdit = {
                            editingTrade = trade
                            showFormSheet = true
                        },
                        onDelete = { confirmDeleteId = trade.id },
                        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showFormSheet) {
        TradeEntrySheet(
            session = session,
            initialTrade = editingTrade,
            onDismiss = { showFormSheet = false },
            onSave = { trade ->
                onSaveTrade(trade) { showFormSheet = false }
            },
            onImportScreenshot = onImportScreenshot,
            onShowMessage = onShowMessage
        )
    }

    inspectingTrade?.let { trade ->
        TradeDetailDialog(
            trade = trade,
            isAdmin = session.role == UserRole.ADMIN,
            onDismiss = { inspectingTrade = null },
            onEdit = {
                inspectingTrade = null
                editingTrade = trade
                showFormSheet = true
            }
        )
    }

    confirmDeleteId?.let { idToDelete ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete Trade Record?") },
            text = { Text("This action permanently removes the trade and recalculates your dashboard metrics.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTrade(idToDelete)
                        confirmDeleteId = null
                    }
                ) {
                    Text("Delete", color = LossCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TradeListCard(
    trade: TradeEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(trade.tradeDateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(trade.tradeDateMillis))
    }
    LuxuryCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = trade.instrument,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                StatusPill(
                    text = trade.direction,
                    containerColor = if (trade.directionEnum == TradeDirection.BUY) {
                        ProfitEmerald.copy(alpha = 0.16f)
                    } else {
                        LossCrimson.copy(alpha = 0.16f)
                    },
                    contentColor = if (trade.directionEnum == TradeDirection.BUY) ProfitEmerald else LossCrimson
                )
                StatusPill(
                    text = trade.timeframe,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatSignedMoney(trade.profitLoss),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FinancialMonoFont,
                    fontWeight = FontWeight.Bold
                ),
                color = pnlColor(trade.profitLoss, BreakEvenAmber)
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${trade.tradeCode} • $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Strategy: ${trade.strategy.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Entry ${trade.entryPrice} → Exit ${trade.exitPrice} | Lot ${trade.lotSize}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                if (trade.screenshotPath.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "Has Chart Screenshot",
                        tint = RoyalGold,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(18.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Trade", tint = RoyalGold, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Trade", tint = LossCrimson, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TradeEntrySheet(
    session: SessionContext,
    initialTrade: TradeEntity?,
    onDismiss: () -> Unit,
    onSave: (TradeEntity) -> Unit,
    onImportScreenshot: suspend (android.net.Uri) -> Result<String>,
    onShowMessage: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var userName by remember { mutableStateOf(initialTrade?.userName ?: session.fullName) }
    var tradeDateMillis by remember { mutableLongStateOf(initialTrade?.tradeDateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var instrument by remember { mutableStateOf(initialTrade?.instrument ?: "XAUUSD") }
    var direction by remember { mutableStateOf(initialTrade?.directionEnum ?: TradeDirection.BUY) }
    var timeframe by remember { mutableStateOf(initialTrade?.timeframe ?: TradeTimeframe.M15.label) }
    var entryPrice by remember { mutableStateOf(initialTrade?.entryPrice?.toString() ?: "") }
    var exitPrice by remember { mutableStateOf(initialTrade?.exitPrice?.toString() ?: "") }
    var stopLoss by remember { mutableStateOf(initialTrade?.stopLoss?.toString() ?: "") }
    var takeProfit by remember { mutableStateOf(initialTrade?.takeProfit?.toString() ?: "") }
    var lotSize by remember { mutableStateOf(initialTrade?.lotSize?.toString() ?: "0.10") }
    var riskAmount by remember { mutableStateOf(initialTrade?.riskAmount?.toString() ?: "") }
    var profitLoss by remember { mutableStateOf(initialTrade?.profitLoss?.toString() ?: "") }
    var result by remember { mutableStateOf(initialTrade?.resultEnum ?: TradeResult.WIN) }
    var strategy by remember { mutableStateOf(initialTrade?.strategy ?: "SMC / Price Action") }
    var entryReason by remember { mutableStateOf(initialTrade?.entryReason ?: "") }
    var exitReason by remember { mutableStateOf(initialTrade?.exitReason ?: "") }
    var mistakes by remember { mutableStateOf(initialTrade?.mistakes ?: "") }
    var lessonsLearned by remember { mutableStateOf(initialTrade?.lessonsLearned ?: "") }
    var screenshotPath by remember { mutableStateOf(initialTrade?.screenshotPath ?: "") }
    var notes by remember { mutableStateOf(initialTrade?.notes ?: "") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                onImportScreenshot(uri)
                    .onSuccess { savedPath -> screenshotPath = savedPath }
                    .onFailure { onShowMessage(it.message ?: "Failed to attach screenshot.") }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initialTrade == null) "Log New Trade" else "Edit ${initialTrade.tradeCode}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Trade ID: ${initialTrade?.tradeCode ?: "Auto-generated on save"}",
                style = MaterialTheme.typography.labelSmall,
                color = RoyalGold
            )

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Trader Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Trade Date: ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(tradeDateMillis))}"
                )
            }

            OutlinedTextField(
                value = instrument,
                onValueChange = { instrument = it.uppercase(Locale.US) },
                label = { Text("Trading Instrument (e.g., XAUUSD, NIFTY, EURUSD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("trade_instrument_input")
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = direction == TradeDirection.BUY,
                    onClick = { direction = TradeDirection.BUY },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("BUY / LONG") }
                SegmentedButton(
                    selected = direction == TradeDirection.SELL,
                    onClick = { direction = TradeDirection.SELL },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("SELL / SHORT") }
            }

            Text("Timeframe", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TradeTimeframe.entries.forEach { tf ->
                    FilterChip(
                        selected = timeframe == tf.label,
                        onClick = { timeframe = tf.label },
                        label = { Text(tf.label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(entryPrice, { entryPrice = it }, "Entry Price", Modifier.weight(1f))
                NumberField(exitPrice, { exitPrice = it }, "Exit Price", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(stopLoss, { stopLoss = it }, "Stop Loss", Modifier.weight(1f))
                NumberField(takeProfit, { takeProfit = it }, "Take Profit", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(lotSize, { lotSize = it }, "Lot / Qty", Modifier.weight(1f))
                NumberField(riskAmount, { riskAmount = it }, "Risk ($)", Modifier.weight(1f))
            }

            OutlinedTextField(
                value = profitLoss,
                onValueChange = { raw ->
                    profitLoss = raw
                    val parsed = raw.toDoubleOrNull()
                    if (parsed != null) {
                        result = when {
                            parsed > 0.0 -> TradeResult.WIN
                            parsed < 0.0 -> TradeResult.LOSS
                            else -> TradeResult.BREAK_EVEN
                        }
                    }
                },
                label = { Text("Actual Profit / Loss ($ — use minus for loss)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("trade_pnl_input")
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = result == TradeResult.WIN,
                    onClick = { result = TradeResult.WIN },
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) { Text("Win") }
                SegmentedButton(
                    selected = result == TradeResult.LOSS,
                    onClick = { result = TradeResult.LOSS },
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) { Text("Loss") }
                SegmentedButton(
                    selected = result == TradeResult.BREAK_EVEN,
                    onClick = { result = TradeResult.BREAK_EVEN },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) { Text("Break Even") }
            }

            OutlinedTextField(
                value = strategy,
                onValueChange = { strategy = it },
                label = { Text("Trading Strategy") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = entryReason,
                onValueChange = { entryReason = it },
                label = { Text("Entry Reason / Confluence") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = exitReason,
                onValueChange = { exitReason = it },
                label = { Text("Exit Reason") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mistakes,
                onValueChange = { mistakes = it },
                label = { Text("Mistakes (comma-separated tags, e.g., FOMO, Late Entry)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = lessonsLearned,
                onValueChange = { lessonsLearned = it },
                label = { Text("Lessons Learned") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Chart Screenshot Upload via Zero-Permission Photo Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (screenshotPath.isBlank()) "Upload Chart Screenshot" else "Change Screenshot")
                }
                if (screenshotPath.isNotBlank()) {
                    TextButton(onClick = { screenshotPath = "" }) {
                        Text("Remove", color = LossCrimson)
                    }
                }
            }

            if (screenshotPath.isNotBlank() && File(screenshotPath).exists()) {
                AsyncImage(
                    model = File(screenshotPath),
                    contentDescription = "Uploaded Chart Screenshot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    val cleanInst = instrument.trim()
                    val pnlVal = profitLoss.toDoubleOrNull()
                    if (cleanInst.isEmpty() || pnlVal == null) {
                        onShowMessage("Please enter a valid Instrument and Actual Profit / Loss.")
                        return@Button
                    }
                    val signedPnl = when {
                        result == TradeResult.LOSS && pnlVal > 0.0 -> -pnlVal
                        result == TradeResult.WIN && pnlVal < 0.0 -> kotlin.math.abs(pnlVal)
                        else -> pnlVal
                    }
                    val trade = TradeEntity(
                        id = initialTrade?.id ?: 0L,
                        tradeCode = initialTrade?.tradeCode ?: "",
                        userId = initialTrade?.userId ?: session.userId,
                        userName = userName.trim().ifBlank { session.fullName },
                        tradeDateMillis = tradeDateMillis,
                        instrument = cleanInst,
                        direction = direction.name,
                        timeframe = timeframe,
                        entryPrice = entryPrice.toDoubleOrNull() ?: 0.0,
                        exitPrice = exitPrice.toDoubleOrNull() ?: 0.0,
                        stopLoss = stopLoss.toDoubleOrNull() ?: 0.0,
                        takeProfit = takeProfit.toDoubleOrNull() ?: 0.0,
                        lotSize = lotSize.toDoubleOrNull() ?: 0.0,
                        riskAmount = riskAmount.toDoubleOrNull() ?: 0.0,
                        profitLoss = signedPnl,
                        result = result.name,
                        strategy = strategy.trim(),
                        entryReason = entryReason.trim(),
                        exitReason = exitReason.trim(),
                        mistakes = mistakes.trim(),
                        lessonsLearned = lessonsLearned.trim(),
                        screenshotPath = screenshotPath,
                        notes = notes.trim(),
                        adminFeedback = initialTrade?.adminFeedback ?: ""
                    )
                    onSave(trade)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_trade_button")
            ) {
                Text(if (initialTrade == null) "Save Trade to Journal" else "Update Trade", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = tradeDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { tradeDateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun TradeDetailDialog(
    trade: TradeEntity,
    isAdmin: Boolean,
    primaryActionLabel: String = "Edit Trade",
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val dateStr = remember(trade.tradeDateMillis) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(trade.tradeDateMillis))
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LuxuryCard(
            highlightBorder = true,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(trade.tradeCode, style = MaterialTheme.typography.labelSmall, color = RoyalGold)
                        Text(
                            "${trade.instrument} (${trade.direction} • ${trade.timeframe})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Text("Trader: ${trade.userName} • $dateStr", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()

                DetailRow("Actual P&L", formatSignedMoney(trade.profitLoss))
                DetailRow("Result", trade.resultEnum.name)
                DetailRow("Entry / Exit", "${trade.entryPrice} → ${trade.exitPrice}")
                DetailRow("Stop Loss / Take Profit", "${trade.stopLoss} / ${trade.takeProfit}")
                DetailRow("Lot Size / Risk", "${trade.lotSize} lots / $${trade.riskAmount}")
                DetailRow("Realized R:R", String.format(Locale.US, "1 : %.2f", trade.riskRewardRatio))
                DetailRow("Strategy", trade.strategy.ifBlank { "—" })
                DetailRow("Entry Reason", trade.entryReason.ifBlank { "—" })
                DetailRow("Exit Reason", trade.exitReason.ifBlank { "—" })
                DetailRow("Mistakes", trade.mistakes.ifBlank { "None logged" })
                DetailRow("Lessons Learned", trade.lessonsLearned.ifBlank { "—" })
                DetailRow("Notes", trade.notes.ifBlank { "—" })
                if (trade.adminFeedback.isNotBlank()) {
                    DetailRow("Mentor Feedback (Bangali Babu)", trade.adminFeedback)
                }

                if (trade.screenshotPath.isNotBlank() && File(trade.screenshotPath).exists()) {
                    Text("Chart Screenshot", style = MaterialTheme.typography.labelLarge, color = RoyalGold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = File(trade.screenshotPath),
                            contentDescription = "Trade Chart Screenshot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = DeepNavy)
                    ) {
                        Text(primaryActionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}
