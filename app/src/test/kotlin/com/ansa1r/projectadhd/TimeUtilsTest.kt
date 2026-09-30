package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.util.dayKey
import com.ansa1r.projectadhd.util.dayStart
import com.ansa1r.projectadhd.util.durationText
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {
    @Test fun durationFormatsWithoutWrappingHoursAtTwentyFour() {
        assertEquals("25:01:02", durationText(90_062_000))
        assertEquals("00:00:00", durationText(-1))
    }
    @Test fun dateUsesChosenLocalTimezone() {
        val utc = TimeZone.getTimeZone("UTC")
        val at = Calendar.getInstance(utc).apply {
            clear(); set(2026, Calendar.SEPTEMBER, 29, 22, 0, 0)
        }.timeInMillis
        assertEquals("2026-09-29", dayKey(at, utc))
        assertEquals("2026-09-30", dayKey(at, TimeZone.getTimeZone("Europe/Moscow")))
    }
    @Test fun midnightRespectsDaylightSavingTransition() {
        val zone = TimeZone.getTimeZone("Europe/Berlin")
        val noon = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.MARCH, 29, 12, 0, 0) }.timeInMillis
        assertEquals(11 * 3_600_000L, noon - dayStart(noon, zone))
        assertEquals("2026-03-29", dayKey(dayStart(noon, zone), zone))
    }
}
