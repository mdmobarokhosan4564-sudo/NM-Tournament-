package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.BannerArtwork
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.RoomSecret
import com.example.data.model.RosterEntry
import com.example.data.model.TeamType
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.ui.components.JoinTournamentDialog
import com.example.ui.components.MetricPill
import com.example.ui.components.RulesAndRegulationsSection
import com.example.ui.components.StatusBadge
import com.example.ui.components.TournamentCountdownCard
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
fun MatchDetailScreen(
    match: TournamentMatch,
    config: AppConfig,
    myRegistration: Registration?,
    roomSecret: RoomSecret?,
    rosterState: UiState<List<RosterEntry>>,
    userProfile: UserProfile?,
    autoFillIdentity: Boolean = true,
    isBusy: Boolean,
    onLoadRoomSecret: () -> Unit,
    onJoinMatch: (ffName: String, ffUid: String, squadName: String, paymentRef: String, onSuccess: () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val clipboard = LocalClipboardManager.current
    var showJoinDialog by remember { mutableStateOf(false) }
    var showRoomDetailsDialog by remember { mutableStateOf(false) }

    val hasActiveRoomAccess = myRegistration != null &&
        !myRegistration.status.equals("REJECTED", ignoreCase = true)
    val visibleRoomSecret = if (hasActiveRoomAccess) roomSecret else null

    LaunchedEffect(match.id, match.roomPublished, hasActiveRoomAccess) {
        if (hasActiveRoomAccess && match.roomPublished) {
            onLoadRoomSecret()
        }
    }

    val artwork = BannerArtwork.fromKey(match.bannerKey)
    val matchStatus = MatchStatus.fromRaw(match.status)
    val isFull = match.filledSlots >= match.totalSlots
    val currentTimeMillis = rememberLiveClockMillis()
    val countdown = computeMatchCountdown(match.startTimeMillis, currentTimeMillis, match.status)

    Scaffold(
        containerColor = ObsidianBg,
        bottomBar = {
            Surface(
                color = ObsidianSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    when {
                        hasActiveRoomAccess && myRegistration != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = TacticalGreen.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.7f)),
                                    modifier = Modifier.weight(1f).height(52.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TacticalGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "JOINED • #${myRegistration.slotNumber}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TacticalGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (match.roomPublished && visibleRoomSecret == null) {
                                            onLoadRoomSecret()
                                        }
                                        showRoomDetailsDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (match.roomPublished) GlooCyan else BlazeGold,
                                        contentColor = ObsidianBg
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .height(52.dp)
                                        .testTag("room_details_button")
                                ) {
                                    Icon(
                                        imageVector = if (match.roomPublished) Icons.Filled.Key else Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ROOM DETAILS",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showRoomDetailsDialog = true },
                                    border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("room_details_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = BlazeGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ROOM DETAILS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BlazeGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                when {
                                    matchStatus != MatchStatus.UPCOMING -> {
                                        Button(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .height(52.dp)
                                        ) {
                                            Text(
                                                text = "CLOSED (${matchStatus.label})",
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    isFull -> {
                                        Button(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .height(52.dp)
                                        ) {
                                            Text(
                                                text = "FULL (${match.filledSlots}/${match.totalSlots})",
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = { showJoinDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1.15f)
                                                .height(52.dp)
                                                .testTag("join_match_button")
                                        ) {
                                            Text(
                                                text = if (match.entryFee == 0L) "JOIN MATCH" else "JOIN • ${match.entryFee} ${config.currencyLabel}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
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
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Image(
                        painter = painterResource(id = artwork.drawableRes),
                        contentDescription = match.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        ObsidianBg.copy(alpha = 0.45f),
                                        ObsidianBg.copy(alpha = 0.72f),
                                        ObsidianBg
                                    )
                                )
                            )
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .testTag("match_detail_back")
                    ) {
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(status = match.status)
                            Surface(
                                color = ObsidianCard,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, OutlineSubtle)
                            ) {
                                Text(
                                    text = "${TeamType.fromRaw(match.teamType).label} • ${match.mapName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = match.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${match.categoryName} • ${match.perspective} • ${match.scheduleText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "PRIZE POOL",
                        value = "${match.prizePool} ${config.currencyLabel}",
                        accent = BlazeGold,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "PER KILL",
                        value = "${match.perKillReward} ${config.currencyLabel}",
                        accent = GlooCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "SLOTS",
                        value = "${match.filledSlots}/${match.totalSlots}",
                        accent = TacticalGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (countdown != null) {
                item {
                    TournamentCountdownCard(
                        countdown = countdown,
                        scheduleText = match.scheduleText,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (!hasActiveRoomAccess) {
                item {
                    RulesAndRegulationsSection(
                        matchRules = match.rules,
                        globalRules = config.globalRules,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("match_rules_regulations_section")
                    )
                }
            }

            // Room ID & Password Vault (Clickable for both buyers and non-buyers)
            item {
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (hasActiveRoomAccess && match.roomPublished) GlooCyan else OutlineSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (hasActiveRoomAccess && match.roomPublished && visibleRoomSecret == null) {
                                onLoadRoomSecret()
                            }
                            showRoomDetailsDialog = true
                        }
                        .testTag("room_details_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (hasActiveRoomAccess && match.roomPublished) Icons.Filled.Key else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (hasActiveRoomAccess && match.roomPublished) GlooCyan else BlazeGold
                            )
                            Text("ROOM DETAILS • ID & PASSWORD", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }

                        when {
                            !hasActiveRoomAccess || myRegistration == null -> {
                                Surface(
                                    color = DangerCrimson.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.room_not_purchased_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerCrimson
                                        )
                                        Text(
                                            text = stringResource(id = R.string.room_not_purchased_message),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            !match.roomPublished -> {
                                Text(
                                    text = "You're registered in Slot #${myRegistration.slotNumber}. Admin will publish the Room ID & Password before match start.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BlazeGold
                                )
                            }
                            visibleRoomSecret == null -> {
                                OutlinedButton(onClick = onLoadRoomSecret) {
                                    Text("LOAD ROOM ID & PASSWORD", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            else -> {
                                CredentialCopyRow(
                                    label = "ROOM ID",
                                    value = visibleRoomSecret.roomId.ifBlank { "Pending" },
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(visibleRoomSecret.roomId))
                                        onCopyFeedback("Room ID copied!")
                                    }
                                )
                                CredentialCopyRow(
                                    label = "PASSWORD",
                                    value = visibleRoomSecret.roomPassword.ifBlank { "None" },
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(visibleRoomSecret.roomPassword))
                                        onCopyFeedback("Room Password copied!")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (match.winnerSummary.isNotBlank()) {
                item {
                    Surface(
                        color = BlazeGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = BlazeGold)
                            Column {
                                Text("OFFICIAL MATCH RESULT", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
                                Text(match.winnerSummary, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            if (hasActiveRoomAccess) {
                item {
                    RulesAndRegulationsSection(
                        matchRules = match.rules,
                        globalRules = config.globalRules,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("match_rules_regulations_section")
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = GlooCyan)
                    Text("JOINED PLAYERS & SCOREBOARD", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }

            when (rosterState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = rosterState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DangerCrimson,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    if (rosterState.data.isEmpty()) {
                        item {
                            Text(
                                text = "No players have joined yet. Be the first to claim Slot #1!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(rosterState.data, key = { it.id }) { entry ->
                            Surface(
                                color = ObsidianCard,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, OutlineSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = ObsidianSurface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, OutlineSubtle)
                                        ) {
                                            Text(
                                                text = "#${entry.slotNumber}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = BlazeGold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Column {
                                            Text(entry.ffName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                            if (entry.squadName.isNotBlank()) {
                                                Text(entry.squadName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (entry.rankPosition > 0 || entry.kills > 0 || entry.prizeWon > 0) {
                                            Text(
                                                text = "${if (entry.rankPosition > 0) "Rank #${entry.rankPosition} • " else ""}${entry.kills} Kills",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = GlooCyan
                                            )
                                            if (entry.prizeWon > 0) {
                                                Text(
                                                    text = "+${entry.prizeWon} ${config.currencyLabel}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BlazeGold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = entry.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (entry.status == "CONFIRMED") TacticalGreen else BlazeGold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinTournamentDialog(
            match = match,
            config = config,
            initialFfName = if (autoFillIdentity) userProfile?.ffName ?: "" else "",
            initialFfUid = if (autoFillIdentity) userProfile?.ffUid ?: "" else "",
            isBusy = isBusy,
            onDismiss = { showJoinDialog = false },
            onSubmit = { ign, uid, squad, payRef ->
                onJoinMatch(ign, uid, squad, payRef) {
                    showJoinDialog = false
                }
            }
        )
    }

    if (showRoomDetailsDialog) {
        RoomDetailsDialog(
            match = match,
            myRegistration = if (hasActiveRoomAccess) myRegistration else null,
            roomSecret = visibleRoomSecret,
            canJoinNow = !hasActiveRoomAccess && matchStatus == MatchStatus.UPCOMING && !isFull,
            onJoinNow = {
                showRoomDetailsDialog = false
                showJoinDialog = true
            },
            onRefresh = onLoadRoomSecret,
            onCopy = { label, value ->
                clipboard.setText(AnnotatedString(value))
                onCopyFeedback("$label copied!")
            },
            onDismiss = { showRoomDetailsDialog = false }
        )
    }
}

@Composable
private fun RoomDetailsDialog(
    match: TournamentMatch,
    myRegistration: Registration?,
    roomSecret: RoomSecret?,
    canJoinNow: Boolean,
    onJoinNow: () -> Unit,
    onRefresh: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isPurchased = myRegistration != null && !myRegistration.status.equals("REJECTED", ignoreCase = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (isPurchased && match.roomPublished) Icons.Filled.Key else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isPurchased && match.roomPublished) GlooCyan else if (!isPurchased) DangerCrimson else BlazeGold
                )
                Text("Room Details", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(match.title, style = MaterialTheme.typography.titleMedium, color = BlazeGold)
                if (!isPurchased) {
                    Surface(
                        color = DangerCrimson.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_not_purchased_dialog_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.room_not_purchased_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DangerCrimson
                            )
                            Text(
                                text = stringResource(id = R.string.room_not_purchased_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Slot #${myRegistration!!.slotNumber} • IGN: ${myRegistration.ffName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalGreen
                    )
                    when {
                        !match.roomPublished -> {
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, OutlineSubtle)
                            ) {
                                Text(
                                    text = "Admin has not published the Room ID & Password yet. Once Admin sets them in the Admin panel, they will appear here automatically for all joined players.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        roomSecret == null -> {
                            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                                Text("REFRESH ROOM DETAILS", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        else -> {
                            CredentialCopyRow(
                                label = "ROOM ID",
                                value = roomSecret.roomId.ifBlank { "Pending" },
                                onCopy = { onCopy("Room ID", roomSecret.roomId) }
                            )
                            CredentialCopyRow(
                                label = "PASSWORD",
                                value = roomSecret.roomPassword.ifBlank { "None" },
                                onCopy = { onCopy("Room Password", roomSecret.roomPassword) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (canJoinNow) {
                Button(
                    onClick = onJoinNow,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("JOIN NOW", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CLOSE", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = {
            if (canJoinNow) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun CredentialCopyRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(value, style = MaterialTheme.typography.titleLarge, color = GlooCyan)
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label", tint = BlazeGold)
            }
        }
    }
}
