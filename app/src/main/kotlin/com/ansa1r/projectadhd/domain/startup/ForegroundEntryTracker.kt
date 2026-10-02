package com.ansa1r.projectadhd.domain.startup

/** UI lifetime only; the service never marks the UI as started. Elapsed time ignores wall-clock changes. */
class ForegroundEntryTracker {
    companion object { const val BACKGROUND_TIMEOUT = 10 * 60_000L }
    private var visible = false
    private var cold = true
    private var sequence = 0L
    private var backgroundElapsed: Long? = null
    var lastUiBackgroundAt: Long? = null
        private set
    var shouldShowStartupAnimation = true
        private set
    fun timeSinceBackground(nowElapsed: Long): Long? = backgroundElapsed?.let { (nowElapsed - it).coerceAtLeast(0) }
    fun onStart(nowElapsed: Long): Long? {
        if (visible) return null
        visible = true
        shouldShowStartupAnimation = cold || (timeSinceBackground(nowElapsed) ?: 0) >= BACKGROUND_TIMEOUT
        cold = false
        return if (shouldShowStartupAnimation) ++sequence else null
    }
    fun onUiDestroyed(changingConfigurations: Boolean) {
        if (!changingConfigurations) { cold = true; visible = false }
    }
    fun onStop(changingConfigurations: Boolean, nowElapsed: Long, nowWall: Long, finishing: Boolean = false) {
        if (changingConfigurations || !visible) return
        visible = false
        backgroundElapsed = nowElapsed
        lastUiBackgroundAt = nowWall
        if (finishing) cold = true
    }
}
