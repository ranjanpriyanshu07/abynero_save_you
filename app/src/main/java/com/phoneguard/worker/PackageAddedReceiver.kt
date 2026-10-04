package com.phoneguard.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Listens for newly installed apps and queues a quick scan of each one.
 *
 * Since Android 8, apps cannot receive `PACKAGE_ADDED` through the manifest, so this
 * receiver is registered at runtime from the Application. Apps installed while PhoneGuard's
 * process is not running are still caught by the daily scan.
 */
class PackageAddedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return
        ScanScheduler.enqueueNewAppScan(context, packageName)
    }

    companion object {
        /** Registers the receiver for the life of the process. */
        fun register(context: Context) {
            val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply { addDataScheme("package") }
            ContextCompat.registerReceiver(
                context.applicationContext,
                PackageAddedReceiver(),
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }
}
