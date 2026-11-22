package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.MatchCategory
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.RegistrationStatus
import com.example.data.model.TeamType
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.model.filterTournaments
import com.example.ui.components.CompactCountdownBadge
import com.example.ui.components.JoinTournamentDialog
import com.example.ui.components.LiveNoticeBanner
import com.example.ui.components.TelegramHelpCard
import com.example.ui.components.TournamentMatchCard
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
import com.example.util.CountdownUrgency
import com.example.util.MatchCountdown
import com.example.util.computeMatchCountdown
import com.example.util.rememberLiveClockMillis
import com.example.viewmodel.UiState

@Composable
fun MatchesScreen(
    config: AppConfig,
    categoriesState: UiState<List<MatchCategory>>,
    tournamentsState: UiState<List<TournamentMatch>>,
    myRegistrationsState: UiState<List<Registration>>,
    selectedCategoryId: String?,
    selectedStatus: String?,
    searchQuery: String = "",
    selectedTeamType: TeamType? = null,
    activeOnlySearch: Boolean = true,
    adminUnlocked: Boolean,
    showCountdowns: Boolean = true,
    userProfile: UserProfile? = null,
    autoFillIdentity: Boolean = true,
    isBusy: Boolean = false,
    onSelectCategory: (String?) -> Unit,
    onSelectStatus: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onSelectTeamType: (TeamType?) -> Unit = {},
    onToggleActiveOnlySearch: (Boolean) -> Unit = {},
    onClearAllFilters: () -> Unit = {},
    onOpenMatch: (TournamentMatch) -> Unit,
    onJoinMatch: ((TournamentMatch, ffName: String, ffUid: String, squadName: String, paymentRef: String, onSuccess: () -> Unit) -> Unit)? = null,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (config.maintenanceMode && !adminUnlocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.5f)),
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = BlazeGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "MAINTENANCE MODE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BlazeGold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = config.maintenanceMessage.ifBlank { "Servers are currently updating. Check back soon." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onOpenAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Admin Unlock", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        return
    }

    val joinedIds = (myRegistrationsState as? UiState.Success)?.data
        ?.filter { !it.status.equals(RegistrationStatus.REJECTED.name, ignoreCase = true) }
        ?.map { it.tournamentId }?.toSet() ?: emptySet()
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val currentTimeMillis = rememberLiveClockMillis()
    val nextTimedPair = (tournamentsState as? UiState.Success)?.data
        ?.mapNotNull { match ->
            val cd = computeMatchCountdown(match.startTimeMillis, currentTimeMillis, match.status)
            if (cd != null) match to cd else null
        }
        ?.minByOrNull { (_, cd) -> cd.remainingMillis }
    var quickJoinMatch by remember { mutableStateOf<TournamentMatch?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (config.noticeEnabled && config.noticeText.isNotBlank()) {
            item {
                LiveNoticeBanner(noticeText = config.noticeText)
            }
        }

        item {
            HeroBannerCard(config = config)
        }

        item {
            TelegramHelpCard(supportContact = config.supportContact)
        }

        if (showCountdowns && nextTimedPair != null) {
            item {
                NextKickoffStrip(
                    match = nextTimedPair.first,
                    countdown = nextTimedPair.second,
                    onClick = { onOpenMatch(nextTimedPair.first) }
                )
            }
        }

        // Search Bar & Quick Category Filter Strip (Solo / Duo / Squad / 1v1 / 4v4)
        item {
            TournamentSearchPanel(
                searchQuery = searchQuery,
                selectedTeamType = selectedTeamType,
                activeOnlySearch = activeOnlySearch,
                onSearchQueryChange = onSearchQueryChange,
                onSelectTeamType = onSelectTeamType,
                onToggleActiveOnlySearch = onToggleActiveOnlySearch
            )
        }

        if (categories.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text("All Modes", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = TextPrimary,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("cat_chip_all")
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategory(if (isSelected) null else cat.id) },
                            label = { Text("${cat.name} (${cat.badge})", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = TextPrimary,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("cat_chip_${cat.id}")
                        )
                    }
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val statuses = listOf(
                    null to "All",
                    "ACTIVE" to "Active",
                    MatchStatus.LIVE.name to "Live",
                    MatchStatus.UPCOMING.name to "Upcoming",
                    MatchStatus.COMPLETED.name to "Completed"
                )
                items(statuses) { (statusKey, label) ->
                    val selected = selectedStatus == statusKey
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectStatus(statusKey) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlazeGold.copy(alpha = 0.22f),
                            selectedLabelColor = BlazeGold,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("status_chip_${statusKey ?: "all"}")
                    )
                }
            }
        }

        when (tournamentsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = DangerCrimson.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tournamentsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val isSearchOrCategoryFiltering = searchQuery.isNotBlank() || selectedTeamType != null
                val applyActiveOnly = activeOnlySearch && isSearchOrCategoryFiltering && selectedStatus == null
                val filtered = filterTournaments(
                    tournaments = tournamentsState.data,
                    searchQuery = searchQuery,
                    selectedCategoryId = selectedCategoryId,
                    selectedTeamType = selectedTeamType,
                    selectedStatus = selectedStatus,
                    activeOnly = applyActiveOnly
                )
                val hasActiveFilters = searchQuery.isNotBlank() ||
                    selectedTeamType != null ||
                    selectedCategoryId != null ||
                    selectedStatus != null

                if (hasActiveFilters && tournamentsState.data.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (applyActiveOnly || selectedStatus == "ACTIVE") {
                                    "Showing ${filtered.size} active tournament${if (filtered.size == 1) "" else "s"}"
                                } else {
                                    "Showing ${filtered.size} tournament${if (filtered.size == 1) "" else "s"}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = GlooCyan,
                                modifier = Modifier.testTag("matches_filter_count")
                            )
                            TextButton(
                                onClick = {
                                    onClearAllFilters()
                                    onSelectCategory(null)
                                    onSelectStatus(null)
                                },
                                modifier = Modifier.testTag("matches_clear_filters_button")
                            ) {
                                Text(
                                    text = "Clear Filters",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlazeGold
                                )
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    val hasCompletedMatchesForQuery = applyActiveOnly && filterTournaments(
                        tournaments = tournamentsState.data,
                        searchQuery = searchQuery,
                        selectedCategoryId = selectedCategoryId,
                        selectedTeamType = selectedTeamType,
                        selectedStatus = null,
                        activeOnly = false
                    ).isNotEmpty()
                    item {
                        EmptyTournamentsCard(
                            hasAnyMatches = tournamentsState.data.isNotEmpty(),
                            hasCompletedHiddenByActiveFilter = hasCompletedMatchesForQuery,
                            onIncludeCompleted = { onToggleActiveOnlySearch(false) },
                            onOpenAdmin = onOpenAdmin,
                            onResetFilter = {
                                onClearAllFilters()
                                onSelectCategory(null)
                                onSelectStatus(null)
                            }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { match ->
                        TournamentMatchCard(
                            match = match,
                            currencyLabel = config.currencyLabel,
                            isJoined = match.id in joinedIds,
                            currentTimeMillis = currentTimeMillis,
                            showCountdown = showCountdowns,
                            onJoinClick = if (onJoinMatch != null) {
                                { quickJoinMatch = match }
                            } else null,
                            onClick = { onOpenMatch(match) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    val selectedMatchForJoin = quickJoinMatch
    if (selectedMatchForJoin != null && onJoinMatch != null) {
        JoinTournamentDialog(
            match = selectedMatchForJoin,
            config = config,
            initialFfName = if (autoFillIdentity) userProfile?.ffName ?: "" else "",
            initialFfUid = if (autoFillIdentity) userProfile?.ffUid ?: "" else "",
            isBusy = isBusy,
            onDismiss = { quickJoinMatch = null },
            onSubmit = { ign, uid, squad, payRef ->
                onJoinMatch(selectedMatchForJoin, ign, uid, squad, payRef) {
                    quickJoinMatch = null
                }
            }
        )
    }
}

@Composable
private fun HeroBannerCard(config: AppConfig) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_banner_br_arena_1795255033295),
                contentDescription = "Tournament spotlight",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ObsidianBg.copy(alpha = 0.94f),
                                ObsidianBg.copy(alpha = 0.72f),
                                ObsidianBg.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = config.heroBannerBadge.ifBlank { "ONLINE ARENA" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = config.heroBannerTitle.ifBlank { "FREE FIRE CHAMPIONSHIP" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = config.heroBannerSubtitle.ifBlank { "Live synced custom rooms & instant prizes." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentSearchPanel(
    searchQuery: String,
    selectedTeamType: TeamType?,
    activeOnlySearch: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectTeamType: (TeamType?) -> Unit,
    onToggleActiveOnlySearch: (Boolean) -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (searchQuery.isNotBlank() || selectedTeamType != null) GlooCyan.copy(alpha = 0.5f) else OutlineSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search active tournaments (Solo, Duo, Squad, Map...)",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search tournaments",
                        tint = if (searchQuery.isNotBlank()) GlooCyan else TextSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("matches_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlooCyan,
                    unfocusedBorderColor = OutlineSubtle,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("matches_search_input")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = activeOnlySearch,
                        onClick = { onToggleActiveOnlySearch(!activeOnlySearch) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (activeOnlySearch) TacticalGreen else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Active Only",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TacticalGreen.copy(alpha = 0.18f),
                            selectedLabelColor = TacticalGreen,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("matches_active_only_chip")
                    )
                }

                items(TeamType.entries) { teamType ->
                    val isSelected = selectedTeamType == teamType
                    val chipLabel = when (teamType) {
                        TeamType.SOLO -> "Solo"
                        TeamType.DUO -> "Duo"
                        TeamType.SQUAD -> "Squad"
                        TeamType.`1V1` -> "1v1"
                        TeamType.`4V4` -> "4v4"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTeamType(if (isSelected) null else teamType) },
                        label = {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GlooCyan.copy(alpha = 0.22f),
                            selectedLabelColor = GlooCyan,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("team_chip_${teamType.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTournamentsCard(
    hasAnyMatches: Boolean,
    hasCompletedHiddenByActiveFilter: Boolean = false,
    onIncludeCompleted: () -> Unit = {},
    onOpenAdmin: () -> Unit,
    onResetFilter: () -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SportsEsports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = when {
                    hasCompletedHiddenByActiveFilter -> "No Active Matches Found"
                    hasAnyMatches -> "No matches match this filter"
                    else -> "No Tournaments Added Yet"
                },
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = when {
                    hasCompletedHiddenByActiveFilter -> "Matching tournaments exist in Completed status. Include completed matches or clear your search."
                    hasAnyMatches -> "Try clearing your search query, mode, or status filter."
                    else -> "Unlock the Admin Panel from the top bar to add matches, categories, and customize the entire app!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (hasAnyMatches) {
                if (hasCompletedHiddenByActiveFilter) {
                    OutlinedButton(
                        onClick = onIncludeCompleted,
                        modifier = Modifier.testTag("include_completed_matches_button")
                    ) {
                        Text("Include Completed Matches", style = MaterialTheme.typography.labelLarge, color = GlooCyan)
                    }
                }
                Button(onClick = onResetFilter) {
                    Text("Show All Matches", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onOpenAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("empty_open_admin_btn")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("OPEN ADMIN PANEL", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun NextKickoffStrip(
    match: TournamentMatch,
    countdown: MatchCountdown,
    onClick: () -> Unit
) {
    val accent = when (countdown.urgency) {
        CountdownUrgency.STARTING_NOW, CountdownUrgency.CRITICAL -> DangerCrimson
        CountdownUrgency.SOON -> MaterialTheme.colorScheme.primary
        CountdownUrgency.NORMAL -> GlooCyan
    }
    Surface(
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("next_kickoff_strip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (countdown.urgency == CountdownUrgency.CRITICAL || countdown.urgency == CountdownUrgency.STARTING_NOW) {
                            "STARTING SOON • HURRY!"
                        } else {
                            "NEXT KICKOFF"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = "${match.title} (${match.filledSlots}/${match.totalSlots} slots)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            CompactCountdownBadge(countdown = countdown)
        }
    }
}
