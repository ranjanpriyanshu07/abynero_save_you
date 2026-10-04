package com.phoneguard.data.repository

import com.phoneguard.di.ApplicationScope
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.LinkStats
import com.phoneguard.domain.model.ScanOutcome
import com.phoneguard.domain.model.ScanProgress
import com.phoneguard.domain.model.ScanResult
import com.phoneguard.domain.model.ScanStage
import com.phoneguard.domain.model.ScanState
import com.phoneguard.domain.model.ScanTrigger
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.repository.HistoryRepository
import com.phoneguard.domain.repository.PreferencesRepository
import com.phoneguard.domain.repository.ScanRepository
import com.phoneguard.domain.scoring.RiskKeys
import com.phoneguard.domain.scoring.SecurityScoreCalculator
import com.phoneguard.scanner.AppScanner
import com.phoneguard.scanner.DeviceSettingsScanner
import com.phoneguard.scanner.WifiScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the scanners, calculates the score, saves history and keeps the latest result in
 * memory for every screen.
 *
 * Scans are serialised with a [Mutex], so a background scan and a manual scan never run at
 * the same time.
 */
@Singleton
class DefaultScanRepository @Inject constructor(
    private val appScanner: AppScanner,
    private val deviceScanner: DeviceSettingsScanner,
    private val wifiScanner: WifiScanner,
    private val calculator: SecurityScoreCalculator,
    private val historyRepository: HistoryRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) : ScanRepository {

    private val mutex = Mutex()
    private val _state = MutableStateFlow(ScanState())
    override val state: StateFlow<ScanState> = _state.asStateFlow()

    override fun startScan() {
        if (_state.value.isScanning) return
        _state.update { it.copy(isScanning = true, progress = ScanProgress(ScanStage.APPS), lastScanFailed = false) }
        appScope.launch {
            try {
                runFullScan(ScanTrigger.MANUAL)
            } catch (e: CancellationException) {
                _state.update { it.copy(isScanning = false, progress = null) }
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isScanning = false, progress = null, lastScanFailed = true) }
            }
        }
    }

    override suspend fun runFullScan(trigger: ScanTrigger): ScanOutcome = mutex.withLock {
        val showPacing = trigger == ScanTrigger.MANUAL
        try {
            setProgress(ScanProgress(ScanStage.APPS))
            val startedAt = clock.millis()
            val apps = appScanner.scanInstalledApps { done, total ->
                setProgress(ScanProgress(ScanStage.APPS, done, total))
            }
            if (showPacing) holdAtLeast(startedAt, MIN_APP_STAGE_MS)

            setProgress(ScanProgress(ScanStage.DEVICE))
            val device = deviceScanner.scan()
            if (showPacing) delay(STAGE_PAUSE_MS)

            setProgress(ScanProgress(ScanStage.NETWORK))
            val wifi = wifiScanner.scan()
            if (showPacing) delay(STAGE_PAUSE_MS)

            setProgress(ScanProgress(ScanStage.SCORING))
            val links = historyRepository.linkStatsSince(weekAgo())
            val result = buildResult(apps, device, wifi, links)
            historyRepository.saveScan(result, trigger)
            val newKeys = updateKnownRisks(result)

            _state.value = ScanState(isScanning = false, progress = null, result = result)
            ScanOutcome(result, newKeys)
        } catch (e: Throwable) {
            _state.update { it.copy(isScanning = false, progress = null) }
            throw e
        }
    }

    override suspend fun findApp(packageName: String): AppRiskReport? =
        _state.value.result?.apps?.firstOrNull { it.packageName == packageName }
            ?: appScanner.scanApp(packageName)

    override suspend fun recheckAfterFix() {
        if (_state.value.result == null || _state.value.isScanning) return
        mutex.withLock {
            val apps = appScanner.scanInstalledApps()
            val device = deviceScanner.scan()
            val wifi = wifiScanner.scan()
            val links = historyRepository.linkStatsSince(weekAgo())
            val result = buildResult(apps, device, wifi, links)
            updateKnownRisks(result)
            _state.update { it.copy(result = result) }
        }
    }

    override suspend fun checkNewApp(packageName: String): AppRiskReport? {
        val report = appScanner.scanApp(packageName) ?: return null
        mutex.withLock {
            _state.value.result?.let { previous ->
                val apps = (previous.apps.filterNot { it.packageName == packageName } + report)
                    .sortedByDescending { it.level }
                val updated = buildResult(apps, previous.deviceChecks, previous.wifi, previous.linkStats)
                _state.update { it.copy(result = updated.copy(timestamp = previous.timestamp)) }
            }
            if (!report.level.needsAttention) return null
            val key = RiskKeys.forApp(packageName)
            val known = preferences.knownRiskKeys()
            if (key in known) return null
            preferences.setKnownRiskKeys(known + key)
        }
        return report
    }

    private fun buildResult(
        apps: List<AppRiskReport>,
        device: List<DeviceCheckResult>,
        wifi: WifiCheckResult,
        links: LinkStats,
    ): ScanResult = ScanResult(
        timestamp = clock.millis(),
        apps = apps,
        deviceChecks = device,
        wifi = wifi,
        linkStats = links,
        score = calculator.calculate(apps, device, wifi, links.dangerous),
    )

    private suspend fun updateKnownRisks(result: ScanResult): Set<String> {
        val current = RiskKeys.from(result)
        val newKeys = RiskKeys.newKeys(current, preferences.knownRiskKeys())
        preferences.setKnownRiskKeys(current)
        return newKeys
    }

    private fun setProgress(progress: ScanProgress) {
        _state.update { it.copy(isScanning = true, progress = progress, lastScanFailed = false) }
    }

    private suspend fun holdAtLeast(startedAt: Long, minMillis: Long) {
        val remaining = minMillis - (clock.millis() - startedAt)
        if (remaining > 0) delay(remaining)
    }

    private fun weekAgo(): Long = clock.millis() - TimeUnit.DAYS.toMillis(7)

    private companion object {
        /** Short pauses so the progress animation is readable on fast phones. */
        const val MIN_APP_STAGE_MS = 1_200L
        const val STAGE_PAUSE_MS = 450L
    }
}
