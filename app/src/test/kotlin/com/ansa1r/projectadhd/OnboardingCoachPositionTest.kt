package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.dialogue.OnboardingDialogues
import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep
import org.junit.Assert.*
import org.junit.Test

class OnboardingCoachPositionTest {
    @Test fun everyCoachStepHasItsOwnInLayoutDialogue() {
        for (step in OnboardingStep.entries.filter { !it.isEditor && it != OnboardingStep.COMPLETED }) {
            val line = OnboardingDialogues.lineFor(step)
            assertNotNull(step.name, line)
            assertEquals(step, line?.onboardingStep)
        }
    }

    @Test fun editorsAndCompletedDoNotReserveCoachSpace() {
        for (step in OnboardingStep.entries.filter { it.isEditor || it == OnboardingStep.COMPLETED }) {
            assertNull(step.name, OnboardingDialogues.lineFor(step))
        }
    }
}
