package com.phoneguard.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.model.ScanHistoryEntry
import com.phoneguard.domain.repository.HistoryRepository
import com.phoneguard.domain.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the history screen. */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val entries: List<ScanHistoryEntry> = emptyList(),
    /** Scores for the chart, oldest first. */
    val trend: List<Int> = emptyList(),
) {
    val latest: ScanHistoryEntry? get() = entries.firstOrNull()
    val best: Int? get() = entries.maxOfOrNull { it.score }

    /** Change from the previous scan to the latest one, or null with fewer than two scans. */
    val change: Int? get() = if (entries.size >= 2) entries[0].score - entries[1].score else null
}

/** Saved scans and the score trend. */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val scanRepository: ScanRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = historyRepository.observeScans()
        .map { entries ->
            HistoryUiState(
                isLoading = false,
                entries = entries,
                trend = entries.take(TREND_POINTS).map { it.score }.reversed(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    /** Deletes all saved scans. */
    fun clearHistory() {
        viewModelScope.launch { runCatching { historyRepository.clearScans() } }
    }

    /** Starts a new scan. */
    fun startScan() = scanRepository.startScan()

    private companion object {
        const val TREND_POINTS = 30
    }
}
