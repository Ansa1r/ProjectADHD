package com.ansa1r.projectadhd.ui.habits

import android.content.Context
import android.content.Intent
import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.Habit
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.*

data class HabitsUiState(val habits: List<Habit> = emptyList(), val loading: Boolean = true, val usageAllowed: Boolean = false,
    val liveTracking: Boolean = false, val conflicts: Set<String> = emptySet())
class HabitsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(HabitsUiState())
    val state = mutable.asStateFlow()
    init {
        execute { container.habits.observeToday().collect { items -> mutable.update { it.copy(habits = items, loading = false) } } }
        execute { combine(container.habits.linkedPackages, container.trackedApps.observeAll()) { linked, apps ->
            linked.intersect(apps.filter { it.enabled }.map { it.packageName }.toSet())
        }.collect { conflicts -> mutable.update { it.copy(conflicts = conflicts) } } }
        execute { container.monitoring.state.collect { monitor -> mutable.update { it.copy(liveTracking = monitor.status == com.ansa1r.projectadhd.monitoring.MonitorStatus.RUNNING) } } }
    }
    fun refresh() { mutable.update { it.copy(usageAllowed = container.permissions.hasUsageAccess()) }; execute { container.habitRuntime.refresh() } }
    fun start(id: Long) { execute { container.habits.start(id) } }
    fun pause(id: Long) { execute { container.habits.pause(id) } }
    fun confirm(id: Long, yes: Boolean) { execute { container.habits.confirm(id, yes) } }
    fun active(habit: Habit, value: Boolean) { execute { container.habits.setActive(habit.id, value) } }
    fun delete(id: Long) { execute { container.habits.delete(id) } }
    fun openApp(context: Context, habit: Habit) {
        if (!container.permissions.hasUsageAccess()) { container.permissions.openUsageSettings(); return }
        val intent = habit.linkedAppPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (intent == null) { inform(R.string.habit_app_missing); return }
        container.controller.start()
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: RuntimeException) { inform(R.string.habit_app_missing) }
    }
}
