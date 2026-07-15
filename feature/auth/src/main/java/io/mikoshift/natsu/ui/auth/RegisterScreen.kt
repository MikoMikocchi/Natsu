package io.mikoshift.natsu.ui.auth

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mikoshift.natsu.feature.auth.R
import io.mikoshift.natsu.ui.theme.NatsuTheme

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RegisterScreenContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordConfirmationChange = viewModel::onPasswordConfirmationChange,
        onSubmit = viewModel::submit,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
internal fun RegisterScreenContent(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
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
        Text(text = stringResource(R.string.sign_up_title), style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.passwordConfirmation,
            onValueChange = onPasswordConfirmationChange,
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = uiState.passwordConfirmationError != null,
            supportingText = uiState.passwordConfirmationError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.generalError != null) {
            Text(
                text = uiState.generalError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
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
                Text(stringResource(R.string.sign_up))
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Already have an account? Sign in")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    NatsuTheme {
        RegisterScreenContent(
            uiState = RegisterUiState(name = "Jane Doe", email = "jane@example.com"),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordConfirmationChange = {},
            onSubmit = {},
            onNavigateToLogin = {},
        )
    }
}
