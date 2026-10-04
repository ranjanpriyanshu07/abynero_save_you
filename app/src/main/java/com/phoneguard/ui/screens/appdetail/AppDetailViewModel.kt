package com.phoneguard.ui.screens.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.repository.ScanRepository
import com.phoneguard.scanner.AppScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the app detail screen. */
sealed interface AppDetailUiState {
    data object Loading : AppDetailUiState
    data class Loaded(val report: AppRiskReport) : AppDetailUiState
    data object NotInstalled : AppDetailUiState
}

/** Loads one app's report and refreshes it after the user changes something in Settings. */
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scanRepository: ScanRepository,
    private val appScanner: AppScanner,
) : ViewModel() {

    /** Package name from the navigation route. */
    val packageName: String = savedStateHandle.get<String>(ARG_PACKAGE_NAME).orEmpty()

    private val _uiState = MutableStateFlow<AppDetailUiState>(AppDetailUiState.Loading)
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    private var fixPending = false

    init {
        load(fresh = false)
    }

    /** Remember that the user went to Settings or the uninstall dialog. */
    fun onFixLaunched() {
        fixPending = true
    }

    /** Reloads after returning from Settings so the screen shows the new state. */
    fun onResume() {
        if (!fixPending) return
        fixPending = false
        load(fresh = true)
        viewModelScope.launch { runCatching { scanRepository.recheckAfterFix() } }
    }

    private fun load(fresh: Boolean) {
        viewModelScope.launch {
            val report = try {
                if (fresh) appScanner.scanApp(packageName) else scanRepository.findApp(packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _uiState.value = report?.let { AppDetailUiState.Loaded(it) } ?: AppDetailUiState.NotInstalled
        }
    }

    companion object {
        const val ARG_PACKAGE_NAME = "packageName"
    }
}
