package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BannerArtwork
import com.example.data.model.MatchCategory
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.RegistrationStatus
import com.example.data.model.RoomSecret
import com.example.data.model.TOURNAMENT_RULE_PRESETS
import com.example.data.model.TeamType
import com.example.data.model.TournamentMatch
import com.example.data.model.isRulePresetSelected
import com.example.data.model.toggleRulePreset
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.computeMatchCountdown
import com.example.util.formatCompactCountdown
import com.example.util.formatScheduleTextFromMillis
import com.example.util.rememberLiveClockMillis
import com.example.viewmodel.UiState
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMatchEditorDialog(
    initialMatch: TournamentMatch?,
    initialSecret: RoomSecret?,
    categories: List<MatchCategory>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (match: TournamentMatch, roomId: String, roomPassword: String) -> Unit
) {
    val context = LocalContext.current
    val currentTimeMillis = rememberLiveClockMillis()
    var title by remember { mutableStateOf(initialMatch?.title ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(initialMatch?.categoryId?.ifBlank { null } ?: categories.firstOrNull()?.id ?: "cat_br")
    }
    var teamType by remember { mutableStateOf(initialMatch?.teamType ?: TeamType.SQUAD.name) }
    var mapName by remember { mutableStateOf(initialMatch?.mapName ?: "Bermuda") }
    var perspective by remember { mutableStateOf(initialMatch?.perspective ?: "Mobile Only") }
    var entryFee by remember { mutableStateOf((initialMatch?.entryFee ?: 0L).toString()) }
    var prizePool by remember { mutableStateOf((initialMatch?.prizePool ?: 500L).toString()) }
    var perKillReward by remember { mutableStateOf((initialMatch?.perKillReward ?: 15L).toString()) }
    var totalSlots by remember { mutableStateOf((initialMatch?.totalSlots ?: 48L).toString()) }
    var startTimeMillis by remember {
        mutableLongStateOf(initialMatch?.startTimeMillis ?: (System.currentTimeMillis() + 60L * 60_000L))
    }
    var scheduleText by remember {
        mutableStateOf(
            initialMatch?.scheduleText ?: formatScheduleTextFromMillis(startTimeMillis)
        )
    }
    var status by remember { mutableStateOf(initialMatch?.status ?: MatchStatus.UPCOMING.name) }
    var bannerKey by remember { mutableStateOf(initialMatch?.bannerKey ?: "br") }
    var featured by remember { mutableStateOf(initialMatch?.featured ?: false) }
    var rules by remember {
        mutableStateOf(
            initialMatch?.rules ?: "No hackers, scripts, or mod APKs\nNo teaming with opponents\nMobile devices only (no emulator)\nJoin custom room 10 mins early"
        )
    }
    var winnerSummary by remember { mutableStateOf(initialMatch?.winnerSummary ?: "") }
    var roomId by remember { mutableStateOf(initialSecret?.roomId ?: "") }
    var roomPassword by remember { mutableStateOf(initialSecret?.roomPassword ?: "") }
    var roomPublished by remember { mutableStateOf(initialMatch?.roomPublished ?: false) }
    var userToggledPublish by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun applyStartTime(newMillis: Long) {
        startTimeMillis = newMillis
        if (newMillis > 0L) {
            scheduleText = formatScheduleTextFromMillis(newMillis)
        }
    }

    fun openDateTimePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (startTimeMillis > 0L) startTimeMillis else (System.currentTimeMillis() + 3600_000L)
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        pickedCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        pickedCal.set(Calendar.MINUTE, minute)
                        applyStartTime(pickedCal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text(
                text = if (initialMatch == null) "Add New Match" else "Edit Match",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Match Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_match_title_input")
                )

                if (categories.isNotEmpty()) {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    bannerKey = cat.bannerKey
                                },
                                label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }
                }

                Text("Team Format", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TeamType.entries.forEach { t ->
                        FilterChip(
                            selected = teamType == t.name,
                            onClick = { teamType = t.name },
                            label = { Text(t.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = mapName,
                    onValueChange = { mapName = it },
                    label = { Text("Map (e.g., Bermuda, Purgatory, Kalahari, Iron Cage)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = perspective,
                    onValueChange = { perspective = it },
                    label = { Text("Device / Mode Note (e.g., Mobile Only)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryFee,
                        onValueChange = { entryFee = it.filter(Char::isDigit) },
                        label = { Text("Entry Fee") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prizePool,
                        onValueChange = { prizePool = it.filter(Char::isDigit) },
                        label = { Text("Prize Pool") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = perKillReward,
                        onValueChange = { perKillReward = it.filter(Char::isDigit) },
                        label = { Text("Per Kill") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalSlots,
                        onValueChange = { totalSlots = it.filter(Char::isDigit) },
                        label = { Text("Total Slots") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = scheduleText,
                    onValueChange = { scheduleText = it },
                    label = { Text("Display Schedule Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = GlooCyan, modifier = Modifier.size(16.dp))
                            Text("Live Countdown Kickoff Timer", style = MaterialTheme.typography.labelMedium, color = GlooCyan)
                        }
                        val previewCountdown = computeMatchCountdown(startTimeMillis, currentTimeMillis, status)
                        Text(
                            text = when {
                                startTimeMillis <= 0L -> "Timer Disabled (shows Schedule Label only)"
                                previewCountdown != null -> "Live Preview: ${formatCompactCountdown(previewCountdown)}"
                                else -> "Timer set (visible when status is UPCOMING)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = BlazeGold
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                15L to "+15m",
                                30L to "+30m",
                                60L to "+1h",
                                180L to "+3h",
                                720L to "+12h",
                                1440L to "+24h"
                            ).forEach { (mins, label) ->
                                FilterChip(
                                    selected = false,
                                    onClick = { applyStartTime(System.currentTimeMillis() + mins * 60_000L) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            FilterChip(
                                selected = startTimeMillis == 0L,
                                onClick = { startTimeMillis = 0L },
                                label = { Text("No Timer", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        OutlinedButton(
                            onClick = { openDateTimePicker() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PICK EXACT DATE & TIME", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text("Match Status", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MatchStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st.name,
                            onClick = { status = st.name },
                            label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Card Banner Artwork", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BannerArtwork.entries.forEach { art ->
                        FilterChip(
                            selected = bannerKey == art.key,
                            onClick = { bannerKey = art.key },
                            label = { Text(art.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin as Featured Match", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }

                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gavel,
                                contentDescription = null,
                                tint = BlazeGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.rules_regulations_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.rules_regulations_admin_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TOURNAMENT_RULE_PRESETS.forEach { preset ->
                                val selected = isRulePresetSelected(rules, preset)
                                FilterChip(
                                    selected = selected,
                                    onClick = { rules = toggleRulePreset(rules, preset) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlazeGold.copy(alpha = 0.18f),
                                        selectedLabelColor = BlazeGold,
                                        selectedLeadingIconColor = BlazeGold
                                    )
                                )
                            }
                        }
                        OutlinedTextField(
                            value = rules,
                            onValueChange = { if (it.length <= 1500) rules = it },
                            label = { Text("Rules & Regulations (one per line)") },
                            supportingText = {
                                Text(
                                    text = "${rules.length}/1500 • Displayed before joining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_match_rules_input")
                        )
                    }
                }

                OutlinedTextField(
                    value = winnerSummary,
                    onValueChange = { winnerSummary = it },
                    label = { Text("Winner Summary (optional, shown when completed)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Room Credentials (Optional)", style = MaterialTheme.typography.labelMedium, color = GlooCyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomId,
                        onValueChange = {
                            roomId = it
                            if (!userToggledPublish && it.isNotBlank()) roomPublished = true
                        },
                        label = { Text("Room ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = roomPassword,
                        onValueChange = {
                            roomPassword = it
                            if (!userToggledPublish && it.isNotBlank()) roomPublished = true
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish Room ID to Joined Players", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(
                        checked = roomPublished,
                        onCheckedChange = {
                            userToggledPublish = true
                            roomPublished = it
                        }
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = DangerCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val slots = (totalSlots.toLongOrNull() ?: 48L).coerceIn(2L, 100L)
                    if (title.trim().length < 2) {
                        errorMsg = "Match title must be at least 2 characters."
                        return@Button
                    }
                    val catName = categories.firstOrNull { it.id == selectedCategoryId }?.name
                        ?: initialMatch?.categoryName
                        ?: "BR Full Map"
                    val match = (initialMatch ?: TournamentMatch()).copy(
                        title = title.trim(),
                        categoryId = selectedCategoryId,
                        categoryName = catName,
                        teamType = teamType,
                        mapName = mapName.trim().ifBlank { "Bermuda" },
                        perspective = perspective.trim().ifBlank { "Mobile Only" },
                        entryFee = (entryFee.toLongOrNull() ?: 0L).coerceIn(0L, 100000L),
                        prizePool = (prizePool.toLongOrNull() ?: 0L).coerceIn(0L, 1000000L),
                        perKillReward = (perKillReward.toLongOrNull() ?: 0L).coerceIn(0L, 50000L),
                        totalSlots = slots,
                        filledSlots = (initialMatch?.filledSlots ?: 0L).coerceAtMost(slots),
                        scheduleText = scheduleText.trim().ifBlank { "TBA" },
                        startTimeMillis = startTimeMillis.coerceAtLeast(0L),
                        status = status,
                        rules = rules.trim(),
                        roomPublished = roomPublished,
                        winnerSummary = winnerSummary.trim(),
                        featured = featured,
                        bannerKey = bannerKey
                    )
                    onSave(match, roomId.trim(), roomPassword.trim())
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("admin_save_match_button")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                else Text("SAVE & PUBLISH", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AdminRoomCredentialsDialog(
    match: TournamentMatch,
    initialSecret: RoomSecret?,
    isBusy: Boolean,
    onLoadSecret: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (roomId: String, roomPassword: String, publishNow: Boolean) -> Unit
) {
    LaunchedEffect(match.id) { onLoadSecret() }

    var roomId by remember(initialSecret) { mutableStateOf(initialSecret?.roomId ?: "") }
    var roomPass by remember(initialSecret) { mutableStateOf(initialSecret?.roomPassword ?: "") }
    var publishNow by remember(match.id) { mutableStateOf(match.roomPublished) }
    var userToggledPublish by remember(match.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Key, contentDescription = null, tint = GlooCyan)
                Text("Room ID & Password", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(match.title, style = MaterialTheme.typography.titleMedium, color = BlazeGold)
                OutlinedTextField(
                    value = roomId,
                    onValueChange = {
                        roomId = it
                        if (!userToggledPublish && it.isNotBlank()) publishNow = true
                    },
                    label = { Text("Custom Room ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomPass,
                    onValueChange = {
                        roomPass = it
                        if (!userToggledPublish && it.isNotBlank()) publishNow = true
                    },
                    label = { Text("Room Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reveal to Joined Players", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(
                        checked = publishNow,
                        onCheckedChange = {
                            userToggledPublish = true
                            publishNow = it
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(roomId, roomPass, publishNow) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = GlooCyan, contentColor = ObsidianSurface)
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("UPDATE ROOM", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCategoryEditorDialog(
    initialCategory: MatchCategory?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (MatchCategory) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var subtitle by remember { mutableStateOf(initialCategory?.subtitle ?: "") }
    var badge by remember { mutableStateOf(initialCategory?.badge ?: "FF") }
    var sortOrder by remember { mutableStateOf((initialCategory?.sortOrder ?: 1L).toString()) }
    var bannerKey by remember { mutableStateOf(initialCategory?.bannerKey ?: "br") }
    var active by remember { mutableStateOf(initialCategory?.active ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text(
                text = if (initialCategory == null) "Add Category" else "Edit Category",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Category Name (e.g., BR Full Map)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = badge,
                        onValueChange = { badge = it },
                        label = { Text("Badge (e.g., 48P)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sortOrder,
                        onValueChange = { sortOrder = it.filter(Char::isDigit) },
                        label = { Text("Order") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Default Banner Art", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BannerArtwork.entries.forEach { art ->
                        FilterChip(
                            selected = bannerKey == art.key,
                            onClick = { bannerKey = art.key },
                            label = { Text(art.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visible to Players", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = DangerCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length < 2) {
                        errorMsg = "Category name must be 2+ characters."
                        return@Button
                    }
                    onSave(
                        (initialCategory ?: MatchCategory()).copy(
                            name = name.trim(),
                            subtitle = subtitle.trim(),
                            badge = badge.trim().ifBlank { "FF" },
                            bannerKey = bannerKey,
                            sortOrder = (sortOrder.toLongOrNull() ?: 1L).coerceIn(0L, 1000L),
                            active = active
                        )
                    )
                },
                enabled = !isBusy
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                else Text("SAVE CATEGORY", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun AdminParticipantsDialog(
    match: TournamentMatch,
    currencyLabel: String,
    registrationsState: UiState<List<Registration>>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSaveParticipantResult: (
        reg: Registration,
        newStatus: RegistrationStatus,
        kills: Long,
        rankPosition: Long,
        prizeWon: Long,
        prizePaid: Boolean
    ) -> Unit
) {
    var editingReg by remember { mutableStateOf<Registration?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = BlazeGold)
                Column {
                    Text("Players & Results", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(match.title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 420.dp)
            ) {
                when (registrationsState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is UiState.Error -> {
                        Text(registrationsState.message, color = DangerCrimson, style = MaterialTheme.typography.bodyMedium)
                    }
                    is UiState.Success -> {
                        if (registrationsState.data.isEmpty()) {
                            Text(
                                text = "No players have registered for this match yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(registrationsState.data, key = { it.id }) { reg ->
                                    Surface(
                                        color = ObsidianSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, OutlineSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "#${reg.slotNumber} ${reg.ffName}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "UID: ${reg.ffUid} • ${reg.status}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (reg.status == "CONFIRMED") TacticalGreen else BlazeGold
                                                )
                                                if (reg.paymentRef.isNotBlank()) {
                                                    Text("PayRef: ${reg.paymentRef}", style = MaterialTheme.typography.labelSmall, color = GlooCyan)
                                                }
                                                Text(
                                                    text = "Rank: ${reg.rankPosition} • Kills: ${reg.kills} • Prize: ${reg.prizeWon} $currencyLabel",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                            IconButton(onClick = { editingReg = reg }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Score / Approve", tint = BlazeGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextPrimary)
            }
        }
    )

    val target = editingReg
    if (target != null) {
        var status by remember(target.id) { mutableStateOf(RegistrationStatus.fromRaw(target.status)) }
        var kills by remember(target.id) { mutableStateOf(target.kills.toString()) }
        var rankPos by remember(target.id) { mutableStateOf(target.rankPosition.toString()) }
        var prizeWon by remember(target.id) { mutableStateOf(target.prizeWon.toString()) }
        var prizePaid by remember(target.id) { mutableStateOf(target.prizePaid) }

        AlertDialog(
            onDismissRequest = { editingReg = null },
            containerColor = ObsidianCard,
            title = { Text("Update ${target.ffName}", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Registration Status", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RegistrationStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = kills,
                            onValueChange = { kills = it.filter(Char::isDigit) },
                            label = { Text("Kills") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rankPos,
                            onValueChange = { rankPos = it.filter(Char::isDigit) },
                            label = { Text("Rank # (1=Win)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = prizeWon,
                        onValueChange = { prizeWon = it.filter(Char::isDigit) },
                        label = { Text("Prize Won ($currencyLabel)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prize Paid Out", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Switch(checked = prizePaid, onCheckedChange = { prizePaid = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveParticipantResult(
                            target,
                            status,
                            (kills.toLongOrNull() ?: 0L).coerceIn(0L, 100L),
                            (rankPos.toLongOrNull() ?: 0L).coerceIn(0L, 100L),
                            (prizeWon.toLongOrNull() ?: 0L).coerceIn(0L, 1000000L),
                            prizePaid
                        )
                        editingReg = null
                    },
                    enabled = !isBusy
                ) {
                    Text("SAVE RESULT", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingReg = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}
