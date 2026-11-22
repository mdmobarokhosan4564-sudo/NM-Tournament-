package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.example.data.model.MatchStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CountdownUrgency {
    NORMAL,
    SOON,
    CRITICAL,
    STARTING_NOW
}

data class MatchCountdown(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val remainingMillis: Long,
    val urgency: CountdownUrgency
)

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

fun computeMatchCountdown(
    startTimeMillis: Long,
    currentTimeMillis: Long,
    status: String
): MatchCountdown? {
    if (startTimeMillis <= 0L) return null
    if (MatchStatus.fromRaw(status) != MatchStatus.UPCOMING) return null

    val diff = startTimeMillis - currentTimeMillis
    if (diff <= 0L) {
        return MatchCountdown(
            days = 0L,
            hours = 0L,
            minutes = 0L,
            seconds = 0L,
            remainingMillis = 0L,
            urgency = CountdownUrgency.STARTING_NOW
        )
    }

    val days = diff / DAY_MILLIS
    val hours = (diff % DAY_MILLIS) / HOUR_MILLIS
    val minutes = (diff % HOUR_MILLIS) / MINUTE_MILLIS
    val seconds = (diff % MINUTE_MILLIS) / 1000L

    val urgency = when {
        diff <= 15 * MINUTE_MILLIS -> CountdownUrgency.CRITICAL
        diff <= HOUR_MILLIS -> CountdownUrgency.SOON
        else -> CountdownUrgency.NORMAL
    }

    return MatchCountdown(
        days = days,
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        remainingMillis = diff,
        urgency = urgency
    )
}

fun formatCompactCountdown(countdown: MatchCountdown): String {
    if (countdown.urgency == CountdownUrgency.STARTING_NOW) {
        return "STARTING NOW"
    }
    return if (countdown.days > 0L) {
        String.format(
            Locale.US,
            "%dd %02dh %02dm",
            countdown.days,
            countdown.hours,
            countdown.minutes
        )
    } else {
        String.format(
            Locale.US,
            "%02d:%02d:%02d",
            countdown.hours,
            countdown.minutes,
            countdown.seconds
        )
    }
}

fun formatScheduleTextFromMillis(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0L) return "TBA"
    val targetCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val tomorrowCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }

    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timePart = timeFmt.format(Date(epochMillis))

    return when {
        targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) ->
            "Today • $timePart"
        targetCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR) ->
            "Tomorrow • $timePart"
        else -> {
            val dateFmt = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
            dateFmt.format(Date(epochMillis))
        }
    }
}

@Composable
fun rememberLiveClockMillis(intervalMillis: Long = 1000L): Long {
    val currentTime by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(intervalMillis)
            value = System.currentTimeMillis()
        }
    }
    return currentTime
}
