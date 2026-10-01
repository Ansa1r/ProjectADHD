package com.ansa1r.projectadhd.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

fun dayKey(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = zone }.format(Date(now))

fun dayStart(now: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

fun currentDayFlow() = flow {
    while (true) {
        emit(dayKey())
        delay(1_000)
    }
}.distinctUntilChanged()

fun durationText(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}

fun timestampText(millis: Long?): String =
    millis?.let { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "—"

data class DayBounds(val start: Long, val end: Long)
fun dayBounds(now: Long, zone: TimeZone = TimeZone.getDefault()): DayBounds {
    val start = dayStart(now, zone)
    val end = Calendar.getInstance(zone).apply { timeInMillis = start; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
    return DayBounds(start, end)
}
fun currentDayBoundsFlow() = flow {
    while (true) { emit(dayBounds(System.currentTimeMillis())); delay(1_000) }
}.distinctUntilChanged()

fun nextDayKey(date: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    return Calendar.getInstance().run {
        time = requireNotNull(parser.parse(date))
        add(Calendar.DAY_OF_YEAR, 1)
        parser.format(time)
    }
}

fun dateBounds(date: String): DayBounds {
    val parsed = requireNotNull(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(date))
    return dayBounds(parsed.time)
}
