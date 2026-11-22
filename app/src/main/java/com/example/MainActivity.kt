package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.TournamentRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.preferences.AccountPreferencesStore
import com.example.notifications.ArenaNotificationHelper
import com.example.notifications.TournamentReminderScheduler
import com.example.ui.components.AdminPasscodeDialog
import com.example.ui.components.ArenaTopBar
import com.example.data.model.RegistrationStatus
import com.example.data.model.WalletRequestStatus
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MatchDetailScreen
import com.example.ui.screens.MatchesScreen
import com.example.ui.screens.MyMatchesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.BlazeZoneTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TournamentViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val ROUTE_MATCHES = "matches"
private const val ROUTE_MY_MATCHES = "my_matches"
private const val ROUTE_WALLET = "wallet"
private const val ROUTE_WINNERS = "winners"
private const val ROUTE_PROFILE = "profile"

private data class ArenaTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val ARENA_TABS = listOf(
    ArenaTab(ROUTE_MATCHES, "Matches", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    ArenaTab(ROUTE_MY_MATCHES, "My Matches", Icons.Filled.ConfirmationNumber, Icons.Outlined.ConfirmationNumber),
    ArenaTab(ROUTE_WALLET, "Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    ArenaTab(ROUTE_WINNERS, "Leaderboard", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    ArenaTab(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

private data class NotificationLaunchRequest(
    val openTab: String? = null,
    val tournamentId: String? = null,
    val sequence: Long = 0L
)

class MainActivity : ComponentActivity() {
    private val launchRequestFlow = MutableStateFlow(NotificationLaunchRequest())
    private var launchSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArenaNotificationHelper.ensureNotificationChannels(this)
        dispatchIntentExtras(intent)
        enableEdgeToEdge()
        setContent {
            val launchRequest by launchRequestFlow.collectAsStateWithLifecycle()
            AppContent(launchRequest = launchRequest)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchIntentExtras(intent)
    }

    private fun dispatchIntentExtras(intent: Intent?) {
        if (intent == null) return
        val openTab = intent.getStringExtra(ArenaNotificationHelper.EXTRA_OPEN_TAB)
        val tournamentId = intent.getStringExtra(ArenaNotificationHelper.EXTRA_TOURNAMENT_ID)
        if (!openTab.isNullOrBlank() || !tournamentId.isNullOrBlank()) {
            launchSequence += 1L
            launchRequestFlow.value = NotificationLaunchRequest(
                openTab = openTab,
                tournamentId = tournamentId,
                sequence = launchSequence
            )
            intent.removeExtra(ArenaNotificationHelper.EXTRA_OPEN_TAB)
            intent.removeExtra(ArenaNotificationHelper.EXTRA_TOURNAMENT_ID)
        }
    }
}

@Composable
private fun AppContent(launchRequest: NotificationLaunchRequest = NotificationLaunchRequest()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val user = currentUser
    if (user == null) {
        BlazeZoneTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            val viewModel: TournamentViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        TournamentViewModel(
                            repository = TournamentRepository(db),
                            currentUserId = user.uid,
                            displayName = user.displayName,
                            email = user.email,
                            preferencesStore = AccountPreferencesStore(app.applicationContext),
                            appContext = app.applicationContext
                        )
                    }
                }
            )
            AuthenticatedArenaShell(
                userId = user.uid,
                viewModel = viewModel,
                launchRequest = launchRequest,
                onSignOut = {
                    val appContext = context.applicationContext
                    scope.launch {
                        runCatching {
                            val store = AccountPreferencesStore(appContext)
                            val subscribed = store.getSubscribedTournamentTopics()
                            TournamentRepository(
                                FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                            ).unsubscribeTournamentTopics(subscribed)
                            store.setSubscribedTournamentTopics(emptySet())
                            store.setJoinedTournamentIds(emptySet())
                            TournamentReminderScheduler.cancelAllReminders(appContext, store)
                        }
                        signOut(credentialManager = credentialManager, scope = scope)
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedArenaShell(
    userId: String,
    viewModel: TournamentViewModel,
    launchRequest: NotificationLaunchRequest,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val appConfig by viewModel.appConfigState.collectAsStateWithLifecycle()
    val activeCategoriesState by viewModel.activeCategoriesState.collectAsStateWithLifecycle()
    val allCategoriesState by viewModel.allCategoriesState.collectAsStateWithLifecycle()
    val tournamentsState by viewModel.tournamentsState.collectAsStateWithLifecycle()
    val myRegistrationsState by viewModel.myRegistrationsState.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val leaderboardSortMetric by viewModel.leaderboardSortMetric.collectAsStateWithLifecycle()
    val selectedResultTournamentId by viewModel.selectedResultTournamentId.collectAsStateWithLifecycle()
    val selectedResultRosterState by viewModel.selectedResultRosterState.collectAsStateWithLifecycle()
    val activeRosterState by viewModel.activeRosterState.collectAsStateWithLifecycle()
    val adminRegistrationsState by viewModel.adminTournamentRegistrationsState.collectAsStateWithLifecycle()
    val myWalletRequestsState by viewModel.myWalletRequestsState.collectAsStateWithLifecycle()
    val adminWalletRequestsState by viewModel.adminWalletRequestsState.collectAsStateWithLifecycle()
    val activeRoomSecret by viewModel.activeRoomSecretState.collectAsStateWithLifecycle()
    val accountSettings by viewModel.accountSettingsState.collectAsStateWithLifecycle()

    val adminUnlocked by viewModel.adminUnlocked.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTeamTypeFilter by viewModel.selectedTeamTypeFilter.collectAsStateWithLifecycle()
    val activeOnlySearch by viewModel.activeOnlySearch.collectAsStateWithLifecycle()
    val activeTournamentId by viewModel.activeTournamentId.collectAsStateWithLifecycle()
    val roomSecretsCache by viewModel.roomSecretsCache.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val pendingNavTab by viewModel.pendingNavTab.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var notificationPermissionGranted by remember {
        mutableStateOf(ArenaNotificationHelper.hasPostNotificationPermission(context))
    }
    var requestedPermissionOnce by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
        if (granted) {
            viewModel.setPushNotificationsEnabled(true)
        }
    }

    LaunchedEffect(accountSettings.pushNotificationsEnabled, accountSettings.matchRemindersEnabled) {
        notificationPermissionGranted = ArenaNotificationHelper.hasPostNotificationPermission(context)
        if (!requestedPermissionOnce &&
            (accountSettings.pushNotificationsEnabled || accountSettings.matchRemindersEnabled) &&
            !notificationPermissionGranted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            requestedPermissionOnce = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(launchRequest.sequence) {
        if (launchRequest.sequence > 0L) {
            viewModel.handleNotificationLaunch(launchRequest.openTab, launchRequest.tournamentId)
        }
    }

    val pendingWalletCount = if (adminUnlocked) {
        (adminWalletRequestsState as? UiState.Success)?.data
            ?.count { it.status == WalletRequestStatus.PENDING.name } ?: 0
    } else 0

    var showAdminDashboard by remember { mutableStateOf(false) }
    var showAdminPasscodeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.notificationEvents.collect { payload ->
            ArenaNotificationHelper.showNotification(context, payload)
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    fun requestAdminEntry() {
        if (adminUnlocked) {
            showAdminDashboard = !showAdminDashboard
        } else {
            showAdminPasscodeDialog = true
        }
    }

    BlazeZoneTheme(accentTheme = appConfig.accentTheme) {
        val tournamentsList = (tournamentsState as? UiState.Success)?.data ?: emptyList()
        val activeMatch = activeTournamentId?.let { tid -> tournamentsList.firstOrNull { it.id == tid } }
        val myRegsList = (myRegistrationsState as? UiState.Success)?.data ?: emptyList()

        Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
            when {
                showAdminDashboard && adminUnlocked -> {
                    Scaffold(
                        containerColor = ObsidianBg,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    ) { innerPadding ->
                        AdminDashboardScreen(
                            config = appConfig,
                            tournamentsState = tournamentsState,
                            categoriesState = allCategoriesState,
                            adminRegistrationsState = adminRegistrationsState,
                            adminWalletRequestsState = adminWalletRequestsState,
                            roomSecretsCache = roomSecretsCache,
                            isBusy = isBusy,
                            onSeedStarterPack = { viewModel.seedStarterPack() },
                            onSaveConfig = { viewModel.saveAppConfig(it) },
                            onSaveCategory = { cat, done -> viewModel.saveCategory(cat, done) },
                            onDeleteCategory = { viewModel.deleteCategory(it) },
                            onSaveTournament = { match, rId, rPass, done ->
                                viewModel.saveTournament(match, rId, rPass, done)
                            },
                            onUpdateRoom = { tid, rId, rPass, pub, done ->
                                viewModel.updateRoomCredentials(tid, rId, rPass, pub, done)
                            },
                            onDeleteTournament = { viewModel.deleteTournament(it) },
                            onLoadRoomSecret = { viewModel.loadRoomSecret(it) },
                            onSelectMatchForRoster = { viewModel.selectAdminTournamentForRoster(it) },
                            onSaveParticipantResult = { reg, st, kills, rank, prize, paid ->
                                viewModel.updateParticipantResult(reg, st, kills, rank, prize, paid)
                            },
                            onProcessWalletRequest = { req, approve, note ->
                                viewModel.processWalletRequest(req, approve, note)
                            },
                            onCopyFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                            onLockAdmin = { viewModel.lockAdminMode() },
                            onBack = { showAdminDashboard = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
                activeMatch != null -> {
                    val myReg = myRegsList.firstOrNull { it.tournamentId == activeMatch.id }
                    val hasActiveBuyerAccess = myReg != null &&
                        !myReg.status.equals(RegistrationStatus.REJECTED.name, ignoreCase = true)
                    val profile = (userProfileState as? UiState.Success)?.data
                    Box(modifier = Modifier.fillMaxSize()) {
                        MatchDetailScreen(
                            match = activeMatch,
                            config = appConfig,
                            myRegistration = myReg,
                            roomSecret = if (hasActiveBuyerAccess || adminUnlocked) {
                                activeRoomSecret ?: roomSecretsCache[activeMatch.id]
                            } else null,
                            rosterState = activeRosterState,
                            userProfile = profile,
                            autoFillIdentity = accountSettings.autoFillIdentity,
                            isBusy = isBusy,
                            onLoadRoomSecret = { viewModel.loadRoomSecret(activeMatch.id) },
                            onJoinMatch = { ign, uid, squad, payRef, onSuccess ->
                                viewModel.joinTournament(activeMatch, ign, uid, squad, payRef, profile, onSuccess)
                            },
                            onCopyFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                            onBack = { viewModel.openTournamentDetail(null) }
                        )
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = 80.dp)
                        )
                    }
                }
                else -> {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_MATCHES

                    fun navigateTab(route: String) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    LaunchedEffect(pendingNavTab) {
                        val target = when (pendingNavTab) {
                            ArenaNotificationHelper.TAB_WALLET -> ROUTE_WALLET
                            ArenaNotificationHelper.TAB_MY_MATCHES -> ROUTE_MY_MATCHES
                            ArenaNotificationHelper.TAB_MATCHES -> ROUTE_MATCHES
                            else -> null
                        }
                        if (target != null) {
                            navigateTab(target)
                            viewModel.consumePendingNavTab()
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
                        val useRail = maxWidth >= 660.dp

                        Scaffold(
                            containerColor = ObsidianBg,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            topBar = {
                                ArenaTopBar(
                                    config = appConfig,
                                    adminUnlocked = adminUnlocked,
                                    inAdminPanel = showAdminDashboard,
                                    pendingWalletCount = pendingWalletCount,
                                    onAdminButtonClick = { requestAdminEntry() },
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                                )
                            },
                            bottomBar = {
                                if (!useRail) {
                                    NavigationBar(
                                        containerColor = ObsidianSurface,
                                        windowInsets = WindowInsets.navigationBars
                                    ) {
                                        ARENA_TABS.forEach { tab ->
                                            val selected = currentRoute == tab.route
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTab(tab.route) },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                ),
                                                modifier = Modifier.testTag("nav_tab_${tab.route}")
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
                                if (useRail) {
                                    NavigationRail(
                                        containerColor = ObsidianSurface,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        ARENA_TABS.forEach { tab ->
                                            val selected = currentRoute == tab.route
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { navigateTab(tab.route) },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                                colors = NavigationRailItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                )
                                            )
                                        }
                                    }
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_MATCHES,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    composable(ROUTE_MATCHES) {
                                        val profile = (userProfileState as? UiState.Success)?.data
                                        MatchesScreen(
                                            config = appConfig,
                                            categoriesState = activeCategoriesState,
                                            tournamentsState = tournamentsState,
                                            myRegistrationsState = myRegistrationsState,
                                            selectedCategoryId = selectedCategoryId,
                                            selectedStatus = selectedStatus,
                                            searchQuery = searchQuery,
                                            selectedTeamType = selectedTeamTypeFilter,
                                            activeOnlySearch = activeOnlySearch,
                                            adminUnlocked = adminUnlocked,
                                            showCountdowns = accountSettings.showMatchCountdowns,
                                            userProfile = profile,
                                            autoFillIdentity = accountSettings.autoFillIdentity,
                                            isBusy = isBusy,
                                            onSelectCategory = { viewModel.setCategoryFilter(it) },
                                            onSelectStatus = { viewModel.setStatusFilter(it) },
                                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                            onSelectTeamType = { viewModel.setTeamTypeFilter(it) },
                                            onToggleActiveOnlySearch = { viewModel.setActiveOnlySearch(it) },
                                            onClearAllFilters = { viewModel.clearAllMatchFilters() },
                                            onOpenMatch = { viewModel.openTournamentDetail(it.id) },
                                            onJoinMatch = { match, ign, uid, squad, payRef, done ->
                                                viewModel.joinTournament(match, ign, uid, squad, payRef, profile, done)
                                            },
                                            onOpenAdmin = { requestAdminEntry() }
                                        )
                                    }
                                    composable(ROUTE_MY_MATCHES) {
                                        MyMatchesScreen(
                                            config = appConfig,
                                            registrationsState = myRegistrationsState,
                                            tournamentsState = tournamentsState,
                                            roomSecretsCache = roomSecretsCache,
                                            showCountdowns = accountSettings.showMatchCountdowns,
                                            highlightRoomReady = accountSettings.roomReminderHighlights,
                                            matchRemindersEnabled = accountSettings.matchRemindersEnabled && notificationPermissionGranted,
                                            onLoadRoomSecret = { viewModel.loadRoomSecret(it) },
                                            onOpenMatch = { viewModel.openTournamentDetail(it.id) },
                                            onCopyFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                            onExploreMatches = { navigateTab(ROUTE_MATCHES) }
                                        )
                                    }
                                    composable(ROUTE_WALLET) {
                                        val profile = (userProfileState as? UiState.Success)?.data
                                        WalletScreen(
                                            config = appConfig,
                                            profileState = userProfileState,
                                            walletRequestsState = myWalletRequestsState,
                                            isBusy = isBusy,
                                            onSubmitAddMoney = { method, amount, sender, txnId, done ->
                                                viewModel.submitAddMoneyRequest(profile, method, amount, sender, txnId, done)
                                            },
                                            onSubmitWithdraw = { method, accountNum, amount, done ->
                                                viewModel.submitWithdrawRequest(profile, method, accountNum, amount, done)
                                            },
                                            onCopyFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                        )
                                    }
                                    composable(ROUTE_WINNERS) {
                                        LeaderboardScreen(
                                            config = appConfig,
                                            currentUserId = userId,
                                            leaderboardState = leaderboardState,
                                            sortMetric = leaderboardSortMetric,
                                            tournamentsState = tournamentsState,
                                            selectedResultTournamentId = selectedResultTournamentId,
                                            selectedResultRosterState = selectedResultRosterState,
                                            onSelectSortMetric = { viewModel.setLeaderboardSortMetric(it) },
                                            onSelectResultTournament = { viewModel.selectResultTournament(it) },
                                            onOpenMatch = { viewModel.openTournamentDetail(it.id) }
                                        )
                                    }
                                    composable(ROUTE_PROFILE) {
                                        val profile = (userProfileState as? UiState.Success)?.data
                                        val top10List = (leaderboardState as? UiState.Success)?.data ?: emptyList()
                                        val myTop10Rank = top10List.indexOfFirst { it.userId == userId }
                                            .takeIf { it >= 0 }
                                            ?.plus(1)
                                        ProfileScreen(
                                            config = appConfig,
                                            userId = userId,
                                            profileState = userProfileState,
                                            registrationsState = myRegistrationsState,
                                            tournamentsState = tournamentsState,
                                            leaderboardTop10Rank = myTop10Rank,
                                            accountSettings = accountSettings,
                                            notificationPermissionGranted = notificationPermissionGranted,
                                            adminUnlocked = adminUnlocked,
                                            isBusy = isBusy,
                                            onSaveIdentity = { name, ign, uid, rank ->
                                                viewModel.updateProfileIdentity(name, ign, uid, rank)
                                            },
                                            onUpdateInGameRank = { rank ->
                                                viewModel.updateInGameRank(rank, profile)
                                            },
                                            onToggleAutoFill = { viewModel.setAutoFillIdentity(it) },
                                            onToggleCountdowns = { viewModel.setShowMatchCountdowns(it) },
                                            onToggleHaptics = { viewModel.setHapticFeedbackEnabled(it) },
                                            onToggleRoomHighlights = { viewModel.setRoomReminderHighlights(it) },
                                            onTogglePushNotifications = { enabled ->
                                                if (enabled && !ArenaNotificationHelper.hasPostNotificationPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                                viewModel.setPushNotificationsEnabled(enabled)
                                            },
                                            onToggleMatchReminders = { enabled ->
                                                if (enabled && !ArenaNotificationHelper.hasPostNotificationPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                                viewModel.setMatchRemindersEnabled(enabled)
                                            },
                                            onOpenWallet = { navigateTab(ROUTE_WALLET) },
                                            onOpenMatch = { viewModel.openTournamentDetail(it.id) },
                                            onExploreMatches = { navigateTab(ROUTE_MATCHES) },
                                            onOpenAdmin = { requestAdminEntry() },
                                            onLockAdmin = { viewModel.lockAdminMode() },
                                            onCopyFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                            onSignOut = onSignOut
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAdminPasscodeDialog) {
                AdminPasscodeDialog(
                    isBusy = isBusy,
                    onDismiss = { showAdminPasscodeDialog = false },
                    onVerify = { passcode, onError ->
                        viewModel.verifyAndUnlockAdmin(
                            passcode = passcode,
                            onSuccess = {
                                showAdminPasscodeDialog = false
                                showAdminDashboard = true
                            },
                            onError = onError
                        )
                    }
                )
            }
        }
    }
}
