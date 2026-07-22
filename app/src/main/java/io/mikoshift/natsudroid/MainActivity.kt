package io.mikoshift.natsudroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import io.mikoshift.natsudroid.ui.navigation.NatsudroidNavHost
import io.mikoshift.natsudroid.ui.theme.NatsudroidTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLinkTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkTrigger++
        setContent {
            NatsudroidTheme {
                NatsudroidNavHost(deepLinkTrigger = deepLinkTrigger)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTrigger++
    }
}
