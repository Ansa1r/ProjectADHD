package com.ansa1r.projectadhd.data.repository

import com.ansa1r.projectadhd.data.local.dao.TrackedAppDao
import com.ansa1r.projectadhd.data.local.entity.TrackedAppEntity
import com.ansa1r.projectadhd.domain.model.TrackedApp
import kotlinx.coroutines.flow.map

class TrackedAppRepository(private val dao: TrackedAppDao) {
    fun observeAll() = dao.observeAll().map { rows -> rows.map { it.model() } }
    suspend fun find(packageName: String) = dao.find(packageName)?.model()
    suspend fun save(app: TrackedApp) = dao.save(
        TrackedAppEntity(app.packageName, app.displayName, app.sessionLimitMinutes, app.enabled)
    )
    suspend fun delete(packageName: String) = dao.delete(packageName)
    private fun TrackedAppEntity.model() = TrackedApp(packageName, displayName, sessionLimitMinutes, enabled)
}
