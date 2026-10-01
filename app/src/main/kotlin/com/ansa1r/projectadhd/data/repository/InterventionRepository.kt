package com.ansa1r.projectadhd.data.repository

import com.ansa1r.projectadhd.data.local.dao.InterventionDao
import com.ansa1r.projectadhd.data.local.entity.InterventionEventEntity
import com.ansa1r.projectadhd.domain.model.InterventionEvent
import com.ansa1r.projectadhd.domain.model.InterventionType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.ansa1r.projectadhd.util.currentDayBoundsFlow

class InterventionRepository(private val dao: InterventionDao) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTodayCount() = currentDayBoundsFlow().flatMapLatest { dao.observeInterventionCount(it.start, it.end) }

    fun observeRecent() = dao.observeRecent().map { rows ->
        rows.map { with(it) { InterventionEvent(id, packageName, appName, sessionDurationMillis, limitMillis, occurredAt, incompleteHabitCount, InterventionType.valueOf(type), detail) } }
    }
    suspend fun add(event: InterventionEvent) = dao.insert(with(event) {
        InterventionEventEntity(packageName = packageName, appName = appName,
            sessionDurationMillis = sessionDurationMillis, limitMillis = limitMillis,
            occurredAt = occurredAt, incompleteHabitCount = incompleteHabitCount, type = type.name, detail = detail)
    })
    suspend fun clear() = dao.clear()
}
