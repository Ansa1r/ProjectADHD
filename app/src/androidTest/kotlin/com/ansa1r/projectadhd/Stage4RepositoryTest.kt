package com.ansa1r.projectadhd

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.local.*
import com.ansa1r.projectadhd.data.local.entity.*
import com.ansa1r.projectadhd.data.repository.*
import com.ansa1r.projectadhd.domain.habits.*
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.domain.model.TrackedApp
import com.ansa1r.projectadhd.monitoring.*
import com.ansa1r.projectadhd.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class Stage4RepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private class Clock : HabitClock {
        var now = HabitClockSample(dayStart(System.currentTimeMillis()) + 12 * 3_600_000L, 1_000_000, 4)
        override fun sample() = now
        fun advance(ms: Long) { now = now.copy(wall = now.wall + ms, elapsed = now.elapsed + ms) }
    }
    private suspend fun withDb(block: suspend (AppDatabase, HabitRepository, Clock) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val clock = Clock()
        val habits = HabitRepository(db, BlockRepository(db), clock)
        try { block(db, habits, clock) } finally { db.close() }
    }
    private suspend fun add(db: AppDatabase, clock: Clock, title: String = "Reading", app: String? = null): Long =
        db.habits().insert(HabitEntity(title = title, createdAt = clock.now.wall, targetDurationMinutes = 1,
            type = if (app == null) "MANUAL" else "APP_BASED", linkedAppPackage = app, activatedAt = clock.now.wall))
    private suspend fun awaitManual(habits: HabitRepository, clock: Clock, id: Long) {
        habits.start(id); clock.advance(60_000); habits.tick()
    }
    @Test fun manualNoTwicePreservesProgressYesAwardsOnceAndReleasesBlock() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock)
        db.trackedApps().save(TrackedAppEntity("video", "Video", 1))
        val blocks = BlockRepository(db)
        blocks.start(InterventionPayload("video", "Video", 60000, 60000, 1), clock.now.wall)
        awaitManual(habits, clock, id)
        assertTrue(requireNotNull(blocks.find("video")).active)
        habits.confirm(id, false)
        var p = requireNotNull(db.progress().find(id, dayKey(clock.now.wall)))
        assertEquals(60000L, p.accumulatedMillis); assertEquals(10L, p.extraTargetMinutes)
        clock.advance(600000); habits.tick(); habits.confirm(id, false)
        p = requireNotNull(db.progress().find(id, dayKey(clock.now.wall)))
        assertEquals(660000L, p.accumulatedMillis); assertEquals(20L, p.extraTargetMinutes)
        clock.advance(600000); habits.tick()
        coroutineScope { repeat(6) { launch { habits.confirm(id, true) } } }
        assertEquals(15L, db.progress().mascot()?.lifetimeXp)
        assertEquals(1L, db.progress().mascot()?.completedHabits)
        assertEquals(1, db.progress().mascot()?.streak)
        assertEquals(2, db.progress().observeAwards().first().size)
        assertFalse(requireNotNull(blocks.find("video")).active)
        assertEquals(1, db.interventions().observeRecent().first().count { it.type == "BLOCK_RELEASED" })
    } }
    @Test fun onlyLastActiveHabitEarnsStreakAndRepeatedTickDoesNotDuplicate() = runBlocking { withDb { db, habits, clock ->
        val a = add(db, clock); val b = add(db, clock, "Exercise")
        awaitManual(habits, clock, a); habits.confirm(a, true)
        assertEquals(5L, db.progress().mascot()?.lifetimeXp)
        assertEquals(0, db.progress().mascot()?.streak)
        awaitManual(habits, clock, b); habits.confirm(b, true)
        repeat(3) { habits.tick() }
        assertEquals(20L, db.progress().mascot()?.lifetimeXp)
        assertEquals(1, db.progress().mascot()?.streak)
        assertEquals(1, db.progress().observeAwards().first().count { it.kind == "STREAK" })
    } }
    @Test fun newDayClearsExtraAndUnfinishedDayBreaksStreak() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock)
        awaitManual(habits, clock, id); habits.confirm(id, true)
        clock.advance(86_400_000); habits.tick()
        assertEquals(1, db.progress().mascot()?.streak)
        awaitManual(habits, clock, id); habits.confirm(id, false); habits.pause(id)
        clock.advance(86_400_000); habits.tick()
        assertEquals(0, db.progress().mascot()?.streak)
        assertNull(db.progress().find(id, dayKey(clock.now.wall)))
        val today = habits.observeToday().first().single()
        assertEquals(0L, today.progress.accumulatedMillis)
        assertEquals(0L, today.progress.extraTargetMinutes)
        assertFalse(today.completedToday)
        assertEquals(15L, db.progress().mascot()?.lifetimeXp)
    } }
    @Test fun noActiveHabitsNeverMintExperienceOrStreak() = runBlocking { withDb { db, habits, clock ->
        habits.tick(); clock.advance(86_400_000); habits.tick()
        assertEquals(0L, db.progress().mascot()?.lifetimeXp)
        assertEquals(0, db.progress().mascot()?.streak)
        assertTrue(db.progress().observeAwards().first().isEmpty())
    } }
    @Test fun usefulAppCompletesAutomaticallyAndEarlyCompletionAvoidsBlock() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock, app = "useful")
        db.trackedApps().save(TrackedAppEntity("video", "Video", 1))
        habits.creditApp(id, 20000); habits.creditApp(id, 40000); habits.creditApp(id, 60000)
        habits.creditApp(id, 60000); habits.creditApp(id, 80000)
        assertTrue(habits.observeToday().first().single().completedToday)
        assertEquals(15L, db.progress().mascot()?.lifetimeXp)
        assertNull(BlockRepository(db).start(InterventionPayload("video", "Video", 60000, 60000, 0), clock.now.wall))
    } }
    @Test fun appCompletionReleasesExistingBlockAndStaleWindowCannotDoubleCredit() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock, app = "useful")
        db.trackedApps().save(TrackedAppEntity("video", "Video", 1))
        val blocks = BlockRepository(db)
        blocks.start(InterventionPayload("video", "Video", 60000, 60000, 1), clock.now.wall)
        val originalWindow = clock.now.wall
        habits.creditApp(id, 20000)
        habits.setActive(id, false); clock.advance(10000); habits.setActive(id, true)
        habits.creditApp(id, 60000, clock.now, originalWindow)
        assertEquals(20000L, db.progress().find(id, dayKey(clock.now.wall))?.accumulatedMillis)
        clock.advance(40000)
        habits.creditApp(id, 40000)
        assertFalse(requireNotNull(blocks.find("video")).active)
        assertEquals(15L, db.progress().mascot()?.lifetimeXp)
    } }
    @Test fun appConflictsRejectedInBothTransactionsLegacyRowsKeptButNotBlocked() = runBlocking { withDb { db, habits, clock ->
        val tracked = TrackedAppRepository(db, BlockRepository(db))
        tracked.save(TrackedApp("limited", "Limited"))
        assertTrue(runCatching { habits.save(null, "No", 1, "limited") }.isFailure)
        habits.save(null, "Useful", 1, "useful")
        assertTrue(runCatching { tracked.replaceSelection(listOf(TrackedApp("useful", "Useful"))) }.isFailure)
        assertNotNull(tracked.find("limited")) // Failed selection rolls back the existing list.
        val id = add(db, clock, "Legacy", "limited") // Simulate imported conflicting data without deleting it.
        assertNull(BlockRepository(db).start(InterventionPayload("limited", "Limited", 900000, 900000, 1), clock.now.wall))
        assertNotNull(db.habits().find(id)); assertNotNull(tracked.find("limited"))
    } }
    @Test fun completionXpAndUnlockRollbackTogetherOnWriteFailure() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock)
        db.trackedApps().save(TrackedAppEntity("video", "Video", 1))
        val blocks = BlockRepository(db)
        blocks.start(InterventionPayload("video", "Video", 60000, 60000, 1), clock.now.wall)
        awaitManual(habits, clock, id)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_xp BEFORE INSERT ON xp_awards BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        assertTrue(runCatching { habits.confirm(id, true) }.isFailure)
        assertTrue(requireNotNull(blocks.find("video")).active)
        assertFalse(habits.observeToday().first().single().completedToday)
        assertEquals("AWAITING_CONFIRMATION", db.progress().find(id, dayKey(clock.now.wall))?.state)
        assertEquals(0L, db.progress().mascot()?.lifetimeXp)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_xp")
        habits.confirm(id, true)
        assertEquals(15L, db.progress().mascot()?.lifetimeXp)
    } }
    @Test fun manualSessionSurvivesProcessReopenAndRebootPausesUnknownTime() = runBlocking {
        val name = "stage4-${UUID.randomUUID()}.db"
        val clock = Clock()
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val id = add(db, clock)
            var habits = HabitRepository(db, BlockRepository(db), clock)
            habits.start(id); clock.advance(20000); habits.tick(); db.close()
            clock.advance(20000)
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            habits = HabitRepository(db, BlockRepository(db), clock); habits.tick()
            assertEquals(40000L, db.progress().find(id, dayKey(clock.now.wall))?.accumulatedMillis)
            db.close(); clock.now = clock.now.copy(wall = clock.now.wall + 60000, elapsed = 2000000, boot = 5)
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            habits = HabitRepository(db, BlockRepository(db), clock); habits.tick()
            assertEquals(40000L, db.progress().find(id, dayKey(clock.now.wall))?.accumulatedMillis)
            assertEquals("PAUSED", db.progress().find(id, dayKey(clock.now.wall))?.state)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun appRebootKeepsKnownProgressAndAddsOnlyNewBootWindow() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock, app = "useful")
        habits.prepareAppWindow(id, clock.now); habits.creditApp(id, 20000)
        clock.now = clock.now.copy(wall = clock.now.wall + 300000, elapsed = 10000, boot = 5)
        val from = habits.prepareAppWindow(id, clock.now)
        assertEquals(clock.now.wall - 10000, from)
        habits.creditApp(id, 10000)
        assertEquals(30000L, db.progress().find(id, dayKey(clock.now.wall))?.accumulatedMillis)
        habits.prepareAppWindow(id, clock.now); habits.creditApp(id, 10000)
        assertEquals(30000L, db.progress().find(id, dayKey(clock.now.wall))?.accumulatedMillis)
    } }
    @Test fun deletingHabitKeepsEarnedXpAndHistory() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock); awaitManual(habits, clock, id); habits.confirm(id, true); habits.delete(id)
        assertNull(db.habits().find(id)); assertEquals(15L, db.progress().mascot()?.lifetimeXp)
        assertEquals(1L, db.progress().mascot()?.completedHabits)
        assertEquals(2, db.progress().observeAwards().first().size)
    } }
    @Test fun multiplierForStreakUsesLevelAfterHabitAward() = runBlocking { withDb { db, habits, clock ->
        val id = add(db, clock); habits.tick()
        db.progress().saveMascot(requireNotNull(db.progress().mascot()).copy(lifetimeXp = 820, currentLevel = 10, currentLevelXp = 145))
        awaitManual(habits, clock, id); habits.confirm(id, true)
        assertEquals(840L, db.progress().mascot()?.lifetimeXp)
        val awards = db.progress().observeAwards().first()
        assertEquals(5L, awards.single { it.kind == "HABIT" }.awardedXp)
        assertEquals(15L, awards.single { it.kind == "STREAK" }.awardedXp)
    } }
}
