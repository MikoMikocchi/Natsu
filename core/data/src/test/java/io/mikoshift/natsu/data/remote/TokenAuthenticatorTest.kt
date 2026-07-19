package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.data.local.TokenStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenStore: TokenStore
    private lateinit var tokenAuthenticator: TokenAuthenticator

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tokenStore = mockk(relaxed = true)
        every { tokenStore.getRefreshTokenBlocking() } returns REFRESH_TOKEN
        every { tokenStore.getSessionBlocking() } returns EXISTING_SESSION

        val networkFactory =
            NetworkFactory(
                baseUrl = mockWebServer.url("/v1/").toString(),
                rootBaseUrl = mockWebServer.url("/").toString(),
                isDebugBuild = false,
            )
        val oauthApi = networkFactory.createUnauthenticatedOAuthApi()
        tokenAuthenticator =
            TokenAuthenticator(
                tokenStore = tokenStore,
                oauthApi = oauthApi,
                clientId = CLIENT_ID,
            )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun authenticate_success_returnsRequestWithNewAccessToken() {
        enqueueRefreshResponse(accessToken = NEW_ACCESS_TOKEN)

        val unauthorizedResponse = unauthorizedResponse()
        val retriedRequest = tokenAuthenticator.authenticate(route = null, response = unauthorizedResponse)

        assertNotNull(retriedRequest)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", retriedRequest!!.header("Authorization"))
        verify(exactly = 1) { tokenStore.saveSessionBlocking(any()) }
    }

    @Test
    fun authenticate_parallel401_performsSingleRefresh() {
        val refreshStarted = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val refreshRequestCount = AtomicInteger(0)

        mockWebServer.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (!request.path!!.endsWith("oauth2/token")) {
                        return MockResponse().setResponseCode(404)
                    }

                    refreshRequestCount.incrementAndGet()
                    refreshStarted.countDown()
                    releaseRefresh.await(2, TimeUnit.SECONDS)

                    return refreshResponse(accessToken = NEW_ACCESS_TOKEN)
                }
            }

        val unauthorizedResponse = unauthorizedResponse()
        val executor = Executors.newFixedThreadPool(PARALLEL_CALLS)
        val done = CountDownLatch(PARALLEL_CALLS)

        repeat(PARALLEL_CALLS) {
            executor.submit {
                try {
                    tokenAuthenticator.authenticate(route = null, response = unauthorizedResponse)
                } finally {
                    done.countDown()
                }
            }
        }

        refreshStarted.await(2, TimeUnit.SECONDS)
        releaseRefresh.countDown()
        done.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(1, refreshRequestCount.get())
        verify(exactly = 1) { tokenStore.saveSessionBlocking(any()) }
    }

    private fun enqueueRefreshResponse(accessToken: String) {
        mockWebServer.enqueue(refreshResponse(accessToken))
    }

    private fun refreshResponse(accessToken: String): MockResponse = MockResponse()
        .setBody(
            """
                {
                  "access_token": "$accessToken",
                  "refresh_token": "rotated-refresh-token",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
            """.trimIndent(),
        ).addHeader("Content-Type", "application/json")

    private fun unauthorizedResponse(): Response {
        val request =
            Request
                .Builder()
                .url(mockWebServer.url("/v1/documents"))
                .header("Authorization", "Bearer expired-token")
                .build()

        return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }

    private companion object {
        const val CLIENT_ID = "natsu-mobile"
        const val REFRESH_TOKEN = "refresh-token"
        const val NEW_ACCESS_TOKEN = "new-access-token"
        const val PARALLEL_CALLS = 8
        val EXISTING_SESSION =
            AuthSession(
                accessToken = "old-access-token",
                refreshToken = REFRESH_TOKEN,
                userId = 1L,
                userName = "Test User",
                userEmail = "test@example.com",
            )
    }
}
