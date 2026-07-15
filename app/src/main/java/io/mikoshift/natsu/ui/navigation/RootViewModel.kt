package io.mikoshift.natsu.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.model.AuthSession
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    val session: StateFlow<AuthSession?> = authRepository.currentSession

    fun onSessionCleared() {
        viewModelScope.launch {
            documentRepository.clearOnLogout()
        }
    }
}
