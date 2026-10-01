package com.ansa1r.projectadhd.ui.apps

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** UI-only, bounded cache. Room and the pure domain model never contain Android icons. */
class AppIconLoader(context: Context) {
    private val packageManager = context.applicationContext.packageManager
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(packageName: String, sizePx: Int, revision: Int): Bitmap? = withContext(Dispatchers.IO) {
        val size = sizePx.coerceIn(32, 192)
        // A refreshed app list gets a new revision, so updated/reinstalled icons are not stale.
        val key = "$revision:$packageName:$size"
        cache.get(key) ?: try {
            packageManager.getApplicationIcon(packageName)
                .toBitmap(size, size, Bitmap.Config.ARGB_8888)
                .also { cache.put(key, it) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: RuntimeException) {
            // Uninstalled package, denied visibility or malformed vendor drawable: show the fallback.
            null
        }
    }
}
