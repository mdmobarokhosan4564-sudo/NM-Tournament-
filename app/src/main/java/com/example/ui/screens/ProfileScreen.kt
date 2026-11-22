package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.FreeFireRank
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.preferences.AccountSettings
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.computePlayerCareerStats
import com.example.viewmodel.UiState

private enum class ProfileSectionTab(val label: String) {
    OVERVIEW("Overview"),
    HISTORY("Tournament History"),
    STATS("Statistics"),
    SETTINGS("Settings")
}

@Composable
fun ProfileScreen(
    config: AppConfig,
    userId: String,
    profileState: UiState<UserProfile?>,
    registrationsState: UiState<List<Registration>>,
    tournamentsState: UiState<List<TournamentMatch>>,
    leaderboardTop10Rank: Int?,
    accountSettings: AccountSettings,
    notificationPermissionGranted: Boolean,
    adminUnlocked: Boolean,
    isBusy: Boolean,
    onSaveIdentity: (displayName: String, ffName: String, ffUid: String, inGameRank: String) -> Unit,
    onUpdateInGameRank: (String) -> Unit,
    onToggleAutoFill: (Boolean) -> Unit,
    onToggleCountdowns: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleRoomHighlights: (Boolean) -> Unit,
    onTogglePushNotifications: (Boolean) -> Unit,
    onToggleMatchReminders: (Boolean) -> Unit,
    onOpenWallet: () -> Unit,
    onOpenMatch: (TournamentMatch) -> Unit,
    onExploreMatches: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLockAdmin: () -> Unit,
    onCopyFeedback: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val registrations = (registrationsState as? UiState.Success)?.data ?: emptyList()
    val tournamentsById = remember(tournamentsState) {
        (tournamentsState as? UiState.Success)?.data?.associateBy { it.id } ?: emptyMap()
    }
    val stats = remember(profile, registrations, tournamentsById) {
        computePlayerCareerStats(profile, registrations, tournamentsById)
    }

    var selectedTab by remember { mutableStateOf(ProfileSectionTab.OVERVIEW) }
    var selectedHistoryFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showRankPicker by remember { mutableStateOf(false) }

    val filteredHistory = remember(registrations, tournamentsById, selectedHistoryFilter) {
        registrations.filter { reg ->
            val matchStatus = tournamentsById[reg.tournamentId]?.status?.let { MatchStatus.fromRaw(it) }
            val isCompleted = matchStatus == MatchStatus.COMPLETED ||
                reg.rankPosition > 0L ||
                reg.kills > 0L ||
                reg.prizeWon > 0L
            when (selectedHistoryFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.COMPLETED -> isCompleted
                HistoryFilter.ACTIVE -> !isCompleted && matchStatus != MatchStatus.CANCELLED
                HistoryFilter.PRIZES -> reg.prizeWon > 0L
            }
        }
    }

    val showStats = selectedTab == ProfileSectionTab.OVERVIEW || selectedTab == ProfileSectionTab.STATS
    val showHistory = selectedTab == ProfileSectionTab.OVERVIEW || selectedTab == ProfileSectionTab.HISTORY
    val showSettings = selectedTab == ProfileSectionTab.OVERVIEW || selectedTab == ProfileSectionTab.SETTINGS

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Player Hero Card with In-Game Rank, Win Count, and Tournament Count Spotlight
        item {
            PlayerProfileHeroCard(
                profile = profile,
                totalWins = stats.totalWins,
                matchesJoined = stats.matchesJoined,
                leaderboardTop10Rank = leaderboardTop10Rank,
                tierTitle = stats.rankTier.title,
                tierBadge = stats.rankTier.badge,
                tierAccent = stats.rankTier.accent,
                adminUnlocked = adminUnlocked,
                onEditRankClick = { showRankPicker = true },
                onWinCountClick = { selectedTab = ProfileSectionTab.STATS },
                onHistoryCountClick = { selectedTab = ProfileSectionTab.HISTORY }
            )
        }

        // Section Filter Strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ProfileSectionTab.entries) { tab ->
                    val selected = selectedTab == tab
                    FilterChip(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = if (tab == ProfileSectionTab.HISTORY) {
                                    "${tab.label} (${registrations.size})"
                                } else {
                                    tab.label
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = TextPrimary,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("profile_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        // 1. Statistics Section
        if (showStats) {
            item {
                PlayerStatisticsCard(
                    stats = stats,
                    inGameRankLabel = profile?.inGameRank ?: FreeFireRank.UNRANKED.label,
                    leaderboardTop10Rank = leaderboardTop10Rank,
                    walletBalance = profile?.walletBalance ?: 0L,
                    currencyLabel = config.currencyLabel,
                    onOpenWallet = onOpenWallet
                )
            }
        }

        // 2. Match History Section
        if (showHistory) {
            item {
                MatchHistoryHeaderCard(
                    totalCount = registrations.size,
                    selectedFilter = selectedHistoryFilter,
                    onSelectFilter = { selectedHistoryFilter = it }
                )
            }

            when (registrationsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GlooCyan)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = registrationsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DangerCrimson
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredHistory.isEmpty()) {
                        item {
                            EmptyMatchHistoryCard(
                                hasAnyRegistrations = registrations.isNotEmpty(),
                                onExploreMatches = onExploreMatches,
                                onResetFilter = { selectedHistoryFilter = HistoryFilter.ALL }
                            )
                        }
                    } else {
                        val visibleHistory = if (selectedTab == ProfileSectionTab.OVERVIEW) {
                            filteredHistory.take(5)
                        } else {
                            filteredHistory
                        }
                        items(visibleHistory, key = { "history_${it.id}" }) { reg ->
                            MatchHistoryItemCard(
                                registration = reg,
                                liveMatch = tournamentsById[reg.tournamentId],
                                currencyLabel = config.currencyLabel,
                                highlightRoomReady = accountSettings.roomReminderHighlights,
                                onOpenMatch = onOpenMatch
                            )
                        }
                        if (selectedTab == ProfileSectionTab.OVERVIEW && filteredHistory.size > 5) {
                            item {
                                TextButton(
                                    onClick = { selectedTab = ProfileSectionTab.HISTORY },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_view_all_history_button")
                                ) {
                                    Text(
                                        text = "VIEW ALL ${filteredHistory.size} MATCHES IN HISTORY →",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = GlooCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Account Settings Section
        if (showSettings) {
            item {
                AccountSettingsSection(
                    profile = profile,
                    userId = userId,
                    config = config,
                    accountSettings = accountSettings,
                    notificationPermissionGranted = notificationPermissionGranted,
                    adminUnlocked = adminUnlocked,
                    isBusy = isBusy,
                    onSaveIdentity = onSaveIdentity,
                    onToggleAutoFill = onToggleAutoFill,
                    onToggleCountdowns = onToggleCountdowns,
                    onToggleHaptics = onToggleHaptics,
                    onToggleRoomHighlights = onToggleRoomHighlights,
                    onTogglePushNotifications = onTogglePushNotifications,
                    onToggleMatchReminders = onToggleMatchReminders,
                    onOpenAdmin = onOpenAdmin,
                    onLockAdmin = onLockAdmin,
                    onCopyFeedback = onCopyFeedback,
                    onRequestSignOut = { showSignOutConfirm = true }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showRankPicker) {
        InGameRankPickerDialog(
            currentRank = profile?.inGameRank ?: FreeFireRank.UNRANKED.label,
            isBusy = isBusy,
            onDismiss = { showRankPicker = false },
            onSelectRank = { chosen ->
                showRankPicker = false
                onUpdateInGameRank(chosen)
            }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = ObsidianCard,
            title = { Text("Sign Out?", style = MaterialTheme.typography.headlineMedium, color = TextPrimary) },
            text = {
                Text(
                    text = "You will return to the Google Sign-In screen. Your stats, match history, and wallet balance stay saved safely online.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("SIGN OUT", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PlayerProfileHeroCard(
    profile: UserProfile?,
    totalWins: Long,
    matchesJoined: Int,
    leaderboardTop10Rank: Int?,
    tierTitle: String,
    tierBadge: String,
    tierAccent: androidx.compose.ui.graphics.Color,
    adminUnlocked: Boolean,
    onEditRankClick: () -> Unit,
    onWinCountClick: () -> Unit,
    onHistoryCountClick: () -> Unit
) {
    val ffRank = FreeFireRank.fromRaw(profile?.inGameRank.orEmpty())
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, tierAccent.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            tierAccent.copy(alpha = 0.14f),
                            ObsidianCard,
                            ObsidianCard
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(tierAccent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                            .border(1.5.dp, tierAccent, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Player avatar",
                            tint = tierAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile?.displayName?.ifBlank { "Survivor" } ?: "Survivor",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (adminUnlocked) {
                                Surface(
                                    color = BlazeGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Filled.Shield, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(11.dp))
                                        Text("ADMIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BlazeGold)
                                    }
                                }
                            }
                        }
                        Text(
                            text = profile?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                color = tierAccent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, tierAccent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = tierAccent, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = if (leaderboardTop10Rank != null) "TOP #$leaderboardTop10Rank" else tierBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tierAccent
                                    )
                                }
                            }
                            if (!profile?.ffName.isNullOrBlank()) {
                                Surface(
                                    color = ObsidianSurface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, OutlineSubtle)
                                ) {
                                    Text(
                                        text = "IGN: ${profile?.ffName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GlooCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3-Column Career Spotlight: Current In-Game Rank | Win Count | Tournaments Joined
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ffRank.accent.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1.25f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onEditRankClick)
                            .testTag("profile_edit_rank_button")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MilitaryTech,
                                        contentDescription = "In-Game Rank",
                                        tint = ffRank.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "IN-GAME RANK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit In-Game Rank",
                                    tint = ffRank.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = ffRank.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ffRank.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onWinCountClick)
                            .testTag("profile_win_count_badge")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = "Win Count",
                                    tint = TacticalGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "WINS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "$totalWins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TacticalGreen,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onHistoryCountClick)
                            .testTag("profile_tournaments_count_badge")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SportsEsports,
                                    contentDescription = "Tournaments Joined",
                                    tint = GlooCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PLAYED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "$matchesJoined",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GlooCyan,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
