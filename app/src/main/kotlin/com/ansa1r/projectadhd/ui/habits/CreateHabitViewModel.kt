package com.ansa1r.projectadhd.ui.habits

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.InstalledApp
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.*

/** Target/type are configured when creating; rename never rewrites ongoing daily sessions. */
data class CreateHabitState(val title: String = "", val hours: String = "00", val minutes: String = "30",
    val linked: InstalledApp? = null, val installed: List<InstalledApp> = emptyList(), val limited: Set<String> = emptySet(),
    val query: String = "", val saving: Boolean = false, val saved: Boolean = false, val loading: Boolean = true) {
    val target: Int? get() {
        val h = hours.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val m = minutes.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return (h * 60 + m).takeIf { it > 0 }
    }
    val valid get() = title.isNotBlank() && title.length <= 120 && target != null && (linked == null || linked.packageName !in limited)
}
class CreateHabitViewModel(private val container: AppContainer, val id: Long? = null) : AppViewModel() {
    private val mutable = MutableStateFlow(CreateHabitState())
    val state = mutable.asStateFlow()
    val icons = container.appIcons
    init {
        execute { container.trackedApps.observeAll().collect { apps -> mutable.update { it.copy(limited = apps.filter { a -> a.enabled }.map { a -> a.packageName }.toSet()) } } }
        execute {
            val apps = container.installedApps.read()
            val habit = id?.let { value -> container.habits.observeToday().first().find { it.id == value } }
            mutable.update { it.copy(installed = apps, loading = false,
                title = habit?.title ?: it.title,
                hours = habit?.let { h -> "%02d".format(h.targetDurationMinutes / 60) } ?: it.hours,
                minutes = habit?.let { h -> "%02d".format(h.targetDurationMinutes % 60) } ?: it.minutes,
                linked = habit?.linkedAppPackage?.let { pkg -> apps.find { a -> a.packageName == pkg } ?: InstalledApp(pkg, pkg) }) }
        }
    }
    fun title(value: String) { if (value.length <= 120) mutable.update { it.copy(title = value) } }
    fun hours(value: String) { if (value.length <= 2 && value.all(Char::isDigit)) mutable.update { it.copy(hours = value) } }
    fun minutes(value: String) { if (value.length <= 2 && value.all(Char::isDigit)) mutable.update { it.copy(minutes = value) } }
    fun search(value: String) { mutable.update { it.copy(query = value) } }
    fun link(app: InstalledApp?) { if (app == null || com.ansa1r.projectadhd.domain.habits.HabitAppConflict.canLink(app.packageName, mutable.value.limited)) mutable.update { it.copy(linked = app) } }
    fun save() {
        val current = mutable.value
        if (!current.valid || current.saving || current.loading) return
        mutable.update { it.copy(saving = true) }
        execute {
            try {
                require(current.linked == null || !container.excludedApps.contains(current.linked.packageName))
                container.habits.save(id, current.title, requireNotNull(current.target), current.linked?.packageName)
                mutable.update { it.copy(saved = true) }
            } finally { mutable.update { it.copy(saving = false) } }
        }
    }
}
