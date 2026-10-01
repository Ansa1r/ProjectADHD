package com.ansa1r.projectadhd.domain

import com.ansa1r.projectadhd.domain.model.UsageSignal

class DailyUsageCalculator {
    fun calculate(events: List<UsageSignal>, dayStart: Long, now: Long): Map<String, Long> {
        if (now <= dayStart) return emptyMap()
        val tracker = SessionTracker()
        val totals = mutableMapOf<String, Long>()
        var previousTime = dayStart
        var foreground: String? = null
        for (event in events.sortedBy { it.timestamp }) {
            if (event.timestamp > now) break
            val boundary = event.timestamp.coerceAtLeast(dayStart)
            val pkg = foreground
            if (pkg != null && boundary > previousTime && event.type != com.ansa1r.projectadhd.domain.model.UsageSignalType.STARTUP) {
                totals[pkg] = (totals[pkg] ?: 0) + boundary - previousTime
            }
            tracker.accept(event)
            foreground = tracker.snapshot(event.timestamp)?.packageName
            previousTime = boundary
        }
        foreground?.let { pkg ->
            totals[pkg] = (totals[pkg] ?: 0) + (now - previousTime).coerceAtLeast(0)
        }
        return totals
    }
}
