package io.mikoshift.natsu.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mikoshift.natsu.core.domain.usecase.ClearDocumentsOnLogoutUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveSessionUseCase
import io.mikoshift.natsu.core.model.AuthSession
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RootViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val clearDocumentsOnLogout: ClearDocumentsOnLogoutUseCase,
) : ViewModel() {

    val session: StateFlow<AuthSession?> = observeSession()

    fun onSessionCleared() {
        viewModelScope.launch {
            clearDocumentsOnLogout()
        }
    }
}
