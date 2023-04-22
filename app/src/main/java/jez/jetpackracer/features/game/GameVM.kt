package jez.jetpackracer.features.game

import androidx.compose.ui.graphics.Color
import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.jetpackracer.features.game.GameVM.Event
import jez.jetpackracer.game.GameConfiguration
import jez.jetpackracer.game.GameEngine
import jez.jetpackracer.game.GameEngineState
import jez.jetpackracer.game.Vector2
import jez.jetpackracer.utils.toViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameVM @Inject constructor(
    private val gameEngine: GameEngine,
) : Consumer<Event>, ViewModel(), DefaultLifecycleObserver {

    sealed class Event {
        object Pause : Event()
        object Resume : Event()
        object StartNewGame : Event()
        data class ViewReady(val width: Float, val height: Float) : Event()
        data class UpdateViewBounds(val width: Float, val height: Float) : Event()
        data class GameTimeUpdate(val deltaNanos: Long) : Event()
    }

    sealed class State {
        object Loading : State()

        data class Running(
            val viewWidth: Float,
            val viewHeight: Float,
            val viewScale: Float,
            val isPaused: Boolean,
        ) : State()
    }

    data class CombinedState(val vmState: State, val engineState: GameEngineState) {
        companion object {
            val Initial = CombinedState(State.Loading, GameEngineState.Uninitialised)
        }
    }

    private val vmState: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    private val combinedState = combine(
        vmState,
        gameEngine.engineState
    ) { vmState, engineState -> CombinedState(vmState, engineState) }

    val viewState: StateFlow<GameViewState> =
        combinedState.toViewState(
            viewModelScope,
            CombinedState.Initial
        ) { combinedState -> StateToViewState(combinedState) }

    override fun accept(event: Event) {
        val currentState = vmState.value
        viewModelScope.launch {
            when (currentState) {
                is State.Loading -> loadingProcessEvent(event)
                is State.Running -> runningProcessEvent(currentState, event)
            }

        }
    }

    private fun loadingProcessEvent(event: Event) {
        when (event) {
            is Event.ViewReady -> {
                vmState.value = State.Running(
                    viewWidth = event.width,
                    viewHeight = event.height,
                    viewScale = 1f,
                    isPaused = false,
                )
            }
            is Event.UpdateViewBounds,
            is Event.GameTimeUpdate,
            is Event.StartNewGame,
            is Event.Pause,
            is Event.Resume -> Unit
        }
    }

    private suspend fun runningProcessEvent(state: State.Running, event: Event) {
        when (event) {
            is Event.GameTimeUpdate -> gameEngine.update(event.deltaNanos)
            is Event.Pause -> vmState.value = state.copy(isPaused = true)
            is Event.Resume -> vmState.value = state.copy(isPaused = false)
            is Event.StartNewGame -> {
                gameEngine.start()
                vmState.value = state.copy(isPaused = false)
            }
            is Event.UpdateViewBounds -> vmState.value =
                state.copy(viewWidth = event.width, viewHeight = event.height)
            is Event.ViewReady -> {
                vmState.value =
                    state.copy(viewWidth = event.width, viewHeight = event.height)
                gameEngine.configure(
                    config = GameConfiguration(
                        worldSize = Vector2(500.0, 1000.0),
                        playerColor = Color.LightGray,
                        playerSpeed = Vector2(20.0, 20.0),
                        playerFriction = Vector2(0.75, 0.75),
                        playerRadius = 5.0,
                    )
                )
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        accept(Event.Pause)
        super.onPause(owner)
    }
}
