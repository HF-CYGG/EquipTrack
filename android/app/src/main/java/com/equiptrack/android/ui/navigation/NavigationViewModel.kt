package com.equiptrack.android.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.session.SessionManager
import com.equiptrack.android.data.settings.SettingsRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    val sessionExpiredEvent = sessionManager.sessionExpiredEvent

    init {
        checkAndUploadFcmToken()
    }

    fun checkAndUploadFcmToken() {
        if (authRepository.isLoggedIn()) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        viewModelScope.launch {
                            authRepository.registerDeviceToken(token)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore if Firebase is not initialized or other errors
            }
        }
    }
}
