package io.mikoshift.natsu.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRoutePolicyTest {

    @Test
    fun authenticatedRoutes_requireSession() {
        assertTrue(isAuthenticatedRoute(HomeRoute::class))
        assertTrue(isAuthenticatedRoute(ProfileRoute::class))
        assertTrue(isAuthenticatedRoute(ChangePasswordRoute::class))
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
}
