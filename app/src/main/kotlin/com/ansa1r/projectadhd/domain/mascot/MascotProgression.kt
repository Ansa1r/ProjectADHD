package com.ansa1r.projectadhd.domain.mascot

import kotlin.math.ceil
import kotlin.math.pow

object MascotProgression {
    /** Total XP needed to leave this level. Ten-level blocks have steps 15, 20, 25… */
    fun threshold(level: Int): Long {
        require(level >= 1)
        val blocks = level.toLong() / 10
        val tail = level.toLong() % 10
        return 150 * blocks + 25 * blocks * (blocks - 1) + tail * (15 + 5 * blocks)
    }
    fun level(totalXp: Long): Int {
        require(totalXp >= 0)
        var low = 1
        var high = Int.MAX_VALUE // Every threshold representable by the Int level fits into Long
        while (low < high) {
            val mid = low + (high - low) / 2
            if (totalXp >= threshold(mid)) low = mid + 1 else high = mid
        }
        return low
    }
    fun multiplier(level: Int): Double { require(level >= 1); return 1.5.pow((level - 1) / 10) }
    fun reward(base: Int, levelBeforeReward: Int): Long {
        require(base > 0)
        return ceil(base * multiplier(levelBeforeReward)).toLong()
    }
    fun add(total: Long, amount: Long): Long = if (amount > Long.MAX_VALUE - total) Long.MAX_VALUE else total + amount
}

object DailyStreak {
    fun qualifies(active: Int, completed: Int, alreadyRewarded: Boolean) =
        active > 0 && completed == active && !alreadyRewarded
    /** Empty days are neutral. A day with an unfinished active habit breaks the series. */
    fun close(streak: Int, active: Int, completed: Int) = if (active > completed) 0 else streak
}
