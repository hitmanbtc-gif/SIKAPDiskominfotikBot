package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.ActivityReport
import com.example.data.model.ReportStatus
import com.example.data.repository.LapkinRepository
import com.example.ui.auth.AuthHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PdfPreviewScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.ReportFormScreen
import com.example.ui.screens.ReportListScreen
import com.example.ui.screens.TelegramBotScreen
import com.example.ui.screens.VerificationQueueScreen
import com.example.ui.theme.LapkinKominfoTheme
import com.example.ui.viewmodel.LapkinViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.PdfReportGenerator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File

private const val ROUTE_BOT_TG = "bot_tg"
private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_REPORTS = "reports"
private const val ROUTE_VERIFICATION = "verification"
private const val ROUTE_ADMIN = "admin_settings"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_FORM = "report_form"
private const val ROUTE_DETAIL = "report_detail"
private const val ROUTE_PDF_PREVIEW = "pdf_preview"

private data class TopNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LapkinKominfoTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = user.uid
        val email = user.email.orEmpty()
        val displayName = user.displayName.orEmpty()
        key(uid) {
            AuthenticatedLapkinApp(
                currentUserId = uid,
                currentUserEmail = email,
                currentUserDisplayName = displayName
            )
        }
    }
}

@Composable
private fun AuthenticatedLapkinApp(
    currentUserId: String,
    currentUserEmail: String,
    currentUserDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: LapkinViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LapkinViewModel(
                    repository = LapkinRepository(db),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    currentUserDisplayName = currentUserDisplayName
                )
            }
        }
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val adminConfig by viewModel.adminConfigState.collectAsStateWithLifecycle()
    val reportsState by viewModel.myReportsState.collectAsStateWithLifecycle()
    val queueState by viewModel.reviewQueueState.collectAsStateWithLifecycle()
    val selectedClass by viewModel.selectedClassificationFilter.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val profile = (profileState as? UiState.Success)?.data
    val myReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val queueReports = (queueState as? UiState.Success)?.data.orEmpty()
    val combinedSupervisionReports = remember(myReports, queueReports) {
        (queueReports + myReports).distinctBy { it.id }
    }
    val pendingCount = queueReports.count {
        val st = it.statusEnum()
        st == ReportStatus.SUBMITTED_PENGAWAS ||
            st == ReportStatus.SUBMITTED_ADMINISTRATOR ||
            st == ReportStatus.SUBMITTED_KADIS
    }

    val topNavItems = listOf(
        TopNavItem(
            route = ROUTE_BOT_TG,
            label = stringResource(id = R.string.nav_bot_tg),
            selectedIcon = Icons.AutoMirrored.Filled.Send,
            unselectedIcon = Icons.AutoMirrored.Outlined.Send,
            testTag = "nav_bot_tg"
        ),
        TopNavItem(
            route = ROUTE_DASHBOARD,
            label = stringResource(id = R.string.nav_beranda),
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_dashboard"
        ),
        TopNavItem(
            route = ROUTE_VERIFICATION,
            label = stringResource(id = R.string.nav_verifikasi),
            selectedIcon = Icons.Filled.FactCheck,
            unselectedIcon = Icons.Outlined.FactCheck,
            testTag = "nav_verification"
        ),
        TopNavItem(
            route = ROUTE_ADMIN,
            label = stringResource(id = R.string.nav_admin),
            selectedIcon = Icons.Filled.AdminPanelSettings,
            unselectedIcon = Icons.Outlined.AdminPanelSettings,
            testTag = "nav_admin"
        ),
        TopNavItem(
            route = ROUTE_PROFILE,
            label = stringResource(id = R.string.nav_profil),
            selectedIcon = Icons.Filled.Badge,
            unselectedIcon = Icons.Outlined.Badge,
            testTag = "nav_profile"
        )
    )

    val isTopLevelDestination = currentRoute in topNavItems.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun exportReportsToPdf(reportsToExport: List<ActivityReport>, periodLabel: String) {
        if (reportsToExport.isEmpty()) {
            viewModel.showFeedback("Belum ada laporan kegiatan untuk diekspor ke PDF.")
            return
        }
        scope.launch {
            PdfReportGenerator.generatePdf(
                context = context,
                profile = profile,
                reports = reportsToExport,
                periodLabel = periodLabel,
                adminConfig = adminConfig
            ).fold(
                onSuccess = { file ->
                    generatedPdfFile = file
                    navController.navigate(ROUTE_PDF_PREVIEW)
                },
                onFailure = { err ->
                    viewModel.showFeedback(err.localizedMessage ?: "Gagal membuat file PDF.")
                }
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (isTopLevelDestination && (currentRoute == ROUTE_DASHBOARD || currentRoute == ROUTE_REPORTS)) {
                    FloatingActionButton(
                        onClick = { navController.navigate("$ROUTE_FORM?reportId=") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_activity")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(id = R.string.action_add_activity))
                    }
                }
            },
            bottomBar = {
                if (isTopLevelDestination && !useNavRail) {
                    NavigationBar(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        topNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    if (item.route == ROUTE_VERIFICATION && pendingCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
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
                if (isTopLevelDestination && useNavRail) {
                    NavigationRail {
                        topNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    if (item.route == ROUTE_VERIFICATION && pendingCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_BOT_TG,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_BOT_TG) {
                        TelegramBotScreen(
                            profileState = profileState,
                            reportsState = reportsState,
                            reviewQueueState = queueState,
                            adminConfig = adminConfig,
                            currentUserEmail = currentUserEmail,
                            currentUserDisplayName = currentUserDisplayName,
                            isBusy = isBusy,
                            onSaveOrSubmitReport = { draft, isSubmit, onDone ->
                                viewModel.saveOrSubmitReport(draft, isSubmit, onDone)
                            },
                            onReviewDecision = { target, approve, note, forwardToNextTier ->
                                val reviewerName = profile?.fullName?.ifBlank { currentUserDisplayName }
                                    ?: currentUserDisplayName
                                viewModel.reviewReport(
                                    report = target,
                                    reviewerName = reviewerName,
                                    approve = approve,
                                    note = note,
                                    forwardToNextTier = forwardToNextTier
                                )
                            },
                            onExportPdf = { list, label ->
                                exportReportsToPdf(list, label)
                            },
                            onOpenDetail = { id ->
                                navController.navigate("$ROUTE_DETAIL/$id")
                            },
                            onOpenProfileSetup = {
                                navigateTopLevel(ROUTE_PROFILE)
                            },
                            onOpenAdminSettings = {
                                navigateTopLevel(ROUTE_ADMIN)
                            }
                        )
                    }

                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            profileState = profileState,
                            reportsState = reportsState,
                            reviewQueueState = queueState,
                            userDisplayName = currentUserDisplayName,
                            onCreateReport = { navigateTopLevel(ROUTE_BOT_TG) },
                            onOpenProfile = { navigateTopLevel(ROUTE_PROFILE) },
                            onOpenReport = { id -> navController.navigate("$ROUTE_DETAIL/$id") },
                            onOpenAllReports = { cls ->
                                viewModel.setClassificationFilter(cls)
                                navController.navigate(ROUTE_REPORTS)
                            },
                            onOpenVerificationQueue = { navigateTopLevel(ROUTE_VERIFICATION) },
                            onExportPdf = { list ->
                                exportReportsToPdf(list, "Rekap Aktivitas Harian ASN")
                            },
                            onDeleteReport = { rep ->
                                viewModel.deleteDraft(rep.id)
                            }
                        )
                    }

                    composable(ROUTE_REPORTS) {
                        ReportListScreen(
                            reportsState = reportsState,
                            selectedClassification = selectedClass,
                            selectedStatus = selectedStatus,
                            searchQuery = searchQuery,
                            onSelectClassification = viewModel::setClassificationFilter,
                            onSelectStatus = viewModel::setStatusFilter,
                            onSearchChange = viewModel::setSearchQuery,
                            onOpenReport = { id -> navController.navigate("$ROUTE_DETAIL/$id") },
                            onCreateReport = { navigateTopLevel(ROUTE_BOT_TG) },
                            onExportPdf = { list ->
                                exportReportsToPdf(list, "Rekap Laporan Terfilter")
                            },
                            onDeleteReport = { rep ->
                                viewModel.deleteDraft(rep.id)
                            }
                        )
                    }

                    composable(ROUTE_VERIFICATION) {
                        VerificationQueueScreen(
                            queueState = queueState,
                            isBusy = isBusy,
                            onOpenReport = { id -> navController.navigate("$ROUTE_DETAIL/$id") },
                            onQuickReviewDecision = { target, approve, note, forwardToNextTier ->
                                val reviewerName = profile?.fullName?.ifBlank { currentUserDisplayName }
                                    ?: currentUserDisplayName
                                viewModel.reviewReport(
                                    report = target,
                                    reviewerName = reviewerName,
                                    approve = approve,
                                    note = note,
                                    forwardToNextTier = forwardToNextTier
                                )
                            }
                        )
                    }

                    composable(ROUTE_ADMIN) {
                        AdminSettingsScreen(
                            adminConfig = adminConfig,
                            profile = profile,
                            allReports = combinedSupervisionReports,
                            currentUserEmail = currentUserEmail,
                            currentUserDisplayName = currentUserDisplayName,
                            isBusy = isBusy,
                            onSaveAdminConfig = { updated ->
                                viewModel.saveAdminBotConfig(updated)
                            },
                            onAdminReviewDecision = { target, approve, note, forceFinal ->
                                val reviewerName = adminConfig.kadisFullName.ifBlank {
                                    profile?.fullName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
                                }
                                viewModel.reviewReport(
                                    report = target,
                                    reviewerName = reviewerName,
                                    approve = approve,
                                    note = note,
                                    forceFinalApproveAsAdmin = forceFinal
                                )
                            },
                            onExportAllPdf = { list, label ->
                                exportReportsToPdf(list, label)
                            },
                            onOpenReportDetail = { id ->
                                navController.navigate("$ROUTE_DETAIL/$id")
                            }
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            profileState = profileState,
                            adminConfig = adminConfig,
                            currentUserEmail = currentUserEmail,
                            currentUserDisplayName = currentUserDisplayName,
                            isBusy = isBusy,
                            onSaveProfile = { updatedProfile ->
                                viewModel.saveUserProfile(updatedProfile)
                            },
                            onOpenAdminSettings = {
                                navigateTopLevel(ROUTE_ADMIN)
                            },
                            onSignOut = {
                                AuthHelper.signOut(credentialManager = credentialManager, scope = scope)
                            }
                        )
                    }

                    composable(
                        route = "$ROUTE_FORM?reportId={reportId}",
                        arguments = listOf(
                            navArgument("reportId") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val reportId = backStackEntry.arguments?.getString("reportId").orEmpty()
                        val existing = myReports.firstOrNull { it.id == reportId }
                        ReportFormScreen(
                            existingReport = existing,
                            profile = profile,
                            currentUserEmail = currentUserEmail,
                            currentUserDisplayName = currentUserDisplayName,
                            isBusy = isBusy,
                            onBack = { navController.popBackStack() },
                            onSaveOrSubmit = { draft, isSubmit ->
                                viewModel.saveOrSubmitReport(draft, isSubmit) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable(
                        route = "$ROUTE_DETAIL/{reportId}",
                        arguments = listOf(navArgument("reportId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val reportId = backStackEntry.arguments?.getString("reportId").orEmpty()
                        val found = myReports.firstOrNull { it.id == reportId }
                            ?: queueReports.firstOrNull { it.id == reportId }
                        ReportDetailScreen(
                            report = found,
                            currentUserId = currentUserId,
                            currentUserEmail = currentUserEmail,
                            reviewerDisplayName = profile?.fullName?.ifBlank { currentUserDisplayName }
                                ?: currentUserDisplayName,
                            isBusy = isBusy,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate("$ROUTE_FORM?reportId=$id") },
                            onDelete = { id ->
                                viewModel.deleteDraft(id) {
                                    navController.popBackStack()
                                }
                            },
                            onExportPdf = { singleReport ->
                                exportReportsToPdf(
                                    listOf(singleReport),
                                    "Tanggal ${singleReport.activityDate}"
                                )
                            },
                            onReviewDecision = { target, approve, note, forwardToNextTier ->
                                val reviewerName = profile?.fullName?.ifBlank { currentUserDisplayName }
                                    ?: currentUserDisplayName
                                viewModel.reviewReport(
                                    report = target,
                                    reviewerName = reviewerName,
                                    approve = approve,
                                    note = note,
                                    forwardToNextTier = forwardToNextTier
                                )
                            }
                        )
                    }

                    composable(ROUTE_PDF_PREVIEW) {
                        PdfPreviewScreen(
                            pdfFile = generatedPdfFile,
                            onBack = { navController.popBackStack() },
                            onFeedback = { msg -> viewModel.showFeedback(msg) }
                        )
                    }
                }
            }
        }
    }
}
