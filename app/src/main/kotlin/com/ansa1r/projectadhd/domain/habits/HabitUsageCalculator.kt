package com.ansa1r.projectadhd.domain.habits

import com.ansa1r.projectadhd.domain.SessionTracker
import com.ansa1r.projectadhd.domain.model.UsageSignalType
import com.ansa1r.projectadhd.domain.model.UsageSignal

class HabitUsageCalculator {
    /** Closed foreground intervals are factual; only an interactive device can have a live tail. */
    fun calculate(events: List<UsageSignal>, from: Long, now: Long, interactive: Boolean): Map<String, Long> {
        if (now <= from) return emptyMap()
        val tracker = SessionTracker()
        val totals = mutableMapOf<String, Long>()
        var boundary = from
        var foreground: String? = null
        for (event in events.sortedBy { it.timestamp }.distinct()) {
            if (event.timestamp > now) break
            val end = event.timestamp.coerceAtLeast(from)
            foreground?.let { pkg -> if (end > boundary && event.type != UsageSignalType.STARTUP) totals[pkg] = (totals[pkg] ?: 0) + end - boundary }
            tracker.accept(event)
            foreground = tracker.snapshot(event.timestamp)?.packageName
            boundary = end
        }
        if (interactive) foreground?.let { pkg -> totals[pkg] = (totals[pkg] ?: 0) + (now - boundary).coerceAtLeast(0) }
        return totals
    }
}
