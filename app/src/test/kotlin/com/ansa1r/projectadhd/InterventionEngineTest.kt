package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.intervention.*
import com.ansa1r.projectadhd.domain.model.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionEngineTest {
    private val engine = InterventionEngine()
    private val app = TrackedApp("video.app", "Видео", 15)
    private val input = InterventionInput("video.app", app, 900_000, 5_000_000, null)

    @Test fun untrackedAppDoesNotNotify() {
        assertEquals(InterventionDecision.None(NoInterventionReason.NOT_TRACKED), engine.decide(input.copy(trackedApp = null)))
    }
    @Test fun disabledAppDoesNotNotify() {
        assertEquals(InterventionDecision.None(NoInterventionReason.NOT_TRACKED), engine.decide(input.copy(trackedApp = app.copy(enabled = false))))
    }
    @Test fun mismatchedPackageDoesNotNotify() {
        assertEquals(InterventionDecision.None(NoInterventionReason.NOT_TRACKED), engine.decide(input.copy(foregroundPackage = "other.app")))
    }
    @Test fun absentForegroundDoesNotNotify() {
        assertEquals(InterventionDecision.None(NoInterventionReason.NO_FOREGROUND), engine.decide(input.copy(foregroundPackage = null)))
    }
    @Test fun belowLimitDoesNotNotify() {
        assertEquals(InterventionDecision.None(NoInterventionReason.BELOW_LIMIT), engine.decide(input.copy(sessionDurationMillis = 899_999)))
    }
    @Test fun exactLimitNotifiesWithRealValues() {
        assertEquals(InterventionDecision.Notify("video.app", "Видео", 900_000, 900_000, 0), engine.decide(input))
    }
    @Test fun exceededLimitPreservesDuration() {
        val result = engine.decide(input.copy(sessionDurationMillis = 1_200_000)) as InterventionDecision.Notify
        assertEquals(1_200_000, result.sessionDurationMillis)
    }
    @Test fun activeCooldownPreventsNotification() {
        assertEquals(InterventionDecision.None(NoInterventionReason.COOLDOWN),
            engine.decide(input.copy(lastInterventionMillis = 3_200_001)))
    }
    @Test fun cooldownBoundaryAllowsNotification() {
        assertEquals(engine.decide(input), engine.decide(input.copy(lastInterventionMillis = 3_200_000)))
    }
    @Test fun expiredCooldownAllowsNotification() {
        assertEquals(engine.decide(input), engine.decide(input.copy(lastInterventionMillis = 3_000_000)))
    }
    @Test fun noHabitsStillAllowsIntervention() {
        assertEquals(0, (engine.decide(input.copy(incompleteHabitCount = 0)) as InterventionDecision.Notify).incompleteHabitCount)
    }
    @Test fun incompleteCountIsPassedThrough() {
        assertEquals(4, (engine.decide(input.copy(incompleteHabitCount = 4)) as InterventionDecision.Notify).incompleteHabitCount)
    }
    @Test fun negativeCountIsClamped() {
        assertEquals(0, (engine.decide(input.copy(incompleteHabitCount = -2)) as InterventionDecision.Notify).incompleteHabitCount)
    }
    @Test fun clockMovingBackCannotBypassCooldown() {
        assertEquals(InterventionDecision.None(NoInterventionReason.COOLDOWN),
            engine.decide(input.copy(lastInterventionMillis = 6_000_000)))
    }
    @Test fun negativeDurationDoesNotNotify() {
        assertEquals(InterventionDecision.None(NoInterventionReason.BELOW_LIMIT), engine.decide(input.copy(sessionDurationMillis = -1)))
    }
    @Test fun persistedGlobalTimestampAlsoThrottlesAnotherApp() {
        val other = input.copy(foregroundPackage = "other.app", trackedApp = app.copy(packageName = "other.app"),
            lastInterventionMillis = input.nowMillis - 1)
        assertEquals(InterventionDecision.None(NoInterventionReason.COOLDOWN), engine.decide(other))
    }
    @Test(expected = IllegalArgumentException::class) fun zeroLimitIsRejected() {
        app.copy(sessionLimitMinutes = 0)
    }
    @Test(expected = IllegalArgumentException::class) fun limitOverMaximumIsRejected() {
        app.copy(sessionLimitMinutes = 181)
    }
}
