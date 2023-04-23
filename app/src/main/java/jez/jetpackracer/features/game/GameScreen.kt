package jez.jetpackracer.features.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jez.jetpackracer.R
import jez.jetpackracer.features.game.GameVM.Event
import jez.jetpackracer.utils.KeepScreenOn
import jez.jetpackracer.utils.observeLifecycle
import jez.jetpackracer.utils.rememberEventConsumer
import timber.log.Timber

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
    GameContainer(eventHandler) { stateFlow.value.inGameState }
    GameUi(eventHandler) { stateFlow.value.uiState }
}

@Composable
private fun GameUi(
    eventHandler: (Event) -> Unit,
    stateProvider: () -> GameViewState.UiState,
) {
    when (stateProvider()) {
        GameViewState.UiState.Initialising -> Unit
        GameViewState.UiState.Idling -> IdleUI(eventHandler)
        GameViewState.UiState.Running -> Unit // TODO: pause and controls
        GameViewState.UiState.Paused -> Unit // TODO: menu and resume
    }
}

@Composable
private fun IdleUI(eventHandler: (Event) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        OutlinedIconButton(
            modifier = Modifier.padding(bottom = 46.dp),
            onClick = { eventHandler(Event.StartNewGame) },
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.command_start_new_game),
            )
        }
    }
}

@Composable
private fun GameContainer(
    eventHandler: (Event) -> Unit,
    stateProvider: () -> GameViewState.InGameViewState?,
) {
    KeepScreenOn()

    val localDensity = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.DarkGray)
    ) {
        val width = remember(maxWidth) {
            with(localDensity) { maxWidth.toPx() }
        }
        val height = remember(maxHeight) {
            with(localDensity) { maxHeight.toPx() }
        }
        LaunchedEffect(width, height) {
            eventHandler(Event.UpdateViewBounds(width, height))
        }
        when (val state = stateProvider()) {
            null -> Loading()
            is GameViewState.InGameViewState.Uninitialised -> Unit
            is GameViewState.InGameViewState.Running -> RunningGame(
                state = state,
                eventHandler = eventHandler,
            )
        }
    }
}

@Composable
private fun Loading() {
}

@Composable
private fun RunningGame(
    state: GameViewState.InGameViewState.Running,
    eventHandler: (Event) -> Unit,
) {
    LaunchedEffect(state.isPaused) {
        var lastFrame = withFrameNanos { it }
        while (!state.isPaused) {
            withFrameNanos {
                eventHandler(Event.GameTimeUpdate(it - lastFrame))
                lastFrame = it
            }
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        state.entities.forEach {
            drawVisuals(it.visuals)
        }

        drawVisuals(visuals = state.player.visuals)

    }
}

private fun DrawScope.drawVisuals(visuals: GameViewState.EntityVisuals) {
    Timber.w("drawing at ${visuals.bounds}")
    drawRect(
        color = visuals.color,
        topLeft = visuals.bounds.topLeft,
        size = visuals.bounds.size,
    )
}
