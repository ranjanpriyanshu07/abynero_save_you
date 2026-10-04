package com.phoneguard.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoneguard.domain.repository.ScanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Scans a single newly installed app and warns the user if it is risky.
 */
@HiltWorker
class NewAppScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val scanRepository: ScanRepository,
    private val notifier: RiskNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        return try {
            scanRepository.checkNewApp(packageName)?.let(notifier::notifyRiskyNewApp)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_PACKAGE_NAME = "package_name"
    }
}
