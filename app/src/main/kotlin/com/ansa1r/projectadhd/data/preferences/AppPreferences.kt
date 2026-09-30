package com.ansa1r.projectadhd.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ansa1r.projectadhd.domain.model.AppSettings
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppPreferences(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val cooldown = intPreferencesKey("cooldown_minutes")
    private val lastIntervention = longPreferencesKey("last_intervention_at")
    private val lastStarted = longPreferencesKey("last_monitoring_started_at")

    val settings = store.data.map { prefs ->
        AppSettings(
            cooldownMinutes = (prefs[cooldown] ?: 30).coerceIn(1, 180),
            lastInterventionAt = prefs[lastIntervention],
            lastMonitoringStartedAt = prefs[lastStarted]
        )
    }

    suspend fun setCooldown(minutes: Int) {
        require(minutes in 1..180)
        store.edit { it[cooldown] = minutes }
    }
    suspend fun markIntervention(now: Long) { store.edit { it[lastIntervention] = now } }
    suspend fun markMonitoringStarted(now: Long) { store.edit { it[lastStarted] = now } }
}
