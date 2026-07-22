package io.mikoshift.natsudroid.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mikoshift.natsudroid.feature.auth.R
import io.mikoshift.natsudroid.ui.theme.NatsudroidTheme

@Composable
fun ForgotPasswordScreen(viewModel: ForgotPasswordViewModel, onNavigateToLogin: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::submit,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
internal fun ForgotPasswordScreenContent(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.forgot_password_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.forgot_password_body),
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.generalError != null) {
            Text(
                text = uiState.generalError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (uiState.successMessage != null) {
            Text(
                text = uiState.successMessage.orEmpty(),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = onSubmit,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.send))
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.back_to_sign_in))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    NatsudroidTheme {
        ForgotPasswordScreenContent(
            uiState = ForgotPasswordUiState(email = "user@example.com"),
            onEmailChange = {},
            onSubmit = {},
            onNavigateToLogin = {},
        )
    }
}
