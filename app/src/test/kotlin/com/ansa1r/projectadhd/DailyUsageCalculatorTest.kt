package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.DailyUsageCalculator
import com.ansa1r.projectadhd.domain.model.UsageSignal
import com.ansa1r.projectadhd.domain.model.UsageSignalType.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyUsageCalculatorTest {
    private val calculator = DailyUsageCalculator()
    private fun resume(t: Long, pkg: String = "one") = UsageSignal(t, RESUMED, pkg, "A")

    @Test fun noEventsProducesNoInventedUsage() {
        assertEquals(emptyMap<String, Long>(), calculator.calculate(emptyList(), 1_000, 10_000))
    }
    @Test fun sessionAcrossMidnightIsClipped() {
        assertEquals(mapOf("one" to 5_000L), calculator.calculate(listOf(resume(1_000)), 5_000, 10_000))
    }
    @Test fun switchesDoNotDoubleCount() {
        val result = calculator.calculate(listOf(resume(1_000), resume(4_000, "two")), 0, 10_000)
        assertEquals(mapOf("one" to 3_000L, "two" to 6_000L), result)
    }
    @Test fun screenOffTimeIsNotCounted() {
        val result = calculator.calculate(listOf(resume(1_000), UsageSignal(3_000, SCREEN_OFF), resume(8_000)), 0, 10_000)
        assertEquals(mapOf("one" to 4_000L), result)
    }
    @Test fun futureEventsAreIgnored() {
        assertEquals(mapOf("one" to 9_000L),
            calculator.calculate(listOf(resume(1_000), resume(20_000, "two")), 0, 10_000))
    }
    @Test fun pauseGapIsNotCounted() {
        val events = listOf(resume(1_000), UsageSignal(2_000, PAUSED, "one", "A"), resume(8_000))
        assertEquals(mapOf("one" to 3_000L), calculator.calculate(events, 0, 10_000))
    }
    @Test fun oldFinishedSessionDoesNotCountToday() {
        val events = listOf(resume(1_000), UsageSignal(2_000, SCREEN_OFF))
        assertEquals(emptyMap<String, Long>(), calculator.calculate(events, 5_000, 10_000))
    }
    @Test fun unsortedEventsAreOrderedBeforeCalculation() {
        assertEquals(mapOf("one" to 3_000L, "two" to 6_000L),
            calculator.calculate(listOf(resume(4_000, "two"), resume(1_000)), 0, 10_000))
    }
}
