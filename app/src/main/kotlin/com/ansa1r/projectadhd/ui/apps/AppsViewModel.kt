package com.ansa1r.projectadhd.ui.apps

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.apps.AppSelectionDraft
import com.ansa1r.projectadhd.domain.model.InstalledApp
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppRow(val installed: InstalledApp, val selected: Boolean, val available: Boolean)
data class AppsUiState(
    val installed: List<InstalledApp> = emptyList(),
    val draft: AppSelectionDraft? = null,
    val query: String = "",
    val loading: Boolean = true,
    val iconsRevision: Int = 0,
    val commonLimit: String = "15",
    val edited: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false
) {
    val rows: List<AppRow> get() {
        val known = installed.map { it.packageName }.toSet()
        val savedApps = draft?.persisted.orEmpty() + draft?.selected?.values.orEmpty().map { it.app }
        val all = (installed + savedApps.filter { it.packageName !in known }
            .map { InstalledApp(it.packageName, it.displayName) }).distinctBy { it.packageName }
        return all.filter { it.displayName.contains(query, true) }.sortedBy { it.displayName.lowercase() }
            .map { AppRow(it, it.packageName in draft?.selected.orEmpty(), it.packageName in known) }
    }
}

class AppsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(AppsUiState())
    val state = mutable.asStateFlow()
    val icons = container.appIcons
    init {
        execute {
            container.trackedApps.observeAll().collect { apps ->
                val current = apps.filterNot { app -> container.excludedApps.contains(app.packageName) }
                mutable.update { state ->
                    state.copy(draft = if (state.edited && state.draft != null)
                        state.draft.copy(persisted = current) else AppSelectionDraft.from(current))
                }
            }
        }
        refresh()
    }
    fun refresh() {
        execute {
            mutable.update { it.copy(loading = true) }
            try {
                val apps = container.installedApps.read()
                mutable.update { it.copy(installed = apps, iconsRevision = it.iconsRevision + 1) }
            } finally { mutable.update { it.copy(loading = false) } }
        }
    }
    fun search(query: String) { mutable.update { it.copy(query = query) } }
    fun select(row: AppRow, checked: Boolean) {
        if (mutable.value.saving || mutable.value.saved || (!row.available && checked) || container.excludedApps.contains(row.installed.packageName)) return
        mutable.update { it.copy(draft = it.draft?.select(row.installed, checked), edited = true) }
    }
    fun limitChanged(packageName: String, value: String) {
        if (!mutable.value.saving && value.length <= 3 && value.all(Char::isDigit)) {
            mutable.update { it.copy(draft = it.draft?.limit(packageName, value), edited = true) }
        }
    }
    fun commonLimitChanged(value: String) {
        if (!mutable.value.saving && value.length <= 3 && value.all(Char::isDigit)) mutable.update { it.copy(commonLimit = value) }
    }
    fun applyToAll() {
        if (!mutable.value.saving) mutable.update { it.copy(draft = it.draft?.applyToAll(it.commonLimit), edited = true) }
    }
    fun save() {
        val snapshot = mutable.value
        val draft = snapshot.draft ?: return
        if (snapshot.saving || snapshot.saved || !draft.valid) return
        val apps = draft.savedApps()
        mutable.update { it.copy(saving = true) }
        // An explicit Save completes even if the user immediately navigates away.
        container.applicationScope.launch {
            try {
                container.saveTrackedSelection(apps)
                mutable.update { it.copy(draft = AppSelectionDraft.from(apps), edited = false, saving = false, saved = true) }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { mutable.update { it.copy(saving = false) }; inform(R.string.apps_save_error) }
        }
    }
}
