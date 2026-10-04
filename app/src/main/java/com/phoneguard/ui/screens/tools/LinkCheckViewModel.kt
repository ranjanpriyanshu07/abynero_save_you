package com.phoneguard.ui.screens.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.model.MessageCheckResult
import com.phoneguard.domain.repository.HistoryRepository
import com.phoneguard.scanner.MessageChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the link and message checker. */
data class LinkCheckUiState(
    val input: String = "",
    val isChecking: Boolean = false,
    val result: MessageCheckResult? = null,
    val failed: Boolean = false,
)

/** Checks pasted, shared or scanned text for dangerous links and scam phrases. */
@HiltViewModel
class LinkCheckViewModel @Inject constructor(
    private val messageChecker: MessageChecker,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkCheckUiState())
    val uiState: StateFlow<LinkCheckUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null

    /** Updates the text box. Clears an old result so it is never shown for new text. */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text, result = null, failed = false) }
    }

    /** Fills the box with shared or scanned text and checks it straight away. */
    fun onIncomingText(text: String) {
        onInputChange(text)
        check()
    }

    /** Checks the current text. */
    fun check() {
        val text = _uiState.value.input
        if (text.isBlank()) return
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, result = null, failed = false) }
            val result = try {
                messageChecker.check(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            result?.links?.forEach { link ->
                runCatching { historyRepository.recordLinkCheck(link.analysis.host, link.level) }
            }
            _uiState.update { it.copy(isChecking = false, result = result, failed = result == null) }
        }
    }
}
