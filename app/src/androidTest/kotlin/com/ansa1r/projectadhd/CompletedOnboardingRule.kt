package com.ansa1r.projectadhd

import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.domain.onboarding.SetupRequirements
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** Test fixture only: old navigation regressions run after onboarding, with monitoring OFF. */
class CompletedOnboardingRule : ExternalResource() {
    override fun before() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = (context.applicationContext as ProjectADHDApplication).container.preferences
        while (!prefs.onboarding.first().onboardingCompleted) {
            val old = prefs.onboarding.first()
            prefs.advanceOnboarding(old.onboardingStep, SetupRequirements(true, true, true))
        }
        prefs.setMonitoringEnabled(false)
    }
}
