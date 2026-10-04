package com.phoneguard.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules background scans with WorkManager. */
@Singleton
class ScanScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Schedules the daily scan. Keeps the existing schedule if there is one. */
    fun scheduleDailyScan() {
        val request = PeriodicWorkRequestBuilder<DailyScanWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(DAILY_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        private const val DAILY_WORK_NAME = "daily_security_scan"

        /** Queues a quick scan of one newly installed app. */
        fun enqueueNewAppScan(context: Context, packageName: String) {
            val request = OneTimeWorkRequestBuilder<NewAppScanWorker>()
                .setInputData(workDataOf(NewAppScanWorker.KEY_PACKAGE_NAME to packageName))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("new_app_$packageName", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
