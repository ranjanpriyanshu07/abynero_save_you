package com.phoneguard.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoneguard.domain.model.ScanTrigger
import com.phoneguard.domain.repository.ScanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Runs a full scan once a day and notifies only about risks that are new since the last scan.
 */
@HiltWorker
class DailyScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val scanRepository: ScanRepository,
    private val notifier: RiskNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val outcome = scanRepository.runFullScan(ScanTrigger.SCHEDULED)
        notifier.notifyNewRisks(outcome)
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}
