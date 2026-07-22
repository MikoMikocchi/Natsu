package io.mikoshift.natsudroid.ui.profile

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mikoshift.natsudroid.feature.profile.R
import io.mikoshift.natsudroid.ui.theme.NatsudroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(viewModel: ChangePasswordViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChangePasswordScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordConfirmationChange = viewModel::onPasswordConfirmationChange,
        onSubmit = viewModel::submit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChangePasswordScreenContent(
    uiState: ChangePasswordUiState,
    onNavigateBack: () -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmationChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.change_password)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = { Text(stringResource(R.string.current_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = uiState.currentPasswordError != null,
                supportingText = uiState.currentPasswordError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.new_password)) },
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
                label = { Text(stringResource(R.string.confirm_password)) },
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
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangePasswordScreenPreview() {
    NatsudroidTheme {
        ChangePasswordScreenContent(
            uiState = ChangePasswordUiState(),
            onNavigateBack = {},
            onCurrentPasswordChange = {},
            onPasswordChange = {},
            onPasswordConfirmationChange = {},
            onSubmit = {},
        )
    }
}
