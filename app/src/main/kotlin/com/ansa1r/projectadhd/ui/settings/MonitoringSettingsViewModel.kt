package com.ansa1r.projectadhd.ui.settings

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.onboarding.OnboardingState
import com.ansa1r.projectadhd.monitoring.MonitoringSnapshot
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MonitoringSettingsState(val loading: Boolean = true, val preference: OnboardingState = OnboardingState(),
    val monitoring: MonitoringSnapshot = MonitoringSnapshot(), val busy: Boolean = false)
class MonitoringSettingsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(MonitoringSettingsState())
    val state = mutable.asStateFlow()
    init { execute {
        combine(container.preferences.onboarding, container.monitoring.state) { preference, monitoring -> preference to monitoring }
            .collect { (preference, monitoring) -> mutable.update { it.copy(loading = false, preference = preference, monitoring = monitoring) } }
    } }
    fun setEnabled(enabled: Boolean) {
        if (mutable.value.loading || mutable.value.busy) return
        mutable.update { it.copy(busy = true) }
        container.applicationScope.launch {
            try { if (enabled) container.controller.enable() else container.controller.stop() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { inform(R.string.error_operation) }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
