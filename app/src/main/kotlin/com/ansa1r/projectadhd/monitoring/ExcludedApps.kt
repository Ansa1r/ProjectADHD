package com.ansa1r.projectadhd.monitoring

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Resolve OEM launcher/settings packages as well as standard system surfaces. */
class ExcludedApps(private val context: Context) {
    @Volatile private var packages: Set<String> = emptySet()
    init { refresh() }
    fun refresh() {
        val result = mutableSetOf(context.packageName, "android", "com.android.systemui",
            "com.android.settings", "com.android.permissioncontroller", "com.google.android.permissioncontroller")
        val intents = listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            Intent(Settings.ACTION_SETTINGS), Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        for (intent in intents) {
            try {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, 0).forEach { result.add(it.activityInfo.packageName) }
            } catch (_: RuntimeException) { }
        }
        packages = result
    }
    fun contains(packageName: String) = packageName in packages
}
