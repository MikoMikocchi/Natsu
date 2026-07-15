package io.mikoshift.natsu.navigation

fun isAuthenticatedRoute(routeClass: kotlin.reflect.KClass<*>): Boolean =
    routeClass == HomeRoute::class ||
        routeClass == ProfileRoute::class ||
        routeClass == ChangePasswordRoute::class

fun isAuthOnlyRoute(routeClass: kotlin.reflect.KClass<*>): Boolean =
    routeClass == LoginRoute::class ||
        routeClass == RegisterRoute::class ||
        routeClass == ForgotPasswordRoute::class ||
        routeClass == ResetPasswordRoute::class
