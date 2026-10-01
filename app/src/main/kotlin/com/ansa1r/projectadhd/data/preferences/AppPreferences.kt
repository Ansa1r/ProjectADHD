package com.ansa1r.projectadhd.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ansa1r.projectadhd.domain.settings.BlockingOpacity
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ansa1r.projectadhd.domain.model.AppSettings
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppPreferences(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)
    private val blockingOpacity = intPreferencesKey("blocking_overlay_opacity_percent")
    private val cooldown = intPreferencesKey("cooldown_minutes")
    private val lastIntervention = longPreferencesKey("last_intervention_at")
    private val praiseCooldown = intPreferencesKey("praise_cooldown_minutes")
    private val lastPraise = longPreferencesKey("last_praise_at")
    private val lastStarted = longPreferencesKey("last_monitoring_started_at")

    val settings = store.data.map { prefs ->
        AppSettings(
            cooldownMinutes = (prefs[cooldown] ?: 30).coerceIn(1, 180),
            lastInterventionAt = prefs[lastIntervention],
            lastMonitoringStartedAt = prefs[lastStarted],
            praiseCooldownMinutes = (prefs[praiseCooldown] ?: 30).coerceIn(1, 180),
            lastPraiseAt = prefs[lastPraise],
            blockingOverlayOpacityPercent = BlockingOpacity.normalize(prefs[blockingOpacity] ?: BlockingOpacity.DEFAULT_PERCENT)
        )
    }

    val blockingOverlayOpacity = settings.map { it.blockingOverlayOpacityPercent }.distinctUntilChanged()

    suspend fun setBlockingOpacity(percent: Int) {
        store.edit { it[blockingOpacity] = BlockingOpacity.normalize(percent) }
    }

    suspend fun setCooldown(minutes: Int) {
        require(minutes in 1..180)
        store.edit { it[cooldown] = minutes }
    }
    suspend fun setPraiseCooldown(minutes: Int) {
        require(minutes in 1..180)
        store.edit { it[praiseCooldown] = minutes }
    }
    suspend fun markPraise(now: Long) { store.edit { it[lastPraise] = now } }
    suspend fun markIntervention(now: Long) { store.edit { it[lastIntervention] = now } }
    suspend fun markMonitoringStarted(now: Long) { store.edit { it[lastStarted] = now } }
}
