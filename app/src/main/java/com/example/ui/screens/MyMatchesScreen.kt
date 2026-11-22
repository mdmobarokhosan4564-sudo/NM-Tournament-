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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.RoomSecret
import com.example.data.model.TournamentMatch
import com.example.ui.components.CompactCountdownBadge
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
import com.example.util.computeMatchCountdown
import com.example.util.rememberLiveClockMillis
import com.example.viewmodel.UiState

@Composable
fun MyMatchesScreen(
    config: AppConfig,
    registrationsState: UiState<List<Registration>>,
    tournamentsState: UiState<List<TournamentMatch>>,
    roomSecretsCache: Map<String, RoomSecret>,
    showCountdowns: Boolean = true,
    highlightRoomReady: Boolean = true,
    matchRemindersEnabled: Boolean = true,
    onLoadRoomSecret: (String) -> Unit,
    onOpenMatch: (TournamentMatch) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onExploreMatches: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val tourneyById = (tournamentsState as? UiState.Success)?.data?.associateBy { it.id } ?: emptyMap()
    val currentTimeMillis = rememberLiveClockMillis()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("MY REGISTERED MATCHES", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Room Details (Room ID & Password) appear here automatically once Admin publishes them.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        when (registrationsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(registrationsState.message, color = DangerCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UiState.Success -> {
                if (registrationsState.data.isEmpty()) {
                    item {
                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, OutlineSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text("No Joined Matches Yet", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(
                                    text = "Browse upcoming Free Fire tournaments and register your IGN & UID to lock your slot.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onExploreMatches,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("BROWSE MATCHES", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                } else {
                    items(registrationsState.data, key = { it.id }) { reg ->
                        val liveMatch = tourneyById[reg.tournamentId]
                        val isActiveRegistration = !reg.status.equals("REJECTED", ignoreCase = true)
                        val roomPublished = isActiveRegistration && liveMatch?.roomPublished == true
                        val secret = if (isActiveRegistration) roomSecretsCache[reg.tournamentId] else null
                        val countdown = liveMatch?.let {
                            computeMatchCountdown(it.startTimeMillis, currentTimeMillis, it.status)
                        }
                        LaunchedEffect(reg.tournamentId, roomPublished, isActiveRegistration) {
                            if (isActiveRegistration && roomPublished && secret == null) {
                                onLoadRoomSecret(reg.tournamentId)
                            }
                        }

                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (roomPublished && highlightRoomReady) GlooCyan else OutlineSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { if (liveMatch != null) onOpenMatch(liveMatch) }
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reg.tournamentTitle, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                        Text(
                                            text = "${reg.teamType} • ${reg.mapName} • ${reg.scheduleText}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Surface(
                                        color = ObsidianSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "SLOT #${reg.slotNumber}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BlazeGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                val isReminderArmed = matchRemindersEnabled &&
                                    liveMatch != null &&
                                    MatchStatus.fromRaw(liveMatch.status) == MatchStatus.UPCOMING &&
                                    liveMatch.startTimeMillis > currentTimeMillis &&
                                    !reg.status.equals("REJECTED", ignoreCase = true)

                                if ((showCountdowns && countdown != null) || isReminderArmed) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (showCountdowns && countdown != null) {
                                            Text(
                                                text = "Match Starts In:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            CompactCountdownBadge(countdown = countdown)
                                        }
                                        if (isReminderArmed) {
                                            Surface(
                                                color = TacticalGreen.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.45f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Alarm,
                                                        contentDescription = "15-minute match alert armed",
                                                        tint = TacticalGreen,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "15m Alert Set",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TacticalGreen
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "IGN: ${reg.ffName} (UID: ${reg.ffUid})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = reg.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (reg.status) {
                                            "CONFIRMED" -> TacticalGreen
                                            "REJECTED" -> DangerCrimson
                                            else -> BlazeGold
                                        }
                                    )
                                }

                                if (reg.kills > 0 || reg.rankPosition > 0 || reg.prizeWon > 0) {
                                    Surface(
                                        color = BlazeGold.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Rank #${if (reg.rankPosition > 0) reg.rankPosition else "-"} • ${reg.kills} Kills",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Won: ${reg.prizeWon} ${config.currencyLabel}${if (reg.prizePaid) " (PAID)" else ""}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = BlazeGold
                                            )
                                        }
                                    }
                                }

                                if (!isActiveRegistration) {
                                    Text(
                                        text = "আপনি রুমটি কেনেননি (Registration Rejected)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DangerCrimson
                                    )
                                } else if (roomPublished) {
                                    if (secret == null) {
                                        Button(
                                            onClick = { onLoadRoomSecret(reg.tournamentId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GlooCyan, contentColor = ObsidianBg),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.size(8.dp))
                                            Text("VIEW ROOM DETAILS", style = MaterialTheme.typography.labelLarge)
                                        }
                                    } else {
                                        Surface(
                                            color = ObsidianSurface,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.6f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("ROOM DETAILS", style = MaterialTheme.typography.labelSmall, color = TacticalGreen)
                                                    Text("ROOM ID: ${secret.roomId.ifBlank { "Pending" }}", style = MaterialTheme.typography.titleMedium, color = GlooCyan)
                                                    Text("PASS: ${secret.roomPassword.ifBlank { "None" }}", style = MaterialTheme.typography.titleMedium, color = BlazeGold)
                                                }
                                                Row {
                                                    IconButton(onClick = {
                                                        clipboard.setText(AnnotatedString(secret.roomId))
                                                        onCopyFeedback("Room ID copied!")
                                                    }) {
                                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Room ID", tint = GlooCyan)
                                                    }
                                                    IconButton(onClick = {
                                                        clipboard.setText(AnnotatedString(secret.roomPassword))
                                                        onCopyFeedback("Password copied!")
                                                    }) {
                                                        Icon(Icons.Filled.Key, contentDescription = "Copy Password", tint = BlazeGold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { if (liveMatch != null) onOpenMatch(liveMatch) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text("Room Details • Waiting for Admin", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
