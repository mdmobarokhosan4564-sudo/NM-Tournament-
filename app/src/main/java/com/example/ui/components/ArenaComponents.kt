package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.BannerArtwork
import com.example.data.model.MatchStatus
import com.example.data.model.TeamType
import com.example.data.model.TournamentMatch
import com.example.data.model.parseTournamentRules
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
import com.example.util.CountdownUrgency
import com.example.util.MatchCountdown
import com.example.util.computeMatchCountdown
import com.example.util.formatCompactCountdown
import com.example.util.openTelegramSupport
import com.example.util.resolveTelegramSupportDisplay
import java.util.Locale

@Composable
fun ArenaTopBar(
    config: AppConfig,
    adminUnlocked: Boolean,
    inAdminPanel: Boolean,
    pendingWalletCount: Int = 0,
    onAdminButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianSurface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_booyah_app_icon_1795255031892),
                    contentDescription = "Platform emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = config.platformName.ifBlank { "Booyah Arena" },
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = config.tagline.ifBlank { "Free Fire Esports" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = onAdminButtonClick,
                shape = RoundedCornerShape(12.dp),
                color = if (adminUnlocked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    ObsidianCard
                },
                border = BorderStroke(
                    1.dp,
                    if (pendingWalletCount > 0) DangerCrimson
                    else if (adminUnlocked) MaterialTheme.colorScheme.primary
                    else OutlineSubtle
                ),
                modifier = Modifier.testTag("admin_access_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (adminUnlocked) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                        contentDescription = "Admin Panel",
                        tint = if (adminUnlocked) MaterialTheme.colorScheme.primary else BlazeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when {
                            inAdminPanel -> "EXIT ADMIN"
                            else -> "ADMIN"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (adminUnlocked) MaterialTheme.colorScheme.primary else TextPrimary
                    )
                    if (pendingWalletCount > 0) {
                        Surface(
                            color = DangerCrimson,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$pendingWalletCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveNoticeBanner(
    noticeText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = "Announcement",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = noticeText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TelegramHelpCard(
    supportContact: String,
    onFallbackMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val displayNumber = resolveTelegramSupportDisplay(supportContact)

    Surface(
        color = GlooCyan.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                openTelegramSupport(
                    context = context,
                    rawContact = supportContact,
                    onUnavailable = { fallback ->
                        clipboard.setText(AnnotatedString(displayNumber))
                        onFallbackMessage?.invoke("$fallback copied!")
                    }
                )
            }
            .testTag("telegram_help_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = GlooCyan.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.5f)),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Telegram Support",
                    tint = GlooCyan,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.help_click_for_support),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Telegram Support • $displayNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = GlooCyan
                )
            }
            Surface(
                color = GlooCyan,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TELEGRAM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBg
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = ObsidianBg,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val parsed = MatchStatus.fromRaw(status)
    val (bg, fg, label) = when (parsed) {
        MatchStatus.LIVE -> Triple(DangerCrimson.copy(alpha = 0.22f), DangerCrimson, "LIVE NOW")
        MatchStatus.UPCOMING -> Triple(TacticalGreen.copy(alpha = 0.2f), TacticalGreen, "UPCOMING")
        MatchStatus.COMPLETED -> Triple(BlazeGold.copy(alpha = 0.18f), BlazeGold, "COMPLETED")
        MatchStatus.CANCELLED -> Triple(OutlineSubtle.copy(alpha = 0.5f), TextSecondary, "CANCELLED")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (parsed == MatchStatus.LIVE) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DangerCrimson)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
fun CompactCountdownBadge(
    countdown: MatchCountdown,
    modifier: Modifier = Modifier
) {
    val accent = when (countdown.urgency) {
        CountdownUrgency.STARTING_NOW, CountdownUrgency.CRITICAL -> DangerCrimson
        CountdownUrgency.SOON -> MaterialTheme.colorScheme.primary
        CountdownUrgency.NORMAL -> GlooCyan
    }
    val isUrgent = countdown.urgency == CountdownUrgency.CRITICAL ||
        countdown.urgency == CountdownUrgency.STARTING_NOW

    val pulseAlpha = if (isUrgent) {
        val transition = rememberInfiniteTransition(label = "countdown_pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        alpha
    } else 1f

    Surface(
        color = accent.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isUrgent) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(accent)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = "Starts in",
                    tint = accent,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = formatCompactCountdown(countdown),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = accent
            )
        }
    }
}

@Composable
fun TournamentCountdownCard(
    countdown: MatchCountdown,
    scheduleText: String,
    modifier: Modifier = Modifier
) {
    val accent = when (countdown.urgency) {
        CountdownUrgency.STARTING_NOW, CountdownUrgency.CRITICAL -> DangerCrimson
        CountdownUrgency.SOON -> MaterialTheme.colorScheme.primary
        CountdownUrgency.NORMAL -> GlooCyan
    }
    val headline = when (countdown.urgency) {
        CountdownUrgency.STARTING_NOW -> "MATCH STARTING NOW • CHECK ROOM CREDENTIALS"
        CountdownUrgency.CRITICAL -> "STARTS IN UNDER 15 MIN • LOCK YOUR SLOT!"
        CountdownUrgency.SOON -> "KICKOFF APPROACHING • GET READY"
        CountdownUrgency.NORMAL -> "TOURNAMENT START COUNTDOWN"
    }

    Surface(
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_countdown_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Text(
                    text = scheduleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (countdown.days > 0L) {
                    CountdownSegmentBox(
                        value = String.format(Locale.US, "%02d", countdown.days),
                        unit = "DAYS",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                }
                CountdownSegmentBox(
                    value = String.format(Locale.US, "%02d", countdown.hours),
                    unit = "HRS",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                CountdownSegmentBox(
                    value = String.format(Locale.US, "%02d", countdown.minutes),
                    unit = "MIN",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                CountdownSegmentBox(
                    value = String.format(Locale.US, "%02d", countdown.seconds),
                    unit = "SEC",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CountdownSegmentBox(
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianBg.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = TextPrimary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}

@Composable
fun TournamentMatchCard(
    match: TournamentMatch,
    currencyLabel: String,
    isJoined: Boolean,
    currentTimeMillis: Long = System.currentTimeMillis(),
    showCountdown: Boolean = true,
    onJoinClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artwork = BannerArtwork.fromKey(match.bannerKey)
    val fillRatio = if (match.totalSlots > 0L) {
        (match.filledSlots.toFloat() / match.totalSlots.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val countdown = if (showCountdown) {
        computeMatchCountdown(match.startTimeMillis, currentTimeMillis, match.status)
    } else null

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard,
        border = BorderStroke(
            1.dp,
            if (isJoined) GlooCyan.copy(alpha = 0.65f) else OutlineSubtle
        ),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("match_card_${match.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
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
                                    ObsidianBg.copy(alpha = 0.25f),
                                    ObsidianBg.copy(alpha = 0.65f),
                                    ObsidianCard
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = match.status)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isJoined) {
                            Surface(
                                color = GlooCyan.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, GlooCyan)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = GlooCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (match.roomPublished) "ROOM READY" else "JOINED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GlooCyan
                                    )
                                }
                            }
                        }
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.8f),
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
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = match.categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = match.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPill(
                        label = "PRIZE POOL",
                        value = "${match.prizePool} $currencyLabel",
                        accent = BlazeGold
                    )
                    MetricPill(
                        label = "PER KILL",
                        value = "${match.perKillReward} $currencyLabel",
                        accent = GlooCyan
                    )
                    MetricPill(
                        label = "ENTRY",
                        value = if (match.entryFee == 0L) "FREE" else "${match.entryFee} $currencyLabel",
                        accent = if (match.entryFee == 0L) TacticalGreen else MaterialTheme.colorScheme.primary
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${match.filledSlots}/${match.totalSlots} Spots Filled",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (countdown != null) {
                                CompactCountdownBadge(countdown = countdown)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = BlazeGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = match.scheduleText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    if (countdown != null) {
                        Text(
                            text = "Kickoff: ${match.scheduleText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fillRatio },
                        color = if (fillRatio >= 1f) DangerCrimson else MaterialTheme.colorScheme.primary,
                        trackColor = OutlineSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                if (match.winnerSummary.isNotBlank() && MatchStatus.fromRaw(match.status) == MatchStatus.COMPLETED) {
                    Surface(
                        color = BlazeGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = BlazeGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = match.winnerSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BlazeGold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val matchStatus = MatchStatus.fromRaw(match.status)
                val isFull = match.filledSlots >= match.totalSlots
                if (onJoinClick != null && matchStatus == MatchStatus.UPCOMING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onClick,
                            border = BorderStroke(1.dp, OutlineSubtle),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("card_details_button_${match.id}")
                        ) {
                            Text(
                                text = if (isJoined) "ROOM DETAILS" else "DETAILS",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isJoined) GlooCyan else TextPrimary
                            )
                        }
                        if (!isJoined) {
                            Button(
                                onClick = onJoinClick,
                                enabled = !isFull,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = OutlineSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(48.dp)
                                    .testTag("card_join_button_${match.id}")
                            ) {
                                Text(
                                    text = if (isFull) "SLOTS FULL" else "JOIN MATCH",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AdminPasscodeDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onVerify: (passcode: String, onError: (String) -> Unit) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Admin Access", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter the Admin password to add matches, customize app branding, set Room IDs, and manage tournament results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = {
                        passcode = it
                        errorMsg = null
                    },
                    label = { Text("Admin Password") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = errorMsg != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerCrimson
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passcode.isBlank()) {
                        errorMsg = "Please enter the admin password."
                    } else {
                        onVerify(passcode) { msg -> errorMsg = msg }
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("admin_unlock_confirm")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                } else {
                    Text("UNLOCK ADMIN", style = MaterialTheme.typography.labelLarge)
                }
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
fun JoinTournamentDialog(
    match: TournamentMatch,
    config: AppConfig,
    initialFfName: String,
    initialFfUid: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ffName: String, ffUid: String, squadName: String, paymentRef: String) -> Unit
) {
    var ffUid by remember(match.id, initialFfUid) { mutableStateOf(initialFfUid) }
    var ffName by remember(match.id, initialFfName) { mutableStateOf(initialFfName) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Join Tournament", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        text = "${match.title} • ${match.filledSlots}/${match.totalSlots} Players",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlooCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
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
                        Text(
                            text = "${TeamType.fromRaw(match.teamType).label} • ${match.mapName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (match.entryFee == 0L) "FREE ENTRY" else "Entry: ${match.entryFee} ${config.currencyLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (match.entryFee == 0L) TacticalGreen else BlazeGold
                        )
                    }
                }

                RulesAndRegulationsSection(
                    matchRules = match.rules,
                    globalRules = config.globalRules,
                    compact = true,
                    modifier = Modifier.testTag("join_dialog_rules_section")
                )

                OutlinedTextField(
                    value = ffUid,
                    onValueChange = { ffUid = it; errorMsg = null },
                    label = { Text("Free Fire UID *") },
                    placeholder = { Text("e.g. 123456789") },
                    singleLine = true,
                    isError = errorMsg != null && ffUid.trim().length < 5,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_ff_uid_input")
                )

                OutlinedTextField(
                    value = ffName,
                    onValueChange = { ffName = it; errorMsg = null },
                    label = { Text("Game ID Name (Free Fire IGN) *") },
                    placeholder = { Text("Enter your in-game name") },
                    singleLine = true,
                    isError = errorMsg != null && ffName.trim().length < 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_ff_name_input")
                )

                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.bodyMedium, color = DangerCrimson)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validationError = com.example.data.model.validateTournamentJoinInput(
                        ffName = ffName,
                        ffUid = ffUid,
                        requireUid = true
                    )
                    if (validationError != null) {
                        errorMsg = validationError
                    } else {
                        onSubmit(ffName.trim(), ffUid.trim(), "", "")
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("confirm_join_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                } else {
                    Text("JOIN & RESERVE SLOT", style = MaterialTheme.typography.labelLarge)
                }
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
fun RulesAndRegulationsSection(
    matchRules: String,
    globalRules: String = "",
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val parsedRules = remember(matchRules, globalRules) {
        parseTournamentRules(matchRules = matchRules, globalRules = globalRules)
    }

    Surface(
        color = if (compact) ObsidianSurface else ObsidianCard,
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
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
                    Icon(
                        imageVector = Icons.Filled.Gavel,
                        contentDescription = null,
                        tint = BlazeGold,
                        modifier = Modifier.size(if (compact) 18.dp else 20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.rules_regulations_title).uppercase(Locale.US),
                        style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Surface(
                    color = BlazeGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "REQUIRED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BlazeGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!compact) {
                Text(
                    text = stringResource(id = R.string.rules_regulations_before_join),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            if (parsedRules.isEmpty()) {
                Text(
                    text = "Follow fair-play esports guidelines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                    parsedRules.forEachIndexed { index, rule ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = rule,
                                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Surface(
                color = DangerCrimson.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = DangerCrimson,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.rules_regulations_fair_play_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
