package com.ansa1r.projectadhd.data.repository

import com.ansa1r.projectadhd.data.local.dao.InterventionDao
import com.ansa1r.projectadhd.data.local.entity.InterventionEventEntity
import com.ansa1r.projectadhd.domain.model.InterventionEvent
import kotlinx.coroutines.flow.map

class InterventionRepository(private val dao: InterventionDao) {
    fun observeRecent() = dao.observeRecent().map { rows ->
        rows.map { with(it) { InterventionEvent(id, packageName, appName, sessionDurationMillis, limitMillis, occurredAt, incompleteHabitCount) } }
    }
    suspend fun add(event: InterventionEvent) = dao.insert(with(event) {
        InterventionEventEntity(packageName = packageName, appName = appName,
            sessionDurationMillis = sessionDurationMillis, limitMillis = limitMillis,
            occurredAt = occurredAt, incompleteHabitCount = incompleteHabitCount)
    })
    suspend fun clear() = dao.clear()
}
