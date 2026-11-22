package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.ChronoViolet
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.TacticalGreen

enum class CombatRankTier(
    val title: String,
    val badge: String,
    val accent: Color,
    val minScore: Long,
    val nextScore: Long
) {
    BRONZE("Bronze Survivor", "BRONZE", Color(0xFFCD7F32), 0L, 10L),
    SILVER("Silver Striker", "SILVER", Color(0xFFC0C0C0), 10L, 25L),
    GOLD("Gold Elite", "GOLD", BlazeGold, 25L, 50L),
    PLATINUM("Platinum Hunter", "PLATINUM", GlooCyan, 50L, 90L),
    DIAMOND("Diamond Vanguard", "DIAMOND", TacticalGreen, 90L, 150L),
    HEROIC("Heroic Champion", "HEROIC", BlazeOrange, 150L, 250L),
    GRANDMASTER("Grandmaster Legend", "GRANDMASTER", ChronoViolet, 250L, 250L)
}

data class PlayerCareerStats(
    val matchesJoined: Int,
    val completedMatches: Int,
    val activeMatches: Int,
    val totalKills: Long,
    val totalWins: Long,
    val totalWinnings: Long,
    val totalEntryInvested: Long,
    val avgKillsPerMatch: Double,
    val winRatePercent: Int,
    val bestRank: Long,
    val bestKillsInMatch: Long,
    val combatScore: Long,
    val rankTier: CombatRankTier,
    val tierProgress: Float
)

fun computePlayerCareerStats(
    profile: UserProfile?,
    registrations: List<Registration>,
    tournamentsById: Map<String, TournamentMatch>
): PlayerCareerStats {
    val regJoined = registrations.size
    val profileJoined = (profile?.matchesJoined ?: 0L).toInt().coerceAtLeast(0)
    val matchesJoined = maxOf(regJoined, profileJoined)

    val completedRegs = registrations.count { reg ->
        val matchStatus = tournamentsById[reg.tournamentId]?.status?.let { MatchStatus.fromRaw(it) }
        matchStatus == MatchStatus.COMPLETED ||
            reg.rankPosition > 0L ||
            reg.kills > 0L ||
            reg.prizeWon > 0L
    }
    val activeRegs = (registrations.size - completedRegs).coerceAtLeast(0)

    val regKills = registrations.sumOf { it.kills.coerceAtLeast(0L) }
    val regWins = registrations.count { it.rankPosition == 1L }.toLong()
    val regWinnings = registrations.sumOf { it.prizeWon.coerceAtLeast(0L) }

    val totalKills = maxOf(profile?.totalKills ?: 0L, regKills)
    val totalWins = maxOf(profile?.totalWins ?: 0L, regWins)
    val totalWinnings = maxOf(profile?.totalWinnings ?: 0L, regWinnings)
    val totalEntryInvested = registrations.sumOf { it.entryFee.coerceAtLeast(0L) }

    val killsDenominator = if (completedRegs > 0) {
        completedRegs
    } else {
        maxOf(totalWins.toInt(), matchesJoined).coerceAtLeast(1)
    }
    val avgKills = if (matchesJoined == 0 && totalKills == 0L) 0.0 else totalKills.toDouble() / killsDenominator.toDouble()
    val winRateDenominator = if (completedRegs > 0) completedRegs else matchesJoined
    val winRate = if (winRateDenominator <= 0) {
        0
    } else {
        ((totalWins.toDouble() / winRateDenominator.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    val bestRank = registrations
        .map { it.rankPosition }
        .filter { it > 0L }
        .minOrNull() ?: if (totalWins > 0L) 1L else 0L

    val bestKills = registrations.maxOfOrNull { it.kills.coerceAtLeast(0L) } ?: totalKills

    // Combat score blends wins, kills, and participation into tier progression
    val combatScore = (totalWins * 20L) + (totalKills * 3L) + (matchesJoined.toLong() * 4L)
    val tier = CombatRankTier.entries.lastOrNull { combatScore >= it.minScore } ?: CombatRankTier.BRONZE
    val tierProgress = if (tier == CombatRankTier.GRANDMASTER || tier.nextScore <= tier.minScore) {
        1f
    } else {
        ((combatScore - tier.minScore).toFloat() / (tier.nextScore - tier.minScore).toFloat()).coerceIn(0f, 1f)
    }

    return PlayerCareerStats(
        matchesJoined = matchesJoined,
        completedMatches = completedRegs,
        activeMatches = activeRegs,
        totalKills = totalKills,
        totalWins = totalWins,
        totalWinnings = totalWinnings,
        totalEntryInvested = totalEntryInvested,
        avgKillsPerMatch = avgKills,
        winRatePercent = winRate,
        bestRank = bestRank,
        bestKillsInMatch = bestKills,
        combatScore = combatScore,
        rankTier = tier,
        tierProgress = tierProgress
    )
}
