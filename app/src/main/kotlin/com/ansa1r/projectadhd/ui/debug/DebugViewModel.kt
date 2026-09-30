package com.ansa1r.projectadhd.ui.debug

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.intervention.*
import com.ansa1r.projectadhd.domain.model.*
import com.ansa1r.projectadhd.monitoring.*
import com.ansa1r.projectadhd.overlay.OverlaySnapshot
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock

data class DebugUiState(
    val permissions: PermissionState = PermissionState(),
    val monitoring: MonitoringSnapshot = MonitoringSnapshot(),
    val settings: AppSettings = AppSettings(),
    val counts: RecordCounts? = null,
    val lastEventAt: Long? = null,
    val simulation: InterventionDecision? = null,
    val blocks: List<BlockSession> = emptyList(),
    val lastUnlock: Long? = null,
    val overlay: OverlaySnapshot = OverlaySnapshot(),
    val tasks: DailyTaskSummary = DailyTaskSummary()
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
        execute { container.blocks.observeActive().collect { blocks -> mutable.update { it.copy(blocks = blocks) } } }
        execute { container.blocks.observeLastUnlock().collect { at -> mutable.update { it.copy(lastUnlock = at) } } }
        execute { container.overlays.state.collect { overlay -> mutable.update { it.copy(overlay = overlay) } } }
        execute { container.habits.observeToday().collect { habits ->
            val active = habits.filter { it.isActive }
            mutable.update { it.copy(tasks = DailyTaskSummary(active.size, active.count { it.completedToday })) }
        } }
        refresh()
    }
    fun refresh() { mutable.update { it.copy(permissions = container.permissions.state()) } }
    fun testNotification() { inform(if (container.notifications.test()) R.string.test_notification_sent else R.string.notifications_required) }
    fun simulate() {
        val decision = container.engine.decide(InterventionInput(
            foregroundPackage = "debug.example", trackedApp = TrackedApp("debug.example", "Debug"),
            sessionDurationMillis = 20 * 60_000L, nowMillis = System.currentTimeMillis(),
            tasks = DailyTaskSummary(2, 0)))
        mutable.update { it.copy(simulation = decision) }
    }
    fun testBlock() = arm(MascotMood.BLOCKING)
    fun testPraise() = arm(MascotMood.PRAISE)
    private fun arm(mood: MascotMood) {
        if (container.monitoring.state.value.status != MonitorStatus.RUNNING) {
            inform(R.string.debug_test_requires_monitor); return
        }
        container.overlays.armTest(mood)
        inform(R.string.debug_test_armed)
    }
    fun clearBlocks() {
        execute {
            container.controller.gate.withLock {
                container.blocks.clear(System.currentTimeMillis(), ReleaseReason.DEBUG_CLEAR)
                container.overlays.hide()
            }
            inform(R.string.blocks_cleared)
        }
    }
    fun stop() = container.controller.stop()
    fun clearHistory() { execute { container.interventions.clear(); inform(R.string.history_cleared) } }
}
