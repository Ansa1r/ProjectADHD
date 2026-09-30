package com.ansa1r.projectadhd.ui.stats

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.InterventionEvent
import com.ansa1r.projectadhd.domain.model.TrackedApp
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

data class StatsUiState(
    val apps: List<TrackedApp> = emptyList(),
    val events: List<InterventionEvent> = emptyList(),
    val usage: Map<String, Long> = emptyMap(),
    val usageAvailable: Boolean = false,
    val refreshing: Boolean = false,
    val updatedAt: Long? = null
)

class StatsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(StatsUiState())
    val state = mutable.asStateFlow()
    init {
        execute { combine(container.trackedApps.observeAll(), container.interventions.observeRecent()) { apps, events ->
            apps to events
        }.collect { (apps, events) -> mutable.update { it.copy(apps = apps, events = events) } } }
    }
    fun refresh() {
        if (mutable.value.refreshing) return
        execute {
            mutable.update { it.copy(refreshing = true) }
            try {
                if (!container.permissions.hasUsageAccess()) {
                    mutable.update { it.copy(usageAvailable = false, usage = emptyMap()) }
                    inform(R.string.usage_required)
                } else {
                    val usage = container.usage.usageToday()
                    mutable.update { it.copy(usage = usage, usageAvailable = true, updatedAt = System.currentTimeMillis()) }
                }
            } catch (error: Exception) {
                mutable.update { it.copy(usageAvailable = false, usage = emptyMap(), updatedAt = null) }
                throw error
            } finally { mutable.update { it.copy(refreshing = false) } }
        }
    }
}
