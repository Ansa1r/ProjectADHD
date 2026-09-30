package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.intervention.*
import com.ansa1r.projectadhd.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class BlockCoordinatorTest {
    private val block = BlockSession("video.app", "Видео", 1_000, "2026-09-30", 900_000, 900_000, 1, setOf(2, 3))
    private fun day(vararg tasks: DailyTask) = DailyTaskSnapshot(block.localDate, tasks.toList())
    @Test fun withoutNewCompletionBlockRemains() {
        assertNull(BlockCoordinator.releaseReason(block, day(DailyTask(1, 500), DailyTask(2, null)), 2_000))
    }
    @Test fun oneNewCompletionReleases() {
        assertEquals(ReleaseReason.COMPLETION, BlockCoordinator.releaseReason(block, day(DailyTask(2, 1_001), DailyTask(3, null)), 2_000))
    }
    @Test fun oldCompletionCannotRelease() {
        assertFalse(BlockCoordinator.isNewCompletion(block, 2, block.localDate, 999))
        assertNull(BlockCoordinator.releaseReason(block, day(DailyTask(2, 999), DailyTask(3, null)), 2_000))
    }
    @Test fun equalTimestampIsNotAfterStart() { assertFalse(BlockCoordinator.isNewCompletion(block, 2, block.localDate, 1_000)) }
    @Test fun redoingHabitCompletedBeforeBlockCannotRelease() {
        assertFalse(BlockCoordinator.isNewCompletion(block, 1, block.localDate, 1_100))
        assertNull(BlockCoordinator.releaseReason(block, day(DailyTask(1, 1_100), DailyTask(2, null)), 2_000))
    }
    @Test fun newlyCreatedHabitIsNotBaselineTask() { assertFalse(BlockCoordinator.isNewCompletion(block, 4, block.localDate, 1_100)) }
    @Test fun otherDayCompletionCannotRelease() { assertFalse(BlockCoordinator.isNewCompletion(block, 2, "2026-09-29", 1_100)) }
    @Test fun alreadyReleasedSessionCannotReleaseAgain() { assertFalse(BlockCoordinator.isNewCompletion(block.copy(active = false), 2, block.localDate, 1_100)) }
    @Test fun removingAllTasksCancelsRatherThanTraps() {
        assertEquals(ReleaseReason.NO_TASKS, BlockCoordinator.releaseReason(block, day(), 2_000))
    }
    @Test fun removingEligibleTasksCancelsRatherThanCountingAsCompletion() {
        assertEquals(ReleaseReason.TASKS_CHANGED, BlockCoordinator.releaseReason(block, day(DailyTask(4, null)), 2_000))
    }
    @Test fun midnightExpiresPreviousDaysBlock() {
        assertEquals(ReleaseReason.NEW_DAY, BlockCoordinator.releaseReason(block, DailyTaskSnapshot("2026-10-01", listOf(DailyTask(2, null))), 2_000))
    }
    @Test fun clockRollbackCancelsAnImpossibleFutureBlock() {
        assertEquals(ReleaseReason.CLOCK_CHANGED, BlockCoordinator.releaseReason(block, day(DailyTask(2, null)), 999))
    }
    @Test fun releaseGrantsFullIntervalEvenIfTrackerStillContainsOldSession() {
        val released = 2_000_000L
        val raw = AppSession("video.app", 100, released - 100)
        val fresh = requireNotNull(SessionAllowance.apply(raw, released, released + 1))
        assertEquals(1L, fresh.durationMillis)
        val engine = InterventionEngine()
        val input = InterventionInput("video.app", TrackedApp("video.app", "Видео", 15), fresh.durationMillis,
            released + 1, DailyTaskSummary(3, 2), activeBlock = block.copy(active = false, releasedAt = released))
        assertEquals(InterventionDecision.None(NoInterventionReason.BELOW_LIMIT), engine.decide(input))
        assertEquals(899_999L, SessionAllowance.apply(raw, released, released + 899_999)?.durationMillis)
        assertTrue(engine.decide(input.copy(sessionDurationMillis = requireNotNull(SessionAllowance.apply(raw, released, released + 900_000)).durationMillis)) is InterventionDecision.Block)
    }
    @Test fun returningAfterReleaseStartsFromNewForegroundEvent() {
        val raw = AppSession("video.app", 4_000, 100)
        assertEquals(raw, SessionAllowance.apply(raw, 3_000, 4_100))
    }
    @Test fun missingReleaseKeepsSession() {
        val raw = AppSession("video.app", 4_000, 100)
        assertEquals(raw, SessionAllowance.apply(raw, null, 4_100))
    }
    @Test fun absentForegroundStaysAbsent() { assertNull(SessionAllowance.apply(null, 3_000, 4_100)) }
}
