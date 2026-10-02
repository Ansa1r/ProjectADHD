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
class Stage4MigrationTest {
    @Test fun versionTwoDataSurvivesAndRoomValidatesVersionFour() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "stage4-migration-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name); path.parentFile?.mkdirs()
        val schema = JSONObject(instrumentation.context.assets.open("com.ansa1r.projectadhd.data.local.AppDatabase/2.json")
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
            old.execSQL("INSERT INTO habits VALUES (7, 'Read', 100, 1)")
            old.execSQL("INSERT INTO habit_completions VALUES (7, '2026-09-30', 200)")
            old.execSQL("INSERT INTO tracked_apps VALUES ('video', 'Video', 17, 1)")
            old.execSQL("INSERT INTO intervention_events VALUES (9, 'video', 'Video', 60000, 1020000, 300, 1, 'BLOCK_TRIGGERED', 'original')")
            old.execSQL("INSERT INTO block_sessions VALUES ('video', 'Video', 300, '2026-09-30', 60000, 1020000, 0, '7', 1, NULL, NULL)")
            old.version = 2
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4).build()
        try {
            val h = requireNotNull(db.habits().find(7))
            assertEquals("Read", h.title); assertEquals(30, h.targetDurationMinutes); assertEquals("MANUAL", h.type); assertNull(h.linkedAppPackage)
            assertTrue(db.habits().observeDay("2026-09-30").first().single().completedToday)
            assertEquals(17, db.trackedApps().find("video")?.sessionLimitMinutes)
            assertEquals("original", db.interventions().observeRecent().first().single().detail)
            assertTrue(requireNotNull(db.blocks().find("video")).active)
            assertEquals(0L, db.progress().mascot()?.lifetimeXp)
            assertEquals(1L, db.progress().mascot()?.completedHabits)
            assertEquals(4, db.openHelper.readableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
