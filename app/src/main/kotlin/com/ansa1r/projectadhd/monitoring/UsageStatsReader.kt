package com.ansa1r.projectadhd.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ansa1r.projectadhd.domain.DailyUsageCalculator
import com.ansa1r.projectadhd.domain.model.UsageSignal
import com.ansa1r.projectadhd.domain.model.UsageSignalType
import com.ansa1r.projectadhd.util.dayStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageStatsReader(context: Context, private val permissions: PermissionManager) {
    private val manager = requireNotNull(context.getSystemService(UsageStatsManager::class.java))

    fun events(from: Long, until: Long): List<UsageSignal> {
        if (!permissions.hasUsageAccess()) throw SecurityException("Usage access unavailable")
        if (until <= from) return emptyList()
        val events = manager.queryEvents(from, until) ?: return emptyList()
        val item = UsageEvents.Event()
        val result = mutableListOf<UsageSignal>()
        while (events.hasNextEvent()) {
            events.getNextEvent(item)
            val type = when (item.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageSignalType.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageSignalType.PAUSED
                UsageEvents.Event.ACTIVITY_STOPPED -> UsageSignalType.STOPPED
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageSignalType.SCREEN_OFF
                UsageEvents.Event.KEYGUARD_SHOWN -> UsageSignalType.LOCKED
                UsageEvents.Event.DEVICE_STARTUP -> UsageSignalType.STARTUP
                UsageEvents.Event.DEVICE_SHUTDOWN -> UsageSignalType.SHUTDOWN
                else -> null
            } ?: continue
            val key = item.className ?: ""
            result.add(UsageSignal(item.timeStamp, type, item.packageName, key))
        }
        return result
    }

    suspend fun usageToday(): Map<String, Long> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val start = dayStart(now)
        DailyUsageCalculator().calculate(events(start - 86_400_000L, now), start, now)
    }
}
