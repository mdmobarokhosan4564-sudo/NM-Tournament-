package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.accountSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "booyah_account_settings"
)

data class AccountSettings(
    val autoFillIdentity: Boolean = true,
    val showMatchCountdowns: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val roomReminderHighlights: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val matchRemindersEnabled: Boolean = true
)

data class ScheduledMatchReminder(
    val tournamentId: String,
    val tournamentTitle: String,
    val teamType: String,
    val mapName: String,
    val slotNumber: Int,
    val startTimeMillis: Long,
    val triggerAtMillis: Long,
    val roomPublished: Boolean
) {
    fun serialize(): String {
        return listOf(
            encodeSegment(tournamentId),
            encodeSegment(tournamentTitle),
            encodeSegment(teamType),
            encodeSegment(mapName),
            slotNumber.toString(),
            startTimeMillis.toString(),
            triggerAtMillis.toString(),
            if (roomPublished) "1" else "0"
        ).joinToString("|")
    }

    companion object {
        private fun encodeSegment(value: String): String = value
            .replace("%", "%25")
            .replace("|", "%7C")
            .replace("\n", "%0A")

        private fun decodeSegment(value: String): String = value
            .replace("%0A", "\n")
            .replace("%7C", "|")
            .replace("%25", "%")

        fun deserialize(raw: String): ScheduledMatchReminder? {
            val parts = raw.split("|")
            if (parts.size < 8) return null
            val tid = decodeSegment(parts[0]).trim()
            if (tid.isBlank()) return null
            val slot = parts[4].toIntOrNull() ?: 0
            val start = parts[5].toLongOrNull() ?: return null
            val trigger = parts[6].toLongOrNull() ?: return null
            val roomPub = parts[7] == "1"
            return ScheduledMatchReminder(
                tournamentId = tid,
                tournamentTitle = decodeSegment(parts[1]),
                teamType = decodeSegment(parts[2]),
                mapName = decodeSegment(parts[3]),
                slotNumber = slot,
                startTimeMillis = start,
                triggerAtMillis = trigger,
                roomPublished = roomPub
            )
        }
    }
}

fun trimSeenNotificationKeys(
    existing: Set<String>,
    newlyAdded: String,
    maxEntries: Int = 120
): Set<String> {
    if (newlyAdded.isBlank()) return existing
    val combined = existing.toMutableList()
    combined.remove(newlyAdded)
    combined.add(newlyAdded)
    return if (combined.size > maxEntries) {
        combined.takeLast(maxEntries).toSet()
    } else {
        combined.toSet()
    }
}

class AccountPreferencesStore(private val context: Context) {
    private object Keys {
        val AUTO_FILL_IDENTITY = booleanPreferencesKey("auto_fill_identity")
        val SHOW_MATCH_COUNTDOWNS = booleanPreferencesKey("show_match_countdowns")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val ROOM_REMINDER_HIGHLIGHTS = booleanPreferencesKey("room_reminder_highlights")
        val PUSH_NOTIFICATIONS_ENABLED = booleanPreferencesKey("push_notifications_enabled")
        val MATCH_REMINDERS_ENABLED = booleanPreferencesKey("match_reminders_enabled")
        val NOTIFIED_EVENT_KEYS = stringSetPreferencesKey("notified_event_keys")
        val SCHEDULED_MATCH_REMINDERS = stringSetPreferencesKey("scheduled_match_reminders")
        val JOINED_TOURNAMENT_IDS = stringSetPreferencesKey("joined_tournament_ids")
        val SUBSCRIBED_TOURNAMENT_TOPICS = stringSetPreferencesKey("subscribed_tournament_topics")
    }

    val settingsFlow: Flow<AccountSettings> = context.accountSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            AccountSettings(
                autoFillIdentity = prefs[Keys.AUTO_FILL_IDENTITY] ?: true,
                showMatchCountdowns = prefs[Keys.SHOW_MATCH_COUNTDOWNS] ?: true,
                hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true,
                roomReminderHighlights = prefs[Keys.ROOM_REMINDER_HIGHLIGHTS] ?: true,
                pushNotificationsEnabled = prefs[Keys.PUSH_NOTIFICATIONS_ENABLED] ?: true,
                matchRemindersEnabled = prefs[Keys.MATCH_REMINDERS_ENABLED] ?: true
            )
        }

    val notifiedEventKeysFlow: Flow<Set<String>> = context.accountSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            prefs[Keys.NOTIFIED_EVENT_KEYS] ?: emptySet()
        }

    suspend fun setAutoFillIdentity(enabled: Boolean) {
        context.accountSettingsDataStore.edit { it[Keys.AUTO_FILL_IDENTITY] = enabled }
    }

    suspend fun setShowMatchCountdowns(enabled: Boolean) {
        context.accountSettingsDataStore.edit { it[Keys.SHOW_MATCH_COUNTDOWNS] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.accountSettingsDataStore.edit { it[Keys.HAPTIC_FEEDBACK_ENABLED] = enabled }
    }

    suspend fun setRoomReminderHighlights(enabled: Boolean) {
        context.accountSettingsDataStore.edit { it[Keys.ROOM_REMINDER_HIGHLIGHTS] = enabled }
    }

    suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        context.accountSettingsDataStore.edit { it[Keys.PUSH_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setMatchRemindersEnabled(enabled: Boolean) {
        context.accountSettingsDataStore.edit { it[Keys.MATCH_REMINDERS_ENABLED] = enabled }
    }

    suspend fun getScheduledReminders(): List<ScheduledMatchReminder> {
        val rawSet = context.accountSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences())
                else throw exception
            }
            .map { prefs -> prefs[Keys.SCHEDULED_MATCH_REMINDERS] ?: emptySet() }
            .first()
        return rawSet.mapNotNull { ScheduledMatchReminder.deserialize(it) }
    }

    suspend fun saveScheduledReminders(reminders: List<ScheduledMatchReminder>) {
        val serialized = reminders.map { it.serialize() }.toSet()
        context.accountSettingsDataStore.edit { prefs ->
            prefs[Keys.SCHEDULED_MATCH_REMINDERS] = serialized
        }
    }

    suspend fun clearScheduledReminders() {
        context.accountSettingsDataStore.edit { prefs ->
            prefs.remove(Keys.SCHEDULED_MATCH_REMINDERS)
        }
    }

    suspend fun getJoinedTournamentIds(): Set<String> {
        return context.accountSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences())
                else throw exception
            }
            .map { prefs -> prefs[Keys.JOINED_TOURNAMENT_IDS] ?: emptySet() }
            .first()
    }

    suspend fun setJoinedTournamentIds(ids: Set<String>) {
        context.accountSettingsDataStore.edit { prefs ->
            prefs[Keys.JOINED_TOURNAMENT_IDS] = ids
        }
    }

    suspend fun getSubscribedTournamentTopics(): Set<String> {
        return context.accountSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences())
                else throw exception
            }
            .map { prefs -> prefs[Keys.SUBSCRIBED_TOURNAMENT_TOPICS] ?: emptySet() }
            .first()
    }

    suspend fun setSubscribedTournamentTopics(ids: Set<String>) {
        context.accountSettingsDataStore.edit { prefs ->
            prefs[Keys.SUBSCRIBED_TOURNAMENT_TOPICS] = ids
        }
    }

    /**
     * Records [eventKey] in DataStore if not already present.
     * Returns true if this is a brand-new event key, false if already seen.
     */
    suspend fun markNotificationSeen(eventKey: String): Boolean {
        if (eventKey.isBlank()) return false
        var isNew = false
        context.accountSettingsDataStore.edit { prefs ->
            val current = prefs[Keys.NOTIFIED_EVENT_KEYS] ?: emptySet()
            if (!current.contains(eventKey)) {
                isNew = true
                prefs[Keys.NOTIFIED_EVENT_KEYS] = trimSeenNotificationKeys(current, eventKey)
            }
        }
        return isNew
    }

    suspend fun seedSeenNotificationKeys(keys: Set<String>) {
        if (keys.isEmpty()) return
        context.accountSettingsDataStore.edit { prefs ->
            val current = prefs[Keys.NOTIFIED_EVENT_KEYS] ?: emptySet()
            var merged = current
            keys.forEach { key ->
                merged = trimSeenNotificationKeys(merged, key)
            }
            prefs[Keys.NOTIFIED_EVENT_KEYS] = merged
        }
    }
}
