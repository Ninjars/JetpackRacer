package jez.jetpackracer.features.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalLifecycleOwner
import jez.jetpackracer.features.game.GameVM.Event
import jez.jetpackracer.utils.KeepScreenOn
import jez.jetpackracer.utils.observeLifecycle
import jez.jetpackracer.utils.rememberEventConsumer

@Composable
fun GameScreen(
    viewModel: GameVM,
) {
    viewModel.observeLifecycle(LocalLifecycleOwner.current.lifecycle)
    GameScreen(viewModel.viewState.collectAsState(), rememberEventConsumer(viewModel))
}

@Composable
private fun GameScreen(
    stateFlow: State<GameViewState>,
    eventHandler: (Event) -> Unit,
) {
    val state = stateFlow.value
    val inGameViewState = state.inGameState
    if (inGameViewState == null) {
        Loading()
    } else {
        Game(inGameViewState, eventHandler)
    }
}

@Composable
private fun Loading() {
}

@Composable
private fun Game(
    state: GameViewState.InGameViewState,
    eventHandler: (Event) -> Unit
) {
    KeepScreenOn()

    LaunchedEffect(state.isPaused) {
        val start = withFrameNanos { it }
        while (!state.isPaused) {
            withFrameNanos {
                eventHandler(Event.GameTimeUpdate(it - start))
            }
        }
    }
}
