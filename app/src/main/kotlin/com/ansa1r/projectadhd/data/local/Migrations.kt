package com.ansa1r.projectadhd.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE intervention_events ADD COLUMN type TEXT NOT NULL DEFAULT 'LEGACY_NOTIFICATION'")
            db.execSQL("ALTER TABLE intervention_events ADD COLUMN detail TEXT NOT NULL DEFAULT ''")
            db.execSQL("""CREATE TABLE IF NOT EXISTS block_sessions (
                packageName TEXT NOT NULL PRIMARY KEY,
                appName TEXT NOT NULL, startedAt INTEGER NOT NULL, localDate TEXT NOT NULL,
                triggerSessionDurationMillis INTEGER NOT NULL, limitMillis INTEGER NOT NULL,
                baselineCompletedCount INTEGER NOT NULL, eligibleHabitIds TEXT NOT NULL,
                active INTEGER NOT NULL, releasedAt INTEGER, releaseReason TEXT
            )""")
        }
    }
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE habits ADD COLUMN targetDurationMinutes INTEGER NOT NULL DEFAULT 30")
            db.execSQL("ALTER TABLE habits ADD COLUMN type TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE habits ADD COLUMN linkedAppPackage TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE habits ADD COLUMN activatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("""CREATE TABLE IF NOT EXISTS habit_daily_progress (
                habitId INTEGER NOT NULL, localDate TEXT NOT NULL, accumulatedMillis INTEGER NOT NULL,
                extraTargetMinutes INTEGER NOT NULL, state TEXT NOT NULL, checkpointElapsed INTEGER,
                bootCount INTEGER NOT NULL, sessionMillis INTEGER NOT NULL, checkpointWall INTEGER NOT NULL,
                appWindowBaseMillis INTEGER NOT NULL, PRIMARY KEY(habitId, localDate),
                FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS mascot_progress (
                id INTEGER NOT NULL PRIMARY KEY, totalXp INTEGER NOT NULL, completedHabits INTEGER NOT NULL,
                streak INTEGER NOT NULL, lastStreakRewardDate TEXT, evaluatedDate TEXT)""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS xp_awards (
                eventKey TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, habitId INTEGER, localDate TEXT NOT NULL,
                baseXp INTEGER NOT NULL, awardedXp INTEGER NOT NULL, levelBefore INTEGER NOT NULL, awardedAt INTEGER NOT NULL)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_awards_awardedAt ON xp_awards(awardedAt)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS habit_days (
                localDate TEXT NOT NULL PRIMARY KEY, activeCount INTEGER NOT NULL, completedCount INTEGER NOT NULL,
                streakAwarded INTEGER NOT NULL)""")
            // Existing completion records stay intact. Historical achievements do not mint retroactive XP.
            db.execSQL("INSERT OR IGNORE INTO mascot_progress VALUES (1, 0, (SELECT COUNT(*) FROM habit_completions), 0, NULL, NULL)")
        }
    }
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE mascot_progress ADD COLUMN currentLevel INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE mascot_progress ADD COLUMN currentLevelXp INTEGER NOT NULL DEFAULT 0")
            db.query("SELECT id, totalXp FROM mascot_progress").use { rows ->
                while (rows.moveToNext()) {
                    val xp = com.ansa1r.projectadhd.domain.mascot.MascotProgression.fromLifetime(rows.getLong(1).coerceAtLeast(0))
                    db.execSQL("UPDATE mascot_progress SET currentLevel = ?, currentLevelXp = ? WHERE id = ?",
                        arrayOf<Any>(xp.currentLevel, xp.currentLevelXp, rows.getInt(0)))
                }
            }
        }
    }

}
