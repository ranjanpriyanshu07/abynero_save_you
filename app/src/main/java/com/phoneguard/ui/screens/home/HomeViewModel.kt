package com.phoneguard.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.model.LinkStats
import com.phoneguard.domain.model.ScanProgress
import com.phoneguard.domain.model.ScoreCategory
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.repository.HistoryRepository
import com.phoneguard.domain.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Everything the home screen shows. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val progress: ScanProgress? = null,
    val score: Int? = null,
    /** Changes after every scan so the gauge animates again. */
    val scoreKey: Long = 0L,
    val lastScanAt: Long? = null,
    val categoryScores: Map<ScoreCategory, Int> = emptyMap(),
    val appsScanned: Int? = null,
    val riskyApps: Int? = null,
    val deviceIssues: Int? = null,
    val wifi: WifiCheckResult? = null,
    val linkStats: LinkStats = LinkStats(),
    val hasDetails: Boolean = false,
    val scanFailed: Boolean = false,
) {
    /** Number of things the user should look at. */
    val problemCount: Int get() = (riskyApps ?: 0) + (deviceIssues ?: 0) + if (wifi?.security?.isUnsafe == true) 1 else 0
}

/** Home screen state: live scan progress, the latest score and its breakdown. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    historyRepository: HistoryRepository,
    clock: Clock,
) : ViewModel() {

    private val weekAgo = clock.millis() - TimeUnit.DAYS.toMillis(7)

    val uiState: StateFlow<HomeUiState> = combine(
        scanRepository.state,
        historyRepository.observeLatestScan(),
        historyRepository.observeLinkStats(weekAgo),
    ) { scan, latest, links ->
        val result = scan.result
        when {
            result != null -> HomeUiState(
                isLoading = false,
                isScanning = scan.isScanning,
                progress = scan.progress,
                score = result.score.overall,
                scoreKey = result.timestamp,
                lastScanAt = result.timestamp,
                categoryScores = result.score.categories.associate { it.category to it.score },
                appsScanned = result.apps.size,
                riskyApps = result.riskyApps.size,
                deviceIssues = result.deviceIssueCount,
                wifi = result.wifi,
                linkStats = links,
                hasDetails = true,
                scanFailed = scan.lastScanFailed,
            )
            latest != null -> HomeUiState(
                isLoading = false,
                isScanning = scan.isScanning,
                progress = scan.progress,
                score = latest.score,
                scoreKey = latest.timestamp,
                lastScanAt = latest.timestamp,
                categoryScores = mapOf(
                    ScoreCategory.APPS to latest.appsScore,
                    ScoreCategory.DEVICE to latest.deviceScore,
                    ScoreCategory.NETWORK to latest.networkScore,
                    ScoreCategory.LINKS to latest.linksScore,
                ),
                appsScanned = latest.appsScanned,
                riskyApps = latest.highRiskApps + latest.mediumRiskApps,
                deviceIssues = latest.deviceIssues,
                linkStats = links,
                hasDetails = false,
                scanFailed = scan.lastScanFailed,
            )
            else -> HomeUiState(
                isLoading = false,
                isScanning = scan.isScanning,
                progress = scan.progress,
                linkStats = links,
                scanFailed = scan.lastScanFailed,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Starts a manual scan. */
    fun onScanClick() = scanRepository.startScan()

    /** Re-checks quietly when the user comes back from a Settings screen. */
    fun refreshAfterFix() {
        viewModelScope.launch { runCatching { scanRepository.recheckAfterFix() } }
    }
}
