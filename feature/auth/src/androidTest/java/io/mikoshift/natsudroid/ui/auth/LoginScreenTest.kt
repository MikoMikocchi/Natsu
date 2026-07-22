package io.mikoshift.natsudroid.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mikoshift.natsudroid.ui.theme.NatsudroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysSignInTitle() {
        composeTestRule.setContent {
            NatsudroidTheme {
                LoginScreenContent(
                    uiState = LoginUiState(email = "test@example.com"),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Sign in").assertIsDisplayed()
        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
    }
}
