package jez.jetpackracer.features.game

import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.jetpackracer.features.game.GameVM.Event
import jez.jetpackracer.game.GameEngine
import jez.jetpackracer.game.GameEngineState
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
        data class UpdateViewBounds(val width: Float, val height: Float) : Event()
        data class GameTimeUpdate(val deltaNanos: Long) : Event()
    }

    sealed class State {
        abstract val viewWidth: Float
        abstract val viewHeight: Float
        abstract val viewScale: Float

        data class Loading(
            override val viewWidth: Float = 0f,
            override val viewHeight: Float = 0f,
            override val viewScale: Float = 1f,
        ) : State()

        data class Running(
            override val viewWidth: Float,
            override val viewHeight: Float,
            override val viewScale: Float,
            val isPaused: Boolean,
        ) : State()
    }

    data class CombinedState(val vmState: State, val engineState: GameEngineState) {
        companion object {
            val Initial = CombinedState(State.Loading(), GameEngineState.Uninitialised)
        }
    }

    private val vmState: MutableStateFlow<State> = MutableStateFlow(State.Loading())
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
                is State.Loading -> loadingProcessEvent(currentState, event)
                is State.Running -> runningProcessEvent(currentState, event)
            }

        }
    }

    private fun loadingProcessEvent(state: State.Loading, event: Event) {
        when (event) {
            is Event.UpdateViewBounds -> vmState.value =
                state.copy(viewWidth = event.width, viewHeight = event.height)
            is Event.GameTimeUpdate,
            is Event.Pause,
            is Event.Resume -> Unit
        }
    }

    private suspend fun runningProcessEvent(state: State.Running, event: Event) {
        when (event) {
            is Event.GameTimeUpdate -> gameEngine.update(event.deltaNanos)
            is Event.Pause -> vmState.value = state.copy(isPaused = true)
            is Event.Resume -> vmState.value = state.copy(isPaused = false)
            is Event.UpdateViewBounds -> vmState.value =
                state.copy(viewWidth = event.width, viewHeight = event.height)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        accept(Event.Pause)
        super.onPause(owner)
    }
}
