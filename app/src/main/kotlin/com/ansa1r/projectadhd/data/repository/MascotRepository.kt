package com.ansa1r.projectadhd.data.repository

import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.*
import com.ansa1r.projectadhd.domain.mascot.*
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.util.*

/** All mutation methods are called inside HabitRepository's Room transaction. */
class MascotRepository(private val database: AppDatabase) {
    private val dao get() = database.progress()
    fun observe() = dao.observeMascot().map { it ?: MascotEntity() }
    fun history() = dao.observeAwards()
    private suspend fun current() = dao.mascot() ?: MascotEntity(completedHabits = dao.completionCount()).also { dao.saveMascot(it) }

    suspend fun grantDebugXp(amount: Long) {
        check(BuildConfig.DEBUG)
        require(amount in 1..1_000)
        database.withTransaction {
            val old = current()
            val next = MascotProgression.advance(MascotXp(old.currentLevel, old.currentLevelXp, old.lifetimeXp), amount)
            val at = System.currentTimeMillis()
            dao.award(XpAwardEntity("debug:" + java.util.UUID.randomUUID(), "DEBUG", null, dayKey(at), amount.toInt(), amount, old.currentLevel, at))
            dao.saveMascot(old.copy(currentLevel = next.currentLevel, currentLevelXp = next.currentLevelXp, lifetimeXp = next.lifetimeXp))
        }
    }

    suspend fun rollTo(date: String) {
        var mascot = current()
        val previous = mascot.evaluatedDate
        if (previous != null && previous < date) {
            val day = dao.day(previous)
            var streak = DailyStreak.close(mascot.streak, day?.activeCount ?: 0, day?.completedCount ?: 0)
            // Intervening unobserved dates cannot earn XP; active unfinished days break the series.
            if (nextDayKey(previous) < date && (day?.activeCount ?: 0) > 0) streak = 0
            mascot = mascot.copy(streak = streak)
        }
        if (previous != date) dao.saveMascot(mascot.copy(evaluatedDate = date))
    }

    suspend fun awardHabit(id: Long, date: String, at: Long) {
        award("habit:$id:$date", "HABIT", id, date, 5, at)
    }
    private suspend fun award(key: String, kind: String, id: Long?, date: String, base: Int, at: Long): Boolean {
        val mascot = current()
        val level = mascot.currentLevel
        val amount = MascotProgression.reward(base, level)
        if (dao.award(XpAwardEntity(key, kind, id, date, base, amount, level, at)) == -1L) return false
        val next = MascotProgression.advance(MascotXp(mascot.currentLevel, mascot.currentLevelXp, mascot.lifetimeXp), amount)
        dao.saveMascot(mascot.copy(lifetimeXp = next.lifetimeXp, currentLevel = next.currentLevel, currentLevelXp = next.currentLevelXp,
            completedHabits = mascot.completedHabits + if (kind == "HABIT") 1 else 0))
        return true
    }
    suspend fun evaluate(date: String, at: Long, allowReward: Boolean = true) {
        rollTo(date)
        val tasks = database.habits().dailyTasksBefore(date, dateBounds(date).end)
        val old = dao.day(date)
        val done = tasks.count { it.completedAt != null }
        val qualifies = allowReward && DailyStreak.qualifies(tasks.size, done, old?.streakAwarded == true)
        var rewarded = old?.streakAwarded == true
        if (qualifies && award("streak:$date", "STREAK", null, date, 10, at)) {
            val mascot = current() // Streak multiplier uses the level AFTER the last habit reward.
            dao.saveMascot(mascot.copy(streak = mascot.streak + 1, lastStreakRewardDate = date))
            rewarded = true
        }
        val day = HabitDayEntity(date, tasks.size, done, rewarded)
        if (day != old) dao.saveDay(day)
    }
}
