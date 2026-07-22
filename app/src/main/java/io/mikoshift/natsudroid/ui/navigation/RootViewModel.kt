package io.mikoshift.natsudroid.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mikoshift.natsudroid.core.domain.usecase.ClearDocumentsOnLogoutUseCase
import io.mikoshift.natsudroid.core.domain.usecase.ObserveSessionUseCase
import io.mikoshift.natsudroid.core.model.AuthSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel
@Inject
constructor(
    observeSession: ObserveSessionUseCase,
    private val clearDocumentsOnLogout: ClearDocumentsOnLogoutUseCase,
) : ViewModel(),
    SessionHost {
    override val session: StateFlow<AuthSession?> = observeSession()

    override fun onSessionCleared() {
        viewModelScope.launch {
            clearDocumentsOnLogout()
        }
    }
}
