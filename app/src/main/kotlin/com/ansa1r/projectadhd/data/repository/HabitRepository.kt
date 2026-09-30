package com.ansa1r.projectadhd.data.repository

import androidx.room.withTransaction
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.HabitCompletionEntity
import com.ansa1r.projectadhd.data.local.entity.HabitEntity
import com.ansa1r.projectadhd.domain.model.Habit
import com.ansa1r.projectadhd.util.currentDayFlow
import com.ansa1r.projectadhd.util.dayKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class HabitRepository(private val database: AppDatabase, private val blocks: BlockRepository) {
    private val dao get() = database.habits()
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday() = currentDayFlow().flatMapLatest(dao::observeDay).map { rows ->
        rows.map { row ->
            with(row.habit) { Habit(id, title, createdAt, isActive, row.completedToday) }
        }
    }

    suspend fun save(id: Long?, rawTitle: String) {
        val title = rawTitle.trim()
        require(title.isNotEmpty() && title.length <= 120)
        if (id == null) dao.insert(HabitEntity(title = title, createdAt = System.currentTimeMillis()))
        else dao.rename(id, title)
    }

    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun setCompleted(id: Long, completed: Boolean) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            if (completed) {
                val inserted = dao.complete(HabitCompletionEntity(id, dayKey(now), now))
                if (inserted != -1L) blocks.onNewCompletion(id, dayKey(now), now)
            } else dao.undoCompletion(id, dayKey(now))
        }
    }
    suspend fun incompleteCount(now: Long) = dao.incompleteCount(dayKey(now))
}
