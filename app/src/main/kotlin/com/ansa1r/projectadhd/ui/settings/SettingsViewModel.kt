package com.ansa1r.projectadhd.ui.settings

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(val cooldown: String = "30", val savedCooldown: Int = 30, val saving: Boolean = false)

class SettingsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(SettingsUiState())
    val state = mutable.asStateFlow()
    private var edited = false
    init { execute { container.preferences.settings.collect { settings ->
        mutable.update { it.copy(savedCooldown = settings.cooldownMinutes,
            cooldown = if (edited) it.cooldown else settings.cooldownMinutes.toString()) }
    } } }
    fun change(value: String) {
        if (value.length <= 3 && value.all(Char::isDigit)) {
            edited = true
            mutable.update { it.copy(cooldown = value) }
        }
    }
    fun save() {
        val value = mutable.value.cooldown.toIntOrNull()?.takeIf { it in 1..180 } ?: return
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true) }
        execute {
            try {
                container.preferences.setCooldown(value)
                edited = false
                inform(R.string.settings_saved)
            } finally { mutable.update { it.copy(saving = false) } }
        }
    }
}
