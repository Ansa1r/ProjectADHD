package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep
import com.ansa1r.projectadhd.ui.onboarding.CoachPosition
import com.ansa1r.projectadhd.ui.onboarding.coachPositionFor
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingCoachPositionTest {
    @Test fun welcomeUsesTop() {
        assertEquals(CoachPosition.TOP, coachPositionFor(OnboardingStep.WELCOME))
    }

    @Test fun remainingCoachStepsKeepBottom() {
        for (step in listOf(OnboardingStep.HOME, OnboardingStep.HABITS, OnboardingStep.APPS,
            OnboardingStep.STATS, OnboardingStep.MASCOT, OnboardingStep.SETUP_APPS,
            OnboardingStep.SETUP_HABIT, OnboardingStep.SETUP_PERMISSIONS, OnboardingStep.FINAL)) {
            assertEquals(step.name, CoachPosition.BOTTOM, coachPositionFor(step))
        }
    }
}
