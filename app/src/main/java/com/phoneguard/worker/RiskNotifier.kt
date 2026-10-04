package com.phoneguard.worker

import android.Manifest
import android.annotation.SuppressLint
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
import com.phoneguard.MainActivity
import com.phoneguard.R
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.ScanOutcome
import com.phoneguard.domain.scoring.RiskKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows notifications, but only for new and risky findings.
 */
@Singleton
class RiskNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Creates the notification channel (safe to call many times). */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Notifies about risks found by a background scan that were not there before. */
    fun notifyNewRisks(outcome: ScanOutcome) {
        val count = outcome.newRiskKeys.size
        if (count == 0) return
        val firstApp = outcome.newRiskKeys
            .firstNotNullOfOrNull { key -> RiskKeys.packageFromKey(key) }
            ?.let { pkg -> outcome.result.apps.firstOrNull { it.packageName == pkg } }
        val text = if (firstApp != null) {
            context.getString(R.string.notification_new_risks_with_app, count, firstApp.appName)
        } else {
            context.getString(R.string.notification_new_risks_text, count)
        }
        show(
            id = NOTIFICATION_ID_SCAN,
            title = context.getString(R.string.notification_new_risks_title),
            text = text,
            intent = MainActivity.openScanIntent(context),
        )
    }

    /** Notifies that a just-installed app looks risky. */
    fun notifyRiskyNewApp(report: AppRiskReport) {
        show(
            id = report.packageName.hashCode(),
            title = context.getString(R.string.notification_new_app_title, report.appName),
            text = context.getString(R.string.notification_new_app_text),
            intent = MainActivity.openAppIntent(context, report.packageName),
        )
    }

    @SuppressLint("MissingPermission")
    private fun show(id: Int, title: String, text: String, intent: Intent) {
        if (!canNotify()) return
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(ContextCompat.getColor(context, R.color.brand_teal))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the call; nothing else to do.
        }
    }

    private fun canNotify(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val CHANNEL_ID = "risk_alerts"
        const val NOTIFICATION_ID_SCAN = 1001
    }
}
