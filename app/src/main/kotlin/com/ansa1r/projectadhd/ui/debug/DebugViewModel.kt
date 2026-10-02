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
    val onboarding: com.ansa1r.projectadhd.domain.onboarding.OnboardingState = com.ansa1r.projectadhd.domain.onboarding.OnboardingState(),
    val lastUiBackgroundAt: Long? = null, val timeSinceBackground: Long? = null,
    val shouldShowStartupAnimation: Boolean = false, val serviceRunning: Boolean = false,
    val mascot: com.ansa1r.projectadhd.data.local.entity.MascotEntity = com.ansa1r.projectadhd.data.local.entity.MascotEntity(),
    val habitDetails: List<Habit> = emptyList(),
    val awards: List<com.ansa1r.projectadhd.data.local.entity.XpAwardEntity> = emptyList(),
    val habitRuntimeError: String? = null,
    val conflicts: Set<String> = emptySet(),
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
        execute { container.preferences.onboarding.collect { session -> mutable.update { it.copy(onboarding = session) } } }
        execute { container.preferences.lastUiBackgroundAt.collect { at -> mutable.update { it.copy(lastUiBackgroundAt = at) } } }
        execute { while (true) { refresh(); kotlinx.coroutines.delay(1_000) } }
        execute { container.habits.mascot.observe().collect { mascot -> mutable.update { it.copy(mascot = mascot) } } }
        execute { container.habits.mascot.history().collect { awards -> mutable.update { it.copy(awards = awards) } } }
        execute { combine(container.habits.linkedPackages, container.trackedApps.observeAll()) { linked, apps ->
            linked.intersect(apps.filter { it.enabled }.map { it.packageName }.toSet())
        }.collect { conflicts -> mutable.update { it.copy(conflicts = conflicts) } } }
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
            mutable.update { it.copy(tasks = DailyTaskSummary(active.size, active.count { it.completedToday }), habitDetails = habits, habitRuntimeError = container.habitRuntime.lastError) }
        } }
        refresh()
    }
    fun refresh() { mutable.update { it.copy(permissions = container.permissions.state(),
        timeSinceBackground = container.uiEntries.timeSinceBackground(android.os.SystemClock.elapsedRealtime()),
        shouldShowStartupAnimation = container.uiEntries.shouldShowStartupAnimation, serviceRunning = container.controller.serviceRunning) } }
    fun grantXp(amount: Long) { execute { container.habits.mascot.grantDebugXp(amount) } }
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
    fun stop() { execute { container.controller.stop() } }
    fun clearHistory() { execute { container.interventions.clear(); inform(R.string.history_cleared) } }
}
