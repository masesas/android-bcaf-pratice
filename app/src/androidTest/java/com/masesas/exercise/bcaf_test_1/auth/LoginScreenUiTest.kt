@file:OptIn(ExperimentalTestApi::class)

package com.masesas.exercise.bcaf_test_1.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.data.auth.local.AuthSessionLocalDataSource
import com.masesas.exercise.bcaf_test_1.presentation.compose.HomeActivityCompose
import com.masesas.exercise.bcaf_test_1.presentation.compose.auth.LoginTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

private const val STEP_DELAY_MILLIS = 2_000L
private const val NETWORK_TIMEOUT_MILLIS = 15_000L
private const val UNKNOWN_EMAIL = "unknown.user@masesas.test"
private const val WRONG_PASSWORD = "wrong-password"

class ClearAuthSessionRule : ExternalResource() {

    private val localDataSource by lazy {
        AuthSessionLocalDataSource(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    override fun before() = runBlocking { localDataSource.clear() }

    override fun after() = runBlocking { localDataSource.clear() }
}

private typealias HomeComposeRule =
    AndroidComposeTestRule<ActivityScenarioRule<HomeActivityCompose>, HomeActivityCompose>

private fun pause() = Thread.sleep(STEP_DELAY_MILLIS)

private fun inputOf(tag: String): SemanticsMatcher = hasSetTextAction() and hasAnyAncestor(hasTestTag(tag))

private fun HomeComposeRule.string(resId: Int): String = activity.getString(resId)

private fun HomeComposeRule.tapLoginButton() {
    onNodeWithText(string(R.string.login_action)).performClick()
}

private fun HomeComposeRule.awaitText(text: String) {
    waitUntilAtLeastOneExists(hasText(text), NETWORK_TIMEOUT_MILLIS)
}

@RunWith(Enclosed::class)
class LoginScreenUiTest {

    @RunWith(AndroidJUnit4::class)
    class Login {

        @get:Rule(order = 0)
        val clearAuthSessionRule = ClearAuthSessionRule()

        @get:Rule(order = 1)
        val composeRule = createAndroidComposeRule<HomeActivityCompose>()

        @Test
        fun opensMainScreenWhenLoggingInWithDebugCredentials() {
            //composeRule.awaitText(composeRule.string(R.string.login_title))
            composeRule.awaitText("Login")
            pause()

            composeRule.tapLoginButton()
            pause()

            composeRule.waitUntilAtLeastOneExists(
                hasText("Selamat Datang"),
                NETWORK_TIMEOUT_MILLIS,
            )
            pause()
        }

        @Test
        fun showsServerMessageWhenCredentialsAreRejected() {
            composeRule.awaitText(composeRule.string(R.string.login_title))
            pause()
            composeRule.onNode(inputOf(LoginTestTags.EMAIL)).performTextReplacement(UNKNOWN_EMAIL)
            pause()
            composeRule.onNode(inputOf(LoginTestTags.PASSWORD)).performTextReplacement(WRONG_PASSWORD)
            pause()

            composeRule.tapLoginButton()

            composeRule.awaitText("Username atau password salah")
            composeRule.onNodeWithText(composeRule.string(R.string.login_action)).assertExists()
            pause()
        }

        @Test
        fun showsValidationMessageWhenPasswordIsEmpty() {
            composeRule.awaitText(composeRule.string(R.string.login_title))
            pause()
            composeRule.onNode(inputOf(LoginTestTags.PASSWORD)).performTextClearance()
            pause()

            composeRule.tapLoginButton()

            composeRule.awaitText("Email dan password tidak boleh kosong")
            pause()
        }

        @Test
        fun showsValidationMessageWhenEmailIsEmpty() {
            composeRule.awaitText(composeRule.string(R.string.login_title))
            pause()
            composeRule.onNode(inputOf(LoginTestTags.EMAIL)).performTextClearance()
            pause()

            composeRule.tapLoginButton()

            composeRule.awaitText("Email dan password tidak boleh kosong")
            pause()
        }
    }
}
