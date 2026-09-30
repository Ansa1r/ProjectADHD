package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.intervention.*
import com.ansa1r.projectadhd.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class InterventionEngineTest {
    private val engine = InterventionEngine()
    private val app = TrackedApp("video.app", "Видео", 15)
    private val input = InterventionInput("video.app", app, 900_000, 5_000_000, DailyTaskSummary(3, 1))
    private fun none(reason: NoInterventionReason, value: InterventionInput) =
        assertEquals(InterventionDecision.None(reason), engine.decide(value))

    @Test fun untrackedAppDoesNothing() = none(NoInterventionReason.NOT_TRACKED, input.copy(trackedApp = null))
    @Test fun disabledAppDoesNothing() = none(NoInterventionReason.NOT_TRACKED, input.copy(trackedApp = app.copy(enabled = false)))
    @Test fun mismatchedPackageDoesNothing() = none(NoInterventionReason.NOT_TRACKED, input.copy(foregroundPackage = "other.app"))
    @Test fun noForegroundDoesNothing() = none(NoInterventionReason.NO_FOREGROUND, input.copy(foregroundPackage = null))
    @Test fun belowLimitDoesNothing() = none(NoInterventionReason.BELOW_LIMIT, input.copy(sessionDurationMillis = 899_999))
    @Test fun exactLimitBlocksAndPreservesPayload() {
        assertEquals(InterventionDecision.Block(InterventionPayload("video.app", "Видео", 900_000, 900_000, 2)), engine.decide(input))
    }
    @Test fun exceededLimitBlocks() { assertTrue(engine.decide(input.copy(sessionDurationMillis = 1_200_000)) is InterventionDecision.Block) }
    @Test fun allTasksCompletePraises() { assertTrue(engine.decide(input.copy(tasks = DailyTaskSummary(3, 3))) is InterventionDecision.Praise) }
    @Test fun zeroTasksNeverBlocks() = none(NoInterventionReason.NO_TASKS, input.copy(tasks = DailyTaskSummary()))
    @Test fun praiseCooldownSuppressesPraise() = none(NoInterventionReason.COOLDOWN,
        input.copy(tasks = DailyTaskSummary(1, 1), lastPraiseMillis = 3_200_001))
    @Test fun praiseCooldownBoundaryAllowsPraise() {
        assertTrue(engine.decide(input.copy(tasks = DailyTaskSummary(1, 1), lastPraiseMillis = 3_200_000)) is InterventionDecision.Praise)
    }
    @Test fun expiredPraiseCooldownAllowsPraise() {
        assertTrue(engine.decide(input.copy(tasks = DailyTaskSummary(1, 1), lastPraiseMillis = 3_000_000)) is InterventionDecision.Praise)
    }
    @Test fun praiseCooldownNeverDelaysBlocking() {
        assertTrue(engine.decide(input.copy(lastPraiseMillis = input.nowMillis - 1)) is InterventionDecision.Block)
    }
    @Test fun clockRollbackDoesNotBypassPraiseCooldown() = none(NoInterventionReason.COOLDOWN,
        input.copy(tasks = DailyTaskSummary(1, 1), lastPraiseMillis = input.nowMillis + 1))
    @Test fun monitoringOffDoesNothing() = none(NoInterventionReason.MONITORING_OFF, input.copy(monitoringEnabled = false))
    @Test fun protectedSystemPackagesDoNothingEvenIfTracked() = none(NoInterventionReason.EXCLUDED, input.copy(excluded = true))
    @Test fun negativeDurationDoesNotBlock() = none(NoInterventionReason.BELOW_LIMIT, input.copy(sessionDurationMillis = -1))
    @Test fun activeBlockReturnsImmediatelyInFreshSession() {
        val block = BlockSession(app.packageName, app.displayName, 100, "2026-09-30", 900_000, 900_000, 1, setOf(2, 3))
        assertTrue(engine.decide(input.copy(activeBlock = block, sessionDurationMillis = 1)) is InterventionDecision.Block)
        none(NoInterventionReason.BELOW_LIMIT, input.copy(activeBlock = block.copy(active = false), sessionDurationMillis = 1))
    }
    @Test fun blockForAnotherPackageDoesNotBlockCurrentApp() {
        val block = BlockSession("other", "Other", 100, "2026-09-30", 900_000, 900_000, 1, setOf(2))
        none(NoInterventionReason.BELOW_LIMIT, input.copy(activeBlock = block, sessionDurationMillis = 1))
    }
    @Test(expected = IllegalArgumentException::class) fun zeroLimitRejected() { app.copy(sessionLimitMinutes = 0) }
    @Test(expected = IllegalArgumentException::class) fun excessiveLimitRejected() { app.copy(sessionLimitMinutes = 181) }
}
