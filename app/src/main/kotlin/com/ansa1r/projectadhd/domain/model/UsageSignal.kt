package com.ansa1r.projectadhd.domain.model

enum class UsageSignalType { RESUMED, PAUSED, STOPPED, SCREEN_OFF, LOCKED, SHUTDOWN }

data class UsageSignal(
    val timestamp: Long,
    val type: UsageSignalType,
    val packageName: String? = null,
    val activityKey: String = ""
)

data class AppSession(
    val packageName: String,
    val startedAt: Long,
    val durationMillis: Long
)
