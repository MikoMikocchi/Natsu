package io.mikoshift.natsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.mikoshift.natsu.ui.navigation.NatsuNavHost
import io.mikoshift.natsu.ui.theme.NatsuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as NatsuApplication).appContainer
        setContent {
            NatsuTheme {
                NatsuNavHost(appContainer)
            }
        }
    }
}
