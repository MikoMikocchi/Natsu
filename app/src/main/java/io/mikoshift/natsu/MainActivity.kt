package io.mikoshift.natsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.mikoshift.natsu.ui.navigation.NatsuNavHost
import io.mikoshift.natsu.ui.theme.NatsuTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NatsuTheme {
                NatsuNavHost()
            }
        }
    }
}
