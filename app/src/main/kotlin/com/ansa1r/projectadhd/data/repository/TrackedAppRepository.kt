package com.ansa1r.projectadhd.data.repository

import androidx.room.withTransaction
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.TrackedAppEntity
import com.ansa1r.projectadhd.domain.model.TrackedApp
import kotlinx.coroutines.flow.map

class TrackedAppRepository(private val database: AppDatabase, private val blocks: BlockRepository) {
    private val dao get() = database.trackedApps()
    fun observeAll() = dao.observeAll().map { rows -> rows.map { it.model() } }
    suspend fun find(packageName: String) = dao.find(packageName)?.model()
    suspend fun save(app: TrackedApp) = dao.save(app.entity())
    suspend fun delete(packageName: String) = database.withTransaction {
        dao.delete(packageName)
        blocks.reconcile(System.currentTimeMillis())
    }
    suspend fun replaceSelection(apps: List<TrackedApp>, now: Long = System.currentTimeMillis()) {
        require(apps.map { it.packageName }.toSet().size == apps.size)
        require(apps.all { it.enabled && it.packageName.isNotBlank() && it.packageName != BuildConfig.APPLICATION_ID })
        database.withTransaction {
            dao.deleteAll()
            dao.saveAll(apps.map { it.entity() })
            // Untracking and cancelling its challenge/history commit together.
            blocks.reconcile(now)
        }
    }
    private fun TrackedApp.entity() = TrackedAppEntity(packageName, displayName, sessionLimitMinutes, enabled)
    private fun TrackedAppEntity.model() = TrackedApp(packageName, displayName, sessionLimitMinutes, enabled)
}
