package com.ansa1r.projectadhd.ui.apps

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.domain.model.InstalledApp
import com.ansa1r.projectadhd.domain.model.TrackedApp
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppRow(val installed: InstalledApp, val tracked: TrackedApp?, val available: Boolean)
data class AppsUiState(
    val installed: List<InstalledApp> = emptyList(),
    val tracked: List<TrackedApp> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val editingPackage: String? = null,
    val limitInput: String = "",
    val saving: Boolean = false
) {
    val rows: List<AppRow> get() {
        val known = installed.map { it.packageName }.toSet()
        val all = installed + tracked.filter { it.packageName !in known }.map { InstalledApp(it.packageName, it.displayName) }
        val selected = tracked.associateBy { it.packageName }
        return all.filter { it.displayName.contains(query, true) || it.packageName.contains(query, true) }
            .sortedBy { it.displayName.lowercase() }
            .map { AppRow(it, selected[it.packageName], it.packageName in known) }
    }
}

class AppsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(AppsUiState())
    val state = mutable.asStateFlow()
    init {
        execute { container.trackedApps.observeAll().collect { apps -> mutable.update { it.copy(tracked = apps) } } }
        refresh()
    }
    fun refresh() {
        execute {
            mutable.update { it.copy(loading = true) }
            try {
                val apps = container.installedApps.read()
                mutable.update { it.copy(installed = apps) }
            } finally { mutable.update { it.copy(loading = false) } }
        }
    }
    fun search(query: String) { mutable.update { it.copy(query = query) } }
    fun select(row: AppRow, enabled: Boolean) {
        execute {
            container.trackedApps.save(row.tracked?.copy(enabled = enabled)
                ?: TrackedApp(row.installed.packageName, row.installed.displayName, enabled = enabled))
        }
    }
    fun remove(packageName: String) { execute { container.trackedApps.delete(packageName) } }
    fun editLimit(app: TrackedApp) {
        mutable.update { it.copy(editingPackage = app.packageName, limitInput = app.sessionLimitMinutes.toString()) }
    }
    fun limitChanged(value: String) {
        if (value.length <= 3 && value.all(Char::isDigit)) mutable.update { it.copy(limitInput = value) }
    }
    fun closeEditor() { if (!mutable.value.saving) mutable.update { it.copy(editingPackage = null) } }
    fun saveLimit() {
        val snapshot = mutable.value
        val minutes = snapshot.limitInput.toIntOrNull()?.takeIf { it in 1..180 } ?: return
        val app = snapshot.tracked.find { it.packageName == snapshot.editingPackage } ?: return
        if (snapshot.saving) return
        mutable.update { it.copy(saving = true) }
        execute {
            try {
                container.trackedApps.save(app.copy(sessionLimitMinutes = minutes))
                mutable.update { it.copy(editingPackage = null) }
            } finally { mutable.update { it.copy(saving = false) } }
        }
    }
}
