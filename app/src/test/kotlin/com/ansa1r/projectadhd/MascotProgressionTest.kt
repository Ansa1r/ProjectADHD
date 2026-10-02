package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.mascot.*
import org.junit.Assert.*
import org.junit.Test

class MascotProgressionTest {
    @Test fun requirementsArePerLevel() {
        mapOf(1 to 15L, 2 to 30L, 3 to 45L, 10 to 150L, 11 to 170L, 12 to 190L, 20 to 350L, 21 to 375L)
            .forEach { (level, xp) -> assertEquals(xp, MascotProgression.requiredXp(level)) }
    }
    @Test fun exactBoundaryAndOverflow() {
        assertEquals(MascotXp(2, 0, 15), MascotProgression.advance(MascotXp(1, 14, 14), 1))
        assertEquals(MascotXp(3, 3, 48), MascotProgression.advance(MascotXp(2, 28, 43), 5))
    }
    @Test fun multipleLevelsInOneReward() {
        assertEquals(MascotXp(11, 5, 830), MascotProgression.advance(MascotXp(), 830))
    }
    @Test fun localProgressDoesNotUseLifetimeAsDirectThreshold() {
        assertEquals(MascotXp(3, 3, 1005), MascotProgression.advance(MascotXp(2, 28, 1000), 5))
    }
    @Test fun oldLifetimeIsRedistributedWithoutResetOrRewardReplay() {
        mapOf(0L to MascotXp(), 15L to MascotXp(2, 0, 15), 45L to MascotXp(3, 0, 45),
            48L to MascotXp(3, 3, 48), 150L to MascotXp(5, 0, 150), 825L to MascotXp(11, 0, 825))
            .forEach { (xp, expected) -> assertEquals(expected, MascotProgression.fromLifetime(xp)) }
    }
    @Test fun conversionMatchesSequentialRequirementSubtractionAtManyBoundaries() {
        var earned = 0L
        for (level in 1..500) {
            earned += MascotProgression.requiredXp(level)
            assertEquals(level, MascotProgression.fromLifetime(earned - 1).currentLevel)
            assertEquals(MascotXp(level + 1, 0, earned), MascotProgression.fromLifetime(earned))
        }
    }
    @Test fun largeRewardDoesNotOverflowOrIterateThroughMillionsOfLevels() {
        val p = MascotProgression.advance(MascotXp(), Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, p.lifetimeXp)
        assertTrue(p.currentLevelXp >= 0 && p.currentLevelXp < MascotProgression.requiredXp(p.currentLevel))
        assertEquals(Long.MAX_VALUE, MascotProgression.add(Long.MAX_VALUE - 3, 5))
    }
    @Test fun multiplierUsesLevelBeforeEachSeparateReward() {
        val old = MascotXp(10, 145, 820)
        val habit = MascotProgression.reward(5, old.currentLevel)
        val next = MascotProgression.advance(old, habit)
        assertEquals(5L, habit); assertEquals(MascotXp(11, 0, 825), next)
        assertEquals(8L, MascotProgression.reward(5, next.currentLevel))
        assertEquals(15L, MascotProgression.reward(10, next.currentLevel))
    }
    @Test fun habitAndStreakRewardsRoundUp() {
        mapOf(1 to 5L, 10 to 5L, 11 to 8L, 21 to 12L, 31 to 17L).forEach { (level, xp) -> assertEquals(xp, MascotProgression.reward(5, level)) }
        mapOf(1 to 10L, 10 to 10L, 11 to 15L, 21 to 23L, 31 to 34L).forEach { (level, xp) -> assertEquals(xp, MascotProgression.reward(10, level)) }
    }
    @Test fun multiplierBlocks() {
        assertEquals(1.0, MascotProgression.multiplier(10), 0.0)
        assertEquals(1.5, MascotProgression.multiplier(11), 0.0)
        assertEquals(2.25, MascotProgression.multiplier(21), 0.0)
        assertEquals(3.375, MascotProgression.multiplier(31), 0.0)
    }
    @Test fun unfinishedAndEmptyDaysCannotEarnStreak() {
        assertFalse(DailyStreak.qualifies(3, 2, false)); assertFalse(DailyStreak.qualifies(0, 0, false))
    }
    @Test fun lastHabitQualifiesOnlyOncePerDay() {
        assertTrue(DailyStreak.qualifies(3, 3, false)); assertFalse(DailyStreak.qualifies(3, 3, true))
    }
    @Test fun missedDayResetsSeriesAndEmptyDayIsNeutral() {
        assertEquals(0, DailyStreak.close(8, 3, 2)); assertEquals(8, DailyStreak.close(8, 0, 0)); assertEquals(8, DailyStreak.close(8, 3, 3))
    }
}
