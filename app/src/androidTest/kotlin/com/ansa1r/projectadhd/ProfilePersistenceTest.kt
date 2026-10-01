package com.ansa1r.projectadhd

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.InterventionEventEntity
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun nicknamePersistsAcrossStoreReopenWithoutChangingCooldown() = runBlocking {
        val file = File(context.cacheDir, "nickname-${UUID.randomUUID()}.preferences_pb")
        val firstJob = SupervisorJob()
        try {
            val prefs = AppPreferences(PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO), produceFile = { file }))
            assertNull(prefs.nickname.first())
            prefs.setCooldown(80)
            prefs.setNickname("  Максим  ")
            assertEquals("Максим", prefs.nickname.first())
            prefs.setNickname("Максим 🌿")
        } finally { firstJob.cancelAndJoin() }
        val secondJob = SupervisorJob()
        try {
            val prefs = AppPreferences(PreferenceDataStoreFactory.create(scope = CoroutineScope(secondJob + Dispatchers.IO), produceFile = { file }))
            assertEquals("Максим 🌿", prefs.nickname.first())
            assertEquals(80, prefs.settings.first().cooldownMinutes)
        } finally { secondJob.cancelAndJoin(); file.delete() }
    }
    @Test fun dailyCountUsesFullHistoryBoundariesAndDoesNotDoubleCountBlockFallback() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            suspend fun event(type: String, at: Long, detail: String = "") = db.interventions().insert(
                InterventionEventEntity(packageName = "test.app", appName = "Test", sessionDurationMillis = 60_000,
                    limitMillis = 60_000, occurredAt = at, incompleteHabitCount = 1, type = type, detail = detail))
            repeat(250) { event("PRAISE_SHOWN", 100) }
            event("BLOCK_TRIGGERED", 110)
            event("BLOCK_RELEASED", 120)
            event("FALLBACK_NOTIFICATION", 130, "BLOCK: OVERLAY_PERMISSION_MISSING")
            event("FALLBACK_NOTIFICATION", 140, "PRAISE: OVERLAY_PERMISSION_MISSING")
            event("LEGACY_NOTIFICATION", 150)
            event("BLOCK_TRIGGERED", 99)
            event("PRAISE_SHOWN", 200)
            assertEquals(200, db.interventions().observeRecent().first().size)
            assertEquals(253, db.interventions().observeInterventionCount(100, 200).first())
            db.interventions().clear()
            assertEquals(0, db.interventions().observeInterventionCount(100, 200).first())
        } finally { db.close() }
    }
}
