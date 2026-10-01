package com.ansa1r.projectadhd.domain.startup

/** Driven only by the single UI Activity, never by the monitoring service. */
class ForegroundEntryTracker {
    private var visible = false
    private var sequence = 0L
    fun onStart(): Long? {
        if (visible) return null
        visible = true
        return ++sequence
    }
    fun onStop(changingConfigurations: Boolean) {
        if (!changingConfigurations) visible = false
    }
}
