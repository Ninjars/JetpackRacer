package jez.jetpackracer.features.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
        GameViewState.UiState.Running -> RunningUi(eventHandler) // TODO: pause
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
private fun RunningUi(eventHandler: (Event) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        val leftButtonInteraction = remember { MutableInteractionSource() }
        LaunchedEffect(leftButtonInteraction) {
            leftButtonInteraction.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> eventHandler(Event.UpdateLeftInput(true))
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> eventHandler(Event.UpdateLeftInput(false))
                }
            }
        }
        val rightButtonInteraction = remember { MutableInteractionSource() }
        LaunchedEffect(rightButtonInteraction) {
            rightButtonInteraction.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> eventHandler(Event.UpdateRightInput(true))
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> eventHandler(Event.UpdateRightInput(false))
                }
            }
        }
        Button(
            modifier = Modifier
                .padding(bottom = 24.dp, start = 16.dp)
                .size(80.dp)
                .align(Alignment.BottomStart),
            interactionSource = leftButtonInteraction,
            shape = IconButtonDefaults.outlinedShape,
            contentPadding = PaddingValues(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder,
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.Default.ArrowLeft,
                contentDescription = stringResource(R.string.command_move_left),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Button(
            modifier = Modifier
                .padding(bottom = 24.dp, end = 16.dp)
                .size(80.dp)
                .align(Alignment.BottomEnd),
            interactionSource = rightButtonInteraction,
            shape = IconButtonDefaults.outlinedShape,
            contentPadding = PaddingValues(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder,
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.Default.ArrowRight,
                contentDescription = stringResource(R.string.command_move_right),
                modifier = Modifier.fillMaxSize(),
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

        drawCollisions(state.collisionPoints)
    }
}

private fun DrawScope.drawCollisions(collisionPoints: List<Offset>) {
    collisionPoints.forEach {
        drawCircle(
            color = Color.Green,
            radius = 10f,
            center = it
        )
    }
}

private fun DrawScope.drawVisuals(visuals: GameViewState.EntityVisuals) {
    drawRect(
        color = visuals.color,
        topLeft = visuals.bounds.topLeft,
        size = visuals.bounds.size,
    )
}
