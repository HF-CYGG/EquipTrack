package com.equiptrack.android.ui.navigation

import androidx.lifecycle.ViewModel
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.session.SessionManager
import com.equiptrack.android.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    val sessionExpiredEvent = sessionManager.sessionExpiredEvent
}
