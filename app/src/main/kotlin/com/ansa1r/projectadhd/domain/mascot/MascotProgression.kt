package com.ansa1r.projectadhd.domain.mascot

import java.math.BigInteger
import kotlin.math.ceil
import kotlin.math.pow

data class MascotXp(val currentLevel: Int = 1, val currentLevelXp: Long = 0, val lifetimeXp: Long = 0)

object MascotProgression {
    /** XP to earn WITHIN this level, not a lifetime threshold. */
    fun requiredXp(level: Int): Long {
        require(level >= 1)
        val blocks = level.toLong() / 10
        val tail = level.toLong() % 10
        return 150 * blocks + 25 * blocks * (blocks - 1) + tail * (15 + 5 * blocks)
    }

    private fun big(value: Long) = BigInteger.valueOf(value)

    /** Sum of requirements for completed levels; used only for conversion / fast carry. */
    private fun spent(completedLevels: Int): BigInteger {
        val q = big(completedLevels.toLong() / 10)
        val r = big(completedLevels.toLong() % 10)
        return big(250) * q * (q - BigInteger.ONE) * (big(2) * q - BigInteger.ONE) / big(6) +
            big(1525) * q * (q - BigInteger.ONE) / big(2) + big(825) * q +
            r * (big(25) * q * q + big(125) * q) + (big(15) + big(5) * q) * r * (r + BigInteger.ONE) / big(2)
    }

    private fun resolve(earned: BigInteger, lifetime: Long): MascotXp {
        var low = 1
        var high = Int.MAX_VALUE
        while (low < high) {
            val mid = low + (high - low) / 2
            if (earned >= spent(mid)) low = mid + 1 else high = mid
        }
        val remainder = (earned - spent(low - 1)).min(big(Long.MAX_VALUE)).toLong()
        return MascotXp(low, remainder, lifetime)
    }

    /** Deterministic migration: keep every earned XP, redistribute it under the new costs. */
    fun fromLifetime(lifetimeXp: Long): MascotXp {
        require(lifetimeXp >= 0)
        return resolve(big(lifetimeXp), lifetimeXp)
    }

    /** Carry starts from stored level + local XP. Lifetime is only a statistics counter. */
    fun advance(state: MascotXp, amount: Long): MascotXp {
        require(state.currentLevel >= 1 && state.currentLevelXp >= 0 && state.lifetimeXp >= 0 && amount >= 0)
        return resolve(spent(state.currentLevel - 1) + big(state.currentLevelXp) + big(amount), add(state.lifetimeXp, amount))
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
