package com.ansa1r.projectadhd.domain.apps

import com.ansa1r.projectadhd.domain.model.InstalledApp
import com.ansa1r.projectadhd.domain.model.TrackedApp

data class AppLimitDraft(val app: TrackedApp, val input: String = app.sessionLimitMinutes.toString()) {
    val minutes: Int? get() = input.toIntOrNull()?.takeIf { it in 1..180 }
}

data class AppSelectionDraft(
    val persisted: List<TrackedApp>,
    val selected: Map<String, AppLimitDraft>
) {
    companion object {
        fun from(apps: List<TrackedApp>) = AppSelectionDraft(apps.toList(),
            apps.filter { it.enabled }.associate { it.packageName to AppLimitDraft(it) })
    }
    val selectionChanged: Boolean get() = selected.keys != persisted.filter { it.enabled }.map { it.packageName }.toSet() ||
        persisted.any { !it.enabled }
    val valid: Boolean get() = selected.values.all { it.minutes != null }
    fun select(app: InstalledApp, checked: Boolean): AppSelectionDraft {
        if (!checked) return copy(selected = selected - app.packageName)
        if (app.packageName in selected) return this
        val tracked = persisted.find { it.packageName == app.packageName }?.copy(displayName = app.displayName, enabled = true)
            ?: TrackedApp(app.packageName, app.displayName)
        return copy(selected = selected + (app.packageName to AppLimitDraft(tracked)))
    }
    fun limit(packageName: String, value: String): AppSelectionDraft {
        val row = selected[packageName] ?: return this
        return copy(selected = selected + (packageName to row.copy(input = value)))
    }
    fun applyToAll(value: String): AppSelectionDraft {
        val minutes = value.toIntOrNull()?.takeIf { it in 1..180 } ?: return this
        return copy(selected = selected.mapValues { (_, row) -> row.copy(input = minutes.toString()) })
    }
    fun savedApps(): List<TrackedApp> {
        require(valid)
        return selected.values.map { it.app.copy(sessionLimitMinutes = requireNotNull(it.minutes), enabled = true) }
            .sortedBy { it.packageName }
    }
}

fun showAppSelectionCta(trackedCount: Int): Boolean = trackedCount == 0
