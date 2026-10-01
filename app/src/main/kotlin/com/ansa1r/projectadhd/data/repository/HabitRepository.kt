package com.ansa1r.projectadhd.data.repository

import androidx.room.withTransaction
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.*
import com.ansa1r.projectadhd.domain.habits.*
import com.ansa1r.projectadhd.domain.model.Habit
import com.ansa1r.projectadhd.monitoring.*
import com.ansa1r.projectadhd.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.math.abs

class HabitRepository(
    private val database: AppDatabase,
    private val blocks: BlockRepository,
    private val clock: HabitClock = HabitClock { HabitClockSample(System.currentTimeMillis(), android.os.SystemClock.elapsedRealtime(), -1) }
) {
    private val dao get() = database.habits()
    private val progress get() = database.progress()
    val mascot = MascotRepository(database)
    val linkedPackages = dao.observeLinkedPackages().map { it.toSet() }
    private val foreground = MutableStateFlow<String?>(null)
    fun foregroundPackage(pkg: String?) { foreground.value = pkg }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<List<Habit>> = currentDayFlow().flatMapLatest { date ->
        combine(dao.observeDay(date), progress.observeDay(date), foreground) { rows, daily, pkg ->
            val byId = daily.associateBy { it.habitId }
            rows.map { row -> with(row.habit) {
                val type = HabitType.valueOf(this.type)
                var p = byId[id]?.progress() ?: HabitProgress()
                if (row.completedToday) p = p.copy(state = HabitState.COMPLETED)
                else if (type == HabitType.APP_BASED && isActive && linkedAppPackage == pkg) p = p.copy(state = HabitState.IN_PROGRESS)
                Habit(id, title, createdAt, isActive, row.completedToday, targetDurationMinutes, type, linkedAppPackage, p, row.completedToday && byId[id] == null)
            } }
        }
    }
    suspend fun evaluateDay(date: String, at: Long) = database.withTransaction {
        mascot.evaluate(date, at, allowReward = false)
    }
    suspend fun prepareAppWindow(id: Long, now: HabitClockSample): Long? = database.withTransaction {
        val habit = dao.find(id)?.takeIf { it.isActive && it.type == "APP_BASED" } ?: return@withTransaction null
        val date = dayKey(now.wall)
        val existing = progress.find(id, date)
        val from = maxOf(dayStart(now.wall), habit.createdAt, habit.activatedAt)
        val row = existing ?: HabitDailyEntity(id, date, checkpointWall = from, bootCount = now.boot)
        val reboot = row.bootCount >= 0 && now.boot >= 0 && row.bootCount != now.boot
        val next = if (reboot) row.copy(appWindowBaseMillis = row.accumulatedMillis, bootCount = now.boot,
            checkpointWall = maxOf(from, now.wall - now.elapsed))
            else row.copy(bootCount = now.boot, checkpointWall = maxOf(from, row.checkpointWall))
        if (existing != next) progress.save(next)
        next.checkpointWall
    }
    suspend fun appWindowFrom(id: Long, date: String) = progress.find(id, date)?.checkpointWall ?: 0L
    suspend fun lastEvaluatedDay() = progress.mascot()?.evaluatedDate
    suspend fun trackingTargets() = dao.active().filter { it.type == HabitType.APP_BASED.name }
    suspend fun hasLinkedPackage(pkg: String) = dao.linkedCount(pkg) > 0
    suspend fun save(id: Long?, rawTitle: String, targetMinutes: Int = 30, linkedPackage: String? = null) = database.withTransaction {
        val title = rawTitle.trim()
        require(title.isNotEmpty() && title.length <= 120)
        val now = clock.sample()
        tickLocked(now)
        if (id == null) {
            require(targetMinutes in 1..1439)
            require(linkedPackage == null || (linkedPackage.isNotBlank() && linkedPackage != BuildConfig.APPLICATION_ID && database.trackedApps().find(linkedPackage)?.enabled != true))
            dao.insert(HabitEntity(title = title, createdAt = now.wall, targetDurationMinutes = targetMinutes,
                type = if (linkedPackage == null) "MANUAL" else "APP_BASED", linkedAppPackage = linkedPackage, activatedAt = now.wall))
        } else dao.rename(id, title) // Existing configuration is stable; editing preserves today's accumulated work.
        mascot.evaluate(dayKey(now.wall), now.wall, allowReward = false)
    }
    suspend fun setActive(id: Long, active: Boolean) = database.withTransaction {
        val now = clock.sample(); tickLocked(now)
        val habit = dao.find(id) ?: return@withTransaction
        if (habit.isActive == active) return@withTransaction
        if (active && habit.linkedAppPackage != null) require(database.trackedApps().find(habit.linkedAppPackage)?.enabled != true)
        val date = dayKey(now.wall)
        val old = daily(habit, date)
        progress.save(old.withProgress(old.progress().pause(), now.wall).copy(appWindowBaseMillis = old.accumulatedMillis))
        if (active) dao.activateAt(id, now.wall)
        dao.setActive(id, active)
        mascot.evaluate(date, now.wall) // Current active set, including an explicit archive, defines the day.
        blocks.reconcile(now.wall)
    }
    suspend fun delete(id: Long) = database.withTransaction {
        val now = clock.sample(); tickLocked(now); dao.delete(id)
        mascot.evaluate(dayKey(now.wall), now.wall); blocks.reconcile(now.wall)
    }
    suspend fun start(id: Long) = database.withTransaction {
        val now = clock.sample(); tickLocked(now)
        val habit = requireNotNull(dao.find(id)); require(habit.isActive && habit.type == "MANUAL")
        val date = dayKey(now.wall); val row = daily(habit, date)
        if (dao.dailyTasks(date).any { it.id == id && it.completedAt != null }) return@withTransaction
        // One physical activity timer at a time; another start pauses the previous timer without losing time.
        for (other in progress.running()) if (other.habitId != id) progress.save(other.withProgress(other.progress().pause(), now.wall))
        progress.save(row.withProgress(row.progress().start(now.elapsed, now.boot), now.wall))
    }
    suspend fun pause(id: Long) = database.withTransaction {
        val now = clock.sample(); tickLocked(now)
        progress.find(id, dayKey(now.wall))?.let { progress.save(it.withProgress(it.progress().pause(), now.wall)) }
    }
    suspend fun confirm(id: Long, yes: Boolean) = database.withTransaction {
        val now = clock.sample(); tickLocked(now)
        val habit = dao.find(id) ?: return@withTransaction
        if (!habit.isActive || habit.type != "MANUAL") return@withTransaction
        val date = dayKey(now.wall); val row = daily(habit, date)
        if (row.state != "AWAITING_CONFIRMATION") return@withTransaction
        val next = row.progress().confirm(yes, now.elapsed, now.boot)
        if (!yes) for (other in progress.running()) if (other.habitId != id) progress.save(other.withProgress(other.progress().pause(), now.wall))
        progress.save(row.withProgress(next, now.wall))
        if (yes) completeLocked(id, date, now.wall)
    }
    /** Retained for callers upgrading from Stage 3; it cannot bypass confirmation or undo a reward. */
    suspend fun setCompleted(id: Long, completed: Boolean) { if (completed) confirm(id, true) }
    suspend fun tick(now: HabitClockSample = clock.sample()) = database.withTransaction { tickLocked(now) }
    suspend fun checkpointManual(now: HabitClockSample = clock.sample()) = database.withTransaction { advanceManualLocked(now) }
    private suspend fun tickLocked(now: HabitClockSample) {
        advanceManualLocked(now)
        mascot.evaluate(dayKey(now.wall), now.wall, allowReward = false)
    }
    private suspend fun advanceManualLocked(now: HabitClockSample) {
        val date = dayKey(now.wall)
        for (row in progress.running()) {
            val habit = dao.find(row.habitId) ?: continue
            if (habit.type != "MANUAL") continue
            val sameDay = row.localDate == date
            // Midnight closes a manual session; a new date always starts at zero and READY.
            val midnight = dayBounds(row.checkpointWall).end
            val coherentWall = abs((now.wall - row.checkpointWall) - (now.elapsed - (row.checkpointElapsed ?: now.elapsed))) <= 5_000
            val until = if (sameDay) now.elapsed else if (coherentWall && row.localDate < date)
                minOf(now.elapsed, (row.checkpointElapsed ?: now.elapsed) + (midnight - row.checkpointWall).coerceAtLeast(0))
                else row.checkpointElapsed ?: now.elapsed
            var next = row.progress().advance(habit.targetDurationMinutes, until, now.boot)
            if (!sameDay || !habit.isActive) next = next.pause()
            val updated = row.withProgress(next, if (sameDay) now.wall else midnight)
            if (updated != row) progress.save(updated)
        }
    }
    suspend fun creditApp(id: Long, observedMillis: Long, now: HabitClockSample = clock.sample(), expectedActivatedAt: Long? = null) = database.withTransaction {
        val habit = dao.find(id) ?: return@withTransaction
        if (!habit.isActive || habit.type != "APP_BASED" || (expectedActivatedAt != null && habit.activatedAt != expectedActivatedAt)) return@withTransaction
        val date = dayKey(now.wall)
        mascot.rollTo(date)
        val row = daily(habit, date)
        if (dao.dailyTasks(date).any { it.id == id && it.completedAt != null }) return@withTransaction
        val next = row.progress().appTotal(habit.targetDurationMinutes, row.appWindowBaseMillis + observedMillis)
        if (next != row.progress()) progress.save(row.withProgress(next.copy(bootCount = now.boot), now.wall)
            .copy(checkpointWall = row.checkpointWall.takeIf { it > 0 } ?: maxOf(dayStart(now.wall), habit.createdAt, habit.activatedAt)))
        if (next.state == HabitState.COMPLETED) completeLocked(id, date, now.wall)
        else mascot.evaluate(date, now.wall, allowReward = false)
    }
    private suspend fun daily(habit: HabitEntity, date: String) = progress.find(habit.id, date) ?: HabitDailyEntity(habit.id, date)
    private suspend fun completeLocked(id: Long, date: String, now: Long) {
        if (dao.complete(HabitCompletionEntity(id, date, now)) != -1L) {
            mascot.awardHabit(id, date, now)
            blocks.onNewCompletion(id, date, now)
            mascot.evaluate(date, now)
        }
    }
    suspend fun incompleteCount(now: Long) = dao.incompleteCount(dayKey(now))
}
