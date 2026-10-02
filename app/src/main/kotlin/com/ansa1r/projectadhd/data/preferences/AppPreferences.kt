package com.ansa1r.projectadhd.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ansa1r.projectadhd.domain.profile.Nickname
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.ansa1r.projectadhd.domain.onboarding.*
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ansa1r.projectadhd.domain.model.AppSettings
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppPreferences(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)
    private val onboardingDone = booleanPreferencesKey("onboarding_completed")
    private val onboardingStepKey = stringPreferencesKey("onboarding_step")
    private val monitoringEnabledKey = booleanPreferencesKey("monitoring_enabled")
    private val backgroundKey = longPreferencesKey("last_ui_background_at")
    val lastUiBackgroundAt = store.data.map { it[backgroundKey] }.distinctUntilChanged()
    suspend fun markUiBackground(at: Long) { store.edit { it[backgroundKey] = at } }

    private fun readOnboarding(prefs: Preferences): OnboardingState {
        val completed = prefs[onboardingDone] ?: false
        val step = if (completed) OnboardingStep.COMPLETED else
            OnboardingStep.entries.firstOrNull { it.name == prefs[onboardingStepKey] && it != OnboardingStep.COMPLETED } ?: OnboardingStep.WELCOME
        return OnboardingState(completed, step, completed && (prefs[monitoringEnabledKey] ?: false))
    }
    val onboarding = store.data.map(::readOnboarding).distinctUntilChanged()
    private suspend fun updateOnboarding(transform: (OnboardingState) -> OnboardingState) {
        store.edit { prefs ->
            val next = transform(readOnboarding(prefs))
            prefs[onboardingDone] = next.onboardingCompleted
            prefs[onboardingStepKey] = next.onboardingStep.name
            prefs[monitoringEnabledKey] = next.monitoringEnabled
        }
    }
    suspend fun advanceOnboarding(expected: OnboardingStep, requirements: SetupRequirements) =
        updateOnboarding { it.advance(expected, requirements) }
    suspend fun beginSetup(expected: OnboardingStep) = updateOnboarding { it.beginSetup(expected) }
    suspend fun leaveOnboardingEditor() = updateOnboarding { it.leaveEditor() }
    suspend fun setMonitoringEnabled(enabled: Boolean) = updateOnboarding { it.manualMonitoring(enabled) }

    private val avatarKey = stringPreferencesKey("profile_avatar_file")
    private val mascotNameKey = stringPreferencesKey("mascot_name")
    val avatar = store.data.map { it[avatarKey] }.distinctUntilChanged()
    val mascotName = store.data.map { Nickname.restore(it[mascotNameKey]) ?: "Боб" }.distinctUntilChanged()
    suspend fun setAvatar(filename: String) { store.edit { it[avatarKey] = filename } }
    suspend fun setMascotName(name: String) { store.edit { it[mascotNameKey] = Nickname.normalize(name) } }
    private val nicknameKey = stringPreferencesKey("profile_nickname")
    val nickname = store.data.map { Nickname.restore(it[nicknameKey]) }.distinctUntilChanged()
    suspend fun setNickname(value: String) {
        val normalized = Nickname.normalize(value)
        store.edit { it[nicknameKey] = normalized }
    }

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
            lastPraiseAt = prefs[lastPraise]
        )
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
