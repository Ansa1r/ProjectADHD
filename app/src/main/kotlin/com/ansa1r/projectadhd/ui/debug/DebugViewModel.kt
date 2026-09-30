package com.ansa1r.projectadhd.ui.debug

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.intervention.InterventionDecision
import com.ansa1r.projectadhd.domain.intervention.InterventionInput
import com.ansa1r.projectadhd.domain.model.AppSettings
import com.ansa1r.projectadhd.domain.model.RecordCounts
import com.ansa1r.projectadhd.domain.model.TrackedApp
import com.ansa1r.projectadhd.monitoring.MonitoringSnapshot
import com.ansa1r.projectadhd.monitoring.PermissionState
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

data class DebugUiState(
    val permissions: PermissionState = PermissionState(),
    val monitoring: MonitoringSnapshot = MonitoringSnapshot(),
    val settings: AppSettings = AppSettings(),
    val counts: RecordCounts? = null,
    val lastEventAt: Long? = null,
    val simulation: InterventionDecision? = null
)

class DebugViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(DebugUiState())
    val state = mutable.asStateFlow()
    init {
        check(BuildConfig.DEBUG)
        execute { container.interventions.observeRecent().collect { events ->
            mutable.update { it.copy(lastEventAt = events.firstOrNull()?.occurredAt) }
        } }
        execute { combine(container.monitoring.state, container.preferences.settings,
            container.recordCounts()) { monitor, settings, counts -> Triple(monitor, settings, counts)
        }.collect { (monitor, settings, counts) ->
            mutable.update { it.copy(monitoring = monitor, settings = settings, counts = counts) }
        } }
        refresh()
    }
    fun refresh() { mutable.update { it.copy(permissions = container.permissions.state()) } }
    fun testNotification() {
        inform(if (container.notifications.test()) R.string.test_notification_sent else R.string.notifications_required)
    }
    fun simulate() {
        val decision = container.engine.decide(InterventionInput(
            foregroundPackage = "debug.example", trackedApp = TrackedApp("debug.example", "Тестовое приложение"),
            sessionDurationMillis = 20 * 60_000L, nowMillis = System.currentTimeMillis(),
            lastInterventionMillis = null, incompleteHabitCount = 2
        ))
        mutable.update { it.copy(simulation = decision) }
    }
    fun clearHistory() { execute { container.interventions.clear(); inform(R.string.history_cleared) } }
}
