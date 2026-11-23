package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SessionContext
import com.example.data.SessionManager
import com.example.data.TradingRepository
import com.example.data.local.AppDatabase
import com.example.data.local.DisciplineProfileEntity
import com.example.data.local.DisciplineReviewEntity
import com.example.data.local.TradeEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.domain.AnalyticsEngine
import com.example.domain.DashboardMetrics
import com.example.domain.DisciplineSummary
import com.example.domain.StudentPerformanceSummary
import com.example.export.ReportExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    DASHBOARD("Dashboard"),
    JOURNAL("Journal"),
    DISCIPLINE("Discipline"),
    STUDENTS("Students"),
    REPORTS("Reports")
}

enum class DashboardScope {
    PERSONAL,
    COMBINED_ACADEMY
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: TradingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _session = MutableStateFlow<SessionContext?>(null)
    val session: StateFlow<SessionContext?> = _session.asStateFlow()

    private val _authInitializing = MutableStateFlow(true)
    val authInitializing: StateFlow<Boolean> = _authInitializing.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val _dashboardScope = MutableStateFlow(DashboardScope.PERSONAL)
    val dashboardScope: StateFlow<DashboardScope> = _dashboardScope.asStateFlow()

    val adminExists: StateFlow<Boolean> = repository.observeAdminExists()
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDarkMode: StateFlow<Boolean> = sessionManager.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customLogoPath: StateFlow<String?> = sessionManager.customLogoPathFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val personalTrades: StateFlow<List<TradeEntity>> = _session
        .flatMapLatest { s ->
            if (s == null) flowOf(emptyList())
            else repository.observeTradesForUser(s, s.userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAcademyTrades: StateFlow<List<TradeEntity>> = _session
        .flatMapLatest { s ->
            if (s == null || s.role != UserRole.ADMIN) flowOf(emptyList())
            else repository.observeAllTradesForAdmin(s)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<UserEntity>> = _session
        .flatMapLatest { s ->
            if (s == null || s.role != UserRole.ADMIN) flowOf(emptyList())
            else repository.observeAllStudents(s)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disciplineProfile: StateFlow<DisciplineProfileEntity> = _session
        .flatMapLatest { s ->
            if (s == null) flowOf(DisciplineProfileEntity(0L))
            else repository.observeDisciplineProfile(s, s.userId).map {
                it ?: DisciplineProfileEntity(userId = s.userId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisciplineProfileEntity(0L))

    val disciplineReviews: StateFlow<List<DisciplineReviewEntity>> = _session
        .flatMapLatest { s ->
            if (s == null) flowOf(emptyList())
            else repository.observeDisciplineReviews(s, s.userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReviewsForAdmin: StateFlow<List<DisciplineReviewEntity>> = _session
        .flatMapLatest { s ->
            if (s == null || s.role != UserRole.ADMIN) flowOf(emptyList())
            else repository.observeAllReviewsForAdmin(s)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allDisciplineProfilesForAdmin: StateFlow<List<DisciplineProfileEntity>> = _session
        .flatMapLatest { s ->
            if (s == null || s.role != UserRole.ADMIN) flowOf(emptyList())
            else repository.observeAllDisciplineProfilesForAdmin(s)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalMetrics: StateFlow<DashboardMetrics> = personalTrades
        .map { AnalyticsEngine.computeMetrics(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val combinedAcademyMetrics: StateFlow<DashboardMetrics> = allAcademyTrades
        .map { AnalyticsEngine.computeMetrics(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val personalDiscipline: StateFlow<DisciplineSummary> = combine(personalTrades, disciplineProfile) { trades, profile ->
        AnalyticsEngine.computeDiscipline(trades, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisciplineSummary())

    val studentSummaries: StateFlow<List<StudentPerformanceSummary>> = combine(
        students,
        allAcademyTrades,
        allDisciplineProfilesForAdmin
    ) { studentList, allTrades, profiles ->
        val tradesByUser = allTrades.groupBy { it.userId }
        val profileByUser = profiles.associateBy { it.userId }
        studentList.map { student ->
            val studentTrades = tradesByUser[student.id].orEmpty()
            val studentProfile = profileByUser[student.id] ?: DisciplineProfileEntity(userId = student.id)
            StudentPerformanceSummary(
                student = student,
                metrics = AnalyticsEngine.computeMetrics(studentTrades),
                discipline = AnalyticsEngine.computeDiscipline(
                    studentTrades,
                    studentProfile
                ),
                screenshotCount = studentTrades.count { it.screenshotPath.isNotBlank() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            sessionManager.loggedInUserIdFlow.collect { savedUserId ->
                if (savedUserId == null) {
                    _session.value = null
                } else {
                    val validated = repository.getValidatedSession(savedUserId)
                    _session.value = validated
                    if (validated == null) {
                        _authError.value = "Session expired or account suspended."
                    }
                }
                _authInitializing.value = false
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun setDashboardScope(scope: DashboardScope) {
        _dashboardScope.value = scope
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            sessionManager.setDarkMode(!isDarkMode.value)
        }
    }

    fun claimInitialAdmin(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            repository.claimInitialAdminAccount(fullName, email, password)
                .onSuccess {
                    _session.value = it
                    _snackbarEvents.tryEmit("Welcome, ${it.fullName}! Admin workspace unlocked.")
                }
                .onFailure { _authError.value = it.message ?: "Could not create Admin account." }
            _authBusy.value = false
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            repository.signIn(email, password)
                .onSuccess {
                    _session.value = it
                    _dashboardScope.value = DashboardScope.PERSONAL
                    _snackbarEvents.tryEmit("Signed in as ${it.fullName} (${it.role.name})")
                }
                .onFailure { _authError.value = it.message ?: "Sign in failed." }
            _authBusy.value = false
        }
    }

    fun registerStudent(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        tradingStyle: String
    ) {
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            repository.registerStudent(fullName, email, password, phone, tradingStyle)
                .onSuccess {
                    _session.value = it
                    _dashboardScope.value = DashboardScope.PERSONAL
                    _snackbarEvents.tryEmit("Account created! Welcome to Trading with Bangali Babu.")
                }
                .onFailure { _authError.value = it.message ?: "Registration failed." }
            _authBusy.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _session.value = null
            _dashboardScope.value = DashboardScope.PERSONAL
        }
    }

    // --- TRADES ---

    fun saveTrade(trade: TradeEntity, onSaved: () -> Unit = {}) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.saveTrade(currentSession, trade)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (trade.id == 0L) "Trade logged successfully." else "Trade updated.")
                    onSaved()
                }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to save trade.") }
        }
    }

    fun deleteTrade(tradeId: Long) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.deleteTrade(currentSession, tradeId)
                .onSuccess { _snackbarEvents.tryEmit("Trade deleted.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Could not delete trade.") }
        }
    }

    fun saveTradeAdminFeedback(tradeId: Long, feedback: String) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.updateTradeAdminFeedback(currentSession, tradeId, feedback)
                .onSuccess { _snackbarEvents.tryEmit("Mentor feedback saved.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to save feedback.") }
        }
    }

    suspend fun importScreenshot(uri: Uri): Result<String> {
        return repository.copyImageUriToInternalFile(uri, "screenshots", "chart")
    }

    // --- ADMIN STUDENT MANAGEMENT ---

    fun adminCreateStudent(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        tradingStyle: String,
        adminNotes: String,
        onDone: () -> Unit
    ) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.adminCreateStudent(currentSession, fullName, email, password, phone, tradingStyle, adminNotes)
                .onSuccess {
                    _snackbarEvents.tryEmit("Student account added.")
                    onDone()
                }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to add student.") }
        }
    }

    fun adminUpdateStudent(
        studentId: Long,
        fullName: String,
        email: String,
        phone: String,
        tradingStyle: String,
        adminNotes: String,
        newPassword: String?,
        onDone: () -> Unit
    ) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.adminUpdateStudent(
                currentSession, studentId, fullName, email, phone, tradingStyle, adminNotes, newPassword
            )
                .onSuccess {
                    _snackbarEvents.tryEmit("Student profile updated.")
                    onDone()
                }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Update failed.") }
        }
    }

    fun adminToggleStudentStatus(studentId: Long) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.adminToggleStudentStatus(currentSession, studentId)
                .onSuccess { status ->
                    _snackbarEvents.tryEmit("Student marked as ${status.name}.")
                }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to update status.") }
        }
    }

    fun adminDeleteStudent(studentId: Long) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.adminDeleteStudent(currentSession, studentId)
                .onSuccess { _snackbarEvents.tryEmit("Student removed.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to delete student.") }
        }
    }

    // --- DISCIPLINE & REVIEWS ---

    fun saveDisciplineProfile(profile: DisciplineProfileEntity) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.saveDisciplineProfile(currentSession, profile)
                .onSuccess { _snackbarEvents.tryEmit("Risk & discipline rules updated.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to update rules.") }
        }
    }

    fun saveDisciplineReview(review: DisciplineReviewEntity, onDone: () -> Unit = {}) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.saveDisciplineReview(currentSession, review)
                .onSuccess {
                    _snackbarEvents.tryEmit("Journal review saved.")
                    onDone()
                }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to save review.") }
        }
    }

    fun deleteDisciplineReview(reviewId: Long) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.deleteDisciplineReview(currentSession, reviewId)
                .onSuccess { _snackbarEvents.tryEmit("Review deleted.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to delete review.") }
        }
    }

    // --- BRANDING & EXPORTS & BACKUP ---

    fun uploadCustomBrandLogo(uri: Uri) {
        viewModelScope.launch {
            repository.saveCustomBrandLogo(uri)
                .onSuccess { _snackbarEvents.tryEmit("Brand logo updated across the app!") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Could not load logo image.") }
        }
    }

    fun resetCustomBrandLogo() {
        viewModelScope.launch {
            repository.resetCustomBrandLogo()
            _snackbarEvents.tryEmit("Default Bangali Babu emblem restored.")
        }
    }

    fun exportCsvToUri(
        context: Context,
        uri: Uri,
        reportTitle: String,
        scopeLabel: String,
        periodLabel: String,
        trades: List<TradeEntity>
    ) {
        viewModelScope.launch {
            runCatching {
                val metrics = AnalyticsEngine.computeMetrics(trades)
                val csv = ReportExporter.buildCsvContent(reportTitle, scopeLabel, periodLabel, trades, metrics)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Cannot write to selected file.")
            }.onSuccess {
                _snackbarEvents.tryEmit("Excel / CSV report exported.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.message ?: "CSV export failed.")
            }
        }
    }

    fun exportPdfToUri(
        context: Context,
        uri: Uri,
        reportTitle: String,
        scopeLabel: String,
        periodLabel: String,
        trades: List<TradeEntity>
    ) {
        viewModelScope.launch {
            runCatching {
                val metrics = AnalyticsEngine.computeMetrics(trades)
                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Cannot open PDF destination.")
                ReportExporter.writePdfReport(
                    context = context,
                    outputStream = out,
                    customLogoPath = customLogoPath.value,
                    reportTitle = reportTitle,
                    scopeLabel = scopeLabel,
                    periodLabel = periodLabel,
                    trades = trades,
                    metrics = metrics
                )
            }.onSuccess {
                _snackbarEvents.tryEmit("PDF report saved successfully.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.message ?: "PDF export failed.")
            }
        }
    }

    fun sharePdfReport(
        context: Context,
        reportTitle: String,
        scopeLabel: String,
        periodLabel: String,
        trades: List<TradeEntity>
    ) {
        viewModelScope.launch {
            val metrics = AnalyticsEngine.computeMetrics(trades)
            val fileName = "BangaliBabu_Report_${System.currentTimeMillis()}.pdf"
            ReportExporter.createShareFileUri(context, fileName) { out ->
                ReportExporter.writePdfReport(
                    context = context,
                    outputStream = out,
                    customLogoPath = customLogoPath.value,
                    reportTitle = reportTitle,
                    scopeLabel = scopeLabel,
                    periodLabel = periodLabel,
                    trades = trades,
                    metrics = metrics
                )
            }.onSuccess { uri ->
                ReportExporter.launchShareIntent(context, uri, "application/pdf", "Share Trading Report PDF")
            }.onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Failed to prepare PDF share.")
            }
        }
    }

    fun shareCsvReport(
        context: Context,
        reportTitle: String,
        scopeLabel: String,
        periodLabel: String,
        trades: List<TradeEntity>
    ) {
        viewModelScope.launch {
            val metrics = AnalyticsEngine.computeMetrics(trades)
            val csv = ReportExporter.buildCsvContent(reportTitle, scopeLabel, periodLabel, trades, metrics)
            val fileName = "BangaliBabu_Report_${System.currentTimeMillis()}.csv"
            ReportExporter.createShareFileUri(context, fileName) { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
            }.onSuccess { uri ->
                ReportExporter.launchShareIntent(context, uri, "text/csv", "Share Excel/CSV Report")
            }.onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Failed to prepare CSV share.")
            }
        }
    }

    fun exportBackupJson(context: Context, uri: Uri) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            repository.exportFullBackupJson(currentSession)
                .onSuccess { json ->
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: throw IllegalStateException("Cannot write backup file.")
                    }.onSuccess {
                        _snackbarEvents.tryEmit("Full database backup exported.")
                    }.onFailure {
                        _snackbarEvents.tryEmit(it.message ?: "Backup write failed.")
                    }
                }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Backup failed.") }
        }
    }

    fun restoreBackupJson(context: Context, uri: Uri) {
        val currentSession = _session.value ?: return
        viewModelScope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Could not read backup file.")
                repository.restoreFromBackupJson(currentSession, json).getOrThrow()
            }.onSuccess { count ->
                _snackbarEvents.tryEmit("Restored $count records from backup.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Restore failed.")
            }
        }
    }

    fun emitMessage(msg: String) {
        _snackbarEvents.tryEmit(msg)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = AppDatabase.getInstance(appCtx)
            val sessionManager = SessionManager(appCtx)
            val repo = TradingRepository(appCtx, db, sessionManager)
            return MainViewModel(repo, sessionManager) as T
        }
    }
}
