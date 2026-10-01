package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.habits.HabitUsageCalculator
import com.ansa1r.projectadhd.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class HabitUsageTest {
    private val calc = HabitUsageCalculator()
    private fun event(at: Long, type: UsageSignalType, pkg: String? = "useful") = UsageSignal(at, type, pkg, "Activity")
    @Test fun onlyTheLinkedForegroundIntervalsCountAcrossThreeSessions() {
        val events = listOf(event(0, UsageSignalType.RESUMED), event(480000, UsageSignalType.RESUMED, "chat"),
            event(600000, UsageSignalType.RESUMED), event(1320000, UsageSignalType.SCREEN_OFF, null),
            event(2000000, UsageSignalType.RESUMED), event(2600000, UsageSignalType.PAUSED))
        val total = calc.calculate(events, 0, 3600000, true)
        assertEquals(30 * 60_000L, total["useful"])
        assertEquals(120000L, total["chat"])
    }
    @Test fun screenOffLockAndShutdownCloseTime() {
        for (stop in listOf(UsageSignalType.SCREEN_OFF, UsageSignalType.LOCKED, UsageSignalType.SHUTDOWN)) {
            assertEquals(1000L, calc.calculate(listOf(event(0, UsageSignalType.RESUMED), event(1000, stop, null)), 0, 60000, false)["useful"])
        }
    }
    @Test fun inactiveDeviceDoesNotTrustAnUnclosedForegroundTail() {
        assertTrue(calc.calculate(listOf(event(0, UsageSignalType.RESUMED)), 0, 60000, false).isEmpty())
        assertEquals(60000L, calc.calculate(listOf(event(0, UsageSignalType.RESUMED)), 0, 60000, true)["useful"])
    }
    @Test fun creationAndCalendarBoundaryClipEarlierUsage() {
        val events = listOf(event(0, UsageSignalType.RESUMED), event(120000, UsageSignalType.PAUSED))
        assertEquals(60000L, calc.calculate(events, 60000, 180000, true)["useful"])
        assertEquals(30000L, calc.calculate(events, 90000, 180000, true)["useful"])
    }
    @Test fun pauseAndHomeDoNotIncreaseUsefulTime() {
        val events = listOf(event(0, UsageSignalType.RESUMED), event(1000, UsageSignalType.PAUSED),
            event(1100, UsageSignalType.RESUMED, "launcher"))
        assertEquals(1000L, calc.calculate(events, 0, 60000, true)["useful"])
    }
    @Test fun startupDiscardsUnknownPowerOffGap() {
        val events = listOf(event(0, UsageSignalType.RESUMED), event(60000, UsageSignalType.STARTUP, null),
            event(61000, UsageSignalType.RESUMED), event(62000, UsageSignalType.PAUSED))
        assertEquals(1000L, calc.calculate(events, 0, 90000, true)["useful"])
    }
    @Test fun duplicateReplayAndOutOfOrderEventsHaveSameResult() {
        val events = listOf(event(0, UsageSignalType.RESUMED), event(1000, UsageSignalType.PAUSED))
        assertEquals(calc.calculate(events, 0, 2000, true), calc.calculate((events + events).reversed(), 0, 2000, true))
    }
}
