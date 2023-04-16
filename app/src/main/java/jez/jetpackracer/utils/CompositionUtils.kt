package jez.jetpackracer.utils

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

/**
 * Hook a lifecycle observer to a lifecycle, removing it when the lifecycle is disposed.
 */
@SuppressLint("ComposableNaming")
@Composable
fun <LO : LifecycleObserver> LO.observeLifecycle(lifecycle: Lifecycle) {
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(this@observeLifecycle)
        onDispose {
            lifecycle.removeObserver(this@observeLifecycle)
        }
    }
}

/**
 * Convenience function to wrap an event consumer lambda in a form which will be
 * treated as Stable by Jetpack Compose and avoid introducing unnecessary recompositions.
 */
@Composable
fun <T> rememberEventConsumer(consumer: Consumer<T>) =
    remember<(T) -> Unit>(consumer) { { consumer.accept(it) } }

/**
 * Keeps screen on whilst part of the composition.
 *
 * Note: if multiple are used in the same composition hierarchy then they
 * will be sharing the same currentView reference, and so disposing ANY of them
 * will unset the flag globally.
 *
 * To avoid unexpected behaviour only one KeepScreenOn should be composed at a time.
 *
 * Fixing this would require keeping a global register of KeepScreenOn locks for a
 * given currentView.
 *
 * Credit: https://stackoverflow.com/a/71293123/3090662
 */
@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}
