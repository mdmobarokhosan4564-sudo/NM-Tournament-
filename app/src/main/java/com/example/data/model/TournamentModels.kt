package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.ChronoViolet
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GlooCyan
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AccentThemeOption(val key: String, val label: String, val swatch: Color) {
    BLAZE_ORANGE("BLAZE_ORANGE", "Blaze Fire", BlazeOrange),
    GOLD_CHAMPION("GOLD_CHAMPION", "Booyah Gold", BlazeGold),
    CYAN_CYBER("CYAN_CYBER", "Gloo Cyan", GlooCyan),
    CRIMSON_FURY("CRIMSON_FURY", "Crimson Fury", DangerCrimson),
    VIOLET_ELITE("VIOLET_ELITE", "Cyber Violet", ChronoViolet);

    companion object {
        fun fromRaw(raw: String): AccentThemeOption =
            entries.firstOrNull { it.key == raw } ?: BLAZE_ORANGE
    }
}

enum class TeamType(val label: String) {
    SOLO("Solo"),
    DUO("Duo"),
    SQUAD("Squad 4P"),
    `1V1`("1v1 Duel"),
    `4V4`("4v4 Clash");

    companion object {
        fun fromRaw(raw: String): TeamType =
            entries.firstOrNull { it.name == raw } ?: SQUAD
    }
}

enum class MatchStatus(val label: String) {
    UPCOMING("UPCOMING"),
    LIVE("LIVE"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromRaw(raw: String): MatchStatus =
            entries.firstOrNull { it.name == raw } ?: UPCOMING
    }
}

enum class RegistrationStatus(val label: String) {
    CONFIRMED("CONFIRMED"),
    PENDING("PENDING"),
    REJECTED("REJECTED");

    companion object {
        fun fromRaw(raw: String): RegistrationStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class BannerArtwork(
    val key: String,
    val label: String,
    @DrawableRes val drawableRes: Int
) {
    BR("br", "Battle Royale", R.drawable.img_banner_br_arena_1795255033295),
    CS("cs", "Clash Squad", R.drawable.img_banner_cs_arena_1795255033326),
    LW("lw", "Lone Wolf", R.drawable.img_banner_lone_wolf_1795255033124),
    CUSTOM("custom", "Bermuda Drop", R.drawable.lobby_bermuda_bg_1795244988621);

    companion object {
        fun fromKey(key: String): BannerArtwork =
            entries.firstOrNull { it.key == key } ?: BR
    }
}

enum class FreeFireRank(val label: String, val badge: String, val accent: Color) {
    UNRANKED("Unranked", "UNRANKED", Color(0xFF9AA3B2)),
    BRONZE("Bronze", "BRONZE", Color(0xFFCD7F32)),
    SILVER("Silver", "SILVER", Color(0xFFC0C0C0)),
    GOLD("Gold", "GOLD", BlazeGold),
    PLATINUM("Platinum", "PLATINUM", GlooCyan),
    DIAMOND("Diamond", "DIAMOND", Color(0xFF2FE68D)),
    HEROIC("Heroic", "HEROIC", BlazeOrange),
    MASTER("Master", "MASTER", DangerCrimson),
    GRANDMASTER("Grandmaster", "GRANDMASTER", ChronoViolet);

    companion object {
        fun fromRaw(raw: String): FreeFireRank {
            val normalized = raw.trim().lowercase()
            if (normalized.isBlank()) return UNRANKED
            return entries.firstOrNull {
                normalized == it.name.lowercase() || normalized.startsWith(it.label.lowercase())
            } ?: UNRANKED
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val platformName: String = "Booyah Arena",
    val tagline: String = "Daily Free Fire Custom Rooms & Esports",
    val noticeText: String = "Welcome! Join upcoming custom rooms and check My Matches for Room ID & Password.",
    val noticeEnabled: Boolean = true,
    val heroBannerTitle: String = "FREE FIRE CHAMPIONSHIP",
    val heroBannerSubtitle: String = "Compete in BR, Clash Squad & Lone Wolf tournaments synced live.",
    val heroBannerBadge: String = "ONLINE ARENA",
    val accentTheme: String = "BLAZE_ORANGE",
    val currencyLabel: String = "BDT",
    val paymentInfo: String = "Add balance via Wallet or join matches directly with your Free Fire UID & Game ID Name.",
    val supportContact: String = "01772902980",
    val globalRules: String = "1. Mobile devices only — no emulators or hacks.\n2. Join the custom room 10 minutes before start time.\n3. Sit in your assigned slot number.\n4. Screenshot final kill/placement screen for dispute support.",
    val walletPaymentNumber: String = "01772902980",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Tournament servers are being updated by Admin. Check back shortly.",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "platformName" to platformName.trim().ifBlank { "Booyah Arena" }.take(40),
        "tagline" to tagline.trim().take(100),
        "noticeText" to noticeText.trim().take(300),
        "noticeEnabled" to noticeEnabled,
        "heroBannerTitle" to heroBannerTitle.trim().ifBlank { "FREE FIRE CHAMPIONSHIP" }.take(60),
        "heroBannerSubtitle" to heroBannerSubtitle.trim().take(140),
        "heroBannerBadge" to heroBannerBadge.trim().take(24),
        "accentTheme" to AccentThemeOption.fromRaw(accentTheme).key,
        "currencyLabel" to currencyLabel.trim().ifBlank { "BDT" }.take(16),
        "paymentInfo" to paymentInfo.trim().take(300),
        "supportContact" to supportContact.trim().take(120),
        "globalRules" to globalRules.trim().take(2000),
        "walletPaymentNumber" to walletPaymentNumber.trim().ifBlank { "01772902980" }.take(30),
        "maintenanceMode" to maintenanceMode,
        "maintenanceMessage" to maintenanceMessage.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "platformName" to platformName.trim().ifBlank { "Booyah Arena" }.take(40),
        "tagline" to tagline.trim().take(100),
        "noticeText" to noticeText.trim().take(300),
        "noticeEnabled" to noticeEnabled,
        "heroBannerTitle" to heroBannerTitle.trim().ifBlank { "FREE FIRE CHAMPIONSHIP" }.take(60),
        "heroBannerSubtitle" to heroBannerSubtitle.trim().take(140),
        "heroBannerBadge" to heroBannerBadge.trim().take(24),
        "accentTheme" to AccentThemeOption.fromRaw(accentTheme).key,
        "currencyLabel" to currencyLabel.trim().ifBlank { "BDT" }.take(16),
        "paymentInfo" to paymentInfo.trim().take(300),
        "supportContact" to supportContact.trim().take(120),
        "globalRules" to globalRules.trim().take(2000),
        "walletPaymentNumber" to walletPaymentNumber.trim().ifBlank { "01772902980" }.take(30),
        "maintenanceMode" to maintenanceMode,
        "maintenanceMessage" to maintenanceMessage.trim().take(300),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class MatchCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val subtitle: String = "",
    val badge: String = "FF",
    val bannerKey: String = "br",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(40),
        "subtitle" to subtitle.trim().take(100),
        "badge" to badge.trim().ifBlank { "FF" }.take(12),
        "bannerKey" to BannerArtwork.fromKey(bannerKey).key,
        "sortOrder" to sortOrder.coerceIn(0L, 1000L),
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(40),
        "subtitle" to subtitle.trim().take(100),
        "badge" to badge.trim().ifBlank { "FF" }.take(12),
        "bannerKey" to BannerArtwork.fromKey(bannerKey).key,
        "sortOrder" to sortOrder.coerceIn(0L, 1000L),
        "active" to active,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class TournamentMatch(
    @DocumentId val id: String = "",
    val title: String = "",
    val categoryId: String = "",
    val categoryName: String = "BR Full Map",
    val teamType: String = TeamType.SQUAD.name,
    val mapName: String = "Bermuda",
    val perspective: String = "Mobile Only",
    val entryFee: Long = 0L,
    val prizePool: Long = 500L,
    val perKillReward: Long = 10L,
    val totalSlots: Long = 48L,
    val filledSlots: Long = 0L,
    val scheduleText: String = "Tonight 9:00 PM",
    val startTimeMillis: Long = 0L,
    val status: String = MatchStatus.UPCOMING.name,
    val rules: String = "Follow global fair-play rules.",
    val roomPublished: Boolean = false,
    val winnerSummary: String = "",
    val featured: Boolean = false,
    val bannerKey: String = "br",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val safeTotal = totalSlots.coerceIn(2L, 100L)
        val safeFilled = filledSlots.coerceIn(0L, safeTotal)
        val safeTitle = title.trim().let { if (it.length < 2) "FF Tournament" else it }.take(80)
        val safeMap = mapName.trim().let { if (it.length < 2) "Bermuda" else it }.take(40)
        val safePerspective = perspective.trim().let { if (it.length < 2) "Mobile Only" else it }.take(40)
        val safeSchedule = scheduleText.trim().let { if (it.length < 2) "TBA" else it }.take(60)
        return mapOf(
            "title" to safeTitle,
            "categoryId" to categoryId.trim().ifBlank { "general" }.take(128),
            "categoryName" to categoryName.trim().ifBlank { "General" }.take(40),
            "teamType" to TeamType.fromRaw(teamType).name,
            "mapName" to safeMap,
            "perspective" to safePerspective,
            "entryFee" to entryFee.coerceIn(0L, 100000L),
            "prizePool" to prizePool.coerceIn(0L, 1000000L),
            "perKillReward" to perKillReward.coerceIn(0L, 50000L),
            "totalSlots" to safeTotal,
            "filledSlots" to safeFilled,
            "scheduleText" to safeSchedule,
            "startTimeMillis" to startTimeMillis.coerceIn(0L, 4102444800000L),
            "status" to MatchStatus.fromRaw(status).name,
            "rules" to rules.trim().take(1500),
            "roomPublished" to roomPublished,
            "winnerSummary" to winnerSummary.trim().take(300),
            "featured" to featured,
            "bannerKey" to BannerArtwork.fromKey(bannerKey).key,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        val safeTotal = totalSlots.coerceIn(2L, 100L)
        val safeFilled = filledSlots.coerceIn(0L, safeTotal)
        val safeTitle = title.trim().let { if (it.length < 2) "FF Tournament" else it }.take(80)
        val safeMap = mapName.trim().let { if (it.length < 2) "Bermuda" else it }.take(40)
        val safePerspective = perspective.trim().let { if (it.length < 2) "Mobile Only" else it }.take(40)
        val safeSchedule = scheduleText.trim().let { if (it.length < 2) "TBA" else it }.take(60)
        return mapOf(
            "title" to safeTitle,
            "categoryId" to categoryId.trim().ifBlank { "general" }.take(128),
            "categoryName" to categoryName.trim().ifBlank { "General" }.take(40),
            "teamType" to TeamType.fromRaw(teamType).name,
            "mapName" to safeMap,
            "perspective" to safePerspective,
            "entryFee" to entryFee.coerceIn(0L, 100000L),
            "prizePool" to prizePool.coerceIn(0L, 1000000L),
            "perKillReward" to perKillReward.coerceIn(0L, 50000L),
            "totalSlots" to safeTotal,
            "filledSlots" to safeFilled,
            "scheduleText" to safeSchedule,
            "startTimeMillis" to startTimeMillis.coerceIn(0L, 4102444800000L),
            "status" to MatchStatus.fromRaw(status).name,
            "rules" to rules.trim().take(1500),
            "roomPublished" to roomPublished,
            "winnerSummary" to winnerSummary.trim().take(300),
            "featured" to featured,
            "bannerKey" to BannerArtwork.fromKey(bannerKey).key,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class RoomSecret(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RosterEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ffName: String = "",
    val squadName: String = "",
    val slotNumber: Long = 1L,
    val kills: Long = 0L,
    val rankPosition: Long = 0L,
    val prizeWon: Long = 0L,
    val status: String = RegistrationStatus.CONFIRMED.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Registration(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val scheduleText: String = "",
    val teamType: String = TeamType.SQUAD.name,
    val mapName: String = "Bermuda",
    val entryFee: Long = 0L,
    val ffName: String = "",
    val ffUid: String = "",
    val squadName: String = "",
    val paymentRef: String = "",
    val slotNumber: Long = 1L,
    val status: String = RegistrationStatus.CONFIRMED.name,
    val kills: Long = 0L,
    val rankPosition: Long = 0L,
    val prizeWon: Long = 0L,
    val prizePaid: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Survivor",
    val ffName: String = "",
    val ffUid: String = "",
    val inGameRank: String = FreeFireRank.UNRANKED.label,
    val email: String = "",
    val matchesJoined: Long = 0L,
    val totalKills: Long = 0L,
    val totalWins: Long = 0L,
    val totalWinnings: Long = 0L,
    val walletBalance: Long = 0L,
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.trim().ifBlank { "Survivor" }.take(40),
        "ffName" to ffName.trim().take(32),
        "ffUid" to ffUid.trim().take(20),
        "inGameRank" to inGameRank.trim().ifBlank { FreeFireRank.UNRANKED.label }.take(32),
        "email" to email.trim().take(120),
        "matchesJoined" to 0L,
        "totalKills" to 0L,
        "totalWins" to 0L,
        "totalWinnings" to 0L,
        "walletBalance" to 0L,
        "fcmToken" to fcmToken.trim().take(512),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ffName: String = "",
    val totalKills: Long = 0L,
    val totalWins: Long = 0L,
    val totalWinnings: Long = 0L,
    val matchesPlayed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class LeaderboardSortMetric(val firestoreField: String, val label: String) {
    WINNINGS("totalWinnings", "Top Winnings"),
    KILLS("totalKills", "Kill Leaders"),
    WINS("totalWins", "Booyah Wins")
}

fun rankTop10Leaderboard(
    entries: List<LeaderboardEntry>,
    sortBy: LeaderboardSortMetric
): List<LeaderboardEntry> {
    val comparator = when (sortBy) {
        LeaderboardSortMetric.WINNINGS -> compareByDescending<LeaderboardEntry> { it.totalWinnings }
            .thenByDescending { it.totalWins }
            .thenByDescending { it.totalKills }
        LeaderboardSortMetric.KILLS -> compareByDescending<LeaderboardEntry> { it.totalKills }
            .thenByDescending { it.totalWins }
            .thenByDescending { it.totalWinnings }
        LeaderboardSortMetric.WINS -> compareByDescending<LeaderboardEntry> { it.totalWins }
            .thenByDescending { it.totalWinnings }
            .thenByDescending { it.totalKills }
    }.thenBy { it.ffName.lowercase() }
    return entries.sortedWith(comparator).take(10)
}

enum class WalletRequestType(val label: String) {
    DEPOSIT("Add Money"),
    WITHDRAW("Withdraw");

    companion object {
        fun fromRaw(raw: String): WalletRequestType =
            entries.firstOrNull { it.name == raw } ?: DEPOSIT
    }
}

enum class PaymentMethod(val label: String) {
    BKASH("bKash"),
    NAGAD("Nagad");

    companion object {
        fun fromRaw(raw: String): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: BKASH
    }
}

enum class WalletRequestStatus(val label: String) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    companion object {
        fun fromRaw(raw: String): WalletRequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class WalletRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val ffName: String = "",
    val type: String = WalletRequestType.DEPOSIT.name,
    val method: String = PaymentMethod.BKASH.name,
    val amount: Long = 0L,
    val accountNumber: String = "",
    val txnId: String = "",
    val status: String = WalletRequestStatus.PENDING.name,
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun validateTournamentJoinInput(
    ffName: String,
    ffUid: String,
    requireUid: Boolean = false
): String? {
    val cleanName = ffName.trim()
    val cleanUid = ffUid.trim()
    return when {
        requireUid && cleanUid.isEmpty() -> "Please enter your Free Fire UID."
        cleanUid.isNotEmpty() && cleanUid.length < 5 -> "Free Fire UID must be at least 5 digits."
        cleanUid.length > 20 -> "Free Fire UID must be 20 characters or fewer."
        cleanName.length < 2 -> "Please enter your Game ID Name (at least 2 characters)."
        cleanName.length > 32 -> "Game ID Name must be 32 characters or fewer."
        else -> null
    }
}

fun TournamentMatch.isActiveTournament(): Boolean {
    val parsed = MatchStatus.fromRaw(status)
    return parsed == MatchStatus.UPCOMING || parsed == MatchStatus.LIVE
}

fun filterTournaments(
    tournaments: List<TournamentMatch>,
    searchQuery: String = "",
    selectedCategoryId: String? = null,
    selectedTeamType: TeamType? = null,
    selectedStatus: String? = null,
    activeOnly: Boolean = false
): List<TournamentMatch> {
    val tokens = searchQuery.trim().lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return tournaments.filter { match ->
        val catOk = selectedCategoryId == null || match.categoryId == selectedCategoryId
        val teamOk = selectedTeamType == null || TeamType.fromRaw(match.teamType) == selectedTeamType
        val statusOk = when {
            selectedStatus == "ACTIVE" -> match.isActiveTournament()
            selectedStatus != null -> match.status == selectedStatus
            activeOnly -> match.isActiveTournament()
            else -> true
        }
        val queryOk = if (tokens.isEmpty()) {
            true
        } else {
            val teamEnum = TeamType.fromRaw(match.teamType)
            val searchable = listOf(
                match.title,
                match.categoryName,
                match.teamType,
                teamEnum.label,
                match.mapName,
                match.perspective,
                match.scheduleText
            ).joinToString(" ").lowercase()
            tokens.all { token -> searchable.contains(token) }
        }
        catOk && teamOk && statusOk && queryOk
    }
}

val TOURNAMENT_RULE_PRESETS: List<String> = listOf(
    "No hackers, scripts, or mod APKs",
    "No teaming with opponents",
    "Mobile devices only (no emulator)",
    "Join custom room 10 mins early",
    "Sit in your assigned slot only",
    "No grenades or landmines",
    "Screenshot final kill screen for disputes"
)

fun parseTournamentRules(
    matchRules: String,
    globalRules: String = ""
): List<String> {
    val raw = matchRules.trim().ifBlank { globalRules.trim() }
    if (raw.isBlank()) return emptyList()

    val primaryLines = raw
        .split(Regex("\\r?\\n"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val expanded = if (primaryLines.size == 1 && (primaryLines[0].contains('•') || primaryLines[0].contains(';'))) {
        primaryLines[0].split(Regex("[•;]+")).map { it.trim() }.filter { it.isNotBlank() }
    } else {
        primaryLines
    }

    val seen = LinkedHashSet<String>()
    val cleaned = mutableListOf<String>()
    val prefixRegex = Regex("^(\\d+[.)]|[-*•])\\s*")
    for (line in expanded) {
        val normalized = line.replace(prefixRegex, "").trim()
        if (normalized.isNotBlank() && seen.add(normalized.lowercase())) {
            cleaned.add(normalized)
        }
    }
    return cleaned
}

fun isRulePresetSelected(currentRules: String, preset: String): Boolean {
    val target = preset.trim().lowercase()
    return parseTournamentRules(currentRules).any { it.lowercase() == target }
}

fun toggleRulePreset(currentRules: String, preset: String, maxChars: Int = 1500): String {
    val cleanPreset = preset.trim()
    if (cleanPreset.isBlank()) return currentRules.take(maxChars)

    val existing = parseTournamentRules(currentRules).toMutableList()
    val matchIdx = existing.indexOfFirst { it.equals(cleanPreset, ignoreCase = true) }
    if (matchIdx >= 0) {
        existing.removeAt(matchIdx)
    } else {
        existing.add(cleanPreset)
    }
    return existing.joinToString(separator = "\n").take(maxChars)
}

