package com.ansa1r.projectadhd.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AvatarRepository(private val context: Context, private val preferences: AppPreferences) {
    private val directory = File(context.filesDir, "avatars")
    private val gate = Mutex()
    val file = preferences.avatar.map { name -> name?.takeIf { it.matches(Regex("avatar-[a-f0-9-]+\\.jpg")) }
        ?.let { File(directory, it) }?.takeIf { it.isFile }?.absolutePath }
    suspend fun saveFromPicker(uri: Uri) = gate.withLock { withContext(Dispatchers.IO) {
        directory.mkdirs()
        val temp = File.createTempFile("picker-", ".tmp", context.cacheDir)
        val target = File(directory, "avatar-${UUID.randomUUID()}.jpg")
        try {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(8192); var total = 0L
                    while (true) { val size = input.read(buffer); if (size < 0) break
                        total += size; require(total <= 20 * 1024 * 1024) { "Avatar exceeds 20 MiB" }; output.write(buffer, 0, size) }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temp.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
            val bitmap = requireNotNull(BitmapFactory.decodeFile(temp.path, BitmapFactory.Options().apply { inSampleSize = sample }))
            val orientation = runCatching { ExifInterface(temp.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
            val matrix = Matrix().apply { when (orientation) {
                2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
                7 -> { setRotate(-90f); postScale(-1f, 1f) }; 8 -> setRotate(-90f)
            } }
            val normalized = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            try { target.outputStream().use { check(normalized.compress(Bitmap.CompressFormat.JPEG, 90, it)) } }
            finally { if (normalized !== bitmap) normalized.recycle(); bitmap.recycle() }
            val old = preferences.avatar.first()
            preferences.setAvatar(target.name) // Expose the new complete private copy atomically.
            if (old != null && old != target.name && old.matches(Regex("avatar-[a-f0-9-]+\\.jpg"))) File(directory, old).delete()
        } catch (error: Exception) {
            // If cancellation arrives just after DataStore committed, retain its referenced image.
            if (preferences.avatar.first() != target.name) target.delete()
            throw error
        } finally { temp.delete() }
    } }
}
