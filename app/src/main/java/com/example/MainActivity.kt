package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SessionContext
import com.example.data.local.UserRole
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.screens.AdminStudentsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DisciplineScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.ReportsAndSettingsScreen
import com.example.ui.theme.BangaliBabuTheme
import com.example.ui.theme.RoyalGold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(context))
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            BangaliBabuTheme(darkTheme = isDarkMode) {
                TradingPlatformRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TradingPlatformRoot(viewModel: MainViewModel) {
    val authInitializing by viewModel.authInitializing.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val adminExists by viewModel.adminExists.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val customLogoPath by viewModel.customLogoPath.collectAsStateWithLifecycle()
    val authBusy by viewModel.authBusy.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    if (authInitializing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RoyalGold)
        }
        return
    }

    val activeSession = session
    if (activeSession == null) {
        AuthScreen(
            adminExists = adminExists,
            customLogoPath = customLogoPath,
            isDarkMode = isDarkMode,
            isBusy = authBusy,
            authError = authError,
            onToggleTheme = viewModel::toggleDarkMode,
            onClaimAdmin = viewModel::claimInitialAdmin,
            onSignIn = viewModel::signIn,
            onRegisterStudent = viewModel::registerStudent,
            onClearError = viewModel::clearAuthError
        )
    } else {
        key(activeSession.userId, activeSession.role) {
            AuthenticatedWorkspace(
                session = activeSession,
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                customLogoPath = customLogoPath
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedWorkspace(
    session: SessionContext,
    viewModel: MainViewModel,
    isDarkMode: Boolean,
    customLogoPath: String?
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val tabs = remember(session.role) {
        if (session.role == UserRole.ADMIN) {
            listOf(AppTab.DASHBOARD, AppTab.JOURNAL, AppTab.DISCIPLINE, AppTab.STUDENTS, AppTab.REPORTS)
        } else {
            listOf(AppTab.DASHBOARD, AppTab.JOURNAL, AppTab.DISCIPLINE, AppTab.REPORTS)
        }
    }
    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    if (currentTab !in tabs) currentTab = AppTab.DASHBOARD

    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        currentTab = AppTab.DASHBOARD
    }

    val scope by viewModel.dashboardScope.collectAsStateWithLifecycle()
    val personalTrades by viewModel.personalTrades.collectAsStateWithLifecycle()
    val allAcademyTrades by viewModel.allAcademyTrades.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val personalMetrics by viewModel.personalMetrics.collectAsStateWithLifecycle()
    val combinedMetrics by viewModel.combinedAcademyMetrics.collectAsStateWithLifecycle()
    val disciplineProfile by viewModel.disciplineProfile.collectAsStateWithLifecycle()
    val disciplineSummary by viewModel.personalDiscipline.collectAsStateWithLifecycle()
    val disciplineReviews by viewModel.disciplineReviews.collectAsStateWithLifecycle()
    val allReviewsForAdmin by viewModel.allReviewsForAdmin.collectAsStateWithLifecycle()
    val studentSummaries by viewModel.studentSummaries.collectAsStateWithLifecycle()

    fun tabIcon(tab: AppTab) = when (tab) {
        AppTab.DASHBOARD -> Icons.Filled.Dashboard
        AppTab.JOURNAL -> Icons.Filled.ReceiptLong
        AppTab.DISCIPLINE -> Icons.Filled.Psychology
        AppTab.STUDENTS -> Icons.Filled.Group
        AppTab.REPORTS -> Icons.Filled.Assessment
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandLogoAvatar(customLogoPath = customLogoPath, size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Trading with Bangali Babu",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${session.fullName} • ${session.role.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalGold,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::toggleDarkMode) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = RoyalGold
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        tabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            session = session,
                            scope = scope,
                            onScopeChange = viewModel::setDashboardScope,
                            personalMetrics = personalMetrics,
                            combinedMetrics = combinedMetrics,
                            studentCount = students.size,
                            onQuickAddTrade = { currentTab = AppTab.JOURNAL }
                        )
                        AppTab.JOURNAL -> JournalScreen(
                            session = session,
                            trades = personalTrades,
                            onSaveTrade = viewModel::saveTrade,
                            onDeleteTrade = viewModel::deleteTrade,
                            onImportScreenshot = viewModel::importScreenshot,
                            onShowMessage = viewModel::emitMessage
                        )
                        AppTab.DISCIPLINE -> DisciplineScreen(
                            session = session,
                            profile = disciplineProfile,
                            summary = disciplineSummary,
                            reviews = disciplineReviews,
                            onSaveProfile = viewModel::saveDisciplineProfile,
                            onSaveReview = viewModel::saveDisciplineReview,
                            onDeleteReview = viewModel::deleteDisciplineReview
                        )
                        AppTab.STUDENTS -> AdminStudentsScreen(
                            summaries = studentSummaries,
                            allTrades = allAcademyTrades,
                            allReviews = allReviewsForAdmin,
                            onAddStudent = viewModel::adminCreateStudent,
                            onEditStudent = viewModel::adminUpdateStudent,
                            onToggleStatus = viewModel::adminToggleStudentStatus,
                            onDeleteStudent = viewModel::adminDeleteStudent,
                            onSaveTradeFeedback = viewModel::saveTradeAdminFeedback
                        )
                        AppTab.REPORTS -> ReportsAndSettingsScreen(
                            session = session,
                            personalTrades = personalTrades,
                            allAcademyTrades = allAcademyTrades,
                            students = students,
                            isDarkMode = isDarkMode,
                            customLogoPath = customLogoPath,
                            onToggleTheme = viewModel::toggleDarkMode,
                            onUploadLogo = viewModel::uploadCustomBrandLogo,
                            onResetLogo = viewModel::resetCustomBrandLogo,
                            onExportPdf = { uri, title, sc, per, list ->
                                viewModel.exportPdfToUri(context, uri, title, sc, per, list)
                            },
                            onExportCsv = { uri, title, sc, per, list ->
                                viewModel.exportCsvToUri(context, uri, title, sc, per, list)
                            },
                            onSharePdf = { title, sc, per, list ->
                                viewModel.sharePdfReport(context, title, sc, per, list)
                            },
                            onShareCsv = { title, sc, per, list ->
                                viewModel.shareCsvReport(context, title, sc, per, list)
                            },
                            onBackupJson = { uri -> viewModel.exportBackupJson(context, uri) },
                            onRestoreJson = { uri -> viewModel.restoreBackupJson(context, uri) },
                            onSignOut = viewModel::signOut
                        )
                    }
                }
            }
        }
    }
}
