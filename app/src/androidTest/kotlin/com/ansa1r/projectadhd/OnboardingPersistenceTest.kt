package com.ansa1r.projectadhd

import androidx.datastore.preferences.core.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import com.ansa1r.projectadhd.domain.onboarding.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OnboardingPersistenceTest {
    private fun file() = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "onboarding-${UUID.randomUUID()}.preferences_pb")
    private suspend fun session(file: File, block: suspend (AppPreferences) -> Unit) {
        val job = SupervisorJob()
        try { block(AppPreferences(PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file }))) }
        finally { job.cancelAndJoin() }
    }
    private suspend fun toFinal(prefs: AppPreferences) {
        val ready = SetupRequirements(true, true, true)
        while (prefs.onboarding.first().onboardingStep != OnboardingStep.FINAL) {
            prefs.advanceOnboarding(prefs.onboarding.first().onboardingStep, ready)
        }
    }
    @Test fun freshAndUpgradedPreferencesDefaultOffWithoutLosingProfile() = runBlocking {
        val f = file()
        try {
            session(f) { p -> p.setNickname("Миша"); p.setAvatar("avatar-example.jpg"); p.setMascotName("Бобби"); p.setCooldown(17) }
            session(f) { p ->
                assertEquals(OnboardingState(), p.onboarding.first())
                assertEquals("Миша", p.nickname.first()); assertEquals("avatar-example.jpg", p.avatar.first())
                assertEquals("Бобби", p.mascotName.first()); assertEquals(17, p.settings.first().cooldownMinutes)
            }
        } finally { f.delete() }
    }
    @Test fun interruptedEditorResumesWithoutGreetingAndDoesNotEnableMonitoring() = runBlocking {
        val f = file()
        try {
            session(f) { p ->
                while (p.onboarding.first().onboardingStep != OnboardingStep.SETUP_HABIT)
                    p.advanceOnboarding(p.onboarding.first().onboardingStep, SetupRequirements(true, false, false))
                p.beginSetup(OnboardingStep.SETUP_HABIT)
            }
            session(f) { p ->
                assertEquals(OnboardingStep.CREATE_HABIT, p.onboarding.first().onboardingStep)
                assertFalse(p.onboarding.first().monitoringEnabled)
                p.leaveOnboardingEditor()
                assertEquals(OnboardingStep.SETUP_HABIT, p.onboarding.first().onboardingStep)
            }
        } finally { f.delete() }
    }
    @Test fun finalIsAtomicAndStopPersistsAcrossStoreReopenUntilManualStart() = runBlocking {
        val f = file(); val ready = SetupRequirements(true, true, true)
        try {
            session(f) { p -> toFinal(p); assertFalse(p.onboarding.first().monitoringEnabled) }
            session(f) { p ->
                assertEquals(OnboardingStep.FINAL, p.onboarding.first().onboardingStep)
                p.advanceOnboarding(OnboardingStep.FINAL, ready)
                assertEquals(OnboardingState(true, OnboardingStep.COMPLETED, true), p.onboarding.first())
                p.setMonitoringEnabled(false)
            }
            session(f) { p ->
                assertEquals(OnboardingState(true, OnboardingStep.COMPLETED, false), p.onboarding.first())
                p.advanceOnboarding(OnboardingStep.FINAL, ready)
                assertFalse(p.onboarding.first().shouldEnsureMonitoring)
                p.setMonitoringEnabled(true)
                assertTrue(p.onboarding.first().shouldEnsureMonitoring)
            }
        } finally { f.delete() }
    }
}
