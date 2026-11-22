package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

enum class ArenaNotificationType {
    WALLET_APPROVED,
    WALLET_REJECTED,
    ROOM_PUBLISHED,
    MATCH_REMINDER,
    GENERAL
}

data class ParsedNotificationPayload(
    val eventKey: String,
    val type: ArenaNotificationType,
    val title: String,
    val body: String,
    val channelId: String,
    val openTab: String,
    val tournamentId: String? = null
)

object ArenaNotificationHelper {
    const val CHANNEL_WALLET_UPDATES = "wallet_updates"
    const val CHANNEL_ROOM_CREDENTIALS = "room_credentials"
    const val CHANNEL_MATCH_REMINDERS = "match_reminders"

    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val EXTRA_TOURNAMENT_ID = "extra_tournament_id"

    const val TAB_MATCHES = "matches"
    const val TAB_MY_MATCHES = "my_matches"
    const val TAB_WALLET = "wallet"

    const val TOPIC_ALL_PLAYERS = "all_players"

    fun sanitizeTopicSegment(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9\\-_.~%]"), "_").take(120)

    fun userTopic(userId: String): String = "user_${sanitizeTopicSegment(userId)}"

    fun tournamentTopic(tournamentId: String): String = "tournament_${sanitizeTopicSegment(tournamentId)}"

    fun walletEventKey(requestId: String, status: String): String =
        "wallet_${requestId.trim()}_${status.trim().uppercase()}"

    fun roomEventKey(tournamentId: String, roomId: String, roomPassword: String): String =
        "room_${tournamentId.trim()}_${roomId.trim()}_${roomPassword.trim()}"

    fun matchReminderEventKey(tournamentId: String, startTimeMillis: Long): String =
        "match_reminder_15m_${tournamentId.trim()}_$startTimeMillis"

    fun buildWalletDecisionNotification(
        requestId: String,
        requestType: String,
        status: String,
        amount: Long,
        currencyLabel: String,
        method: String,
        accountNumber: String,
        adminNote: String
    ): ParsedNotificationPayload {
        val approved = status.equals("APPROVED", ignoreCase = true)
        val isDeposit = requestType.equals("DEPOSIT", ignoreCase = true)
        val unit = currencyLabel.ifBlank { "BDT" }
        val title = when {
            approved && isDeposit -> "Payment Approved! +$amount $unit"
            approved && !isDeposit -> "Withdrawal Paid! $amount $unit"
            !approved && isDeposit -> "Add Money Update ($method)"
            else -> "Withdraw Request Update ($method)"
        }
        val body = when {
            approved && isDeposit -> {
                val note = adminNote.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                "Your $method deposit of $amount $unit has been added to your Wallet$note."
            }
            approved && !isDeposit -> {
                val dest = accountNumber.takeIf { it.isNotBlank() } ?: method
                val note = adminNote.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                "Admin sent $amount $unit via $method to $dest$note."
            }
            else -> {
                val reason = adminNote.ifBlank { "Please check details or contact support." }
                "Your $method request for $amount $unit was rejected: $reason"
            }
        }
        return ParsedNotificationPayload(
            eventKey = walletEventKey(requestId, status.uppercase()),
            type = if (approved) ArenaNotificationType.WALLET_APPROVED else ArenaNotificationType.WALLET_REJECTED,
            title = title,
            body = body,
            channelId = CHANNEL_WALLET_UPDATES,
            openTab = TAB_WALLET,
            tournamentId = null
        )
    }

    fun buildRoomPublishedNotification(
        tournamentId: String,
        tournamentTitle: String,
        roomId: String,
        roomPassword: String
    ): ParsedNotificationPayload {
        val safeTitle = tournamentTitle.ifBlank { "Your Tournament" }
        val hasCreds = roomId.isNotBlank()
        val title = "Room ID & Password Released!"
        val body = if (hasCreds) {
            "$safeTitle • Room ID: $roomId | Pass: ${roomPassword.ifBlank { "None" }}. Join custom room now!"
        } else {
            "$safeTitle room is now live! Tap to open Room Details and view credentials."
        }
        return ParsedNotificationPayload(
            eventKey = roomEventKey(tournamentId, roomId, roomPassword),
            type = ArenaNotificationType.ROOM_PUBLISHED,
            title = title,
            body = body,
            channelId = CHANNEL_ROOM_CREDENTIALS,
            openTab = TAB_MY_MATCHES,
            tournamentId = tournamentId.takeIf { it.isNotBlank() }
        )
    }

    fun buildMatchReminderNotification(
        tournamentId: String,
        tournamentTitle: String,
        teamType: String,
        mapName: String,
        slotNumber: Int,
        startTimeMillis: Long,
        nowMillis: Long,
        roomPublished: Boolean
    ): ParsedNotificationPayload {
        val safeTitle = tournamentTitle.ifBlank { "Registered Tournament" }
        val remainingMinutes = ((startTimeMillis - nowMillis).coerceAtLeast(0L) + 59_999L) / 60_000L
        val timeHeading = when {
            remainingMinutes <= 1L -> "Starting Now"
            remainingMinutes >= 14L -> "Starts in 15 Minutes"
            else -> "Starts in $remainingMinutes Minutes"
        }
        val matchDetails = listOfNotNull(
            teamType.takeIf { it.isNotBlank() },
            mapName.takeIf { it.isNotBlank() },
            slotNumber.takeIf { it > 0 }?.let { "Slot #$it" }
        ).joinToString(" • ")
        val roomHint = if (roomPublished) {
            "Room ID & Password are ready — join the custom room now!"
        } else {
            "Open Match Details & check Room Credentials before kickoff."
        }
        val body = if (matchDetails.isNotBlank()) {
            "$safeTitle ($matchDetails) — $roomHint"
        } else {
            "$safeTitle — $roomHint"
        }
        return ParsedNotificationPayload(
            eventKey = matchReminderEventKey(tournamentId, startTimeMillis),
            type = ArenaNotificationType.MATCH_REMINDER,
            title = "$safeTitle • $timeHeading!",
            body = body,
            channelId = CHANNEL_MATCH_REMINDERS,
            openTab = TAB_MY_MATCHES,
            tournamentId = tournamentId.takeIf { it.isNotBlank() }
        )
    }

    fun parseFcmPayload(
        data: Map<String, String>,
        fallbackTitle: String?,
        fallbackBody: String?,
        fallbackMessageId: String?
    ): ParsedNotificationPayload {
        val rawType = data["type"]?.trim()?.uppercase().orEmpty()
        return when (rawType) {
            "WALLET_APPROVED", "WALLET_REJECTED", "WALLET_UPDATE" -> {
                val status = when {
                    rawType == "WALLET_APPROVED" -> "APPROVED"
                    rawType == "WALLET_REJECTED" -> "REJECTED"
                    else -> data["status"]?.trim()?.uppercase().orEmpty().ifBlank { "APPROVED" }
                }
                val requestId = data["requestId"]?.trim().orEmpty()
                    .ifBlank { fallbackMessageId?.trim().orEmpty().ifBlank { System.currentTimeMillis().toString() } }
                val amount = data["amount"]?.trim()?.toLongOrNull() ?: 0L
                val payload = buildWalletDecisionNotification(
                    requestId = requestId,
                    requestType = data["requestType"]?.trim().orEmpty().ifBlank { "DEPOSIT" },
                    status = status,
                    amount = amount,
                    currencyLabel = data["currencyLabel"]?.trim().orEmpty().ifBlank { "BDT" },
                    method = data["method"]?.trim().orEmpty().ifBlank { "bKash/Nagad" },
                    accountNumber = data["accountNumber"]?.trim().orEmpty(),
                    adminNote = data["adminNote"]?.trim().orEmpty()
                )
                payload.copy(
                    title = fallbackTitle?.takeIf { it.isNotBlank() } ?: payload.title,
                    body = fallbackBody?.takeIf { it.isNotBlank() } ?: payload.body
                )
            }
            "ROOM_PUBLISHED", "ROOM_RELEASED", "ROOM_CREDENTIALS" -> {
                val tournamentId = data["tournamentId"]?.trim().orEmpty()
                val roomId = data["roomId"]?.trim().orEmpty()
                val roomPassword = data["roomPassword"]?.trim().orEmpty()
                val payload = buildRoomPublishedNotification(
                    tournamentId = tournamentId,
                    tournamentTitle = data["tournamentTitle"]?.trim().orEmpty(),
                    roomId = roomId,
                    roomPassword = roomPassword
                )
                payload.copy(
                    eventKey = if (tournamentId.isNotBlank()) {
                        roomEventKey(tournamentId, roomId, roomPassword)
                    } else {
                        "fcm_room_${fallbackMessageId ?: System.currentTimeMillis()}"
                    },
                    title = fallbackTitle?.takeIf { it.isNotBlank() } ?: payload.title,
                    body = fallbackBody?.takeIf { it.isNotBlank() } ?: payload.body
                )
            }
            else -> {
                val tournamentId = data["tournamentId"]?.trim()?.takeIf { it.isNotBlank() }
                val openTab = data["openTab"]?.trim()?.takeIf { it.isNotBlank() } ?: TAB_MATCHES
                val channelId = if (openTab == TAB_WALLET) CHANNEL_WALLET_UPDATES else CHANNEL_ROOM_CREDENTIALS
                ParsedNotificationPayload(
                    eventKey = "fcm_gen_${fallbackMessageId ?: System.currentTimeMillis()}",
                    type = ArenaNotificationType.GENERAL,
                    title = fallbackTitle?.takeIf { it.isNotBlank() } ?: "Booyah Arena Alert",
                    body = fallbackBody?.takeIf { it.isNotBlank() } ?: "Check Booyah Arena for the latest tournament update.",
                    channelId = channelId,
                    openTab = openTab,
                    tournamentId = tournamentId
                )
            }
        }
    }

    fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val walletChannel = NotificationChannel(
            CHANNEL_WALLET_UPDATES,
            context.getString(R.string.notif_channel_wallet_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_wallet_desc)
            enableVibration(true)
        }

        val roomChannel = NotificationChannel(
            CHANNEL_ROOM_CREDENTIALS,
            context.getString(R.string.notif_channel_room_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_room_desc)
            enableVibration(true)
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_MATCH_REMINDERS,
            context.getString(R.string.notif_channel_reminder_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_reminder_desc)
            enableVibration(true)
        }

        manager.createNotificationChannel(walletChannel)
        manager.createNotificationChannel(roomChannel)
        manager.createNotificationChannel(reminderChannel)
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, payload: ParsedNotificationPayload): Boolean {
        ensureNotificationChannels(context)
        if (!hasPostNotificationPermission(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, payload.openTab)
            payload.tournamentId?.let { putExtra(EXTRA_TOURNAMENT_ID, it) }
        }
        val requestCode = payload.eventKey.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
