package com.phoneguard.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.CheckStatus
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanProgress
import com.phoneguard.domain.model.ScanResult
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row in the results list. */
sealed interface ResultItem {
    val key: String
    val level: RiskLevel

    data class App(val report: AppRiskReport) : ResultItem {
        override val key: String get() = "app:${report.packageName}"
        override val level: RiskLevel get() = report.level
    }

    data class Device(val check: DeviceCheckResult) : ResultItem {
        override val key: String get() = "device:${check.type}"
        override val level: RiskLevel get() = if (check.status == CheckStatus.PASSED) RiskLevel.SAFE else check.level
    }

    data class Wifi(val wifi: WifiCheckResult) : ResultItem {
        override val key: String get() = "wifi"
        override val level: RiskLevel get() = wifi.level
    }
}

/** Results with the same severity. */
data class ResultGroup(val level: RiskLevel, val items: List<ResultItem>)

/** State for the Scan tab. */
data class ScanUiState(
    val isScanning: Boolean = false,
    val progress: ScanProgress? = null,
    val result: ScanResult? = null,
    val filter: RiskLevel? = null,
    val groups: List<ResultGroup> = emptyList(),
    val counts: Map<RiskLevel, Int> = emptyMap(),
    val scanFailed: Boolean = false,
)

/** Scan progress and results grouped by severity. */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<RiskLevel?>(null)
    private var fixPending = false

    val uiState: StateFlow<ScanUiState> = combine(scanRepository.state, filter) { scan, selected ->
        val result = scan.result
        val items = result?.let { buildItems(it) }.orEmpty()
        val counts = items.groupingBy { it.level }.eachCount()
        val groups = RiskLevel.entries.reversed()
            .filter { selected == null || it == selected }
            .mapNotNull { level ->
                items.filter { it.level == level }.takeIf { it.isNotEmpty() }?.let { ResultGroup(level, it) }
            }
        ScanUiState(
            isScanning = scan.isScanning,
            progress = scan.progress,
            result = result,
            filter = selected,
            groups = groups,
            counts = counts,
            scanFailed = scan.lastScanFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    /** Starts a new scan. */
    fun startScan() = scanRepository.startScan()

    /** Shows only one severity, or everything when [level] is null. */
    fun setFilter(level: RiskLevel?) {
        filter.value = level
    }

    /** Remember that the user went to Settings to fix something. */
    fun onFixLaunched() {
        fixPending = true
    }

    /** Called when the screen is visible again; re-checks if the user fixed something. */
    fun onResume() {
        if (!fixPending) return
        fixPending = false
        viewModelScope.launch { runCatching { scanRepository.recheckAfterFix() } }
    }

    private fun buildItems(result: ScanResult): List<ResultItem> = buildList {
        result.deviceChecks.forEach { add(ResultItem.Device(it)) }
        if (result.wifi.isWifiConnected) add(ResultItem.Wifi(result.wifi))
        result.apps.forEach { add(ResultItem.App(it)) }
    }
}
