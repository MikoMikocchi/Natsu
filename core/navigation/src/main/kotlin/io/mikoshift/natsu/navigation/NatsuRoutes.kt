package io.mikoshift.natsu.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data object ForgotPasswordRoute

@Serializable
data class ResetPasswordRoute(val token: String = "")

@Serializable
data object HomeRoute

@Serializable
data object ProfileRoute

@Serializable
data object ChangePasswordRoute
