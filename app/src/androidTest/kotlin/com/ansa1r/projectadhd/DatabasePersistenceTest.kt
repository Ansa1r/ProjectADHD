package com.ansa1r.projectadhd

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.HabitCompletionEntity
import com.ansa1r.projectadhd.data.local.entity.HabitEntity
import com.ansa1r.projectadhd.data.local.entity.TrackedAppEntity
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {
    @Test fun dataSurvivesReopenAndCompletionsAreUniqueAndCascaded() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "room-test-" + UUID.randomUUID() + ".db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val id = db.habits().insert(HabitEntity(title = "Прочитать главу", createdAt = 100))
            val completion = HabitCompletionEntity(id, "2026-09-29", 200)
            db.habits().complete(completion)
            db.habits().complete(completion.copy(completedAt = 300))
            db.trackedApps().save(TrackedAppEntity("test.app", "Тест", 7))
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(1, db.habits().observeCompletionCount().first())
            val today = db.habits().observeDay("2026-09-29").first().single()
            assertEquals("Прочитать главу", today.habit.title)
            assertTrue(today.completedToday)
            assertFalse(db.habits().observeDay("2026-09-30").first().single().completedToday)
            assertEquals(7, db.trackedApps().find("test.app")?.sessionLimitMinutes)
            db.habits().delete(id)
            assertEquals(0, db.habits().observeCompletionCount().first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
