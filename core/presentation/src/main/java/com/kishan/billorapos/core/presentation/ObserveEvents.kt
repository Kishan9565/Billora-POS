package com.kishan.billorapos.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/** Only the foreground destination consumes navigation and feedback events. */
@Composable
fun <T> ObserveEvents(events: Flow<T>, onEvent: suspend (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val handler = rememberUpdatedState(onEvent)
    LaunchedEffect(events, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            events.collect { handler.value(it) }
        }
    }
}
