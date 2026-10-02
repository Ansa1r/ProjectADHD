package com.ansa1r.projectadhd

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.local.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FinalizationMigrationTest {
    @Test fun exportedVersionThreeMigratesXpAndPreservesAllNineTables() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "final-migration-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name); path.parentFile?.mkdirs()
        val schema = JSONObject(instrumentation.context.assets.open("com.ansa1r.projectadhd.data.local.AppDatabase/3.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i); val table = e.getString("tableName")
                old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = e.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO habits VALUES (7, 'Read', 100, 1, 30, 'MANUAL', NULL, 100)")
            old.execSQL("INSERT INTO habit_completions VALUES (7, '2026-09-30', 200)")
            old.execSQL("INSERT INTO tracked_apps VALUES ('video', 'Video', 17, 1)")
            old.execSQL("INSERT INTO intervention_events VALUES (9, 'video', 'Video', 60000, 1020000, 300, 1, 'BLOCK_TRIGGERED', 'kept')")
            old.execSQL("INSERT INTO block_sessions VALUES ('video', 'Video', 300, '2026-09-30', 60000, 1020000, 0, '7', 1, NULL, NULL)")
            old.execSQL("INSERT INTO habit_daily_progress VALUES (7, '2026-10-01', 12345, 10, 'PAUSED', NULL, 3, 12000, 300, 0)")
            old.execSQL("INSERT INTO mascot_progress VALUES (1, 48, 9, 4, '2026-09-30', '2026-10-01')")
            old.execSQL("INSERT INTO xp_awards VALUES ('kept', 'HABIT', 7, '2026-09-30', 5, 5, 4, 200)")
            old.execSQL("INSERT INTO habit_days VALUES ('2026-09-30', 1, 1, 1)")
            old.version = 3
        }
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(Migrations.MIGRATION_3_4).build()
        try {
            val mascot = requireNotNull(db.progress().mascot())
            assertEquals(48L, mascot.lifetimeXp); assertEquals(3, mascot.currentLevel); assertEquals(3L, mascot.currentLevelXp)
            assertEquals(9L, mascot.completedHabits); assertEquals(4, mascot.streak)
            assertEquals("2026-09-30", mascot.lastStreakRewardDate); assertEquals("2026-10-01", mascot.evaluatedDate)
            assertEquals("Read", db.habits().find(7)?.title)
            assertTrue(db.habits().observeDay("2026-09-30").first().single().completedToday)
            assertEquals(17, db.trackedApps().find("video")?.sessionLimitMinutes)
            assertEquals("kept", db.interventions().observeRecent().first().single().detail)
            assertTrue(requireNotNull(db.blocks().find("video")).active)
            assertEquals(12345L, db.progress().find(7, "2026-10-01")?.accumulatedMillis)
            assertEquals(10L, db.progress().find(7, "2026-10-01")?.extraTargetMinutes)
            assertEquals("kept", db.progress().observeAwards().first().single().eventKey)
            assertTrue(requireNotNull(db.progress().day("2026-09-30")).streakAwarded)
            assertEquals(4, db.openHelper.readableDatabase.version)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(Migrations.MIGRATION_3_4).build()
            assertEquals(mascot, db.progress().mascot()) // Conversion is one-time, not every open.
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
