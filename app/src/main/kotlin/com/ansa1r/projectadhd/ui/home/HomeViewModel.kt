package com.ansa1r.projectadhd.ui.home

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.AppSettings
import com.ansa1r.projectadhd.monitoring.MonitoringSnapshot
import com.ansa1r.projectadhd.monitoring.PermissionState
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val loading: Boolean = true,
    val permissions: PermissionState = PermissionState(),
    val monitoring: MonitoringSnapshot = MonitoringSnapshot(),
    val incompleteHabits: Int = 0,
    val lastEventAt: Long? = null,
    val settings: AppSettings = AppSettings()
)

class HomeViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(HomeUiState())
    val state = mutable.asStateFlow()
    init {
        execute {
            combine(container.habits.observeToday(), container.interventions.observeRecent(),
                container.preferences.settings, container.monitoring.state) { habits, events, settings, monitor ->
                HomeUiState(false, mutable.value.permissions, monitor,
                    habits.count { it.isActive && !it.completedToday }, events.firstOrNull()?.occurredAt, settings)
            }.collect { data -> mutable.update { data.copy(permissions = it.permissions) } }
        }
        refresh()
    }
    fun refresh() { mutable.update { it.copy(permissions = container.permissions.state()) } }
    fun start() { refresh(); container.controller.start() }
    fun stop() { container.controller.stop() }
    fun usageSettings() { if (!container.permissions.openUsageSettings()) inform(R.string.error_settings) }
    fun notificationSettings() { if (!container.permissions.openNotificationSettings()) inform(R.string.error_settings) }
}
