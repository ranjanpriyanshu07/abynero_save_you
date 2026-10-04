package com.phoneguard

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.phoneguard.worker.PackageAddedReceiver
import com.phoneguard.worker.RiskNotifier
import com.phoneguard.worker.ScanScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point: sets up Hilt, WorkManager, the notification channel, the daily
 * scan and the new-app listener.
 */
@HiltAndroidApp
class PhoneGuardApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var scanScheduler: ScanScheduler

    @Inject lateinit var riskNotifier: RiskNotifier

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        riskNotifier.ensureChannel()
        scanScheduler.scheduleDailyScan()
        PackageAddedReceiver.register(this)
    }
}
