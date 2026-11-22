package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentThemeOption
import com.example.data.model.AppConfig
import com.example.data.model.MatchCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.Registration
import com.example.data.model.RegistrationStatus
import com.example.data.model.RoomSecret
import com.example.data.model.TournamentMatch
import com.example.data.model.WalletRequest
import com.example.data.model.WalletRequestStatus
import com.example.data.model.WalletRequestType
import com.example.ui.components.CompactCountdownBadge
import com.example.ui.components.MetricPill
import com.example.ui.components.StatusBadge
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
import com.example.util.computeMatchCountdown
import com.example.util.rememberLiveClockMillis
import com.example.viewmodel.UiState

@Composable
fun AdminDashboardScreen(
    config: AppConfig,
    tournamentsState: UiState<List<TournamentMatch>>,
    categoriesState: UiState<List<MatchCategory>>,
    adminRegistrationsState: UiState<List<Registration>>,
    adminWalletRequestsState: UiState<List<WalletRequest>>,
    roomSecretsCache: Map<String, RoomSecret>,
    isBusy: Boolean,
    onSeedStarterPack: () -> Unit,
    onSaveConfig: (AppConfig) -> Unit,
    onSaveCategory: (MatchCategory, () -> Unit) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveTournament: (TournamentMatch, String, String, () -> Unit) -> Unit,
    onUpdateRoom: (String, String, String, Boolean, () -> Unit) -> Unit,
    onDeleteTournament: (String) -> Unit,
    onLoadRoomSecret: (String) -> Unit,
    onSelectMatchForRoster: (String?) -> Unit,
    onSaveParticipantResult: (Registration, RegistrationStatus, Long, Long, Long, Boolean) -> Unit,
    onProcessWalletRequest: (WalletRequest, Boolean, String) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onLockAdmin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val walletRequests = (adminWalletRequestsState as? UiState.Success)?.data ?: emptyList()
    val pendingWalletCount = walletRequests.count { it.status == WalletRequestStatus.PENDING.name }
    val tabs = listOf(
        "Matches",
        if (pendingWalletCount > 0) "Wallet ($pendingWalletCount)" else "Wallet",
        "Categories",
        "Customize App",
        "Overview"
    )

    val matches = (tournamentsState as? UiState.Success)?.data ?: emptyList()
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()

    var editingMatch by remember { mutableStateOf<TournamentMatch?>(null) }
    var showAddMatch by remember { mutableStateOf(false) }
    var roomDialogMatch by remember { mutableStateOf<TournamentMatch?>(null) }
    var rosterDialogMatch by remember { mutableStateOf<TournamentMatch?>(null) }
    var confirmDeleteMatch by remember { mutableStateOf<TournamentMatch?>(null) }

    var editingCategory by remember { mutableStateOf<MatchCategory?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Surface(color = ObsidianSurface, tonalElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_exit_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to App", tint = TextPrimary)
                    }
                    Column {
                        Text("ADMIN COMMAND CENTER", style = MaterialTheme.typography.titleLarge, color = BlazeGold)
                        Text("Live cloud sync • All changes push instantly", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                TextButton(onClick = {
                    onLockAdmin()
                    onBack()
                }) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = DangerCrimson, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LOCK", style = MaterialTheme.typography.labelMedium, color = DangerCrimson)
                }
            }
        }

        if (pendingWalletCount > 0 && selectedTab != 1) {
            Surface(
                color = DangerCrimson.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedTab = 1 }
                    .testTag("admin_wallet_notice_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = "$pendingWalletCount New Add Money / Withdraw Notice! Tap to Review",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                    Text("OPEN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BlazeGold)
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = BlazeGold,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == index) BlazeGold else if (index == 1 && pendingWalletCount > 0) DangerCrimson else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> AdminMatchesTab(
                matches = matches,
                currencyLabel = config.currencyLabel,
                onAddMatch = { showAddMatch = true },
                onEditMatch = { m ->
                    onLoadRoomSecret(m.id)
                    editingMatch = m
                },
                onEditRoom = { m -> roomDialogMatch = m },
                onViewPlayers = { m ->
                    onSelectMatchForRoster(m.id)
                    rosterDialogMatch = m
                },
                onDeleteMatch = { m -> confirmDeleteMatch = m }
            )
            1 -> AdminWalletTab(
                requests = walletRequests,
                currencyLabel = config.currencyLabel,
                isBusy = isBusy,
                onProcessRequest = onProcessWalletRequest,
                onCopyFeedback = onCopyFeedback
            )
            2 -> AdminCategoriesTab(
                categories = categories,
                onAddCategory = { showAddCategory = true },
                onEditCategory = { editingCategory = it },
                onDeleteCategory = onDeleteCategory
            )
            3 -> AdminCustomizeAppTab(
                config = config,
                isBusy = isBusy,
                onSaveConfig = onSaveConfig
            )
            4 -> AdminOverviewTab(
                config = config,
                matchCount = matches.size,
                categoryCount = categories.size,
                pendingWalletCount = pendingWalletCount,
                isBusy = isBusy,
                onSeedStarterPack = onSeedStarterPack,
                onAddMatch = {
                    selectedTab = 0
                    showAddMatch = true
                },
                onOpenWalletTab = { selectedTab = 1 },
                onCustomizeApp = { selectedTab = 3 }
            )
        }
    }

    if (showAddMatch || editingMatch != null) {
        AdminMatchEditorDialog(
            initialMatch = editingMatch,
            initialSecret = editingMatch?.let { roomSecretsCache[it.id] },
            categories = categories,
            isBusy = isBusy,
            onDismiss = {
                showAddMatch = false
                editingMatch = null
            },
            onSave = { match, rId, rPass ->
                onSaveTournament(match, rId, rPass) {
                    showAddMatch = false
                    editingMatch = null
                }
            }
        )
    }

    val rMatch = roomDialogMatch
    if (rMatch != null) {
        AdminRoomCredentialsDialog(
            match = rMatch,
            initialSecret = roomSecretsCache[rMatch.id],
            isBusy = isBusy,
            onLoadSecret = { onLoadRoomSecret(rMatch.id) },
            onDismiss = { roomDialogMatch = null },
            onSave = { rId, rPass, pub ->
                onUpdateRoom(rMatch.id, rId, rPass, pub) {
                    roomDialogMatch = null
                }
            }
        )
    }

    val pMatch = rosterDialogMatch
    if (pMatch != null) {
        AdminParticipantsDialog(
            match = pMatch,
            currencyLabel = config.currencyLabel,
            registrationsState = adminRegistrationsState,
            isBusy = isBusy,
            onDismiss = {
                rosterDialogMatch = null
                onSelectMatchForRoster(null)
            },
            onSaveParticipantResult = onSaveParticipantResult
        )
    }

    val delMatch = confirmDeleteMatch
    if (delMatch != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteMatch = null },
            containerColor = ObsidianCard,
            title = { Text("Delete Match?", style = MaterialTheme.typography.titleLarge, color = DangerCrimson) },
            text = { Text("Remove '${delMatch.title}' permanently for all players?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTournament(delMatch.id)
                        confirmDeleteMatch = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson)
                ) {
                    Text("DELETE", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteMatch = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    if (showAddCategory || editingCategory != null) {
        AdminCategoryEditorDialog(
            initialCategory = editingCategory,
            isBusy = isBusy,
            onDismiss = {
                showAddCategory = false
                editingCategory = null
            },
            onSave = { cat ->
                onSaveCategory(cat) {
                    showAddCategory = false
                    editingCategory = null
                }
            }
        )
    }
}

@Composable
private fun AdminMatchesTab(
    matches: List<TournamentMatch>,
    currencyLabel: String,
    onAddMatch: () -> Unit,
    onEditMatch: (TournamentMatch) -> Unit,
    onEditRoom: (TournamentMatch) -> Unit,
    onViewPlayers: (TournamentMatch) -> Unit,
    onDeleteMatch: (TournamentMatch) -> Unit
) {
    val currentTimeMillis = rememberLiveClockMillis()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onAddMatch,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_add_match_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD NEW TOURNAMENT MATCH", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (matches.isEmpty()) {
            item {
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(36.dp))
                        Text("No Matches Published", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "Tap 'ADD NEW TOURNAMENT MATCH' above or load Starter Pack from Overview.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(matches, key = { it.id }) { match ->
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
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
                                Text(match.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(
                                    text = "${match.categoryName} • ${match.teamType} • ${match.mapName} • ${match.filledSlots}/${match.totalSlots} slots",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Entry: ${match.entryFee} $currencyLabel • Prize: ${match.prizePool} $currencyLabel • ${match.scheduleText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlazeGold
                                )
                                val countdown = computeMatchCountdown(match.startTimeMillis, currentTimeMillis, match.status)
                                if (countdown != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    CompactCountdownBadge(countdown = countdown)
                                }
                            }
                            StatusBadge(status = match.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEditMatch(match) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onEditRoom(match) },
                                border = BorderStroke(1.dp, if (match.roomPublished) GlooCyan else OutlineSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, tint = GlooCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (match.roomPublished) "Room ON" else "Room",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlooCyan
                                )
                            }
                            OutlinedButton(
                                onClick = { onViewPlayers(match) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Players", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
                            }
                            IconButton(onClick = { onDeleteMatch(match) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Match", tint = DangerCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCategoriesTab(
    categories: List<MatchCategory>,
    onAddCategory: () -> Unit,
    onEditCategory: (MatchCategory) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onAddCategory,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_add_category_button")
            ) {
                Icon(Icons.Filled.Category, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD TOURNAMENT CATEGORY", style = MaterialTheme.typography.labelLarge)
            }
        }

        items(categories, key = { it.id }) { cat ->
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${cat.name} (${cat.badge})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(cat.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            text = if (cat.active) "VISIBLE TO PLAYERS • Order ${cat.sortOrder}" else "HIDDEN",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cat.active) TacticalGreen else DangerCrimson
                        )
                    }
                    Row {
                        IconButton(onClick = { onEditCategory(cat) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Category", tint = BlazeGold)
                        }
                        IconButton(onClick = { onDeleteCategory(cat.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Category", tint = DangerCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCustomizeAppTab(
    config: AppConfig,
    isBusy: Boolean,
    onSaveConfig: (AppConfig) -> Unit
) {
    var platformName by remember { mutableStateOf(config.platformName) }
    var tagline by remember { mutableStateOf(config.tagline) }
    var noticeText by remember { mutableStateOf(config.noticeText) }
    var noticeEnabled by remember { mutableStateOf(config.noticeEnabled) }
    var heroBadge by remember { mutableStateOf(config.heroBannerBadge) }
    var heroTitle by remember { mutableStateOf(config.heroBannerTitle) }
    var heroSubtitle by remember { mutableStateOf(config.heroBannerSubtitle) }
    var accentTheme by remember { mutableStateOf(config.accentTheme) }
    var currencyLabel by remember { mutableStateOf(config.currencyLabel) }
    var walletPaymentNumber by remember { mutableStateOf(config.walletPaymentNumber) }
    var paymentInfo by remember { mutableStateOf(config.paymentInfo) }
    var supportContact by remember { mutableStateOf(config.supportContact) }
    var globalRules by remember { mutableStateOf(config.globalRules) }
    var maintenanceMode by remember { mutableStateOf(config.maintenanceMode) }
    var maintenanceMessage by remember { mutableStateOf(config.maintenanceMessage) }

    LaunchedEffect(config.updatedAt) {
        platformName = config.platformName
        tagline = config.tagline
        noticeText = config.noticeText
        noticeEnabled = config.noticeEnabled
        heroBadge = config.heroBannerBadge
        heroTitle = config.heroBannerTitle
        heroSubtitle = config.heroBannerSubtitle
        accentTheme = config.accentTheme
        currencyLabel = config.currencyLabel
        walletPaymentNumber = config.walletPaymentNumber
        paymentInfo = config.paymentInfo
        supportContact = config.supportContact
        globalRules = config.globalRules
        maintenanceMode = config.maintenanceMode
        maintenanceMessage = config.maintenanceMessage
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("BRANDING & LIVE THEME", style = MaterialTheme.typography.titleMedium, color = BlazeGold)
            Text(
                text = "Everything you edit here syncs online to every player's device in real time.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            OutlinedTextField(
                value = platformName,
                onValueChange = { platformName = it },
                label = { Text("App Header Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_platform_name_input")
            )
        }
        item {
            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("Tagline Subtitle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Live Accent Color Theme", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentThemeOption.entries.forEach { option ->
                            val selected = accentTheme == option.key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { accentTheme = option.key }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(option.swatch)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) TextPrimary else OutlineSubtle,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.label.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Live Notice Banner", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Switch(checked = noticeEnabled, onCheckedChange = { noticeEnabled = it })
            }
            OutlinedTextField(
                value = noticeText,
                onValueChange = { noticeText = it },
                label = { Text("Notice Banner Text") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("FEATURED HERO BANNER", style = MaterialTheme.typography.titleMedium, color = BlazeGold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = heroBadge,
                onValueChange = { heroBadge = it },
                label = { Text("Hero Badge (e.g., SEASON 5 LIVE)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = heroTitle,
                onValueChange = { heroTitle = it },
                label = { Text("Hero Headline") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = heroSubtitle,
                onValueChange = { heroSubtitle = it },
                label = { Text("Hero Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("ECONOMY, PAYMENT & RULES", style = MaterialTheme.typography.titleMedium, color = BlazeGold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = currencyLabel,
                onValueChange = { currencyLabel = it },
                label = { Text("Currency Label (e.g., BDT ৳, Coins, Diamonds)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = walletPaymentNumber,
                onValueChange = { walletPaymentNumber = it },
                label = { Text("bKash / Nagad Deposit Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = paymentInfo,
                onValueChange = { paymentInfo = it },
                label = { Text("Payment / Entry Instructions") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = supportContact,
                onValueChange = { supportContact = it },
                label = { Text("Support Contact (Telegram / WhatsApp / Discord)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = globalRules,
                onValueChange = { globalRules = it },
                label = { Text("Global Tournament Rules") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Surface(
                color = DangerCrimson.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Maintenance Mode", style = MaterialTheme.typography.titleMedium, color = DangerCrimson)
                        Switch(checked = maintenanceMode, onCheckedChange = { maintenanceMode = it })
                    }
                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = { Text("Maintenance Message") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    onSaveConfig(
                        config.copy(
                            platformName = platformName,
                            tagline = tagline,
                            noticeText = noticeText,
                            noticeEnabled = noticeEnabled,
                            heroBannerBadge = heroBadge,
                            heroBannerTitle = heroTitle,
                            heroBannerSubtitle = heroSubtitle,
                            accentTheme = accentTheme,
                            currencyLabel = currencyLabel,
                            walletPaymentNumber = walletPaymentNumber,
                            paymentInfo = paymentInfo,
                            supportContact = supportContact,
                            globalRules = globalRules,
                            maintenanceMode = maintenanceMode,
                            maintenanceMessage = maintenanceMessage
                        )
                    )
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("admin_publish_config_button")
            ) {
                Icon(Icons.Filled.Publish, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PUBLISH ALL CHANGES LIVE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdminWalletTab(
    requests: List<WalletRequest>,
    currencyLabel: String,
    isBusy: Boolean,
    onProcessRequest: (WalletRequest, Boolean, String) -> Unit,
    onCopyFeedback: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showPendingOnly by remember { mutableStateOf(true) }
    val filtered = if (showPendingOnly) {
        requests.filter { it.status == WalletRequestStatus.PENDING.name }
    } else requests

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showPendingOnly,
                    onClick = { showPendingOnly = true },
                    label = {
                        val pending = requests.count { it.status == WalletRequestStatus.PENDING.name }
                        Text("Pending Notices ($pending)", style = MaterialTheme.typography.labelMedium)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = TextPrimary
                    )
                )
                FilterChip(
                    selected = !showPendingOnly,
                    onClick = { showPendingOnly = false },
                    label = { Text("All History (${requests.size})", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = GlooCyan, modifier = Modifier.size(38.dp))
                        Text(
                            text = if (showPendingOnly) "No Pending Wallet Notices" else "No Wallet Requests Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "New Add Money (TxnID) and Withdraw (bKash/Nagad number) requests appear here in real time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { req ->
                val isDeposit = WalletRequestType.fromRaw(req.type) == WalletRequestType.DEPOSIT
                val methodLabel = PaymentMethod.fromRaw(req.method).label
                val statusEnum = WalletRequestStatus.fromRaw(req.status)
                val borderAccent = when {
                    statusEnum == WalletRequestStatus.PENDING && isDeposit -> TacticalGreen
                    statusEnum == WalletRequestStatus.PENDING && !isDeposit -> BlazeGold
                    else -> OutlineSubtle
                }

                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, borderAccent.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
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
                            Surface(
                                color = (if (isDeposit) TacticalGreen else BlazeGold).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${if (isDeposit) "ADD MONEY NOTICE" else "WITHDRAW NOTICE"} • $methodLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeposit) TacticalGreen else BlazeGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "${req.amount} $currencyLabel",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "Player: ${req.userName}${if (req.ffName.isNotBlank()) " (IGN: ${req.ffName})" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        if (isDeposit) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TxnID: ${req.txnId}", style = MaterialTheme.typography.titleMedium, color = GlooCyan)
                                    if (req.accountNumber.isNotBlank()) {
                                        Text("Sender Number: ${req.accountNumber}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(req.txnId))
                                    onCopyFeedback("TxnID copied!")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy TxnID", tint = GlooCyan)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Player $methodLabel No: ${req.accountNumber}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                    color = BlazeGold
                                )
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(req.accountNumber))
                                    onCopyFeedback("$methodLabel number copied!")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Player Number", tint = BlazeGold)
                                }
                            }
                        }

                        if (statusEnum == WalletRequestStatus.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onProcessRequest(req, true, "") },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDeposit) TacticalGreen else BlazeGold,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier.weight(1f).testTag("admin_approve_wallet_${req.id}")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDeposit) "OK • ADD MONEY" else "MARK PAID",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onProcessRequest(req, false, "") },
                                    enabled = !isBusy,
                                    border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.6f)),
                                    modifier = Modifier.testTag("admin_reject_wallet_${req.id}")
                                ) {
                                    Icon(Icons.Filled.Cancel, contentDescription = null, tint = DangerCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject", style = MaterialTheme.typography.labelMedium, color = DangerCrimson)
                                }
                            }
                        } else {
                            Text(
                                text = "Status: ${statusEnum.label}${if (req.adminNote.isNotBlank()) " • ${req.adminNote}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (statusEnum == WalletRequestStatus.APPROVED) TacticalGreen else DangerCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    config: AppConfig,
    matchCount: Int,
    categoryCount: Int,
    pendingWalletCount: Int,
    isBusy: Boolean,
    onSeedStarterPack: () -> Unit,
    onAddMatch: () -> Unit,
    onOpenWalletTab: () -> Unit,
    onCustomizeApp: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(label = "MATCHES", value = "$matchCount", accent = BlazeGold, modifier = Modifier.weight(1f))
                MetricPill(label = "WALLET", value = "$pendingWalletCount Pending", accent = if (pendingWalletCount > 0) DangerCrimson else GlooCyan, modifier = Modifier.weight(1f))
                MetricPill(label = "CATEGORIES", value = "$categoryCount", accent = GlooCyan, modifier = Modifier.weight(1f))
                MetricPill(
                    label = "STATUS",
                    value = if (config.maintenanceMode) "MAINT" else "ONLINE",
                    accent = if (config.maintenanceMode) DangerCrimson else TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (pendingWalletCount > 0) {
            item {
                Button(
                    onClick = onOpenWalletTab,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REVIEW $pendingWalletCount PENDING WALLET NOTICES", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BlazeGold)
                        Text("Quick Start Tools", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    Text(
                        text = "Populate starter Free Fire categories (BR Full Map, Clash Squad, Lone Wolf) and 3 sample online tournaments with 1 tap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onSeedStarterPack,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = BlazeGold, contentColor = ObsidianBg),
                        modifier = Modifier.fillMaxWidth().testTag("admin_seed_starter_button")
                    ) {
                        Text("LOAD STARTER TOURNAMENTS & CATEGORIES", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(onClick = onAddMatch, modifier = Modifier.fillMaxWidth()) {
                        Text("Create Custom Match", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(onClick = onCustomizeApp, modifier = Modifier.fillMaxWidth()) {
                        Text("Customize App Theme, Rules & Branding", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
