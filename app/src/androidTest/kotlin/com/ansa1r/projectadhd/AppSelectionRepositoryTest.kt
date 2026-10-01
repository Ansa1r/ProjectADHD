package com.ansa1r.projectadhd

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.HabitEntity
import com.ansa1r.projectadhd.data.repository.*
import com.ansa1r.projectadhd.domain.apps.AppSelectionDraft
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSelectionRepositoryTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
    @Test fun draftIsIsolatedAndSaveReplacesAppsAndReleasesRemovedChallenge() = runBlocking {
        val db = database()
        try {
            val blocks = BlockRepository(db)
            val repo = TrackedAppRepository(db, blocks)
            val now = System.currentTimeMillis()
            val original = listOf(TrackedApp("old.app", "Old", 30), TrackedApp("keep.app", "Keep", 20))
            repo.replaceSelection(original, now)
            db.habits().insert(HabitEntity(title = "Read", createdAt = now))
            blocks.start(InterventionPayload("old.app", "Old", 1_800_000, 1_800_000, 1), now)
            val draft = AppSelectionDraft.from(repo.observeAll().first())
                .select(InstalledApp("old.app", "Old"), false).select(InstalledApp("new.app", "New"), true)
            assertEquals(original.toSet(), repo.observeAll().first().toSet())
            assertTrue(requireNotNull(blocks.find("old.app")).active)
            repo.replaceSelection(draft.savedApps(), now + 1)
            assertNull(repo.find("old.app"))
            assertEquals(20, repo.find("keep.app")?.sessionLimitMinutes)
            assertEquals(15, repo.find("new.app")?.sessionLimitMinutes)
            assertEquals(ReleaseReason.TRACKING_DISABLED, blocks.find("old.app")?.releaseReason)
            assertFalse(requireNotNull(blocks.find("old.app")).active)
            assertEquals(1, db.interventions().observeRecent().first().count { it.type == "BLOCK_RELEASED" })
        } finally { db.close() }
    }
    @Test fun emptySelectionClearsTrackingAndActiveBlocks() = runBlocking {
        val db = database()
        try {
            val blocks = BlockRepository(db); val repo = TrackedAppRepository(db, blocks)
            val now = System.currentTimeMillis()
            repo.replaceSelection(listOf(TrackedApp("old.app", "Old")), now)
            db.habits().insert(HabitEntity(title = "Read", createdAt = now))
            blocks.start(InterventionPayload("old.app", "Old", 900_000, 900_000, 1), now)
            repo.replaceSelection(emptyList(), now + 1)
            assertTrue(repo.observeAll().first().isEmpty())
            assertTrue(db.blocks().active().isEmpty())
        } finally { db.close() }
    }
    @Test fun failedInsertRollsBackDeletionAndDoesNotReleaseExistingBlock() = runBlocking {
        val db = database()
        try {
            val blocks = BlockRepository(db); val repo = TrackedAppRepository(db, blocks)
            val now = System.currentTimeMillis(); val old = TrackedApp("old.app", "Old", 10)
            repo.replaceSelection(listOf(old), now)
            db.habits().insert(HabitEntity(title = "Read", createdAt = now))
            blocks.start(InterventionPayload("old.app", "Old", 600_000, 600_000, 1), now)
            db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER fail_selection BEFORE INSERT ON tracked_apps
                WHEN NEW.packageName = 'fail.app' BEGIN SELECT RAISE(ABORT, 'forced failure'); END""")
            var failed = false
            try { repo.replaceSelection(listOf(TrackedApp("fail.app", "Fail")), now + 1) }
            catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertEquals(listOf(old), repo.observeAll().first())
            assertTrue(requireNotNull(blocks.find("old.app")).active)
            assertEquals(1, db.interventions().observeRecent().first().size)
        } finally { db.close() }
    }
    @Test fun duplicateAndSelfSelectionCannotReplaceExistingData() = runBlocking {
        val db = database()
        try {
            val repo = TrackedAppRepository(db, BlockRepository(db)); val old = TrackedApp("old.app", "Old")
            repo.replaceSelection(listOf(old))
            for (invalid in listOf(listOf(old, old), listOf(TrackedApp(BuildConfig.APPLICATION_ID, "Self")))) {
                var failed = false
                try { repo.replaceSelection(invalid) } catch (_: IllegalArgumentException) { failed = true }
                assertTrue(failed)
                assertEquals(listOf(old), repo.observeAll().first())
            }
        } finally { db.close() }
    }
}
