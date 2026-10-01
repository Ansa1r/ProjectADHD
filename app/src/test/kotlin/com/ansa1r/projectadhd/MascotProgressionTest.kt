package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.mascot.*
import org.junit.Assert.*
import org.junit.Test

class MascotProgressionTest {
    @Test fun exactThresholds() {
        val expected = mapOf(1 to 15L, 2 to 30L, 10 to 150L, 11 to 170L, 12 to 190L,
            20 to 350L, 21 to 375L, 30 to 600L, 31 to 630L, 40 to 900L, 100 to 3750L)
        expected.forEach { (level, xp) -> assertEquals(xp, MascotProgression.threshold(level)) }
    }
    @Test fun boundariesBelongToNextLevel() {
        assertEquals(1, MascotProgression.level(0))
        for (level in 1..500) {
            val threshold = MascotProgression.threshold(level)
            assertEquals(level, MascotProgression.level(threshold - 1))
            assertEquals(level + 1, MascotProgression.level(threshold))
        }
    }
    @Test fun incrementChangesAfterEachTenLevels() {
        for (level in 2..500) assertEquals(15L + 5L * ((level - 1) / 10),
            MascotProgression.threshold(level) - MascotProgression.threshold(level - 1))
    }
    @Test fun habitRewardsRoundUp() {
        mapOf(1 to 5L, 10 to 5L, 11 to 8L, 20 to 8L, 21 to 12L, 31 to 17L).forEach { (level, xp) ->
            assertEquals(xp, MascotProgression.reward(5, level))
        }
    }
    @Test fun streakRewardsRoundUp() {
        mapOf(1 to 10L, 11 to 15L, 21 to 23L, 31 to 34L).forEach { (level, xp) -> assertEquals(xp, MascotProgression.reward(10, level)) }
    }
    @Test fun multiplierUsesLevelBeforeEachSeparateAward() {
        var xp = 145L // Level 10
        val habitReward = MascotProgression.reward(5, MascotProgression.level(xp))
        assertEquals(5L, habitReward)
        xp += habitReward
        assertEquals(11, MascotProgression.level(xp))
        assertEquals(8L, MascotProgression.reward(5, MascotProgression.level(xp)))
        assertEquals(15L, MascotProgression.reward(10, MascotProgression.level(xp)))
    }
    @Test fun multiplierBlocks() {
        assertEquals(1.0, MascotProgression.multiplier(10), 0.0)
        assertEquals(1.5, MascotProgression.multiplier(11), 0.0)
        assertEquals(2.25, MascotProgression.multiplier(21), 0.0)
        assertEquals(3.375, MascotProgression.multiplier(31), 0.0)
    }
    @Test fun largeLevelsDoNotNeedTableOrOverflow() {
        val threshold = MascotProgression.threshold(100_000)
        assertEquals(100_001, MascotProgression.level(threshold))
        assertEquals(Long.MAX_VALUE, MascotProgression.add(Long.MAX_VALUE - 3, 5))
    }
    @Test fun unfinishedAndEmptyDaysCannotEarnStreak() {
        assertFalse(DailyStreak.qualifies(3, 2, false))
        assertFalse(DailyStreak.qualifies(0, 0, false))
    }
    @Test fun lastHabitQualifiesOnlyOncePerDay() {
        assertTrue(DailyStreak.qualifies(3, 3, false))
        assertFalse(DailyStreak.qualifies(3, 3, true))
    }
    @Test fun missedDayResetsSeriesAndEmptyDayIsNeutral() {
        assertEquals(0, DailyStreak.close(8, 3, 2))
        assertEquals(8, DailyStreak.close(8, 0, 0))
        assertEquals(8, DailyStreak.close(8, 3, 3))
    }
}
