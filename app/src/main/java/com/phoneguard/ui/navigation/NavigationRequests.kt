package com.phoneguard.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Something outside the UI (a notification or the share sheet) wants us to open. */
sealed interface PendingNavigation {
    /** Text shared from another app, e.g. an SMS. */
    data class SharedText(val text: String) : PendingNavigation

    /** Open the scan results. */
    data object OpenScan : PendingNavigation

    /** Open one app's details. */
    data class OpenApp(val packageName: String) : PendingNavigation
}

/** Hands navigation requests from MainActivity intents to the Compose navigation graph. */
@Singleton
class NavigationRequests @Inject constructor() {
    private val _pending = MutableStateFlow<PendingNavigation?>(null)
    val pending: StateFlow<PendingNavigation?> = _pending.asStateFlow()

    /** Queues a request; it replaces any request that was not handled yet. */
    fun submit(request: PendingNavigation) {
        _pending.value = request
    }

    /** Marks the current request as handled. */
    fun consume() {
        _pending.value = null
    }
}
