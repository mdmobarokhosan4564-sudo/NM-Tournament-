package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.FreeFireRank
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.TeamType
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.preferences.AccountSettings
import androidx.compose.material3.TextButton
import com.example.ui.components.MetricPill
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelegramHelpCard
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
import com.example.util.CombatRankTier
import com.example.util.PlayerCareerStats
import java.util.Locale

enum class HistoryFilter(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    ACTIVE("Upcoming / Live"),
    PRIZES("Won Prize")
}

@Composable
fun ProfileSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = accent.copy(alpha = 0.16f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.padding(8.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun PlayerStatisticsCard(
    stats: PlayerCareerStats,
    inGameRankLabel: String,
    leaderboardTop10Rank: Int?,
    walletBalance: Long,
    currencyLabel: String,
    onOpenWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier = stats.rankTier
    val ffRank = FreeFireRank.fromRaw(inGameRankLabel)
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, tier.accent.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_stats_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileSectionHeader(
                icon = Icons.Filled.EmojiEvents,
                title = "PLAYER STATISTICS",
                subtitle = "Career performance across all Free Fire custom rooms",
                accent = tier.accent
            )

            // Combat Tier Progression Bar
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, tier.accent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IN-GAME RANK: ${ffRank.label.uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ffRank.accent
                        )
                        Text(
                            text = if (leaderboardTop10Rank != null) {
                                "ARENA TOP #$leaderboardTop10Rank"
                            } else {
                                tier.title.uppercase()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tier.accent
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ARENA TIER: ${tier.title.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = if (tier == CombatRankTier.GRANDMASTER) {
                                "${stats.combatScore} PTS • MAX TIER"
                            } else {
                                "${stats.combatScore} / ${tier.nextScore} PTS"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { stats.tierProgress },
                        color = tier.accent,
                        trackColor = OutlineSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Primary 3-col grid Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "TOURNAMENTS",
                    value = "${stats.matchesJoined}",
                    accent = GlooCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "WIN COUNT",
                    value = "${stats.totalWins}",
                    accent = TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "TOTAL KILLS",
                    value = "${stats.totalKills}",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Secondary 3-col grid Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "WIN RATE",
                    value = "${stats.winRatePercent}%",
                    accent = TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "AVG KILLS",
                    value = String.format(Locale.US, "%.1f", stats.avgKillsPerMatch),
                    accent = GlooCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "TOTAL WON",
                    value = "${stats.totalWinnings} $currencyLabel",
                    accent = BlazeGold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Personal Bests Strip
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersonalBestItem(
                        label = "BEST RANK",
                        value = if (stats.bestRank > 0L) "#${stats.bestRank}" else "--",
                        accent = BlazeGold
                    )
                    PersonalBestItem(
                        label = "BEST KILLS",
                        value = "${stats.bestKillsInMatch}",
                        accent = GlooCyan
                    )
                    PersonalBestItem(
                        label = "COMPLETED",
                        value = "${stats.completedMatches}",
                        accent = TacticalGreen
                    )
                    PersonalBestItem(
                        label = "ACTIVE",
                        value = "${stats.activeMatches}",
                        accent = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = onOpenWallet,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = ObsidianBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("profile_open_wallet_button")
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WALLET: $walletBalance $currencyLabel • ADD MONEY / WITHDRAW",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun PersonalBestItem(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
fun MatchHistoryHeaderCard(
    totalCount: Int,
    selectedFilter: HistoryFilter,
    onSelectFilter: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_history_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProfileSectionHeader(
            icon = Icons.Filled.History,
            title = "TOURNAMENT HISTORY ($totalCount)",
            subtitle = "Tap any tournament to view Room Details, placement & rewards",
            accent = GlooCyan
        )
        if (totalCount > 0) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryFilter.entries) { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GlooCyan.copy(alpha = 0.22f),
                            selectedLabelColor = GlooCyan,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("profile_history_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun MatchHistoryItemCard(
    registration: Registration,
    liveMatch: TournamentMatch?,
    currencyLabel: String,
    highlightRoomReady: Boolean,
    onOpenMatch: (TournamentMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val roomReady = highlightRoomReady && liveMatch?.roomPublished == true
    val statusToShow = liveMatch?.status ?: if (registration.rankPosition > 0 || registration.kills > 0 || registration.prizeWon > 0) {
        MatchStatus.COMPLETED.name
    } else {
        MatchStatus.UPCOMING.name
    }
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            when {
                roomReady -> GlooCyan
                registration.prizeWon > 0L -> BlazeGold.copy(alpha = 0.6f)
                else -> OutlineSubtle
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = liveMatch != null) {
                if (liveMatch != null) onOpenMatch(liveMatch)
            }
            .testTag("profile_history_card_${registration.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = registration.tournamentTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${TeamType.fromRaw(registration.teamType).label} • ${registration.mapName} • ${registration.scheduleText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = statusToShow)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slot #${registration.slotNumber} • IGN: ${registration.ffName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Text(
                        text = registration.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (registration.status) {
                            "CONFIRMED" -> TacticalGreen
                            "REJECTED" -> DangerCrimson
                            else -> BlazeGold
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            val hasResult = registration.rankPosition > 0L || registration.kills > 0L || registration.prizeWon > 0L
            Surface(
                color = if (registration.prizeWon > 0L) BlazeGold.copy(alpha = 0.12f) else ObsidianSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (registration.prizeWon > 0L) BlazeGold.copy(alpha = 0.4f) else OutlineSubtle
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasResult) {
                        Text(
                            text = "Rank #${if (registration.rankPosition > 0) registration.rankPosition else "-"} • ${registration.kills} Kills",
                            style = MaterialTheme.typography.labelMedium,
                            color = GlooCyan
                        )
                        Text(
                            text = if (registration.prizeWon > 0L) {
                                "+${registration.prizeWon} $currencyLabel${if (registration.prizePaid) " • PAID" else ""}"
                            } else {
                                "Entry: ${if (registration.entryFee == 0L) "FREE" else "${registration.entryFee} $currencyLabel"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (registration.prizeWon > 0L) BlazeGold else TextSecondary
                        )
                    } else {
                        Text(
                            text = if (roomReady) "Room ID & Password Ready" else "Entry: ${if (registration.entryFee == 0L) "FREE" else "${registration.entryFee} $currencyLabel"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (roomReady) GlooCyan else TextSecondary
                        )
                        if (liveMatch != null) {
                            Text(
                                text = "VIEW MATCH →",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMatchHistoryCard(
    hasAnyRegistrations: Boolean,
    onExploreMatches: () -> Unit,
    onResetFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SportsEsports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = if (hasAnyRegistrations) "No Matches in This Filter" else "No Match History Yet",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (hasAnyRegistrations) {
                    "Switch filters above to view all tournaments you have entered."
                } else {
                    "Join a Free Fire custom room from Matches to track your slots, kills, ranks, and rewards here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (hasAnyRegistrations) {
                OutlinedButton(onClick = onResetFilter) {
                    Text("SHOW ALL HISTORY", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onExploreMatches,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("profile_browse_matches_button")
                ) {
                    Text("BROWSE MATCHES", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun AccountSettingsSection(
    profile: UserProfile?,
    userId: String,
    config: AppConfig,
    accountSettings: AccountSettings,
    notificationPermissionGranted: Boolean,
    adminUnlocked: Boolean,
    isBusy: Boolean,
    onSaveIdentity: (displayName: String, ffName: String, ffUid: String, inGameRank: String) -> Unit,
    onToggleAutoFill: (Boolean) -> Unit,
    onToggleCountdowns: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleRoomHighlights: (Boolean) -> Unit,
    onTogglePushNotifications: (Boolean) -> Unit,
    onToggleMatchReminders: (Boolean) -> Unit,
    onOpenAdmin: () -> Unit,
    onLockAdmin: () -> Unit,
    onCopyFeedback: (String) -> Unit,
    onRequestSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    var displayName by remember { mutableStateOf("") }
    var ffName by remember { mutableStateOf("") }
    var ffUid by remember { mutableStateOf("") }
    var selectedRank by remember { mutableStateOf(FreeFireRank.UNRANKED.label) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile?.userId, profile?.displayName, profile?.ffName, profile?.ffUid, profile?.inGameRank) {
        if (profile != null) {
            displayName = profile.displayName
            ffName = profile.ffName
            ffUid = profile.ffUid
            selectedRank = profile.inGameRank.ifBlank { FreeFireRank.UNRANKED.label }
        }
    }

    fun triggerHaptic() {
        if (accountSettings.hapticFeedbackEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_settings_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileSectionHeader(
            icon = Icons.Filled.Settings,
            title = "ACCOUNT SETTINGS",
            subtitle = "Manage your Free Fire identity, preferences & security",
            accent = MaterialTheme.colorScheme.primary
        )

        // 1. Free Fire Game Identity Form
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FREE FIRE ACCOUNT DETAILS", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (ffUid.isNotBlank()) {
                        Surface(
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    triggerHaptic()
                                    clipboard.setText(AnnotatedString(ffUid))
                                    onCopyFeedback("Free Fire UID copied!")
                                }
                                .testTag("profile_copy_uid_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy UID", tint = GlooCyan, modifier = Modifier.size(13.dp))
                                Text("UID", style = MaterialTheme.typography.labelSmall, color = GlooCyan)
                            }
                        }
                    }
                }
                Text(
                    text = "Save your Free Fire IGN and UID here so match registration auto-fills.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; validationError = null },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_input")
                )
                OutlinedTextField(
                    value = ffName,
                    onValueChange = { ffName = it; validationError = null },
                    label = { Text("Free Fire In-Game Name (IGN)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_ff_name_input")
                )
                OutlinedTextField(
                    value = ffUid,
                    onValueChange = { ffUid = it; validationError = null },
                    label = { Text("Free Fire UID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_ff_uid_input")
                )
                Text(
                    text = "Current In-Game Rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(FreeFireRank.entries) { rank ->
                        val isSelected = FreeFireRank.fromRaw(selectedRank) == rank
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                triggerHaptic()
                                selectedRank = rank.label
                            },
                            label = {
                                Text(
                                    text = rank.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = rank.accent.copy(alpha = 0.22f),
                                selectedLabelColor = rank.accent,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("profile_settings_rank_${rank.name.lowercase()}")
                        )
                    }
                }
                if (validationError != null) {
                    Text(validationError ?: "", style = MaterialTheme.typography.bodyMedium, color = DangerCrimson)
                }
                Button(
                    onClick = {
                        if (displayName.trim().length < 2) {
                            validationError = "Display Name must be at least 2 characters."
                        } else {
                            triggerHaptic()
                            onSaveIdentity(displayName, ffName, ffUid, selectedRank)
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_save_identity_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE FREE FIRE PROFILE", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // 2. App & Gameplay Preferences (DataStore)
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("APP & MATCH PREFERENCES", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))

                SettingSwitchRow(
                    icon = Icons.Filled.Badge,
                    title = "Auto-Fill IGN & UID",
                    subtitle = "Pre-fill saved Free Fire identity when joining matches",
                    checked = accountSettings.autoFillIdentity,
                    onCheckedChange = {
                        triggerHaptic()
                        onToggleAutoFill(it)
                    },
                    testTag = "profile_setting_autofill"
                )
                HorizontalDivider(color = OutlineSubtle.copy(alpha = 0.5f))
                SettingSwitchRow(
                    icon = Icons.Filled.Timer,
                    title = "Live Match Countdowns",
                    subtitle = "Show real-time kickoff countdown timers on match cards",
                    checked = accountSettings.showMatchCountdowns,
                    onCheckedChange = {
                        triggerHaptic()
                        onToggleCountdowns(it)
                    },
                    testTag = "profile_setting_countdowns"
                )
                HorizontalDivider(color = OutlineSubtle.copy(alpha = 0.5f))
                SettingSwitchRow(
                    icon = Icons.Filled.Notifications,
                    title = "Instant Push Alerts (FCM)",
                    subtitle = if (notificationPermissionGranted) {
                        "Receive instant alerts for payment approvals & Room ID / Password"
                    } else {
                        "Tap to allow system notifications for payments & Room ID drops"
                    },
                    checked = accountSettings.pushNotificationsEnabled && notificationPermissionGranted,
                    onCheckedChange = {
                        triggerHaptic()
                        onTogglePushNotifications(it)
                    },
                    testTag = "profile_setting_push_notifications"
                )
                HorizontalDivider(color = OutlineSubtle.copy(alpha = 0.5f))
                SettingSwitchRow(
                    icon = Icons.Filled.Alarm,
                    title = "15-Min Match Start Reminders",
                    subtitle = if (notificationPermissionGranted) {
                        "Get a local alert 15 minutes before joined matches start"
                    } else {
                        "Tap to allow notifications so 15-min match alerts can ring"
                    },
                    checked = accountSettings.matchRemindersEnabled && notificationPermissionGranted,
                    onCheckedChange = {
                        triggerHaptic()
                        onToggleMatchReminders(it)
                    },
                    testTag = "profile_setting_match_reminders"
                )
                HorizontalDivider(color = OutlineSubtle.copy(alpha = 0.5f))
                SettingSwitchRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Room Ready Highlights",
                    subtitle = "Highlight joined matches as soon as Room ID & Password drop",
                    checked = accountSettings.roomReminderHighlights,
                    onCheckedChange = {
                        triggerHaptic()
                        onToggleRoomHighlights(it)
                    },
                    testTag = "profile_setting_room_highlights"
                )
                HorizontalDivider(color = OutlineSubtle.copy(alpha = 0.5f))
                SettingSwitchRow(
                    icon = Icons.Filled.TouchApp,
                    title = "Tactile Haptic Feedback",
                    subtitle = "Vibrate subtly on profile saves, copies, and toggles",
                    checked = accountSettings.hapticFeedbackEnabled,
                    onCheckedChange = {
                        if (it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleHaptics(it)
                    },
                    testTag = "profile_setting_haptics"
                )
            }
        }

        // 3. Account Security & Support Card
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SECURITY & SUPPORT", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TacticalGreen, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Google Account Linked", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            Text(
                                text = profile?.email?.ifBlank { "Verified Sign-In" } ?: "Verified Sign-In",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        color = TacticalGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TacticalGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PLAYER ACCOUNT ID", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = userId.take(16) + if (userId.length > 16) "..." else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                clipboard.setText(AnnotatedString(userId))
                                onCopyFeedback("Account ID copied!")
                            },
                            modifier = Modifier.testTag("profile_copy_account_id")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Account ID", tint = GlooCyan)
                        }
                    }
                }

                TelegramHelpCard(
                    supportContact = config.supportContact,
                    onFallbackMessage = onCopyFeedback
                )
            }
        }

        // 4. Tournament Admin Command Card
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = BlazeGold)
                    Text("TOURNAMENT ADMIN COMMAND", style = MaterialTheme.typography.titleMedium, color = BlazeGold)
                }
                Text(
                    text = if (adminUnlocked) {
                        "Admin mode is unlocked. Customize matches, Room IDs, categories, banners, rules & themes."
                    } else {
                        "Organizer? Unlock Admin Panel with your Admin Password to manage everything live online."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = onOpenAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = BlazeGold, contentColor = ObsidianBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_admin_button")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (adminUnlocked) "OPEN ADMIN DASHBOARD" else "UNLOCK ADMIN PANEL",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (adminUnlocked) {
                    OutlinedButton(
                        onClick = onLockAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Admin Mode", color = TextSecondary)
                    }
                }
            }
        }

        // 5. Sign Out
        OutlinedButton(
            onClick = onRequestSignOut,
            border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerCrimson)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIGN OUT", style = MaterialTheme.typography.labelLarge, color = DangerCrimson)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) GlooCyan else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = GlooCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ObsidianSurface
            )
        )
    }
}

@Composable
fun InGameRankPickerDialog(
    currentRank: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSelectRank: (String) -> Unit
) {
    var chosenRank by remember(currentRank) { mutableStateOf(FreeFireRank.fromRaw(currentRank)) }
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = ObsidianCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = chosenRank.accent)
                Text(
                    text = "Set Current In-Game Rank",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select your current Free Fire Battle Royale or Clash Squad rank to display on your player profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FreeFireRank.entries.chunked(3).forEach { rowRanks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowRanks.forEach { rank ->
                                val selected = chosenRank == rank
                                Surface(
                                    color = if (selected) rank.accent.copy(alpha = 0.2f) else ObsidianSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) rank.accent else OutlineSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isBusy) { chosenRank = rank }
                                        .testTag("rank_picker_option_${rank.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rank.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) rank.accent else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectRank(chosenRank.label) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = chosenRank.accent, contentColor = ObsidianBg),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("confirm_in_game_rank_button")
            ) {
                Text("SAVE RANK", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

