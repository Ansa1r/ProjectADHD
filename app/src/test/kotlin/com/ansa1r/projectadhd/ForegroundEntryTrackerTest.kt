package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.startup.ForegroundEntryTracker
import org.junit.Assert.*
import org.junit.Test

class ForegroundEntryTrackerTest {
    @Test fun coldStartIsAnEntry() { assertEquals(1L, ForegroundEntryTracker().onStart(10)) }
    private fun returning(after: Long): Long? {
        val tracker = ForegroundEntryTracker(); tracker.onStart(10); tracker.onStop(false, 20, 110)
        return tracker.onStart(20 + after)
    }
    @Test fun thirtySecondsSkips() { assertNull(returning(30_000)) }
    @Test fun nineMinutesFiftyNineSecondsSkips() { assertNull(returning(599_000)) }
    @Test fun tenMinutesShows() { assertEquals(2L, returning(600_000)) }
    @Test fun fifteenMinutesShows() { assertEquals(2L, returning(900_000)) }
    @Test fun internalNavigationNeverProducesAnotherEntry() {
        val tracker = ForegroundEntryTracker(); tracker.onStart(0)
        repeat(5) { assertNull(tracker.onStart(900_000)) }
    }
    @Test fun configurationChangeDoesNotStartBackgroundClock() {
        val tracker = ForegroundEntryTracker(); tracker.onStart(0)
        tracker.onStop(true, 10, 110); assertNull(tracker.lastUiBackgroundAt)
        assertNull(tracker.onStart(900_000))
    }
    @Test fun latestBackgroundTimestampIsUsedAndWallClockChangesAreIgnored() {
        val tracker = ForegroundEntryTracker(); tracker.onStart(0)
        tracker.onStop(false, 10, 110); assertNull(tracker.onStart(20))
        tracker.onStop(false, 30, 99_000_010)
        assertEquals(99_000_010L, tracker.lastUiBackgroundAt)
        assertNull(tracker.onStart(600_020))
    }
    @Test fun finishedActivityIsColdEvenIfServiceKeepsProcessAlive() {
        val tracker = ForegroundEntryTracker(); tracker.onStart(0)
        tracker.onStop(false, 10, 110, finishing = true)
        assertEquals(2L, tracker.onStart(20))
    }
    @Test fun destroyedUiIsColdWithoutRelyingOnServiceProcessLifetime() {
        val tracker = ForegroundEntryTracker(); tracker.onStart(0); tracker.onStop(false, 10, 110)
        tracker.onUiDestroyed(false)
        assertEquals(2L, tracker.onStart(20))
    }
    @Test fun processDeathCreatesFreshColdDecision() {
        val old = ForegroundEntryTracker(); old.onStart(0); old.onStop(false, 10, 110)
        assertEquals(1L, ForegroundEntryTracker().onStart(20))
    }
}
