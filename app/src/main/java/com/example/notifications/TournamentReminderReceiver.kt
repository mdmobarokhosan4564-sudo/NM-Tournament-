package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.AccountPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TournamentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TournamentReminderScheduler.ACTION_SHOW_TOURNAMENT_REMINDER) return

        val tournamentId = intent.getStringExtra(TournamentReminderScheduler.EXTRA_TOURNAMENT_ID)
            ?.trim().orEmpty()
        if (tournamentId.isBlank()) return

        val tournamentTitle = intent.getStringExtra(TournamentReminderScheduler.EXTRA_TOURNAMENT_TITLE).orEmpty()
        val teamType = intent.getStringExtra(TournamentReminderScheduler.EXTRA_TEAM_TYPE).orEmpty()
        val mapName = intent.getStringExtra(TournamentReminderScheduler.EXTRA_MAP_NAME).orEmpty()
        val slotNumber = intent.getIntExtra(TournamentReminderScheduler.EXTRA_SLOT_NUMBER, 0)
        val startTimeMillis = intent.getLongExtra(TournamentReminderScheduler.EXTRA_START_TIME_MILLIS, 0L)
        val roomPublished = intent.getBooleanExtra(TournamentReminderScheduler.EXTRA_ROOM_PUBLISHED, false)

        if (startTimeMillis <= 0L) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val nowMillis = System.currentTimeMillis()
                if (startTimeMillis <= nowMillis) return@launch

                val store = AccountPreferencesStore(appContext)
                val settings = store.settingsFlow.first()
                if (!settings.matchRemindersEnabled) return@launch

                val eventKey = ArenaNotificationHelper.matchReminderEventKey(tournamentId, startTimeMillis)
                if (store.markNotificationSeen(eventKey)) {
                    val payload = ArenaNotificationHelper.buildMatchReminderNotification(
                        tournamentId = tournamentId,
                        tournamentTitle = tournamentTitle,
                        teamType = teamType,
                        mapName = mapName,
                        slotNumber = slotNumber,
                        startTimeMillis = startTimeMillis,
                        nowMillis = nowMillis,
                        roomPublished = roomPublished
                    )
                    ArenaNotificationHelper.showNotification(appContext, payload)
                }

                val remaining = store.getScheduledReminders()
                    .filterNot { it.tournamentId == tournamentId && it.startTimeMillis == startTimeMillis }
                store.saveScheduledReminders(remaining)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
