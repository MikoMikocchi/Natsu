package io.mikoshift.natsu.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlin.reflect.KClass

val authenticatedRouteClasses: Set<KClass<out Any>> = setOf(
    HomeRoute::class,
    ProfileRoute::class,
    ChangePasswordRoute::class,
)

val authOnlyRouteClasses: Set<KClass<out Any>> = setOf(
    LoginRoute::class,
    RegisterRoute::class,
    ForgotPasswordRoute::class,
    ResetPasswordRoute::class,
)

fun isAuthenticatedRoute(routeClass: KClass<*>): Boolean =
    routeClass in authenticatedRouteClasses

fun isAuthOnlyRoute(routeClass: KClass<*>): Boolean =
    routeClass in authOnlyRouteClasses

fun NavDestination.matchesAuthenticatedRoute(): Boolean =
    authenticatedRouteClasses.any { routeClass -> hasRoute(routeClass) }

fun NavDestination.matchesAuthOnlyRoute(): Boolean =
    authOnlyRouteClasses.any { routeClass -> hasRoute(routeClass) }

enum class SessionRedirect {
    ToLogin,
    ToHome,
}

fun startDestinationForSession(hasSession: Boolean): Any =
    if (hasSession) HomeRoute else LoginRoute

fun resolveSessionRedirect(
    hasSession: Boolean,
    onAuthenticatedRoute: Boolean,
    onAuthOnlyRoute: Boolean,
): SessionRedirect? = when {
    !hasSession && onAuthenticatedRoute -> SessionRedirect.ToLogin
    hasSession && onAuthOnlyRoute -> SessionRedirect.ToHome
    else -> null
}

fun resolveSessionRedirect(
    hasSession: Boolean,
    destination: NavDestination?,
): SessionRedirect? {
    if (destination == null) return null
    return resolveSessionRedirect(
        hasSession = hasSession,
        onAuthenticatedRoute = destination.matchesAuthenticatedRoute(),
        onAuthOnlyRoute = destination.matchesAuthOnlyRoute(),
    )
}
