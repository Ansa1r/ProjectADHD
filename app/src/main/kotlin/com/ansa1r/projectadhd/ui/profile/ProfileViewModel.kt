package com.ansa1r.projectadhd.ui.profile

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.domain.model.DailyTaskSummary
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.*

data class ProfileState(
    val nickname: String? = null,
    val tasks: DailyTaskSummary = DailyTaskSummary(),
    val trackedCount: Int = 0,
    val interventions: Int = 0,
    val loading: Boolean = true
)

class ProfileViewModel(container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(ProfileState())
    val state = mutable.asStateFlow()
    init {
        execute {
            combine(container.preferences.nickname, container.habits.observeToday(),
                container.trackedApps.observeAll(), container.interventions.observeTodayCount()) { nickname, habits, apps, count ->
                val active = habits.filter { it.isActive }
                ProfileState(nickname, DailyTaskSummary(active.size, active.count { it.completedToday }),
                    apps.count { it.enabled }, count, false)
            }.collect { mutable.value = it }
        }
    }
}
