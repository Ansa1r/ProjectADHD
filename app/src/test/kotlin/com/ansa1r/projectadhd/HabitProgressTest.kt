package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.habits.*
import org.junit.Assert.*
import org.junit.Test

class HabitProgressTest {
    @Test fun startPauseResumeAcrossMultipleSessions() {
        var p = HabitProgress().start(1000, 1)
        assertEquals(HabitState.IN_PROGRESS, p.state)
        p = p.advance(30, 481000, 1).pause()
        assertEquals(480000L, p.accumulatedMillis)
        assertEquals(p, p.advance(30, 4_081_000, 1))
        p = p.start(4_081_000, 1).advance(30, 4_801_000, 1).pause()
        assertEquals(20 * 60_000L, p.accumulatedMillis)
        p = p.start(5_000_000, 1).advance(30, 5_600_000, 1)
        assertEquals(30 * 60_000L, p.accumulatedMillis)
        assertEquals(HabitState.AWAITING_CONFIRMATION, p.state)
    }
    @Test fun screenOffAndBackgroundTimeCountUntilTargetOnly() {
        val p = HabitProgress().start(0, 7).advance(30, 120 * 60_000L, 7)
        assertEquals(30 * 60_000L, p.accumulatedMillis)
        assertEquals(HabitState.AWAITING_CONFIRMATION, p.state)
        assertNull(p.checkpointElapsed)
    }
    @Test fun noPreservesProgressAndAddsTenMinutesRepeatedly() {
        var p = HabitProgress().start(0, 1).advance(30, 1_800_000, 1)
        p = p.confirm(false, 1_800_000, 1)
        assertEquals(30 * 60_000L, p.accumulatedMillis)
        assertEquals(40 * 60_000L, p.targetMillis(30))
        p = p.advance(30, 2_400_000, 1).confirm(false, 2_400_000, 1)
        assertEquals(40 * 60_000L, p.accumulatedMillis)
        assertEquals(50 * 60_000L, p.targetMillis(30))
    }
    @Test fun earlyOrRepeatedYesCannotChangeState() {
        val ready = HabitProgress()
        assertEquals(ready, ready.confirm(true, 0, 1))
        val done = ready.start(0, 1).advance(1, 60_000, 1).confirm(true, 60_000, 1)
        assertEquals(HabitState.COMPLETED, done.state)
        assertEquals(done, done.confirm(true, 90_000, 1))
        assertEquals(done, done.confirm(false, 90_000, 1))
        assertEquals(done, done.start(90_000, 1))
    }
    @Test fun rebootPausesAtLastReliableCheckpointEvenIfNewUptimeIsLonger() {
        val p = HabitProgress().start(0, 1).advance(30, 60_000, 1)
        val rebooted = p.advance(30, 1_000_000, 2)
        assertEquals(60_000L, rebooted.accumulatedMillis)
        assertEquals(HabitState.PAUSED, rebooted.state)
        assertNull(rebooted.checkpointElapsed)
    }
    @Test fun monotonicClockRegressionAndUnknownBootAreConservative() {
        val p = HabitProgress().start(100, 1)
        assertEquals(HabitState.PAUSED, p.advance(1, 90, 1).state)
        assertEquals(HabitState.PAUSED, p.advance(1, 300, -1).state)
    }
    @Test fun newDayStartsWithoutPreviousExtrasOrCompletion() {
        val yesterday = HabitProgress().start(0, 1).advance(1, 60_000, 1).confirm(false, 60_000, 1)
        assertEquals(11 * 60_000L, yesterday.targetMillis(1))
        val today = HabitProgress()
        assertEquals(60_000L, today.targetMillis(1))
        assertEquals(0L, today.accumulatedMillis)
        assertEquals(HabitState.READY, today.state)
    }
    @Test fun appProgressUsesCumulativeMaximumAndAutomaticallyCompletes() {
        var p = HabitProgress().appTotal(30, 8 * 60_000L).appTotal(30, 20 * 60_000L)
        assertEquals(20 * 60_000L, p.accumulatedMillis)
        p = p.appTotal(30, 18 * 60_000L)
        assertEquals(20 * 60_000L, p.accumulatedMillis)
        p = p.appTotal(30, 35 * 60_000L)
        assertEquals(30 * 60_000L, p.accumulatedMillis)
        assertEquals(HabitState.COMPLETED, p.state)
        assertEquals(p, p.appTotal(30, 45 * 60_000L))
    }
    @Test fun bothConflictDirectionsAndUnrelatedApps() {
        assertFalse(HabitAppConflict.canLink("limited", setOf("limited")))
        assertFalse(HabitAppConflict.canLimit("useful", setOf("useful")))
        assertTrue(HabitAppConflict.canLink("other", setOf("limited")))
        assertTrue(HabitAppConflict.canLimit("other", setOf("useful")))
    }
}
