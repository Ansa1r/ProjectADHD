package com.ansa1r.projectadhd.monitoring

import android.content.Context
import android.content.Intent
import com.ansa1r.projectadhd.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppReader(private val context: Context, private val excluded: ExcludedApps) {
    suspend fun read(): List<InstalledApp> = withContext(Dispatchers.IO) {
        excluded.refresh()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
            .filter { !excluded.contains(it.activityInfo.packageName) }
            .map {
                val info = it.activityInfo.applicationInfo
                InstalledApp(info.packageName, pm.getApplicationLabel(info).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.displayName.lowercase() }
    }

    fun nameOf(packageName: String): String = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: RuntimeException) { packageName }
      catch (_: android.content.pm.PackageManager.NameNotFoundException) { packageName }
}
