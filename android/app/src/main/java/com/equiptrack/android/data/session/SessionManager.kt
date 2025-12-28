package com.equiptrack.android.data.session

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _sessionExpiredChannel = Channel<Unit>(Channel.BUFFERED)
    val sessionExpiredEvent = _sessionExpiredChannel.receiveAsFlow()

    fun onSessionExpired() {
        _sessionExpiredChannel.trySend(Unit)
    }
}
