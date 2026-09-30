package com.ansa1r.projectadhd.ui.home

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.*
import com.ansa1r.projectadhd.monitoring.MonitoringSnapshot
import com.ansa1r.projectadhd.monitoring.PermissionState
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.*

data class HomeUiState(
    val loading: Boolean = true,
    val permissions: PermissionState = PermissionState(),
    val monitoring: MonitoringSnapshot = MonitoringSnapshot(),
    val tasks: DailyTaskSummary = DailyTaskSummary(),
    val blocks: List<BlockSession> = emptyList()
)
class HomeViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(HomeUiState())
    val state = mutable.asStateFlow()
    init {
        execute {
            combine(container.habits.observeToday(), container.blocks.observeActive(), container.monitoring.state) { habits, blocks, monitor ->
                val active = habits.filter { it.isActive }
                HomeUiState(false, mutable.value.permissions, monitor,
                    DailyTaskSummary(active.size, active.count { it.completedToday }), blocks)
            }.collect { data -> mutable.update { data.copy(permissions = it.permissions) } }
        }
        refresh()
    }
    fun refresh() {
        mutable.update { it.copy(permissions = container.permissions.state()) }
        execute { container.blocks.reconcile(System.currentTimeMillis()) { !container.excludedApps.contains(it) } }
    }
    fun start() { refresh(); container.controller.start() }
    fun stop() { container.controller.stop() }
    fun usageSettings() { if (!container.permissions.openUsageSettings()) inform(R.string.error_settings) }
    fun overlaySettings() { if (!container.permissions.openOverlaySettings()) inform(R.string.error_settings) }
    fun notificationSettings() { if (!container.permissions.openNotificationSettings()) inform(R.string.error_settings) }
}
