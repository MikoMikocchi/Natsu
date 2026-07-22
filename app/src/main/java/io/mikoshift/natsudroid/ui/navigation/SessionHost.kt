package io.mikoshift.natsudroid.ui.navigation

import io.mikoshift.natsudroid.core.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

interface SessionHost {
    val session: StateFlow<AuthSession?>

    fun onSessionCleared()
}
