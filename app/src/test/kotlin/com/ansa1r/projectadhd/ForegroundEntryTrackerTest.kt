package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.startup.ForegroundEntryTracker
import org.junit.Assert.*
import org.junit.Test

class ForegroundEntryTrackerTest {
    @Test fun coldStartIsAnEntry() { assertEquals(1L, ForegroundEntryTracker().onStart()) }
    @Test fun everyBackgroundReturnGetsANewEntryInTheSameProcess() {
        val tracker = ForegroundEntryTracker()
        assertEquals(1L, tracker.onStart())
        tracker.onStop(false)
        assertEquals(2L, tracker.onStart())
        tracker.onStop(false)
        assertEquals(3L, tracker.onStart())
    }
    @Test fun navigationOrRepeatedStartDoesNotProduceAnEntry() {
        val tracker = ForegroundEntryTracker()
        tracker.onStart()
        repeat(5) { assertNull(tracker.onStart()) }
    }
    @Test fun configurationRecreationDoesNotReplayButLaterBackgroundDoes() {
        val tracker = ForegroundEntryTracker()
        tracker.onStart()
        tracker.onStop(true)
        assertNull(tracker.onStart())
        tracker.onStop(false)
        assertEquals(2L, tracker.onStart())
    }
}
