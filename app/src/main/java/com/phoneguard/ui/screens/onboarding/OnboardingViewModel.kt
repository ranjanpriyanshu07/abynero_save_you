package com.phoneguard.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Saves that onboarding is finished. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {

    /** Marks onboarding as done, then calls [onDone] on the main thread. */
    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { preferences.setOnboardingCompleted() }
            onDone()
        }
    }
}
