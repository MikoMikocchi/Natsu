package io.mikoshift.natsu.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mikoshift.natsu.core.testing.fixture.AuthFixtures
import io.mikoshift.natsu.navigation.HomeRoute
import io.mikoshift.natsu.navigation.LoginRoute
import io.mikoshift.natsu.navigation.ProfileRoute
import io.mikoshift.natsu.navigation.ReaderRoute
import io.mikoshift.natsu.navigation.RegisterRoute
import io.mikoshift.natsu.ui.theme.NatsuTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NatsuNavHostTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun startsAtLoginWhenNoSession() {
        val sessionHost = SessionHostFixtures.loggedOutHost()

        composeTestRule.setContent {
            NatsuTheme {
                NatsuNavHost(
                    sessionHost = sessionHost,
                    navGraphBuilder = { stubNavGraph() },
                )
            }
        }

        composeTestRule.onNodeWithText("Login screen").assertIsDisplayed()
    }

    @Test
    fun startsAtHomeWhenSessionPresent() {
        val sessionHost = SessionHostFixtures.loggedInHost()

        composeTestRule.setContent {
            NatsuTheme {
                NatsuNavHost(
                    sessionHost = sessionHost,
                    navGraphBuilder = { stubNavGraph() },
                )
            }
        }

        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
    }

    @Test
    fun redirectsToLoginWhenSessionClearedOnHome() {
        val sessionHost = SessionHostFixtures.loggedInHost()

        composeTestRule.setContent {
            NatsuTheme {
                NatsuNavHost(
                    sessionHost = sessionHost,
                    navGraphBuilder = { stubNavGraph() },
                )
            }
        }

        composeTestRule.onNodeWithText("Library").assertIsDisplayed()

        composeTestRule.runOnIdle {
            sessionHost.setSession(null)
        }

        composeTestRule.onNodeWithText("Login screen").assertIsDisplayed()
    }

    @Test
    fun redirectsToHomeWhenSessionAppearsOnLogin() {
        val sessionHost = SessionHostFixtures.loggedOutHost()

        composeTestRule.setContent {
            NatsuTheme {
                NatsuNavHost(
                    sessionHost = sessionHost,
                    navGraphBuilder = { stubNavGraph() },
                )
            }
        }

        composeTestRule.onNodeWithText("Login screen").assertIsDisplayed()

        composeTestRule.runOnIdle {
            sessionHost.setSession(AuthFixtures.session())
        }

        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
    }

    @Test
    fun clearsDocumentsWhenSessionBecomesNull() {
        val sessionHost = SessionHostFixtures.loggedInHost()

        composeTestRule.setContent {
            NatsuTheme {
                NatsuNavHost(
                    sessionHost = sessionHost,
                    navGraphBuilder = { stubNavGraph() },
                )
            }
        }

        composeTestRule.runOnIdle {
            sessionHost.setSession(null)
        }

        composeTestRule.runOnIdle {
            assertEquals(1, sessionHost.sessionClearedCalls)
        }
    }

    private fun NavGraphBuilder.stubNavGraph() {
        composable<LoginRoute> { Text("Login screen") }
        composable<RegisterRoute> { Text("Register screen") }
        composable<HomeRoute> { Text("Library") }
        composable<ProfileRoute> { Text("Profile") }
        composable<ReaderRoute> { Text("Reader") }
    }
}
