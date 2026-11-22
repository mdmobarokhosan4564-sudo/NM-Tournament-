package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LeaderboardSortMetric
import com.example.data.model.MatchStatus
import com.example.data.model.RosterEntry
import com.example.data.model.TournamentMatch
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelegramHelpCard
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.UiState

@Composable
fun LeaderboardScreen(
    config: AppConfig,
    currentUserId: String,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    sortMetric: LeaderboardSortMetric,
    tournamentsState: UiState<List<TournamentMatch>>,
    selectedResultTournamentId: String?,
    selectedResultRosterState: UiState<List<RosterEntry>>,
    onSelectSortMetric: (LeaderboardSortMetric) -> Unit,
    onSelectResultTournament: (String) -> Unit,
    onOpenMatch: (TournamentMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedTournaments = remember(tournamentsState) {
        (tournamentsState as? UiState.Success)?.data
            ?.filter {
                MatchStatus.fromRaw(it.status) == MatchStatus.COMPLETED || it.winnerSummary.isNotBlank()
            }
            ?: emptyList()
    }
    val activeResultMatch = remember(completedTournaments, selectedResultTournamentId) {
        completedTournaments.firstOrNull { it.id == selectedResultTournamentId }
            ?: completedTournaments.firstOrNull()
    }

    val top10Players = remember(leaderboardState) {
        (leaderboardState as? UiState.Success)?.data?.take(10) ?: emptyList()
    }
    val myRankIndex = remember(top10Players, currentUserId) {
        top10Players.indexOfFirst { it.userId == currentUserId }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leaderboard_header_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    BlazeGold.copy(alpha = 0.16f),
                                    ObsidianCard,
                                    GlooCyan.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = BlazeGold.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.6f)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Leaderboard,
                                        contentDescription = "Leaderboard",
                                        tint = BlazeGold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "TOP 10 LEADERBOARD",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Synced live from Firestore tournament results",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                color = TacticalGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(TacticalGreen)
                                    )
                                    Text(
                                        text = "TOP 10",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TacticalGreen
                                    )
                                }
                            }
                        }

                        if (myRankIndex >= 0) {
                            val me = top10Players[myRankIndex]
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "YOUR RANK: #${myRankIndex + 1} (${me.ffName})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GlooCyan
                                    )
                                    Text(
                                        text = "${me.totalWinnings} ${config.currencyLabel} • ${me.totalKills} Kills",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BlazeGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sort Metric Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LeaderboardSortMetric.entries) { metric ->
                    val selected = sortMetric == metric
                    val activeAccent = when (metric) {
                        LeaderboardSortMetric.WINNINGS -> BlazeGold
                        LeaderboardSortMetric.KILLS -> GlooCyan
                        LeaderboardSortMetric.WINS -> TacticalGreen
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSortMetric(metric) },
                        label = { Text(metric.label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeAccent,
                            selectedLabelColor = ObsidianBg,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("leaderboard_sort_${metric.name.lowercase()}")
                    )
                }
            }
        }

        when (leaderboardState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BlazeGold)
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
                            text = leaderboardState.message,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (top10Players.isEmpty()) {
                    item {
                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, OutlineSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leaderboard_empty_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = BlazeGold,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Top 10 Leaderboard Updates Live",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Top 10 players appear here automatically as tournament results are published in Firestore.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Top 3 Champions Podium
                    if (top10Players.size >= 2) {
                        item {
                            ChampionsPodiumSection(
                                topEntries = top10Players.take(3),
                                sortMetric = sortMetric,
                                currencyLabel = config.currencyLabel,
                                currentUserId = currentUserId
                            )
                        }
                    }

                    val listEntries = if (top10Players.size >= 2) {
                        top10Players.drop(3).mapIndexed { idx, entry -> (idx + 4) to entry }
                    } else {
                        top10Players.mapIndexed { idx, entry -> (idx + 1) to entry }
                    }

                    if (listEntries.isNotEmpty()) {
                        items(listEntries, key = { (_, entry) -> entry.id }) { (rank, entry) ->
                            LeaderboardPlayerRow(
                                rank = rank,
                                entry = entry,
                                isCurrentUser = entry.userId == currentUserId,
                                sortMetric = sortMetric,
                                currencyLabel = config.currencyLabel
                            )
                        }
                    }
                }
            }
        }

        // Completed Tournament Results Section (from Firestore tournaments + roster)
        if (completedTournaments.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = GlooCyan)
                    Text(
                        text = "RECENT TOURNAMENT RESULTS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(completedTournaments, key = { it.id }) { match ->
                        val selected = activeResultMatch?.id == match.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectResultTournament(match.id) },
                            label = {
                                Text(
                                    text = match.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlooCyan.copy(alpha = 0.22f),
                                selectedLabelColor = GlooCyan,
                                containerColor = ObsidianCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("result_match_chip_${match.id}")
                        )
                    }
                }
            }

            activeResultMatch?.let { match ->
                item {
                    CompletedTournamentResultCard(
                        match = match,
                        rosterState = selectedResultRosterState,
                        currencyLabel = config.currencyLabel,
                        onOpenMatch = { onOpenMatch(match) }
                    )
                }
            }
        }

        // Global Tournament Rules
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("GLOBAL TOURNAMENT RULES", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text(
                        text = config.globalRules.ifBlank { "Follow fair-play esports rules." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Support & Payment Info
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = GlooCyan)
                        Text("SUPPORT & PAYMENT INFO", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    if (config.paymentInfo.isNotBlank()) {
                        Text(config.paymentInfo, style = MaterialTheme.typography.bodyMedium, color = BlazeGold)
                    }
                    TelegramHelpCard(supportContact = config.supportContact)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun ChampionsPodiumSection(
    topEntries: List<LeaderboardEntry>,
    sortMetric: LeaderboardSortMetric,
    currencyLabel: String,
    currentUserId: String
) {
    val first = topEntries.getOrNull(0)
    val second = topEntries.getOrNull(1)
    val third = topEntries.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_podium"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (second != null) {
            PodiumTile(
                rank = 2,
                entry = second,
                accent = GlooCyan,
                topPadding = 18.dp,
                isCurrentUser = second.userId == currentUserId,
                sortMetric = sortMetric,
                currencyLabel = currencyLabel,
                modifier = Modifier.weight(1f)
            )
        }
        if (first != null) {
            PodiumTile(
                rank = 1,
                entry = first,
                accent = BlazeGold,
                topPadding = 0.dp,
                isCurrentUser = first.userId == currentUserId,
                sortMetric = sortMetric,
                currencyLabel = currencyLabel,
                modifier = Modifier.weight(1.12f)
            )
        }
        if (third != null) {
            PodiumTile(
                rank = 3,
                entry = third,
                accent = MaterialTheme.colorScheme.primary,
                topPadding = 24.dp,
                isCurrentUser = third.userId == currentUserId,
                sortMetric = sortMetric,
                currencyLabel = currencyLabel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PodiumTile(
    rank: Int,
    entry: LeaderboardEntry,
    accent: Color,
    topPadding: androidx.compose.ui.unit.Dp,
    isCurrentUser: Boolean,
    sortMetric: LeaderboardSortMetric,
    currencyLabel: String,
    modifier: Modifier = Modifier
) {
    val primaryStat = when (sortMetric) {
        LeaderboardSortMetric.WINNINGS -> "${entry.totalWinnings} $currencyLabel"
        LeaderboardSortMetric.KILLS -> "${entry.totalKills} Kills"
        LeaderboardSortMetric.WINS -> "${entry.totalWins} Booyahs"
    }
    Column(modifier = modifier.padding(top = topPadding)) {
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = if (rank == 1 || isCurrentUser) 1.5.dp else 1.dp,
                color = if (isCurrentUser) TacticalGreen else accent.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("leaderboard_rank_$rank")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.16f), ObsidianCard)
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, accent),
                    modifier = Modifier.size(if (rank == 1) 40.dp else 34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (rank == 1) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Rank 1",
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                    }
                }
                Text(
                    text = entry.ffName.ifBlank { "Player" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (isCurrentUser) {
                    Surface(
                        color = TacticalGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "YOU",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TacticalGreen,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = primaryStat,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.totalKills}K • ${entry.totalWins}W",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun LeaderboardPlayerRow(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    sortMetric: LeaderboardSortMetric,
    currencyLabel: String
) {
    val rankColor = when (rank) {
        1 -> BlazeGold
        2 -> GlooCyan
        3 -> MaterialTheme.colorScheme.primary
        else -> TextSecondary
    }
    val highlightedValue = when (sortMetric) {
        LeaderboardSortMetric.WINNINGS -> "${entry.totalWinnings} $currencyLabel"
        LeaderboardSortMetric.KILLS -> "${entry.totalKills} Kills"
        LeaderboardSortMetric.WINS -> "${entry.totalWins} Booyahs"
    }
    val secondaryLine = when (sortMetric) {
        LeaderboardSortMetric.WINNINGS -> "${entry.totalKills} Kills • ${entry.totalWins} Booyahs"
        LeaderboardSortMetric.KILLS -> "${entry.totalWinnings} $currencyLabel • ${entry.totalWins} Booyahs"
        LeaderboardSortMetric.WINS -> "${entry.totalWinnings} $currencyLabel • ${entry.totalKills} Kills"
    }

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isCurrentUser) 1.5.dp else 1.dp,
            color = if (isCurrentUser) TacticalGreen else OutlineSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_rank_$rank")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, rankColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = rankColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.ffName.ifBlank { "Player" },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Surface(
                                color = TacticalGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "YOU",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${entry.totalWins} Booyahs • ${entry.matchesPlayed} Matches",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = highlightedValue,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = if (sortMetric == LeaderboardSortMetric.KILLS) GlooCyan else BlazeGold
                )
                Text(
                    text = secondaryLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CompletedTournamentResultCard(
    match: TournamentMatch,
    rosterState: UiState<List<RosterEntry>>,
    currencyLabel: String,
    onOpenMatch: () -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("completed_tournament_result_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(match.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "${match.mapName} • ${match.scheduleText} • Prize ${match.prizePool} $currencyLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                StatusBadge(status = match.status)
            }

            if (match.winnerSummary.isNotBlank()) {
                Surface(
                    color = BlazeGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(18.dp))
                        Text(match.winnerSummary, style = MaterialTheme.typography.bodyMedium, color = BlazeGold)
                    }
                }
            }

            when (rosterState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = GlooCyan)
                    }
                }
                is UiState.Error -> {
                    Text(rosterState.message, style = MaterialTheme.typography.labelSmall, color = DangerCrimson)
                }
                is UiState.Success -> {
                    val topMatchRoster = rosterState.data
                        .sortedWith(
                            compareBy<RosterEntry> { if (it.rankPosition > 0L) it.rankPosition else 999L }
                                .thenByDescending { it.prizeWon }
                                .thenByDescending { it.kills }
                        )
                        .take(10)

                    if (topMatchRoster.isEmpty()) {
                        Text(
                            text = "Match standings will appear once Admin updates participant scores.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        topMatchRoster.forEachIndexed { idx, entry ->
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OutlineSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (entry.rankPosition > 0L) "#${entry.rankPosition}" else "#${idx + 1}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                            color = if (entry.rankPosition == 1L) BlazeGold else GlooCyan
                                        )
                                        Text(
                                            text = entry.ffName.ifBlank { "Player" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${entry.kills} Kills • +${entry.prizeWon} $currencyLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (entry.prizeWon > 0L) BlazeGold else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onOpenMatch,
                    modifier = Modifier.testTag("result_view_match_button")
                ) {
                    Text("VIEW FULL MATCH →", style = MaterialTheme.typography.labelLarge, color = GlooCyan)
                }
            }
        }
    }
}
