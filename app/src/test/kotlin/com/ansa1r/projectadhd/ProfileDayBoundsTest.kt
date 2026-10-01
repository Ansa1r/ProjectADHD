package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.util.dayBounds
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class ProfileDayBoundsTest {
    private fun at(zone: TimeZone, month: Int, day: Int) = Calendar.getInstance(zone).apply {
        clear(); set(2026, month, day, 12, 0)
    }.timeInMillis
    @Test fun normalLocalDayHasExclusiveNextMidnight() {
        val zone = TimeZone.getTimeZone("Europe/Moscow")
        val bounds = dayBounds(at(zone, Calendar.OCTOBER, 1), zone)
        assertEquals(24L * 60 * 60 * 1000, bounds.end - bounds.start)
        assertEquals(bounds.end, dayBounds(bounds.end, zone).start)
    }
    @Test fun springDayIsNotAssumedToBeTwentyFourHours() {
        val zone = TimeZone.getTimeZone("Europe/Berlin")
        val bounds = dayBounds(at(zone, Calendar.MARCH, 29), zone)
        assertEquals(23L * 60 * 60 * 1000, bounds.end - bounds.start)
    }
    @Test fun autumnDayIsNotAssumedToBeTwentyFourHours() {
        val zone = TimeZone.getTimeZone("Europe/Berlin")
        val bounds = dayBounds(at(zone, Calendar.OCTOBER, 25), zone)
        assertEquals(25L * 60 * 60 * 1000, bounds.end - bounds.start)
    }
}
