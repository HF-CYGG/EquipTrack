package com.equiptrack.android.ui.onboarding

import androidx.lifecycle.ViewModel
import com.equiptrack.android.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
    }
}
