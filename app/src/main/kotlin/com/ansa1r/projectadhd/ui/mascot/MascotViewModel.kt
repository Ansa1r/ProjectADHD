package com.ansa1r.projectadhd.ui.mascot

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.data.local.entity.*
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.*

data class MascotUiState(val name: String = "Боб", val progress: MascotEntity = MascotEntity(), val history: List<XpAwardEntity> = emptyList(), val loading: Boolean = true)
class MascotViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(MascotUiState())
    val state = mutable.asStateFlow()
    init { execute { combine(container.preferences.mascotName, container.habits.mascot.observe(), container.habits.mascot.history()) { name, progress, history ->
        MascotUiState(name, progress, history, false)
    }.collect { mutable.value = it } } }
    fun rename(name: String) { execute { container.preferences.setMascotName(name) } }
}
