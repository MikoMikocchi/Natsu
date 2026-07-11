package io.mikoshift.natsu.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.mikoshift.natsu.data.repository.AuthRepository

@Composable
fun HomeScreen(
    authRepository: AuthRepository,
    onNavigateToProfile: () -> Unit,
) {
    val session by authRepository.currentSession.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = "Hello, ${session?.userName.orEmpty()}!",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(text = session?.userEmail.orEmpty())

            Button(
                onClick = onNavigateToProfile,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Profile")
            }
        }
    }
}
