package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AccentThemeOption
import com.example.data.model.AppConfig
import com.example.data.model.BannerArtwork
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LeaderboardSortMetric
import com.example.data.model.MatchCategory
import com.example.data.model.MatchStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Registration
import com.example.data.model.RegistrationStatus
import com.example.data.model.TeamType
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.model.WalletRequestStatus
import com.example.data.model.WalletRequestType
import com.example.data.model.rankTop10Leaderboard
import com.example.data.preferences.AccountPreferencesStore
import com.example.util.CombatRankTier
import com.example.util.CountdownUrgency
import com.example.util.buildTelegramSupportTarget
import com.example.util.computeMatchCountdown
import com.example.util.computePlayerCareerStats
import com.example.util.formatCompactCountdown
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Booyah Arena`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Booyah Arena", appName)
    }

    @Test
    fun `app config serializes cleanly with no document id in write map`() {
        val config = AppConfig(
            id = "main",
            platformName = "Booyah Arena Pro",
            accentTheme = AccentThemeOption.GOLD_CHAMPION.key,
            noticeEnabled = true,
            maintenanceMode = false
        )
        val createMap = config.toCreateMap()
        assertFalse(createMap.containsKey("id"))
        assertEquals("Booyah Arena Pro", createMap["platformName"])
        assertEquals("GOLD_CHAMPION", createMap["accentTheme"])
        assertEquals("01772902980", createMap["walletPaymentNumber"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))

        val updateMap = config.toUpdateMap()
        assertFalse(updateMap.containsKey("id"))
        assertFalse(updateMap.containsKey("createdAt"))
        assertEquals("01772902980", updateMap["walletPaymentNumber"])
        assertTrue(updateMap.containsKey("updatedAt"))
    }

    @Test
    fun `tournament match and category clamp bounds and enums safely`() {
        val cat = MatchCategory(
            id = "cat_br",
            name = "BR Full Map",
            badge = "48P",
            bannerKey = "unknown_key",
            sortOrder = 2000L,
            active = true
        )
        val catMap = cat.toCreateMap()
        assertEquals("br", catMap["bannerKey"])
        assertEquals(1000L, catMap["sortOrder"])

        val match = TournamentMatch(
            id = "match_1",
            title = "Bermuda Clash",
            teamType = "INVALID_TEAM",
            status = "INVALID_STATUS",
            totalSlots = 200L,
            filledSlots = 150L,
            entryFee = -10L,
            prizePool = 2000L,
            startTimeMillis = 1795255031892L
        )
        val matchMap = match.toCreateMap()
        assertEquals("SQUAD", matchMap["teamType"])
        assertEquals("UPCOMING", matchMap["status"])
        assertEquals(100L, matchMap["totalSlots"])
        assertEquals(100L, matchMap["filledSlots"])
        assertEquals(0L, matchMap["entryFee"])
        assertEquals(1795255031892L, matchMap["startTimeMillis"])
        assertFalse(matchMap.containsKey("id"))
    }

    @Test
    fun `user profile and enum fallbacks resolve safely`() {
        val profile = UserProfile(
            id = "u1",
            userId = "u1",
            displayName = "Safi",
            ffName = "SAFI_FF",
            ffUid = "12345678"
        )
        val map = profile.toCreateMap()
        assertFalse(map.containsKey("id"))
        assertEquals(0L, map["matchesJoined"])
        assertEquals(0L, map["totalWinnings"])
        assertEquals(0L, map["walletBalance"])

        assertEquals(TeamType.`4V4`, TeamType.fromRaw("4V4"))
        assertEquals(MatchStatus.LIVE, MatchStatus.fromRaw("LIVE"))
        assertEquals(RegistrationStatus.CONFIRMED, RegistrationStatus.fromRaw("CONFIRMED"))
        assertEquals(BannerArtwork.LW, BannerArtwork.fromKey("lw"))
        assertEquals(AccentThemeOption.CYAN_CYBER, AccentThemeOption.fromRaw("CYAN_CYBER"))
        assertEquals(WalletRequestType.DEPOSIT, WalletRequestType.fromRaw("INVALID"))
        assertEquals(WalletRequestType.WITHDRAW, WalletRequestType.fromRaw("WITHDRAW"))
        assertEquals(PaymentMethod.BKASH, PaymentMethod.fromRaw("INVALID"))
        assertEquals(PaymentMethod.NAGAD, PaymentMethod.fromRaw("NAGAD"))
        assertEquals(WalletRequestStatus.PENDING, WalletRequestStatus.fromRaw("INVALID"))
        assertEquals(WalletRequestStatus.APPROVED, WalletRequestStatus.fromRaw("APPROVED"))
    }

    @Test
    fun `countdown utility calculates urgency tiers and formats cleanly`() {
        val now = 1_700_000_000_000L

        assertNull(computeMatchCountdown(0L, now, MatchStatus.UPCOMING.name))
        assertNull(computeMatchCountdown(now + 60_000L, now, MatchStatus.LIVE.name))

        val normal = computeMatchCountdown(now + 26 * 3_600_000L + 5 * 60_000L, now, MatchStatus.UPCOMING.name)
        assertNotNull(normal)
        assertEquals(CountdownUrgency.NORMAL, normal!!.urgency)
        assertEquals("1d 02h 05m", formatCompactCountdown(normal))

        val soon = computeMatchCountdown(now + 45 * 60_000L + 9_000L, now, MatchStatus.UPCOMING.name)
        assertNotNull(soon)
        assertEquals(CountdownUrgency.SOON, soon!!.urgency)
        assertEquals("00:45:09", formatCompactCountdown(soon))

        val critical = computeMatchCountdown(now + 10 * 60_000L + 3_000L, now, MatchStatus.UPCOMING.name)
        assertNotNull(critical)
        assertEquals(CountdownUrgency.CRITICAL, critical!!.urgency)
        assertEquals("00:10:03", formatCompactCountdown(critical))

        val startingNow = computeMatchCountdown(now - 5_000L, now, MatchStatus.UPCOMING.name)
        assertNotNull(startingNow)
        assertEquals(CountdownUrgency.STARTING_NOW, startingNow!!.urgency)
        assertEquals("STARTING NOW", formatCompactCountdown(startingNow))
    }

    @Test
    fun `player career stats compute win rate, avg kills, best rank, and combat tier`() {
        val profile = UserProfile(
            id = "u1",
            userId = "u1",
            displayName = "Safi",
            ffName = "SAFI_FF",
            ffUid = "12345678",
            matchesJoined = 3L,
            totalKills = 12L,
            totalWins = 1L,
            totalWinnings = 500L
        )
        val registrations = listOf(
            Registration(
                id = "r1",
                tournamentId = "m1",
                tournamentTitle = "Bermuda Clash",
                kills = 7L,
                rankPosition = 1L,
                prizeWon = 350L
            ),
            Registration(
                id = "r2",
                tournamentId = "m2",
                tournamentTitle = "Purgatory Pro",
                kills = 5L,
                rankPosition = 3L,
                prizeWon = 150L
            ),
            Registration(
                id = "r3",
                tournamentId = "m3",
                tournamentTitle = "Upcoming Lone Wolf",
                kills = 0L,
                rankPosition = 0L,
                prizeWon = 0L
            )
        )
        val tourneys = mapOf(
            "m1" to TournamentMatch(id = "m1", status = MatchStatus.COMPLETED.name),
            "m2" to TournamentMatch(id = "m2", status = MatchStatus.COMPLETED.name),
            "m3" to TournamentMatch(id = "m3", status = MatchStatus.UPCOMING.name)
        )

        val stats = computePlayerCareerStats(profile, registrations, tourneys)
        assertEquals(3, stats.matchesJoined)
        assertEquals(2, stats.completedMatches)
        assertEquals(1, stats.activeMatches)
        assertEquals(12L, stats.totalKills)
        assertEquals(1L, stats.totalWins)
        assertEquals(500L, stats.totalWinnings)
        assertEquals(50, stats.winRatePercent)
        assertEquals(6.0, stats.avgKillsPerMatch, 0.01)
        assertEquals(1L, stats.bestRank)
        assertEquals(7L, stats.bestKillsInMatch)
        // score = 1*20 + 12*3 + 3*4 = 68 -> Platinum (minScore=50, next=90)
        assertEquals(CombatRankTier.PLATINUM, stats.rankTier)
    }

    @Test
    fun `account preferences store persists settings updates via DataStore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AccountPreferencesStore(context)
        val initial = store.settingsFlow.first()
        assertTrue(initial.autoFillIdentity)
        assertTrue(initial.showMatchCountdowns)

        store.setAutoFillIdentity(false)
        store.setShowMatchCountdowns(false)
        store.setHapticFeedbackEnabled(false)
        store.setRoomReminderHighlights(false)

        val updated = store.settingsFlow.first()
        assertFalse(updated.autoFillIdentity)
        assertFalse(updated.showMatchCountdowns)
        assertFalse(updated.hapticFeedbackEnabled)
        assertFalse(updated.roomReminderHighlights)
    }

    @Test
    fun `telegram support target resolves 01772902980 and string label matches Bengali request`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("সাহায্যের জন্য ক্লিক করুন", context.getString(R.string.help_click_for_support))

        val defaultTarget = buildTelegramSupportTarget("")
        assertEquals("01772902980", defaultTarget.displayLabel)
        assertEquals("tg://resolve?phone=8801772902980", defaultTarget.appUri)
        assertEquals("https://t.me/+8801772902980", defaultTarget.webUrl)

        val legacyTarget = buildTelegramSupportTarget("Telegram: @BooyahSupport")
        assertEquals("01772902980", legacyTarget.displayLabel)
        assertEquals("https://t.me/+8801772902980", legacyTarget.webUrl)

        val customHandle = buildTelegramSupportTarget("@CustomArenaHelp")
        assertEquals("tg://resolve?domain=CustomArenaHelp", customHandle.appUri)
        assertEquals("https://t.me/CustomArenaHelp", customHandle.webUrl)
    }

    @Test
    fun `top 10 leaderboard caps results to 10 and sorts by selected metric`() {
        val players = (1..12).map { i ->
            LeaderboardEntry(
                id = "u$i",
                userId = "u$i",
                ffName = "Player_$i",
                totalKills = (20 - i).toLong(),
                totalWins = (i % 5).toLong(),
                totalWinnings = (i * 100).toLong(),
                matchesPlayed = i.toLong()
            )
        }

        val byWinnings = rankTop10Leaderboard(players, LeaderboardSortMetric.WINNINGS)
        assertEquals(10, byWinnings.size)
        assertEquals("Player_12", byWinnings.first().ffName)
        assertEquals("Player_3", byWinnings.last().ffName)

        val byKills = rankTop10Leaderboard(players, LeaderboardSortMetric.KILLS)
        assertEquals(10, byKills.size)
        assertEquals("Player_1", byKills.first().ffName)
        assertEquals("Player_10", byKills.last().ffName)

        val byWins = rankTop10Leaderboard(players, LeaderboardSortMetric.WINS)
        assertEquals(10, byWins.size)
        assertEquals(4L, byWins.first().totalWins)
    }
}
