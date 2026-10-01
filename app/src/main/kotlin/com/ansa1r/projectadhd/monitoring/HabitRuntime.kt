package com.ansa1r.projectadhd.monitoring

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import com.ansa1r.projectadhd.data.repository.HabitRepository
import com.ansa1r.projectadhd.domain.habits.HabitUsageCalculator
import com.ansa1r.projectadhd.domain.SessionTracker
import com.ansa1r.projectadhd.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared by the foreground service and the app. Replaying UsageEvents + monotonic MAX avoids duplicate credit. */
class HabitRuntime(private val context: Context, private val habits: HabitRepository,
    private val usage: UsageStatsReader, private val permissions: PermissionManager, private val clock: HabitClock) {
    private val gate = Mutex()
    var lastError: String? = null; private set
    suspend fun refresh() = gate.withLock {
        val now = clock.sample()
        val targets = habits.trackingTargets()
        if (targets.isEmpty() || !permissions.hasUsageAccess()) {
            habits.foregroundPackage(null); habits.tick(now); return@withLock
        }
        val today = dayKey(now.wall)
        val previous = habits.lastEvaluatedDay()?.takeIf { it < today } ?: today
        val firstStart = dateBounds(previous).start
        val events = withContext(Dispatchers.IO) { usage.events(firstStart - 86_400_000L, now.wall) }
        val active = context.getSystemService(PowerManager::class.java)?.isInteractive == true &&
            context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == false
        val tracker = SessionTracker()
        events.sortedBy { it.timestamp }.forEach(tracker::accept)
        habits.foregroundPackage(if (active) tracker.snapshot(now.wall)?.packageName else null)
        var date = previous
        while (date <= today) {
            val bounds = dateBounds(date)
            val until = if (date == today) now.wall else bounds.end
            for (habit in targets) {
                val windowStart = if (date == today) (habits.prepareAppWindow(habit.id, now) ?: continue) else habits.appWindowFrom(habit.id, date)
                val from = maxOf(bounds.start, habit.createdAt, habit.activatedAt, windowStart)
                if (from >= until) continue
                val totals = HabitUsageCalculator().calculate(events, from, until, date < today || active)
                habits.creditApp(habit.id, habit.linkedAppPackage?.let { totals[it] } ?: 0,
                    now.copy(wall = if (date == today) until else until - 1), habit.activatedAt)
            }
            // Finalize chronologically, including a genuinely empty/incomplete historical day.
            habits.evaluateDay(date, if (date == today) until else until - 1)
            date = nextDayKey(date)
        }
        habits.tick(now)
        lastError = null
    }
    fun failed(error: Exception) { lastError = error.javaClass.simpleName }
}
