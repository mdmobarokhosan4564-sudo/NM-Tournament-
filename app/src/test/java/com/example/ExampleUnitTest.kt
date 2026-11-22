package com.example

import com.example.data.model.LeaderboardEntry
import com.example.data.model.LeaderboardSortMetric
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.model.rankTop10Leaderboard
import com.example.data.preferences.trimSeenNotificationKeys
import com.example.notifications.ArenaNotificationHelper
import com.example.notifications.ArenaNotificationType
import com.example.util.CombatRankTier
import com.example.util.buildTelegramSupportTarget
import com.example.util.computePlayerCareerStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun top10Leaderboard_capsToTenAndSortsByMetric() {
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

    @Test
    fun playerCareerStats_calculatesCorrectly() {
        val profile = UserProfile(
            id = "u1",
            userId = "u1",
            displayName = "Safi",
            matchesJoined = 3L,
            totalKills = 12L,
            totalWins = 1L,
            totalWinnings = 500L
        )
        val registrations = listOf(
            Registration(id = "r1", tournamentId = "m1", kills = 7L, rankPosition = 1L, prizeWon = 350L),
            Registration(id = "r2", tournamentId = "m2", kills = 5L, rankPosition = 3L, prizeWon = 150L),
            Registration(id = "r3", tournamentId = "m3", kills = 0L, rankPosition = 0L, prizeWon = 0L)
        )
        val tourneys = mapOf(
            "m1" to TournamentMatch(id = "m1", status = MatchStatus.COMPLETED.name),
            "m2" to TournamentMatch(id = "m2", status = MatchStatus.COMPLETED.name),
            "m3" to TournamentMatch(id = "m3", status = MatchStatus.UPCOMING.name)
        )
        val stats = computePlayerCareerStats(profile, registrations, tourneys)
        assertEquals(3, stats.matchesJoined)
        assertEquals(2, stats.completedMatches)
        assertEquals(50, stats.winRatePercent)
        assertEquals(6.0, stats.avgKillsPerMatch, 0.01)
        assertEquals(CombatRankTier.PLATINUM, stats.rankTier)
    }

    @Test
    fun telegramSupport_resolvesDefaultNumber() {
        val target = buildTelegramSupportTarget("")
        assertEquals("01772902980", target.displayLabel)
        assertEquals("https://t.me/+8801772902980", target.webUrl)
    }

    @Test
    fun fcmPayload_parsesWalletApprovalAndRoomCredentialsCorrectly() {
        val walletPayload = ArenaNotificationHelper.parseFcmPayload(
            data = mapOf(
                "type" to "WALLET_APPROVED",
                "requestId" to "dep_999",
                "requestType" to "DEPOSIT",
                "amount" to "500",
                "currencyLabel" to "BDT",
                "method" to "NAGAD"
            ),
            fallbackTitle = null,
            fallbackBody = null,
            fallbackMessageId = "msg_1"
        )
        assertEquals(ArenaNotificationType.WALLET_APPROVED, walletPayload.type)
        assertEquals(ArenaNotificationHelper.CHANNEL_WALLET_UPDATES, walletPayload.channelId)
        assertEquals(ArenaNotificationHelper.TAB_WALLET, walletPayload.openTab)
        assertEquals("wallet_dep_999_APPROVED", walletPayload.eventKey)
        assertTrue(walletPayload.title.contains("500 BDT"))

        val roomPayload = ArenaNotificationHelper.parseFcmPayload(
            data = mapOf(
                "type" to "ROOM_PUBLISHED",
                "tournamentId" to "match_bermuda_1",
                "tournamentTitle" to "Bermuda Pro",
                "roomId" to "88776655",
                "roomPassword" to "FF99"
            ),
            fallbackTitle = null,
            fallbackBody = null,
            fallbackMessageId = "msg_2"
        )
        assertEquals(ArenaNotificationType.ROOM_PUBLISHED, roomPayload.type)
        assertEquals(ArenaNotificationHelper.CHANNEL_ROOM_CREDENTIALS, roomPayload.channelId)
        assertEquals("match_bermuda_1", roomPayload.tournamentId)
        assertTrue(roomPayload.body.contains("88776655"))
        assertTrue(roomPayload.body.contains("FF99"))
        assertEquals("room_match_bermuda_1_88776655_FF99", roomPayload.eventKey)
    }

    @Test
    fun fcmTopics_andSeenKeysTrimmingStayValidAndBounded() {
        assertEquals("tournament_match_1_pro", ArenaNotificationHelper.tournamentTopic("match@1/pro"))
        assertEquals("user_uid_123", ArenaNotificationHelper.userTopic("uid:123"))

        var set = emptySet<String>()
        for (i in 1..130) {
            set = trimSeenNotificationKeys(set, "key_$i", maxEntries = 100)
        }
        assertEquals(100, set.size)
        assertFalse(set.contains("key_1"))
        assertTrue(set.contains("key_130"))
    }

    @Test
    fun tournamentJoinValidation_requiresInGameUsernameAndAllowsOptionalUid() {
        assertTrue(com.example.data.model.validateTournamentJoinInput("A", "") != null)
        assertEquals(null, com.example.data.model.validateTournamentJoinInput("SAFI_PRO", ""))
        assertEquals(null, com.example.data.model.validateTournamentJoinInput("SAFI_PRO", "123456789"))
        assertTrue(com.example.data.model.validateTournamentJoinInput("SAFI_PRO", "123") != null)
    }

    @Test
    fun tournamentJoinValidation_whenUidRequired_rejectsBlankAndShortUid() {
        assertTrue(com.example.data.model.validateTournamentJoinInput("SAFI_PRO", "", requireUid = true) != null)
        assertTrue(com.example.data.model.validateTournamentJoinInput("SAFI_PRO", "1234", requireUid = true) != null)
        assertTrue(com.example.data.model.validateTournamentJoinInput("A", "123456789", requireUid = true) != null)
        assertEquals(
            null,
            com.example.data.model.validateTournamentJoinInput("SAFI_PRO", "123456789", requireUid = true)
        )
    }

    @Test
    fun freeFireRank_resolvesTiersAndDefaultsCleanly() {
        assertEquals(com.example.data.model.FreeFireRank.HEROIC, com.example.data.model.FreeFireRank.fromRaw("heroic"))
        assertEquals(com.example.data.model.FreeFireRank.GRANDMASTER, com.example.data.model.FreeFireRank.fromRaw("Grandmaster"))
        assertEquals(com.example.data.model.FreeFireRank.UNRANKED, com.example.data.model.FreeFireRank.fromRaw(""))
        val profile = UserProfile(userId = "u1")
        assertEquals("Unranked", profile.inGameRank)
        assertEquals("Unranked", profile.toCreateMap()["inGameRank"])
    }

    @Test
    fun searchTournaments_filtersActiveMatchesBySoloDuoSquadCategory() {
        val matches = listOf(
            TournamentMatch(
                id = "m_solo",
                title = "Bermuda Rush",
                categoryName = "BR Full Map",
                teamType = com.example.data.model.TeamType.SOLO.name,
                mapName = "Bermuda",
                status = MatchStatus.UPCOMING.name
            ),
            TournamentMatch(
                id = "m_duo",
                title = "Purgatory Pairs",
                categoryName = "BR Full Map",
                teamType = com.example.data.model.TeamType.DUO.name,
                mapName = "Purgatory",
                status = MatchStatus.LIVE.name
            ),
            TournamentMatch(
                id = "m_squad_active",
                title = "Kalahari Masters",
                categoryName = "BR Full Map",
                teamType = com.example.data.model.TeamType.SQUAD.name,
                mapName = "Kalahari",
                status = MatchStatus.UPCOMING.name
            ),
            TournamentMatch(
                id = "m_squad_done",
                title = "Old Squad Cup",
                categoryName = "BR Full Map",
                teamType = com.example.data.model.TeamType.SQUAD.name,
                mapName = "Bermuda",
                status = MatchStatus.COMPLETED.name
            )
        )

        val soloActive = com.example.data.model.filterTournaments(matches, searchQuery = "Solo", activeOnly = true)
        assertEquals(listOf("m_solo"), soloActive.map { it.id })

        val duoActive = com.example.data.model.filterTournaments(matches, searchQuery = "duo", activeOnly = true)
        assertEquals(listOf("m_duo"), duoActive.map { it.id })

        val squadActive = com.example.data.model.filterTournaments(matches, searchQuery = "Squad", activeOnly = true)
        assertEquals(listOf("m_squad_active"), squadActive.map { it.id })

        val squadAll = com.example.data.model.filterTournaments(matches, searchQuery = "Squad", activeOnly = false)
        assertEquals(listOf("m_squad_active", "m_squad_done"), squadAll.map { it.id })

        val squadChipOnly = com.example.data.model.filterTournaments(
            matches,
            selectedTeamType = com.example.data.model.TeamType.SQUAD,
            activeOnly = true
        )
        assertEquals(listOf("m_squad_active"), squadChipOnly.map { it.id })
    }

    @Test
    fun tournamentReminders_schedules15MinBeforeAndHandlesInsideWindowAndInactiveMatches() {
        val now = 1_700_000_000_000L
        val fifteenMinMs = com.example.notifications.MATCH_REMINDER_LEAD_MILLIS

        val futurePlan = com.example.notifications.computeMatchReminderSchedule(
            startTimeMillis = now + 30L * 60_000L,
            status = MatchStatus.UPCOMING.name,
            registrationStatus = "CONFIRMED",
            remindersEnabled = true,
            nowMillis = now
        )
        assertEquals(
            com.example.notifications.ReminderScheduleDecision.FIFTEEN_MINUTES_BEFORE,
            futurePlan.decision
        )
        assertEquals(now + 15L * 60_000L, futurePlan.triggerAtMillis)

        val insideWindowPlan = com.example.notifications.computeMatchReminderSchedule(
            startTimeMillis = now + 8L * 60_000L,
            status = MatchStatus.UPCOMING.name,
            registrationStatus = "PENDING",
            remindersEnabled = true,
            nowMillis = now
        )
        assertEquals(
            com.example.notifications.ReminderScheduleDecision.IMMEDIATE_WITHIN_WINDOW,
            insideWindowPlan.decision
        )

        val disabledPlan = com.example.notifications.computeMatchReminderSchedule(
            startTimeMillis = now + 30L * 60_000L,
            status = MatchStatus.UPCOMING.name,
            registrationStatus = "CONFIRMED",
            remindersEnabled = false,
            nowMillis = now
        )
        assertEquals(com.example.notifications.ReminderScheduleDecision.NONE, disabledPlan.decision)

        val completedPlan = com.example.notifications.computeMatchReminderSchedule(
            startTimeMillis = now + 30L * 60_000L,
            status = MatchStatus.COMPLETED.name,
            registrationStatus = "CONFIRMED",
            remindersEnabled = true,
            nowMillis = now
        )
        assertEquals(com.example.notifications.ReminderScheduleDecision.NONE, completedPlan.decision)

        val matchesById = mapOf(
            "m_future" to TournamentMatch(
                id = "m_future",
                title = "Bermuda Clash | Pro",
                teamType = "SQUAD",
                mapName = "Bermuda",
                startTimeMillis = now + 40L * 60_000L,
                status = MatchStatus.UPCOMING.name,
                roomPublished = true
            ),
            "m_rejected" to TournamentMatch(
                id = "m_rejected",
                title = "Purgatory Duo",
                startTimeMillis = now + 40L * 60_000L,
                status = MatchStatus.UPCOMING.name
            ),
            "m_soon" to TournamentMatch(
                id = "m_soon",
                title = "Kalahari Rush",
                startTimeMillis = now + 10L * 60_000L,
                status = MatchStatus.UPCOMING.name
            )
        )
        val registrations = listOf(
            Registration(id = "r1", tournamentId = "m_future", slotNumber = 7L, status = "CONFIRMED"),
            Registration(id = "r2", tournamentId = "m_rejected", slotNumber = 2L, status = "REJECTED"),
            Registration(id = "r3", tournamentId = "m_soon", slotNumber = 4L, status = "CONFIRMED")
        )

        val planned = com.example.notifications.buildScheduledMatchReminders(
            registrations = registrations,
            tournamentsById = matchesById,
            remindersEnabled = true,
            nowMillis = now
        )
        assertEquals(listOf("m_future", "m_soon"), planned.map { it.reminder.tournamentId })
        assertEquals(
            now + 40L * 60_000L - fifteenMinMs,
            planned.first().reminder.triggerAtMillis
        )

        val serialized = planned.first().reminder.serialize()
        val roundTripped = com.example.data.preferences.ScheduledMatchReminder.deserialize(serialized)
        assertEquals(planned.first().reminder, roundTripped)

        val payload = ArenaNotificationHelper.buildMatchReminderNotification(
            tournamentId = "m_future",
            tournamentTitle = "Bermuda Clash | Pro",
            teamType = "SQUAD",
            mapName = "Bermuda",
            slotNumber = 7,
            startTimeMillis = now + fifteenMinMs,
            nowMillis = now,
            roomPublished = true
        )
        assertEquals(ArenaNotificationType.MATCH_REMINDER, payload.type)
        assertEquals(ArenaNotificationHelper.CHANNEL_MATCH_REMINDERS, payload.channelId)
        assertEquals("m_future", payload.tournamentId)
        assertTrue(payload.title.contains("15 Minutes"))
        assertTrue(payload.body.contains("Room ID & Password"))
    }

    @Test
    fun tournamentRules_parsesLinesFallsBackAndTogglesPresets() {
        val parsed = com.example.data.model.parseTournamentRules(
            matchRules = "1. No hackers, scripts, or mod APKs\n- No teaming with opponents\n• No teaming with opponents\nMobile only",
            globalRules = "Global fallback"
        )
        assertEquals(
            listOf(
                "No hackers, scripts, or mod APKs",
                "No teaming with opponents",
                "Mobile only"
            ),
            parsed
        )

        val fallbackParsed = com.example.data.model.parseTournamentRules(
            matchRules = "   ",
            globalRules = "No hackers\nNo teaming"
        )
        assertEquals(listOf("No hackers", "No teaming"), fallbackParsed)

        val semicolonParsed = com.example.data.model.parseTournamentRules(
            matchRules = "No hackers • No teaming; Join 10 mins early"
        )
        assertEquals(listOf("No hackers", "No teaming", "Join 10 mins early"), semicolonParsed)

        val added = com.example.data.model.toggleRulePreset("No hackers, scripts, or mod APKs", "No teaming with opponents")
        assertTrue(com.example.data.model.isRulePresetSelected(added, "No teaming with opponents"))
        assertTrue(com.example.data.model.isRulePresetSelected(added, "No hackers, scripts, or mod APKs"))

        val removed = com.example.data.model.toggleRulePreset(added, "No teaming with opponents")
        assertFalse(com.example.data.model.isRulePresetSelected(removed, "No teaming with opponents"))
        assertTrue(com.example.data.model.isRulePresetSelected(removed, "No hackers, scripts, or mod APKs"))
    }
}
