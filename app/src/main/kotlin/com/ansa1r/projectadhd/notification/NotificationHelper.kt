package com.ansa1r.projectadhd.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ansa1r.projectadhd.MainActivity
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.intervention.InterventionDecision
import com.ansa1r.projectadhd.monitoring.UsageMonitoringService

class NotificationHelper(private val context: Context) {
    companion object {
        const val MONITORING_CHANNEL = "monitoring"
        const val INTERVENTION_CHANNEL = "interventions"
        const val MONITORING_ID = 1
        private const val INTERVENTION_ID = 2
        private const val TEST_ID = 3
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
        manager.createNotificationChannels(listOf(
            NotificationChannel(MONITORING_CHANNEL, context.getString(R.string.channel_monitoring), NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(INTERVENTION_CHANNEL, context.getString(R.string.channel_interventions), NotificationManager.IMPORTANCE_DEFAULT)
        ))
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun monitoringNotification(): Notification {
        val stop = PendingIntent.getService(context, 1,
            Intent(context, UsageMonitoringService::class.java).setAction(UsageMonitoringService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, MONITORING_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.monitoring_notification_title))
            .setContentText(context.getString(R.string.monitoring_notification_body))
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(R.drawable.ic_notification, context.getString(R.string.stop_monitoring), stop)
            .build()
    }

    fun intervention(decision: InterventionDecision.Notify): Boolean {
        val body = context.getString(R.string.intervention_body,
            decision.appName, decision.sessionDurationMillis / 60_000, decision.incompleteHabitCount)
        return post(INTERVENTION_ID, context.getString(R.string.intervention_title), body)
    }

    fun test(): Boolean = post(TEST_ID, context.getString(R.string.test_notification_title),
        context.getString(R.string.test_notification_body))

    private fun post(id: Int, title: String, body: String): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 26 && context.getSystemService(NotificationManager::class.java)
                ?.getNotificationChannel(INTERVENTION_CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return try {
            manager.notify(id, NotificationCompat.Builder(context, INTERVENTION_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(openApp())
                .setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
            true
        } catch (_: SecurityException) { false }
    }
}
