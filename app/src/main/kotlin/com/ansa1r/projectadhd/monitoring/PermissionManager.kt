package com.ansa1r.projectadhd.monitoring

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ansa1r.projectadhd.notification.NotificationHelper

data class PermissionState(
    val usageAccess: Boolean = false,
    val notifications: Boolean = false,
    val monitoringChannel: Boolean = false,
    val interventionChannel: Boolean = false,
    val overlay: Boolean = false,
    val overlaySupported: Boolean = Build.VERSION.SDK_INT >= 26
) {
    val canMonitor: Boolean get() = usageAccess && notifications && monitoringChannel && interventionChannel
}

class PermissionManager(private val context: Context) {
    fun hasUsageAccess(): Boolean = try {
        val manager = requireNotNull(context.getSystemService(AppOpsManager::class.java))
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            manager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            manager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: RuntimeException) { false }

    fun state(): PermissionState {
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val notifications = granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        return PermissionState(hasUsageAccess(), notifications,
            channelEnabled(NotificationHelper.MONITORING_CHANNEL),
            channelEnabled(NotificationHelper.INTERVENTION_CHANNEL), canDrawOverlays())
    }

    private fun channelEnabled(id: String): Boolean {
        if (Build.VERSION.SDK_INT < 26) return true
        val channel = context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(id)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canDrawOverlays(): Boolean = try { Settings.canDrawOverlays(context) } catch (_: RuntimeException) { false }

    fun openOverlaySettings(): Boolean = launch(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
    )

    fun openUsageSettings(): Boolean = launch(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + context.packageName)),
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    )

    fun openNotificationSettings(): Boolean = launch(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
    )

    private fun launch(primary: Intent, fallback: Intent): Boolean {
        for (intent in listOf(primary, fallback)) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: RuntimeException) { }
        }
        return false
    }
}
