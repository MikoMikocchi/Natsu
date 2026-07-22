package io.mikoshift.natsudroid.data.repository

import io.mikoshift.natsudroid.core.model.AuthError
import io.mikoshift.natsudroid.core.model.AuthSession
import io.mikoshift.natsudroid.data.local.TokenStore
import io.mikoshift.natsudroid.data.remote.NetworkFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenStore: TokenStore
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tokenStore = mockk(relaxed = true)
        every { tokenStore.sessionFlow } returns MutableStateFlow(null)
        coEvery { tokenStore.saveSession(any()) } returns Unit

        val networkFactory =
            NetworkFactory(
                baseUrl = mockWebServer.url("/v1/").toString(),
                rootBaseUrl = mockWebServer.url("/").toString(),
                isDebugBuild = false,
            )
        val unauthenticatedClient = networkFactory.createUnauthenticatedOkHttpClient()
        val api =
            networkFactory.createAuthApi(
                networkFactory.createRetrofit(unauthenticatedClient),
            )
        val oauthApi =
            networkFactory.createOAuthApi(
                networkFactory.createRootRetrofit(unauthenticatedClient),
            )
        val userInfoApi =
            networkFactory.createUserInfoApi(
                networkFactory.createRootRetrofit(unauthenticatedClient),
            )

        authRepository =
            AuthRepositoryImpl(
                unauthenticatedApi = api,
                authenticatedApi = api,
                oauthApi = oauthApi,
                userInfoApi = userInfoApi,
                tokenStore = tokenStore,
                networkFactory = networkFactory,
                clientId = "natsudroid-mobile",
            )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun login_success_persistsSession() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody(
                    """
                        {
                          "access_token": "access-token",
                          "refresh_token": "refresh-token",
                          "token_type": "Bearer",
                          "expires_in": 3600
                        }
                    """.trimIndent(),
                ).addHeader("Content-Type", "application/json"),
        )
        mockWebServer.enqueue(
            MockResponse()
                .setBody(
                    """
                        {
                          "sub": "1",
                          "email": "test@example.com",
                          "name": "Test User"
                        }
                    """.trimIndent(),
                ).addHeader("Content-Type", "application/json"),
        )

        val result = authRepository.login(email = "test@example.com", password = "password")

        assertTrue(result.isSuccess)
        coVerify {
            tokenStore.saveSession(
                AuthSession(
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    userId = 1L,
                    userName = "Test User",
                    userEmail = "test@example.com",
                ),
            )
        }
    }

    @Test
    fun register_success_doesNotPersistSession() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                        {
                          "user": {
                            "id": 1,
                            "name": "Test User",
                            "email": "test@example.com",
                            "created_at": "2026-01-01T00:00:00Z"
                          },
                          "server_time_ms": 1000
                        }
                    """.trimIndent(),
                ).addHeader("Content-Type", "application/json"),
        )

        val result =
            authRepository.register(
                name = "Test User",
                email = "test@example.com",
                password = "password",
                passwordConfirmation = "password",
            )

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { tokenStore.saveSession(any()) }
    }

    @Test
    fun login_unauthorized_returnsFailure() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val result = authRepository.login(email = "test@example.com", password = "wrong")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.Unauthorized)
    }
}
