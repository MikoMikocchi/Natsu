package io.mikoshift.natsu.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRoutePolicyTest {

    @Test
    fun authenticatedRoutes_requireSession() {
        assertTrue(isAuthenticatedRoute(HomeRoute::class))
        assertTrue(isAuthenticatedRoute(ProfileRoute::class))
        assertTrue(isAuthenticatedRoute(ChangePasswordRoute::class))
        assertTrue(isAuthenticatedRoute(ReaderRoute::class))
        assertFalse(isAuthenticatedRoute(LoginRoute::class))
    }

    @Test
    fun authOnlyRoutes_redirectWhenSessionPresent() {
        assertTrue(isAuthOnlyRoute(LoginRoute::class))
        assertTrue(isAuthOnlyRoute(RegisterRoute::class))
        assertTrue(isAuthOnlyRoute(ForgotPasswordRoute::class))
        assertTrue(isAuthOnlyRoute(ResetPasswordRoute::class))
        assertFalse(isAuthOnlyRoute(HomeRoute::class))
    }

    @Test
    fun startDestinationForSession_returnsLoginWhenLoggedOut() {
        assertEquals(LoginRoute, startDestinationForSession(hasSession = false))
    }

    @Test
    fun startDestinationForSession_returnsHomeWhenLoggedIn() {
        assertEquals(HomeRoute, startDestinationForSession(hasSession = true))
    }

    @Test
    fun resolveSessionRedirect_sendsUnauthenticatedUsersToLoginFromProtectedRoutes() {
        assertEquals(
            SessionRedirect.ToLogin,
            resolveSessionRedirect(
                hasSession = false,
                onAuthenticatedRoute = true,
                onAuthOnlyRoute = false,
            ),
        )
    }

    @Test
    fun resolveSessionRedirect_sendsAuthenticatedUsersToHomeFromAuthRoutes() {
        assertEquals(
            SessionRedirect.ToHome,
            resolveSessionRedirect(
                hasSession = true,
                onAuthenticatedRoute = false,
                onAuthOnlyRoute = true,
            ),
        )
    }

    @Test
    fun resolveSessionRedirect_keepsUnauthenticatedUsersOnAuthRoutes() {
        assertNull(
            resolveSessionRedirect(
                hasSession = false,
                onAuthenticatedRoute = false,
                onAuthOnlyRoute = true,
            ),
        )
    }

    @Test
    fun resolveSessionRedirect_keepsAuthenticatedUsersOnProtectedRoutes() {
        assertNull(
            resolveSessionRedirect(
                hasSession = true,
                onAuthenticatedRoute = true,
                onAuthOnlyRoute = false,
            ),
        )
    }

    @Test
    fun resolveSessionRedirect_doesNothingWhenDestinationUnknown() {
        assertNull(
            resolveSessionRedirect(
                hasSession = true,
                onAuthenticatedRoute = false,
                onAuthOnlyRoute = false,
            ),
        )
    }
}
