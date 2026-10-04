package com.phoneguard.ui.screens.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.scanner.WifiScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the Wi-Fi safety screen. */
data class WifiUiState(
    val isLoading: Boolean = true,
    val result: WifiCheckResult? = null,
)

/** Runs the Wi-Fi check on demand. */
@HiltViewModel
class WifiViewModel @Inject constructor(
    private val wifiScanner: WifiScanner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Checks the current Wi-Fi again. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = WifiUiState(isLoading = false, result = wifiScanner.scan())
        }
    }
}
