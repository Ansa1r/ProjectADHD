package com.ansa1r.projectadhd

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.local.*
import com.ansa1r.projectadhd.data.local.entity.*
import com.ansa1r.projectadhd.data.repository.*
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.domain.model.*
import com.ansa1r.projectadhd.util.dayKey
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Stage2DatabaseTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun migrationPreservesStage1DataAndRoomValidatesVersion3() = runBlocking {
        val name = "migration-" + UUID.randomUUID() + ".db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        val schema = JSONObject(instrumentation.context.assets.open("com.ansa1r.projectadhd.data.local.AppDatabase/1.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) {
                    old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO habits VALUES (7, 'Read', 100, 1)")
            old.execSQL("INSERT INTO habit_completions VALUES (7, '2026-09-30', 200)")
            old.execSQL("INSERT INTO tracked_apps VALUES ('video.app', 'Video', 15, 1)")
            old.execSQL("INSERT INTO intervention_events VALUES (9, 'video.app', 'Video', 900000, 900000, 300, 1)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4).build()
        try {
            assertEquals("Read", db.habits().observeDay("2026-09-30").first().single().habit.title)
            assertTrue(db.habits().observeDay("2026-09-30").first().single().completedToday)
            assertEquals(15, db.trackedApps().find("video.app")?.sessionLimitMinutes)
            val event = db.interventions().observeRecent().first().single()
            assertEquals(9L, event.id)
            assertEquals("LEGACY_NOTIFICATION", event.type)
            assertEquals("", event.detail)
            assertTrue(db.blocks().active().isEmpty())
            assertEquals(4, db.openHelper.readableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun blockSurvivesReopenAndOnlyNewBaselineCompletionReleasesAtomically() = runBlocking {
        val name = "block-" + UUID.randomUUID() + ".db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val now = System.currentTimeMillis()
            val date = dayKey(now)
            val oldId = db.habits().insert(HabitEntity(title = "Already done", createdAt = now - 100))
            val newId = db.habits().insert(HabitEntity(title = "Next step", createdAt = now - 100))
            db.habits().complete(HabitCompletionEntity(oldId, date, now - 50))
            db.trackedApps().save(TrackedAppEntity("video.app", "Video", 1))
            var blocks = BlockRepository(db)
            val payload = InterventionPayload("video.app", "Video", 60_000, 60_000, 1)
            blocks.start(payload, now - 10)
            blocks.start(payload, now)
            assertEquals(1, db.blocks().active().size)
            assertEquals(1, db.interventions().observeRecent().first().size)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            blocks = BlockRepository(db)
            assertEquals(setOf(newId), blocks.find("video.app")?.eligibleHabitIds)
            val habits = HabitRepository(db, blocks)
            habits.setCompleted(oldId, false)
            habits.setCompleted(oldId, true)
            assertTrue(requireNotNull(blocks.find("video.app")).active)
            db.progress().save(HabitDailyEntity(newId, date, accumulatedMillis = 1_800_000, state = "AWAITING_CONFIRMATION"))
            habits.setCompleted(newId, true)
            // Undo is no longer supported; repeated calls cannot restore the block or mint another reward.
            habits.setCompleted(newId, false)
            val released = requireNotNull(blocks.find("video.app"))
            assertFalse(released.active)
            assertNotNull(released.releasedAt)
            assertEquals(ReleaseReason.COMPLETION, released.releaseReason)
            assertEquals(2, db.interventions().observeRecent().first().size)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertFalse(requireNotNull(db.blocks().find("video.app")).active)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
