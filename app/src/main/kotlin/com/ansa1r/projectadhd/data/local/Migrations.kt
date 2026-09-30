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
}
