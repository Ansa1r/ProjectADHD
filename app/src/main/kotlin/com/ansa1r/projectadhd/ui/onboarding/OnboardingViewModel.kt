package com.ansa1r.projectadhd.ui.onboarding

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.onboarding.*
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OnboardingViewModel(private val container: AppContainer) : AppViewModel() {
    private val commands = Mutex()
    private val mutable = MutableStateFlow<OnboardingState?>(null)
    val state = mutable.asStateFlow()
    private val working = MutableStateFlow(false)
    val busy = working.asStateFlow()
    private val requirementsMutable = MutableStateFlow(SetupRequirements())
    val requirements = requirementsMutable.asStateFlow()
    init {
        reload()
        execute {
            combine(container.trackedApps.observeAll(), container.habits.observeToday()) { apps, habits ->
                SetupRequirements(apps.any { it.enabled && it.sessionLimitMinutes > 0 && !container.excludedApps.contains(it.packageName) }, habits.any { it.isActive })
            }.collect { requirementsMutable.value = it }
        }
    }
    fun reload() { dismissMessage(); execute { container.preferences.onboarding.collect { mutable.value = it } } }
    private fun commit(block: suspend () -> Unit) {
        container.applicationScope.launch { commands.withLock {
            working.value = true
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { inform(R.string.error_operation) }
            finally { working.value = false }
        } }
    }
    fun advance(expected: OnboardingStep) = commit {
        container.preferences.advanceOnboarding(expected, container.setupRequirements())
        container.controller.ensureIfEnabled()
    }
    fun begin(expected: OnboardingStep) = commit { container.preferences.beginSetup(expected) }
    fun backFromEditor() = commit { container.preferences.leaveOnboardingEditor() }
    fun refreshPermissions() {
        if (mutable.value?.onboardingStep == OnboardingStep.GRANT_PERMISSIONS && container.permissions.state().onboardingReady)
            advance(OnboardingStep.GRANT_PERMISSIONS)
    }
}
