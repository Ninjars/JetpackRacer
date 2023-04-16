package jez.jetpackracer.utils

import androidx.compose.runtime.State

fun <T> previewState(stateBuilder: () -> T) = object : State<T> {
    override val value: T
        get() = stateBuilder()
}
