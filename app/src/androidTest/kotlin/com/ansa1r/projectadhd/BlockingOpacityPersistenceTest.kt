package com.ansa1r.projectadhd

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BlockingOpacityPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun defaultsAndAllValidValuesPersistAndRestore() = runBlocking {
        val file = File(context.cacheDir, "opacity-${UUID.randomUUID()}.preferences_pb")
        val job = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
            val preferences = AppPreferences(store)
            assertEquals(65, preferences.blockingOverlayOpacity.first())
            preferences.markPraise(123L)
            for (value in 30..90 step 5) {
                preferences.setBlockingOpacity(value)
                assertEquals(value, preferences.blockingOverlayOpacity.first())
                assertEquals(123L, preferences.settings.first().lastPraiseAt ?: 0L)
            }
        } finally { job.cancelAndJoin() }
        val reopenedJob = SupervisorJob()
        try {
            val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(reopenedJob + Dispatchers.IO), produceFile = { file })
            assertEquals(90, AppPreferences(reopened).blockingOverlayOpacity.first())
        } finally { reopenedJob.cancelAndJoin(); file.delete() }
    }

    @Test fun invalidStoredValuesAreSafelyNormalized() = runBlocking {
        val file = File(context.cacheDir, "opacity-invalid-${UUID.randomUUID()}.preferences_pb")
        val job = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
            val preferences = AppPreferences(store)
            val key = intPreferencesKey("blocking_overlay_opacity_percent")
            for ((stored, expected) in listOf(-100 to 30, 52 to 50, 68 to 70, 500 to 90)) {
                store.edit { it[key] = stored }
                assertEquals(expected, preferences.blockingOverlayOpacity.first())
            }
        } finally { job.cancelAndJoin(); file.delete() }
    }

    @Test fun existingOverlayControllerReceivesLiveChangesWithoutRestart() = runBlocking {
        val container = (context.applicationContext as ProjectADHDApplication).container
        val controller = container.overlays
        val original = container.preferences.blockingOverlayOpacity.first()
        try {
            for (value in listOf(30, 85)) {
                container.preferences.setBlockingOpacity(value)
                val observed = withTimeout(5_000) { controller.state.first { it.blockingOpacityPercent == value } }
                assertEquals(value, observed.blockingOpacityPercent)
                assertSame(controller, container.overlays)
            }
        } finally {
            container.preferences.setBlockingOpacity(original)
            withTimeout(5_000) { controller.state.first { it.blockingOpacityPercent == original } }
        }
    }
}
