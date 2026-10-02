package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.onboarding.*
import org.junit.Assert.*
import org.junit.Test

class OnboardingStateTest {
    private val ready = SetupRequirements(true, true, true)
    @Test fun firstRunCannotEnableMonitoring() {
        val s = OnboardingState()
        assertEquals(OnboardingStep.WELCOME, s.onboardingStep)
        assertFalse(s.shouldEnsureMonitoring); assertEquals(s, s.manualMonitoring(true))
    }
    @Test fun greetingPrecedesRealScreenTourAndSetup() {
        var s = OnboardingState()
        for (next in listOf(OnboardingStep.HOME, OnboardingStep.HABITS, OnboardingStep.APPS,
            OnboardingStep.STATS, OnboardingStep.MASCOT, OnboardingStep.SETUP_APPS)) {
            s = s.advance(s.onboardingStep, ready)
            assertEquals(next, s.onboardingStep); assertFalse(s.monitoringEnabled)
        }
    }
    @Test fun appsRequireSavedSelection() {
        val s = OnboardingState(onboardingStep = OnboardingStep.SELECT_APPS)
        assertEquals(s, s.advance(s.onboardingStep, SetupRequirements(false, true, true)))
        assertEquals(OnboardingStep.SETUP_HABIT, s.advance(s.onboardingStep, ready).onboardingStep)
    }
    @Test fun habitMustExist() {
        val s = OnboardingState(onboardingStep = OnboardingStep.CREATE_HABIT)
        assertEquals(s, s.advance(s.onboardingStep, SetupRequirements(true, false, true)))
        assertEquals(OnboardingStep.SETUP_PERMISSIONS, s.advance(s.onboardingStep, ready).onboardingStep)
    }
    @Test fun openingSystemSettingsIsNotGrantingPermissions() {
        val s = OnboardingState(onboardingStep = OnboardingStep.GRANT_PERMISSIONS)
        assertEquals(s, s.advance(s.onboardingStep, SetupRequirements(true, true, false)))
        val final = s.advance(s.onboardingStep, ready)
        assertEquals(OnboardingStep.FINAL, final.onboardingStep); assertFalse(final.monitoringEnabled)
    }
    @Test fun onlyFinalButtonEnablesMonitoringAndCompletedNeverRestarts() {
        val final = OnboardingState(onboardingStep = OnboardingStep.FINAL)
        val done = final.advance(OnboardingStep.FINAL, ready)
        assertTrue(done.onboardingCompleted); assertTrue(done.shouldEnsureMonitoring)
        assertEquals(OnboardingStep.COMPLETED, done.onboardingStep)
        for (step in OnboardingStep.entries) assertEquals(done, done.advance(step, ready))
    }
    @Test fun finalRechecksAllPrerequisites() {
        val s = OnboardingState(onboardingStep = OnboardingStep.FINAL)
        assertEquals(OnboardingStep.SETUP_APPS, s.advance(s.onboardingStep, SetupRequirements(false, true, true)).onboardingStep)
        assertEquals(OnboardingStep.SETUP_HABIT, s.advance(s.onboardingStep, SetupRequirements(true, false, true)).onboardingStep)
        assertEquals(OnboardingStep.SETUP_PERMISSIONS, s.advance(s.onboardingStep, SetupRequirements(true, true, false)).onboardingStep)
    }
    @Test fun doubleTapAndOldSaveCannotSkipSteps() {
        val s = OnboardingState().advance(OnboardingStep.WELCOME, ready)
        assertEquals(s, s.advance(OnboardingStep.WELCOME, ready))
        assertEquals(s, s.advance(OnboardingStep.SELECT_APPS, ready))
    }
    @Test fun backFromEditorsReturnsToMatchingCoachWithoutCompleting() {
        for (step in listOf(OnboardingStep.SETUP_APPS, OnboardingStep.SETUP_HABIT, OnboardingStep.SETUP_PERMISSIONS)) {
            val s = OnboardingState(onboardingStep = step)
            val editor = s.beginSetup(step)
            assertTrue(editor.onboardingStep.isEditor); assertEquals(s, editor.leaveEditor())
            assertFalse(editor.monitoringEnabled)
        }
    }
    @Test fun manualStopSurvivesForegroundAndExplicitStartRestoresPreference() {
        val done = OnboardingState(true, OnboardingStep.COMPLETED, true)
        val stopped = done.manualMonitoring(false)
        assertFalse(stopped.shouldEnsureMonitoring)
        assertEquals(stopped, stopped.advance(OnboardingStep.FINAL, ready))
        assertTrue(stopped.manualMonitoring(true).shouldEnsureMonitoring)
    }
}
