package com.example.notifications

import android.util.Log
import com.example.R
import com.example.data.firebase.TournamentRepository
import com.example.data.preferences.AccountPreferencesStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ArenaMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ArenaNotificationHelper.ensureNotificationChannels(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token received")
        val uid = Firebase.auth.currentUser?.uid ?: return
        serviceScope.launch {
            runCatching {
                val databaseId = applicationContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                TournamentRepository(db).saveUserFcmToken(uid, token)
            }.onFailure { err ->
                Log.w(TAG, "Failed saving refreshed FCM token", err)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val payload = ArenaNotificationHelper.parseFcmPayload(
            data = message.data,
            fallbackTitle = message.notification?.title,
            fallbackBody = message.notification?.body,
            fallbackMessageId = message.messageId
        )

        serviceScope.launch {
            val prefs = AccountPreferencesStore(applicationContext)
            val settings = runCatching { prefs.settingsFlow.first() }.getOrNull()
            if (settings?.pushNotificationsEnabled == false) {
                return@launch
            }
            if (payload.type == ArenaNotificationType.ROOM_PUBLISHED) {
                val tid = payload.tournamentId
                if (tid.isNullOrBlank()) return@launch
                val joinedIds = runCatching { prefs.getJoinedTournamentIds() }.getOrDefault(emptySet())
                if (!joinedIds.contains(tid)) {
                    return@launch
                }
            }
            val isNew = runCatching { prefs.markNotificationSeen(payload.eventKey) }.getOrDefault(true)
            if (isNew) {
                ArenaNotificationHelper.showNotification(applicationContext, payload)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "ArenaMessagingService"
    }
}
