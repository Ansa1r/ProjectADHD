package com.ansa1r.projectadhd

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import com.ansa1r.projectadhd.ui.theme.BrandOpacity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BlockingOpacityPersistenceTest {
    @Test fun legacyOpacityKeysDoNotChangeCurrentSettingsAndSurviveStoreReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "legacy-opacity-${UUID.randomUUID()}.preferences_pb")
        val key = intPreferencesKey("blocking_overlay_opacity_percent")
        val job = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
            val preferences = AppPreferences(store)
            preferences.setCooldown(17)
            preferences.setPraiseCooldown(44)
            preferences.markPraise(123L)
            val settings = preferences.settings.first()
            for (legacyValue in listOf(-100, 30, 52, 65, 85, 90, 500)) {
                store.edit { it[key] = legacyValue }
                assertEquals(settings, preferences.settings.first())
                assertEquals(0.85f, BrandOpacity.Blocking, 0f)
            }
        } finally { job.cancelAndJoin() }
        val reopenedJob = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(reopenedJob + Dispatchers.IO), produceFile = { file })
            val settings = AppPreferences(store).settings.first()
            assertEquals(17, settings.cooldownMinutes)
            assertEquals(44, settings.praiseCooldownMinutes)
            assertEquals(123L, settings.lastPraiseAt ?: 0L)
            assertEquals(500, store.data.first()[key])
            assertEquals(0.85f, BrandOpacity.Blocking, 0f)
        } finally { reopenedJob.cancelAndJoin(); file.delete() }
    }
}
