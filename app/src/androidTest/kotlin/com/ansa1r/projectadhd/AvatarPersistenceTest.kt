package com.ansa1r.projectadhd

import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import com.ansa1r.projectadhd.data.repository.AvatarRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarPersistenceTest {
    @Test fun avatarIsPrivateCopyAndSurvivesSourceDeletionAndStoreReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "image-${UUID.randomUUID()}.png")
        val prefsFile = File(context.cacheDir, "avatar-${UUID.randomUUID()}.preferences_pb")
        Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).let { bitmap ->
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
        val job = SupervisorJob()
        var copy: File? = null
        try {
            val prefs = AppPreferences(PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { prefsFile }))
            prefs.setNickname("Saved nickname")
            val repo = AvatarRepository(context, prefs)
            repo.saveFromPicker(Uri.fromFile(source))
            copy = File(requireNotNull(repo.file.first()))
            assertTrue(requireNotNull(copy).canonicalPath.startsWith(File(context.filesDir, "avatars").canonicalPath))
            assertTrue(requireNotNull(copy).length() > 0)
            source.delete()
            assertTrue(requireNotNull(copy).isFile)
            assertEquals("Saved nickname", prefs.nickname.first())
        } finally { job.cancelAndJoin() }
        val second = SupervisorJob()
        try {
            val prefs = AppPreferences(PreferenceDataStoreFactory.create(scope = CoroutineScope(second + Dispatchers.IO), produceFile = { prefsFile }))
            assertEquals(copy?.absolutePath, AvatarRepository(context, prefs).file.first())
            assertEquals("Saved nickname", prefs.nickname.first())
        } finally { second.cancelAndJoin(); copy?.delete(); source.delete(); prefsFile.delete() }
    }
}
