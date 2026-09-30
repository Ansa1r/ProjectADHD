package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.SessionTracker
import com.ansa1r.projectadhd.domain.model.AppSession
import com.ansa1r.projectadhd.domain.model.UsageSignal
import com.ansa1r.projectadhd.domain.model.UsageSignalType.*
import org.junit.Assert.*
import org.junit.Test

class SessionTrackerTest {
    private val tracker = SessionTracker()
    private fun resume(at: Long, pkg: String = "one", activity: String = "A") =
        tracker.accept(UsageSignal(at, RESUMED, pkg, activity))

    @Test fun emptyTrackerHasNoSession() { assertNull(tracker.snapshot(1_000)) }
    @Test fun sessionStartsAtAndroidEventTime() {
        resume(1_000)
        assertEquals(AppSession("one", 1_000, 4_000), tracker.snapshot(5_000))
    }
    @Test fun duplicateResumePreservesStart() {
        resume(1_000); resume(2_000)
        assertEquals(1_000L, tracker.snapshot(5_000)?.startedAt)
    }
    @Test fun switchingAppStartsNewSession() {
        resume(1_000); resume(4_000, "two")
        assertEquals(AppSession("two", 4_000, 1_000), tracker.snapshot(5_000))
    }
    @Test fun returningToAnAppStartsAgain() {
        resume(1_000); resume(2_000, "two"); resume(4_000)
        assertEquals(4_000L, tracker.snapshot(5_000)?.startedAt)
    }
    @Test fun pauseHidesSessionImmediately() {
        resume(1_000)
        tracker.accept(UsageSignal(2_000, PAUSED, "one", "A"))
        assertNull(tracker.snapshot(2_001))
    }
    @Test fun internalActivityTransitionKeepsContinuousSession() {
        resume(1_000)
        tracker.accept(UsageSignal(2_000, PAUSED, "one", "A"))
        resume(2_100, activity = "B")
        tracker.accept(UsageSignal(2_200, STOPPED, "one", "A"))
        assertEquals(AppSession("one", 1_000, 2_000), tracker.snapshot(3_000))
    }
    @Test fun resumeBeforeOldPauseDoesNotEndNewActivity() {
        resume(1_000); resume(2_000, activity = "B")
        tracker.accept(UsageSignal(2_100, PAUSED, "one", "A"))
        assertEquals("one", tracker.snapshot(3_000)?.packageName)
    }
    @Test fun lateStopOfSameActivityClassDoesNotEndNewInstance() {
        resume(1_000)
        tracker.accept(UsageSignal(2_000, PAUSED, "one", "A"))
        resume(2_100)
        tracker.accept(UsageSignal(2_200, STOPPED, "one", "A"))
        assertEquals(AppSession("one", 1_000, 2_000), tracker.snapshot(3_000))
    }
    @Test fun sameAppAfterLongPauseStartsNewSession() {
        resume(1_000)
        tracker.accept(UsageSignal(2_000, PAUSED, "one", "A"))
        resume(10_000)
        assertEquals(AppSession("one", 10_000, 1_000), tracker.snapshot(11_000))
    }
    @Test fun screenOffEndsSession() {
        resume(1_000)
        tracker.accept(UsageSignal(2_000, SCREEN_OFF))
        assertNull(tracker.snapshot(6_000))
        resume(7_000)
        assertEquals(7_000L, tracker.snapshot(8_000)?.startedAt)
    }
    @Test fun lockEndsSession() {
        resume(1_000)
        tracker.accept(UsageSignal(2_000, LOCKED))
        assertNull(tracker.snapshot(6_000))
    }
    @Test fun shutdownEndsSession() {
        resume(1_000)
        tracker.accept(UsageSignal(2_000, SHUTDOWN))
        assertNull(tracker.snapshot(6_000))
    }
    @Test fun latePauseFromPreviousAppDoesNotEndCurrentOne() {
        resume(1_000); resume(2_000, "two")
        tracker.accept(UsageSignal(2_100, PAUSED, "one", "A"))
        assertEquals("two", tracker.snapshot(3_000)?.packageName)
    }
    @Test fun outOfOrderOldEventIsIgnored() {
        resume(5_000, "two")
        resume(1_000, "one")
        assertEquals("two", tracker.snapshot(6_000)?.packageName)
    }
    @Test fun resetDropsSessionAndEventCursor() {
        resume(5_000)
        tracker.reset()
        assertNull(tracker.snapshot(6_000))
        resume(1_000, "two")
        assertEquals("two", tracker.snapshot(2_000)?.packageName)
    }
    @Test fun wallClockBeforeSessionDoesNotProduceNegativeDuration() {
        resume(5_000)
        assertNull(tracker.snapshot(4_000))
    }
}
