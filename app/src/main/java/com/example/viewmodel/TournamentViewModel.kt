package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.TournamentRepository
import com.example.data.model.AppConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LeaderboardSortMetric
import com.example.data.model.MatchCategory
import com.example.data.model.MatchStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Registration
import com.example.data.model.RegistrationStatus
import com.example.data.model.RoomSecret
import com.example.data.model.RosterEntry
import com.example.data.model.TeamType
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.model.WalletRequest
import com.example.data.model.WalletRequestStatus
import com.example.data.preferences.AccountPreferencesStore
import com.example.data.preferences.AccountSettings
import com.example.notifications.ArenaNotificationHelper
import com.example.notifications.ParsedNotificationPayload
import com.example.notifications.TournamentReminderScheduler
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class TournamentViewModel(
    private val repository: TournamentRepository,
    private val currentUserId: String,
    private val displayName: String?,
    private val email: String?,
    private val preferencesStore: AccountPreferencesStore? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _adminUnlocked = MutableStateFlow(false)
    val adminUnlocked: StateFlow<Boolean> = _adminUnlocked.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String?>(null)
    val selectedStatusFilter: StateFlow<String?> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTeamTypeFilter = MutableStateFlow<TeamType?>(null)
    val selectedTeamTypeFilter: StateFlow<TeamType?> = _selectedTeamTypeFilter.asStateFlow()

    private val _activeOnlySearch = MutableStateFlow(true)
    val activeOnlySearch: StateFlow<Boolean> = _activeOnlySearch.asStateFlow()

    private val _activeTournamentId = MutableStateFlow<String?>(null)
    val activeTournamentId: StateFlow<String?> = _activeTournamentId.asStateFlow()

    private val _adminRegTournamentId = MutableStateFlow<String?>(null)

    private val _roomSecretsCache = MutableStateFlow<Map<String, RoomSecret>>(emptyMap())
    val roomSecretsCache: StateFlow<Map<String, RoomSecret>> = _roomSecretsCache.asStateFlow()

    private val _leaderboardSortMetric = MutableStateFlow(LeaderboardSortMetric.WINNINGS)
    val leaderboardSortMetric: StateFlow<LeaderboardSortMetric> = _leaderboardSortMetric.asStateFlow()

    private val _selectedResultTournamentId = MutableStateFlow<String?>(null)
    val selectedResultTournamentId: StateFlow<String?> = _selectedResultTournamentId.asStateFlow()

    private val _pendingNavTab = MutableStateFlow<String?>(null)
    val pendingNavTab: StateFlow<String?> = _pendingNavTab.asStateFlow()

    private val _notificationEvents = MutableSharedFlow<ParsedNotificationPayload>(extraBufferCapacity = 16)
    val notificationEvents: SharedFlow<ParsedNotificationPayload> = _notificationEvents.asSharedFlow()

    private fun <T> Flow<T>.toUiStateFlow(tagLabel: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tagLabel", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to sync $tagLabel"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val appConfigState: StateFlow<AppConfig> = repository.observeAppConfig(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing app_config", firestoreError ?: error)
            emit(AppConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppConfig()
        )

    val activeCategoriesState: StateFlow<UiState<List<MatchCategory>>> =
        repository.observeActiveCategories(currentUserId).toUiStateFlow("categories")

    val allCategoriesState: StateFlow<UiState<List<MatchCategory>>> =
        _adminUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) flowOf(emptyList())
            else repository.observeAllCategories(currentUserId)
        }.toUiStateFlow("admin categories")

    val tournamentsState: StateFlow<UiState<List<TournamentMatch>>> =
        repository.observeTournaments(currentUserId).toUiStateFlow("tournaments")

    val myRegistrationsState: StateFlow<UiState<List<Registration>>> =
        repository.observeMyRegistrations(currentUserId).toUiStateFlow("my registrations")

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        _leaderboardSortMetric.flatMapLatest { metric ->
            repository.observeTop10Leaderboard(currentUserId, metric)
        }.toUiStateFlow("leaderboard")

    val selectedResultRosterState: StateFlow<UiState<List<RosterEntry>>> =
        combine(tournamentsState, _selectedResultTournamentId) { tourneysUi, selectedId ->
            val tourneys = (tourneysUi as? UiState.Success)?.data ?: emptyList()
            val completed = tourneys.filter { it.status == "COMPLETED" || it.winnerSummary.isNotBlank() }
            val resolvedId = selectedId?.takeIf { id -> completed.any { it.id == id } }
                ?: completed.firstOrNull()?.id
            resolvedId
        }.flatMapLatest { tid ->
            if (tid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeTournamentRoster(currentUserId, tid)
        }.toUiStateFlow("tournament results")

    val activeRosterState: StateFlow<UiState<List<RosterEntry>>> =
        _activeTournamentId.flatMapLatest { tid ->
            if (tid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeTournamentRoster(currentUserId, tid)
        }.toUiStateFlow("roster")

    val adminTournamentRegistrationsState: StateFlow<UiState<List<Registration>>> =
        _adminRegTournamentId.flatMapLatest { tid ->
            if (tid.isNullOrBlank() || !_adminUnlocked.value) flowOf(emptyList())
            else repository.observeTournamentRegistrations(currentUserId, tid)
        }.toUiStateFlow("match registrations")

    val myWalletRequestsState: StateFlow<UiState<List<WalletRequest>>> =
        repository.observeMyWalletRequests(currentUserId).toUiStateFlow("wallet requests")

    val adminWalletRequestsState: StateFlow<UiState<List<WalletRequest>>> =
        _adminUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) flowOf(emptyList())
            else repository.observeAllWalletRequests(currentUserId)
        }.toUiStateFlow("admin wallet requests")

    val activeRoomSecretState: StateFlow<RoomSecret?> = combine(
        _activeTournamentId,
        tournamentsState,
        myRegistrationsState,
        _adminUnlocked
    ) { tid, tourneyUi, regsUi, unlocked ->
        if (tid.isNullOrBlank()) return@combine null
        val match = (tourneyUi as? UiState.Success)?.data?.firstOrNull { it.id == tid }
        val isJoined = (regsUi as? UiState.Success)?.data?.any {
            it.tournamentId == tid && !it.status.equals(RegistrationStatus.REJECTED.name, ignoreCase = true)
        } == true
        if (unlocked || (isJoined && match?.roomPublished == true)) tid else null
    }.flatMapLatest { tid ->
        if (tid == null) flowOf(null)
        else repository.observeRoomSecret(currentUserId, tid)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(null)
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    val accountSettingsState: StateFlow<AccountSettings> =
        (preferencesStore?.settingsFlow ?: flowOf(AccountSettings()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AccountSettings()
            )

    init {
        viewModelScope.launch {
            repository.ensureUserProfileInitialized(currentUserId, displayName, email)
                .onFailure { err ->
                    Log.w(TAG, "Profile init warning", err)
                }
        }
        viewModelScope.launch {
            val previouslySubscribed = runCatching {
                preferencesStore?.getSubscribedTournamentTopics() ?: emptySet()
            }.getOrDefault(emptySet())
            var lastSyncedIds: Set<String> = previouslySubscribed
            repository.syncMessagingTokenAndTopics(currentUserId, emptySet(), emptySet())
            myRegistrationsState.collect { ui ->
                if (ui is UiState.Success) {
                    val joinedIds = ui.data
                        .filter { !it.status.equals(RegistrationStatus.REJECTED.name, ignoreCase = true) }
                        .mapNotNull { it.tournamentId.takeIf(String::isNotBlank) }
                        .toSet()
                    preferencesStore?.setJoinedTournamentIds(joinedIds)
                    if (joinedIds != lastSyncedIds) {
                        val updated = repository.syncMessagingTokenAndTopics(
                            userId = currentUserId,
                            joinedTournamentIds = joinedIds,
                            previouslySubscribedIds = lastSyncedIds
                        )
                        lastSyncedIds = updated
                        preferencesStore?.setSubscribedTournamentTopics(updated)
                    }
                }
            }
        }
        observeWalletDecisionAlerts()
        observeRoomCredentialAlerts()
        observeMatchStartReminders()
    }

    private fun observeWalletDecisionAlerts() {
        viewModelScope.launch {
            var initialized = false
            val previousStatuses = mutableMapOf<String, String>()
            combine(myWalletRequestsState, appConfigState) { walletUi, config ->
                walletUi to config.currencyLabel
            }.collect { (walletUi, currencyLabel) ->
                if (walletUi !is UiState.Success) return@collect
                val requests = walletUi.data
                val settings = accountSettingsState.value
                val nowMillis = System.currentTimeMillis()
                val seedKeys = mutableSetOf<String>()

                requests.forEach { req ->
                    val status = WalletRequestStatus.fromRaw(req.status).name
                    val prev = previousStatuses[req.id]
                    previousStatuses[req.id] = status

                    if (status == WalletRequestStatus.APPROVED.name || status == WalletRequestStatus.REJECTED.name) {
                        val eventKey = ArenaNotificationHelper.walletEventKey(req.id, status)
                        val updatedMillis = req.updatedAt?.toDate()?.time ?: 0L
                        val isFresh = updatedMillis > 0L && (nowMillis - updatedMillis) <= RECENT_EVENT_WINDOW_MS
                        val transitioned = prev == WalletRequestStatus.PENDING.name

                        if (!initialized && !isFresh) {
                            seedKeys.add(eventKey)
                        } else if ((transitioned || isFresh) && settings.pushNotificationsEnabled) {
                            val isNew = preferencesStore?.markNotificationSeen(eventKey) ?: true
                            if (isNew) {
                                val payload = ArenaNotificationHelper.buildWalletDecisionNotification(
                                    requestId = req.id,
                                    requestType = req.type,
                                    status = status,
                                    amount = req.amount,
                                    currencyLabel = currencyLabel,
                                    method = req.method,
                                    accountNumber = req.accountNumber,
                                    adminNote = req.adminNote
                                )
                                _notificationEvents.tryEmit(payload)
                                _statusMessage.value = payload.title
                            }
                        }
                    }
                }

                if (seedKeys.isNotEmpty()) {
                    preferencesStore?.seedSeenNotificationKeys(seedKeys)
                }
                initialized = true
            }
        }
    }

    private fun observeRoomCredentialAlerts() {
        viewModelScope.launch {
            var initialized = false
            val previousPublished = mutableMapOf<String, Boolean>()
            val previousUpdatedMillis = mutableMapOf<String, Long>()
            combine(tournamentsState, myRegistrationsState) { tourneysUi, regsUi ->
                tourneysUi to regsUi
            }.collect { (tourneysUi, regsUi) ->
                if (tourneysUi !is UiState.Success || regsUi !is UiState.Success) return@collect
                val joinedIds = regsUi.data
                    .filter { !it.status.equals(RegistrationStatus.REJECTED.name, ignoreCase = true) }
                    .map { it.tournamentId }
                    .toSet()
                val settings = accountSettingsState.value
                val seenKeys = runCatching {
                    preferencesStore?.notifiedEventKeysFlow?.first() ?: emptySet()
                }.getOrDefault(emptySet())

                tourneysUi.data.forEach { match ->
                    val wasPublished = previousPublished[match.id]
                    val prevUpdated = previousUpdatedMillis[match.id]
                    val curUpdated = match.updatedAt?.toDate()?.time ?: 0L
                    previousPublished[match.id] = match.roomPublished
                    previousUpdatedMillis[match.id] = curUpdated

                    if (!joinedIds.contains(match.id) || !match.roomPublished) return@forEach
                    val matchStatus = MatchStatus.fromRaw(match.status)
                    if (matchStatus == MatchStatus.COMPLETED || matchStatus == MatchStatus.CANCELLED) return@forEach

                    val transitionedToPublished = wasPublished == false
                    val updatedWhilePublished = prevUpdated != null && curUpdated > prevUpdated
                    val recentlyUpdated = curUpdated > 0L && (System.currentTimeMillis() - curUpdated) <= RECENT_EVENT_WINDOW_MS
                    val shouldCheckSecret = transitionedToPublished || updatedWhilePublished || (!initialized && recentlyUpdated) ||
                        (!initialized && seenKeys.none { it.startsWith("room_${match.id}_") })

                    if (shouldCheckSecret && settings.pushNotificationsEnabled) {
                        val secret = if (updatedWhilePublished || transitionedToPublished) {
                            repository.fetchRoomSecret(match.id).getOrNull()?.also { fetched ->
                                _roomSecretsCache.value = _roomSecretsCache.value + (match.id to fetched)
                            } ?: _roomSecretsCache.value[match.id]
                        } else {
                            _roomSecretsCache.value[match.id]
                                ?: repository.fetchRoomSecret(match.id).getOrNull()?.also { fetched ->
                                    _roomSecretsCache.value = _roomSecretsCache.value + (match.id to fetched)
                                }
                        }
                        val payload = ArenaNotificationHelper.buildRoomPublishedNotification(
                            tournamentId = match.id,
                            tournamentTitle = match.title,
                            roomId = secret?.roomId.orEmpty(),
                            roomPassword = secret?.roomPassword.orEmpty()
                        )
                        val isNew = preferencesStore?.markNotificationSeen(payload.eventKey) ?: true
                        if (isNew) {
                            _notificationEvents.tryEmit(payload)
                            if (transitionedToPublished || updatedWhilePublished) {
                                _statusMessage.value = "Room ID & Password released for ${match.title}!"
                            }
                        }
                    }
                }
                initialized = true
            }
        }
    }

    private fun observeMatchStartReminders() {
        val context = appContext ?: return
        val store = preferencesStore ?: return
        viewModelScope.launch {
            combine(tournamentsState, myRegistrationsState, accountSettingsState) { tourneysUi, regsUi, settings ->
                Triple(tourneysUi, regsUi, settings.matchRemindersEnabled)
            }.collect { (tourneysUi, regsUi, remindersEnabled) ->
                if (!remindersEnabled) {
                    TournamentReminderScheduler.cancelAllReminders(context, store)
                    return@collect
                }
                if (tourneysUi is UiState.Success && regsUi is UiState.Success) {
                    TournamentReminderScheduler.syncReminders(
                        context = context,
                        preferencesStore = store,
                        registrations = regsUi.data,
                        tournaments = tourneysUi.data,
                        remindersEnabled = true
                    )
                }
            }
        }
    }

    fun handleNotificationLaunch(openTab: String?, tournamentId: String?) {
        if (!tournamentId.isNullOrBlank()) {
            _activeTournamentId.value = tournamentId
        }
        if (!openTab.isNullOrBlank()) {
            _pendingNavTab.value = openTab
        }
    }

    fun consumePendingNavTab() {
        _pendingNavTab.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setStatusFilter(status: String?) {
        _selectedStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTeamTypeFilter(teamType: TeamType?) {
        _selectedTeamTypeFilter.value = teamType
    }

    fun setActiveOnlySearch(activeOnly: Boolean) {
        _activeOnlySearch.value = activeOnly
    }

    fun clearAllMatchFilters() {
        _searchQuery.value = ""
        _selectedTeamTypeFilter.value = null
        _selectedCategoryFilter.value = null
        _selectedStatusFilter.value = null
        _activeOnlySearch.value = true
    }

    fun openTournamentDetail(tournamentId: String?) {
        _activeTournamentId.value = tournamentId
    }

    fun selectAdminTournamentForRoster(tournamentId: String?) {
        _adminRegTournamentId.value = tournamentId
    }

    fun setLeaderboardSortMetric(metric: LeaderboardSortMetric) {
        _leaderboardSortMetric.value = metric
    }

    fun selectResultTournament(tournamentId: String?) {
        _selectedResultTournamentId.value = tournamentId
    }

    fun verifyAndUnlockAdmin(passcode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.unlockAdminWithPasscode(currentUserId, passcode)
                .onSuccess {
                    _adminUnlocked.value = true
                    _statusMessage.value = "Admin Command unlocked!"
                    onSuccess()
                }
                .onFailure { err ->
                    onError(err.localizedMessage ?: "Invalid admin password.")
                }
            _isBusy.value = false
        }
    }

    fun lockAdminMode() {
        _adminUnlocked.value = false
        _adminRegTournamentId.value = null
        _statusMessage.value = "Admin mode locked."
    }

    fun loadRoomSecret(tournamentId: String) {
        val isJoined = (myRegistrationsState.value as? UiState.Success)?.data?.any {
            it.tournamentId == tournamentId && !it.status.equals(RegistrationStatus.REJECTED.name, ignoreCase = true)
        } == true
        if (!_adminUnlocked.value && !isJoined) {
            _statusMessage.value = "আপনি রুমটি কেনেননি"
            return
        }
        viewModelScope.launch {
            repository.fetchRoomSecret(tournamentId)
                .onSuccess { secret ->
                    if (secret != null) {
                        _roomSecretsCache.value = _roomSecretsCache.value + (tournamentId to secret)
                    } else {
                        _statusMessage.value = "Room credentials not set yet by Admin."
                    }
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Room details are locked until published."
                }
        }
    }

    fun updateProfileIdentity(
        displayName: String,
        ffName: String,
        ffUid: String,
        inGameRank: String? = null
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updatePlayerIdentity(currentUserId, displayName, ffName, ffUid, inGameRank)
                .onSuccess { _statusMessage.value = "Free Fire profile updated!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Update failed" }
            _isBusy.value = false
        }
    }

    fun updateInGameRank(inGameRank: String, profile: UserProfile?) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateInGameRank(currentUserId, inGameRank, profile)
                .onSuccess { _statusMessage.value = "In-game rank updated to $inGameRank!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Rank update failed" }
            _isBusy.value = false
        }
    }

    fun setAutoFillIdentity(enabled: Boolean) {
        viewModelScope.launch { preferencesStore?.setAutoFillIdentity(enabled) }
    }

    fun setShowMatchCountdowns(enabled: Boolean) {
        viewModelScope.launch { preferencesStore?.setShowMatchCountdowns(enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore?.setHapticFeedbackEnabled(enabled) }
    }

    fun setRoomReminderHighlights(enabled: Boolean) {
        viewModelScope.launch { preferencesStore?.setRoomReminderHighlights(enabled) }
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore?.setPushNotificationsEnabled(enabled) }
    }

    fun setMatchRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore?.setMatchRemindersEnabled(enabled)
            if (!enabled && appContext != null && preferencesStore != null) {
                TournamentReminderScheduler.cancelAllReminders(appContext, preferencesStore)
            }
        }
    }

    fun joinTournament(
        tournament: TournamentMatch,
        ffName: String,
        ffUid: String,
        squadName: String,
        paymentRef: String,
        profile: UserProfile?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.joinTournament(
                userId = currentUserId,
                tournament = tournament,
                ffName = ffName,
                ffUid = ffUid,
                squadName = squadName,
                paymentRef = paymentRef,
                currentProfile = profile
            ).onSuccess { result ->
                _statusMessage.value =
                    "Joined ${tournament.title}! Slot #${result.slotNumber} (${result.updatedPlayerCount}/${result.totalSlots} players)"
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not join match"
            }
            _isBusy.value = false
        }
    }

    fun submitAddMoneyRequest(
        profile: UserProfile?,
        method: PaymentMethod,
        amount: Long,
        senderNumber: String,
        txnId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitAddMoneyRequest(
                userId = currentUserId,
                profile = profile,
                method = method,
                amount = amount,
                senderNumber = senderNumber,
                txnId = txnId
            ).onSuccess {
                _statusMessage.value = "Add Money request sent to Admin! Money adds once approved."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not submit Add Money request"
            }
            _isBusy.value = false
        }
    }

    fun submitWithdrawRequest(
        profile: UserProfile?,
        method: PaymentMethod,
        accountNumber: String,
        amount: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val myReqs = (myWalletRequestsState.value as? UiState.Success)?.data ?: emptyList()
            val pendingWithdrawSum = myReqs
                .filter { it.type == "WITHDRAW" && it.status == WalletRequestStatus.PENDING.name }
                .sumOf { it.amount }
            repository.submitWithdrawRequest(
                userId = currentUserId,
                profile = profile,
                method = method,
                accountNumber = accountNumber,
                amount = amount,
                pendingWithdrawSum = pendingWithdrawSum
            ).onSuccess {
                _statusMessage.value = "Withdraw request sent to Admin!"
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not submit Withdraw request"
            }
            _isBusy.value = false
        }
    }

    fun processWalletRequest(
        request: WalletRequest,
        approve: Boolean,
        adminNote: String = ""
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.processWalletRequest(request, approve, adminNote)
                .onSuccess {
                    _statusMessage.value = if (approve) {
                        if (request.type == "DEPOSIT") {
                            "Approved! Added ${request.amount} to ${request.userName}'s Wallet."
                        } else {
                            "Marked Paid & deducted ${request.amount} from ${request.userName}."
                        }
                    } else {
                        "Wallet request rejected."
                    }
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Could not process request"
                }
            _isBusy.value = false
        }
    }

    // --- ADMIN ACTIONS ---

    fun seedStarterPack() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.seedStarterPack()
                .onSuccess { _statusMessage.value = "Starter categories & tournaments published online!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Starter pack failed" }
            _isBusy.value = false
        }
    }

    fun saveAppConfig(config: AppConfig) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveAppConfig(config)
                .onSuccess { _statusMessage.value = "Live app settings published to all players!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save settings" }
            _isBusy.value = false
        }
    }

    fun saveCategory(category: MatchCategory, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveCategory(category)
                .onSuccess {
                    _statusMessage.value = "Category '${category.name}' saved!"
                    onDone()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save category" }
            _isBusy.value = false
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteCategory(categoryId)
                .onSuccess { _statusMessage.value = "Category deleted." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Delete failed" }
            _isBusy.value = false
        }
    }

    fun saveTournament(
        tournament: TournamentMatch,
        roomId: String,
        roomPassword: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveTournament(tournament, roomId, roomPassword)
                .onSuccess { tid ->
                    if (roomId.isNotBlank() || roomPassword.isNotBlank()) {
                        _roomSecretsCache.value = _roomSecretsCache.value + (tid to RoomSecret(tid, roomId, roomPassword))
                    }
                    _statusMessage.value = "Match '${tournament.title}' published live!"
                    onDone()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save match" }
            _isBusy.value = false
        }
    }

    fun updateRoomCredentials(
        tournamentId: String,
        roomId: String,
        roomPassword: String,
        publishNow: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateRoomCredentials(tournamentId, roomId, roomPassword, publishNow)
                .onSuccess {
                    _roomSecretsCache.value = _roomSecretsCache.value + (tournamentId to RoomSecret(tournamentId, roomId, roomPassword))
                    _statusMessage.value = if (publishNow) {
                        "Room ID & Password published to joined players!"
                    } else {
                        "Room credentials saved (hidden until published)."
                    }
                    onDone()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update room" }
            _isBusy.value = false
        }
    }

    fun deleteTournament(tournamentId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteTournament(tournamentId)
                .onSuccess { _statusMessage.value = "Match deleted." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete match" }
            _isBusy.value = false
        }
    }

    fun updateParticipantResult(
        registration: Registration,
        newStatus: RegistrationStatus,
        kills: Long,
        rankPosition: Long,
        prizeWon: Long,
        prizePaid: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateParticipantResult(registration, newStatus, kills, rankPosition, prizeWon, prizePaid)
                .onSuccess {
                    _statusMessage.value = "Updated ${registration.ffName} result & leaderboard!"
                    onDone()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Result update failed" }
            _isBusy.value = false
        }
    }

    private companion object {
        const val TAG = "TournamentVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val RECENT_EVENT_WINDOW_MS = 10L * 60_000L
    }
}
