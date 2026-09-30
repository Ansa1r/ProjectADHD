package com.ansa1r.projectadhd.ui.habits

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.domain.model.Habit
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HabitDraft(val id: Long? = null, val title: String = "")
data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val loading: Boolean = true,
    val draft: HabitDraft? = null,
    val saving: Boolean = false
)

class HabitsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(HabitsUiState())
    val state = mutable.asStateFlow()
    init { execute { container.habits.observeToday().collect { items ->
        mutable.update { it.copy(habits = items, loading = false) }
    } } }
    fun edit(habit: Habit? = null) { mutable.update { it.copy(draft = HabitDraft(habit?.id, habit?.title ?: "")) } }
    fun titleChanged(title: String) {
        if (title.length <= 120) mutable.update { it.copy(draft = it.draft?.copy(title = title)) }
    }
    fun closeEditor() { if (!mutable.value.saving) mutable.update { it.copy(draft = null) } }
    fun save() {
        val draft = mutable.value.draft ?: return
        if (draft.title.isBlank() || mutable.value.saving) return
        mutable.update { it.copy(saving = true) }
        execute {
            try {
                container.habits.save(draft.id, draft.title)
                mutable.update { it.copy(draft = null) }
            } finally { mutable.update { it.copy(saving = false) } }
        }
    }
    fun active(habit: Habit, value: Boolean) { execute { container.habits.setActive(habit.id, value) } }
    fun completed(habit: Habit, value: Boolean) { execute { container.habits.setCompleted(habit.id, value) } }
    fun delete(id: Long) { execute { container.habits.delete(id) } }
}
