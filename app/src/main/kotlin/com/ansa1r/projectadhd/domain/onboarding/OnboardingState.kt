package com.ansa1r.projectadhd.domain.onboarding

enum class OnboardingStep {
    WELCOME, HOME, HABITS, APPS, STATS, MASCOT,
    SETUP_APPS, SELECT_APPS, SETUP_HABIT, CREATE_HABIT,
    SETUP_PERMISSIONS, GRANT_PERMISSIONS, FINAL, COMPLETED;
    val isEditor get() = this == SELECT_APPS || this == CREATE_HABIT || this == GRANT_PERMISSIONS
}

data class SetupRequirements(val appsSaved: Boolean = false, val habitSaved: Boolean = false, val permissionsGranted: Boolean = false)

data class OnboardingState(
    val onboardingCompleted: Boolean = false,
    val onboardingStep: OnboardingStep = OnboardingStep.WELCOME,
    val monitoringEnabled: Boolean = false
) {
    val shouldEnsureMonitoring get() = onboardingCompleted && monitoringEnabled
    fun manualMonitoring(enabled: Boolean) = if (onboardingCompleted) copy(monitoringEnabled = enabled) else this

    /** Expected-step compare prevents double taps and stale Save callbacks from skipping a step. */
    fun advance(expected: OnboardingStep, requirements: SetupRequirements): OnboardingState {
        if (onboardingCompleted || onboardingStep != expected) return this
        val next = when (expected) {
            OnboardingStep.WELCOME -> OnboardingStep.HOME
            OnboardingStep.HOME -> OnboardingStep.HABITS
            OnboardingStep.HABITS -> OnboardingStep.APPS
            OnboardingStep.APPS -> OnboardingStep.STATS
            OnboardingStep.STATS -> OnboardingStep.MASCOT
            OnboardingStep.MASCOT -> OnboardingStep.SETUP_APPS
            OnboardingStep.SETUP_APPS, OnboardingStep.SELECT_APPS -> if (requirements.appsSaved) OnboardingStep.SETUP_HABIT else return this
            OnboardingStep.SETUP_HABIT, OnboardingStep.CREATE_HABIT -> if (requirements.habitSaved) OnboardingStep.SETUP_PERMISSIONS else return this
            OnboardingStep.SETUP_PERMISSIONS, OnboardingStep.GRANT_PERMISSIONS -> if (requirements.permissionsGranted) OnboardingStep.FINAL else return this
            OnboardingStep.FINAL -> when {
                !requirements.appsSaved -> OnboardingStep.SETUP_APPS
                !requirements.habitSaved -> OnboardingStep.SETUP_HABIT
                !requirements.permissionsGranted -> OnboardingStep.SETUP_PERMISSIONS
                else -> return OnboardingState(true, OnboardingStep.COMPLETED, true)
            }
            OnboardingStep.COMPLETED -> return this
        }
        return copy(onboardingStep = next)
    }

    fun beginSetup(expected: OnboardingStep): OnboardingState {
        if (onboardingCompleted || onboardingStep != expected) return this
        val next = when (expected) {
            OnboardingStep.SETUP_APPS -> OnboardingStep.SELECT_APPS
            OnboardingStep.SETUP_HABIT -> OnboardingStep.CREATE_HABIT
            OnboardingStep.SETUP_PERMISSIONS -> OnboardingStep.GRANT_PERMISSIONS
            else -> return this
        }
        return copy(onboardingStep = next)
    }

    fun leaveEditor(): OnboardingState = copy(onboardingStep = when (onboardingStep) {
        OnboardingStep.SELECT_APPS -> OnboardingStep.SETUP_APPS
        OnboardingStep.CREATE_HABIT -> OnboardingStep.SETUP_HABIT
        OnboardingStep.GRANT_PERMISSIONS -> OnboardingStep.SETUP_PERMISSIONS
        else -> onboardingStep
    })
}
