package com.phoneguard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneguard.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Decides whether to show onboarding. Null while the setting is still loading. */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: PreferencesRepository,
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> = preferences.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
