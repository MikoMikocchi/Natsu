package io.mikoshift.natsu.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mikoshift.natsu.core.ui.CollectEffects
import io.mikoshift.natsu.feature.profile.R
import io.mikoshift.natsu.ui.theme.NatsuTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ProfileEffect.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNavigateToChangePassword = onNavigateToChangePassword,
        onDeletePasswordChange = viewModel::onDeletePasswordChange,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onDeleteAccount = viewModel::deleteAccount,
        onRevokeSession = viewModel::revokeSession,
        onLogout = viewModel::logout,
        onShowDeleteDialog = viewModel::showDeleteDialog,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onDeletePasswordChange: (String) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onDeleteAccount: () -> Unit,
    onRevokeSession: (Long, Boolean) -> Unit,
    onLogout: () -> Unit,
    onShowDeleteDialog: () -> Unit,
) {
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text("Delete account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This action cannot be undone. Enter your password to confirm.")
                    OutlinedTextField(
                        value = uiState.deletePassword,
                        onValueChange = onDeletePasswordChange,
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = uiState.deletePasswordError != null,
                        supportingText = uiState.deletePasswordError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteAccount,
                    enabled = !uiState.isDeletingAccount,
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.account), style = MaterialTheme.typography.titleMedium)

            if (uiState.isLoadingUser && uiState.user == null) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(text = uiState.user?.displayName.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                Text(text = uiState.user?.email.orEmpty())
                Text(
                    text = "Created: ${uiState.user?.memberSince.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onNavigateToChangePassword,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Change password")
            }

            HorizontalDivider()

            Text(text = stringResource(R.string.active_sessions), style = MaterialTheme.typography.titleMedium)

            if (uiState.isLoadingSessions && uiState.sessions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (uiState.sessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_active_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                uiState.sessions.forEach { session ->
                    SessionRow(
                        session = session,
                        isRevoking = uiState.revokingSessionId == session.id,
                        onRevoke = { onRevokeSession(session.id, session.isCurrent) },
                    )
                }
            }

            if (uiState.generalError != null) {
                Text(
                    text = uiState.generalError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = onLogout,
                enabled = !uiState.isLoggingOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoggingOut) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Sign out")
                }
            }

            TextButton(
                onClick = onShowDeleteDialog,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete account", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: SessionUiModel,
    isRevoking: Boolean,
    onRevoke: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = session.deviceName, style = MaterialTheme.typography.bodyLarge)
            Text(text = session.subtitle, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(
            onClick = onRevoke,
            enabled = !isRevoking,
        ) {
            if (isRevoking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Revoke")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    NatsuTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                user = UserUiModel(
                    displayName = "Jane Doe",
                    email = "jane@example.com",
                    memberSince = "2024-01-15",
                ),
                sessions = listOf(
                    SessionUiModel(
                        id = 1,
                        deviceName = "Pixel 8",
                        subtitle = "Current · 2026-07-01",
                        isCurrent = true,
                    ),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onNavigateToChangePassword = {},
            onDeletePasswordChange = {},
            onDismissDeleteDialog = {},
            onDeleteAccount = {},
            onRevokeSession = { _, _ -> },
            onLogout = {},
            onShowDeleteDialog = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenEmptySessionsPreview() {
    NatsuTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                user = UserUiModel(
                    displayName = "Jane Doe",
                    email = "jane@example.com",
                    memberSince = "2024-01-15",
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {},
            onNavigateToChangePassword = {},
            onDeletePasswordChange = {},
            onDismissDeleteDialog = {},
            onDeleteAccount = {},
            onRevokeSession = { _, _ -> },
            onLogout = {},
            onShowDeleteDialog = {},
        )
    }
}
