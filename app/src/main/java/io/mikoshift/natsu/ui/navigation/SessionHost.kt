package io.mikoshift.natsu.ui.navigation

import io.mikoshift.natsu.core.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

interface SessionHost {
    val session: StateFlow<AuthSession?>
    fun onSessionCleared()
}
