package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

const val DEFAULT_TELEGRAM_SUPPORT_NUMBER = "01772902980"
private const val LEGACY_DEFAULT_SUPPORT = "Telegram: @BooyahSupport"

data class TelegramSupportTarget(
    val displayLabel: String,
    val appUri: String,
    val webUrl: String
)

fun resolveTelegramSupportDisplay(rawContact: String): String {
    val trimmed = rawContact.trim()
    return if (trimmed.isBlank() || trimmed.equals(LEGACY_DEFAULT_SUPPORT, ignoreCase = true)) {
        DEFAULT_TELEGRAM_SUPPORT_NUMBER
    } else {
        trimmed
    }
}

fun buildTelegramSupportTarget(rawContact: String): TelegramSupportTarget {
    val display = resolveTelegramSupportDisplay(rawContact)
    val tMeMatch = Regex("(?:https?://)?(?:t\\.me|telegram\\.me)/([+A-Za-z0-9_]+)", RegexOption.IGNORE_CASE)
        .find(display)
    if (tMeMatch != null) {
        val path = tMeMatch.groupValues[1]
        return if (path.startsWith("+")) {
            val digits = path.drop(1).filter(Char::isDigit)
            TelegramSupportTarget(
                displayLabel = display,
                appUri = "tg://resolve?phone=$digits",
                webUrl = "https://t.me/+$digits"
            )
        } else {
            TelegramSupportTarget(
                displayLabel = display,
                appUri = "tg://resolve?domain=$path",
                webUrl = "https://t.me/$path"
            )
        }
    }

    val usernameMatch = Regex("@([A-Za-z0-9_]{4,})").find(display)
    if (usernameMatch != null) {
        val username = usernameMatch.groupValues[1]
        return TelegramSupportTarget(
            displayLabel = display,
            appUri = "tg://resolve?domain=$username",
            webUrl = "https://t.me/$username"
        )
    }

    val digitsOnly = display.filter(Char::isDigit)
    val intlDigits = when {
        digitsOnly.length == 11 && digitsOnly.startsWith("01") -> "88$digitsOnly"
        digitsOnly.length >= 10 -> digitsOnly
        else -> "88$DEFAULT_TELEGRAM_SUPPORT_NUMBER"
    }
    val finalDisplay = if (digitsOnly.length >= 10) display else DEFAULT_TELEGRAM_SUPPORT_NUMBER
    return TelegramSupportTarget(
        displayLabel = finalDisplay,
        appUri = "tg://resolve?phone=$intlDigits",
        webUrl = "https://t.me/+$intlDigits"
    )
}

fun openTelegramSupport(
    context: Context,
    rawContact: String,
    onUnavailable: ((String) -> Unit)? = null
) {
    val target = buildTelegramSupportTarget(rawContact)
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target.appUri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(appIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // Fall through to universal https://t.me link
    } catch (_: Exception) {
        // Fall through to universal https://t.me link
    }

    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target.webUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(webIntent)
    } catch (_: Exception) {
        onUnavailable?.invoke("Telegram Support: ${target.displayLabel}")
    }
}
