package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.MatchStatus
import com.example.data.model.Registration
import com.example.data.model.TournamentMatch
import com.example.data.preferences.AccountPreferencesStore
import com.example.data.preferences.ScheduledMatchReminder
import kotlinx.coroutines.flow.first

const val MATCH_REMINDER_LEAD_MILLIS = 15L * 60_000L

enum class ReminderScheduleDecision {
    NONE,
    FIFTEEN_MINUTES_BEFORE,
    IMMEDIATE_WITHIN_WINDOW
}

data class MatchReminderPlan(
    val decision: ReminderScheduleDecision,
    val triggerAtMillis: Long
)

data class PlannedMatchReminder(
    val reminder: ScheduledMatchReminder,
    val decision: ReminderScheduleDecision
)

fun computeMatchReminderSchedule(
    startTimeMillis: Long,
    status: String,
    registrationStatus: String,
    remindersEnabled: Boolean,
    nowMillis: Long
): MatchReminderPlan {
    if (!remindersEnabled) return MatchReminderPlan(ReminderScheduleDecision.NONE, 0L)
    if (registrationStatus.equals("REJECTED", ignoreCase = true)) {
        return MatchReminderPlan(ReminderScheduleDecision.NONE, 0L)
    }
    if (MatchStatus.fromRaw(status) != MatchStatus.UPCOMING) {
        return MatchReminderPlan(ReminderScheduleDecision.NONE, 0L)
    }
    if (startTimeMillis <= 0L || startTimeMillis <= nowMillis) {
        return MatchReminderPlan(ReminderScheduleDecision.NONE, 0L)
    }
    val triggerAtMillis = startTimeMillis - MATCH_REMINDER_LEAD_MILLIS
    return if (triggerAtMillis > nowMillis) {
        MatchReminderPlan(ReminderScheduleDecision.FIFTEEN_MINUTES_BEFORE, triggerAtMillis)
    } else {
        MatchReminderPlan(ReminderScheduleDecision.IMMEDIATE_WITHIN_WINDOW, nowMillis)
    }
}

fun buildScheduledMatchReminders(
    registrations: List<Registration>,
    tournamentsById: Map<String, TournamentMatch>,
    remindersEnabled: Boolean,
    nowMillis: Long
): List<PlannedMatchReminder> {
    if (!remindersEnabled) return emptyList()
    val seenTournamentIds = mutableSetOf<String>()
    val planned = mutableListOf<PlannedMatchReminder>()

    registrations.forEach { reg ->
        val tid = reg.tournamentId.trim()
        if (tid.isBlank() || !seenTournamentIds.add(tid)) return@forEach
        val match = tournamentsById[tid] ?: return@forEach
        val schedule = computeMatchReminderSchedule(
            startTimeMillis = match.startTimeMillis,
            status = match.status,
            registrationStatus = reg.status,
            remindersEnabled = remindersEnabled,
            nowMillis = nowMillis
        )
        if (schedule.decision == ReminderScheduleDecision.NONE) return@forEach

        val reminder = ScheduledMatchReminder(
            tournamentId = tid,
            tournamentTitle = match.title.ifBlank { reg.tournamentTitle },
            teamType = match.teamType.ifBlank { reg.teamType },
            mapName = match.mapName.ifBlank { reg.mapName },
            slotNumber = reg.slotNumber.toInt(),
            startTimeMillis = match.startTimeMillis,
            triggerAtMillis = schedule.triggerAtMillis,
            roomPublished = match.roomPublished
        )
        planned.add(PlannedMatchReminder(reminder = reminder, decision = schedule.decision))
    }

    return planned
}

object TournamentReminderScheduler {
    const val ACTION_SHOW_TOURNAMENT_REMINDER = "com.example.action.SHOW_TOURNAMENT_REMINDER"
    const val EXTRA_TOURNAMENT_ID = "extra_reminder_tournament_id"
    const val EXTRA_TOURNAMENT_TITLE = "extra_reminder_tournament_title"
    const val EXTRA_TEAM_TYPE = "extra_reminder_team_type"
    const val EXTRA_MAP_NAME = "extra_reminder_map_name"
    const val EXTRA_SLOT_NUMBER = "extra_reminder_slot_number"
    const val EXTRA_START_TIME_MILLIS = "extra_reminder_start_time_millis"
    const val EXTRA_ROOM_PUBLISHED = "extra_reminder_room_published"

    private fun alarmRequestCode(tournamentId: String): Int =
        "match_reminder_alarm_${tournamentId.trim()}".hashCode()

    private fun buildAlarmIntent(context: Context, reminder: ScheduledMatchReminder): Intent =
        Intent(context, TournamentReminderReceiver::class.java).apply {
            action = ACTION_SHOW_TOURNAMENT_REMINDER
            putExtra(EXTRA_TOURNAMENT_ID, reminder.tournamentId)
            putExtra(EXTRA_TOURNAMENT_TITLE, reminder.tournamentTitle)
            putExtra(EXTRA_TEAM_TYPE, reminder.teamType)
            putExtra(EXTRA_MAP_NAME, reminder.mapName)
            putExtra(EXTRA_SLOT_NUMBER, reminder.slotNumber)
            putExtra(EXTRA_START_TIME_MILLIS, reminder.startTimeMillis)
            putExtra(EXTRA_ROOM_PUBLISHED, reminder.roomPublished)
        }

    private fun buildCancelIntent(context: Context): Intent =
        Intent(context, TournamentReminderReceiver::class.java).apply {
            action = ACTION_SHOW_TOURNAMENT_REMINDER
        }

    private fun scheduleAlarm(context: Context, reminder: ScheduledMatchReminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(reminder.tournamentId),
            buildAlarmIntent(context, reminder),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.triggerAtMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(context: Context, tournamentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val existing = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(tournamentId),
            buildCancelIntent(context),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    suspend fun syncReminders(
        context: Context,
        preferencesStore: AccountPreferencesStore,
        registrations: List<Registration>,
        tournaments: List<TournamentMatch>,
        remindersEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val appContext = context.applicationContext
        val previous = preferencesStore.getScheduledReminders()

        if (!remindersEnabled) {
            previous.forEach { cancelAlarm(appContext, it.tournamentId) }
            preferencesStore.clearScheduledReminders()
            return
        }

        val tournamentsById = tournaments.associateBy { it.id }
        val planned = buildScheduledMatchReminders(
            registrations = registrations,
            tournamentsById = tournamentsById,
            remindersEnabled = true,
            nowMillis = nowMillis
        )
        val seenKeys = runCatching { preferencesStore.notifiedEventKeysFlow.first() }
            .getOrDefault(emptySet())

        val futureReminders = mutableListOf<ScheduledMatchReminder>()
        val plannedFutureIds = mutableSetOf<String>()

        planned.forEach { entry ->
            val rem = entry.reminder
            val eventKey = ArenaNotificationHelper.matchReminderEventKey(
                rem.tournamentId,
                rem.startTimeMillis
            )
            when (entry.decision) {
                ReminderScheduleDecision.FIFTEEN_MINUTES_BEFORE -> {
                    plannedFutureIds.add(rem.tournamentId)
                    futureReminders.add(rem)
                    if (!seenKeys.contains(eventKey)) {
                        scheduleAlarm(appContext, rem)
                    } else {
                        cancelAlarm(appContext, rem.tournamentId)
                    }
                }
                ReminderScheduleDecision.IMMEDIATE_WITHIN_WINDOW -> {
                    cancelAlarm(appContext, rem.tournamentId)
                    if (!seenKeys.contains(eventKey) && preferencesStore.markNotificationSeen(eventKey)) {
                        val payload = ArenaNotificationHelper.buildMatchReminderNotification(
                            tournamentId = rem.tournamentId,
                            tournamentTitle = rem.tournamentTitle,
                            teamType = rem.teamType,
                            mapName = rem.mapName,
                            slotNumber = rem.slotNumber,
                            startTimeMillis = rem.startTimeMillis,
                            nowMillis = nowMillis,
                            roomPublished = rem.roomPublished
                        )
                        ArenaNotificationHelper.showNotification(appContext, payload)
                    }
                }
                ReminderScheduleDecision.NONE -> Unit
            }
        }

        previous.forEach { old ->
            if (!plannedFutureIds.contains(old.tournamentId)) {
                cancelAlarm(appContext, old.tournamentId)
            }
        }

        preferencesStore.saveScheduledReminders(futureReminders)
    }

    suspend fun reschedulePersistedReminders(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val appContext = context.applicationContext
        val preferencesStore = AccountPreferencesStore(appContext)
        val settings = runCatching { preferencesStore.settingsFlow.first() }.getOrNull()
        val stored = preferencesStore.getScheduledReminders()
        if (settings != null && !settings.matchRemindersEnabled) {
            stored.forEach { cancelAlarm(appContext, it.tournamentId) }
            preferencesStore.clearScheduledReminders()
            return
        }

        val seenKeys = runCatching { preferencesStore.notifiedEventKeysFlow.first() }
            .getOrDefault(emptySet())
        val activeFuture = mutableListOf<ScheduledMatchReminder>()

        stored.forEach { rem ->
            if (rem.startTimeMillis <= nowMillis) {
                cancelAlarm(appContext, rem.tournamentId)
                return@forEach
            }
            val eventKey = ArenaNotificationHelper.matchReminderEventKey(
                rem.tournamentId,
                rem.startTimeMillis
            )
            if (seenKeys.contains(eventKey)) {
                cancelAlarm(appContext, rem.tournamentId)
                return@forEach
            }
            if (rem.triggerAtMillis > nowMillis) {
                scheduleAlarm(appContext, rem)
                activeFuture.add(rem)
            } else if (preferencesStore.markNotificationSeen(eventKey)) {
                val payload = ArenaNotificationHelper.buildMatchReminderNotification(
                    tournamentId = rem.tournamentId,
                    tournamentTitle = rem.tournamentTitle,
                    teamType = rem.teamType,
                    mapName = rem.mapName,
                    slotNumber = rem.slotNumber,
                    startTimeMillis = rem.startTimeMillis,
                    nowMillis = nowMillis,
                    roomPublished = rem.roomPublished
                )
                ArenaNotificationHelper.showNotification(appContext, payload)
            }
        }

        preferencesStore.saveScheduledReminders(activeFuture)
    }

    suspend fun cancelAllReminders(
        context: Context,
        preferencesStore: AccountPreferencesStore
    ) {
        val appContext = context.applicationContext
        val stored = preferencesStore.getScheduledReminders()
        stored.forEach { cancelAlarm(appContext, it.tournamentId) }
        preferencesStore.clearScheduledReminders()
    }
}
