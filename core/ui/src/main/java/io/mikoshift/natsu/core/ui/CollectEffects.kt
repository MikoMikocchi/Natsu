package io.mikoshift.natsu.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <Effect> CollectEffects(effects: Flow<Effect>, onEffect: suspend (Effect) -> Unit) {
    LaunchedEffect(effects) {
        effects.collect { effect -> onEffect(effect) }
    }
}
