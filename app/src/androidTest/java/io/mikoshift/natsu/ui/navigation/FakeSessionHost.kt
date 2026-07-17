package io.mikoshift.natsu.ui.navigation

import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.core.testing.fixture.AuthFixtures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSessionHost(
    initialSession: AuthSession? = null,
) : SessionHost {
    private val _session = MutableStateFlow(initialSession)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()
    var sessionClearedCalls: Int = 0
        private set

    fun setSession(session: AuthSession?) {
        _session.value = session
    }

    override fun onSessionCleared() {
        sessionClearedCalls++
    }
}

object SessionHostFixtures {
    fun loggedInHost(session: AuthSession = AuthFixtures.session()) =
        FakeSessionHost(initialSession = session)

    fun loggedOutHost() = FakeSessionHost(initialSession = null)
}
