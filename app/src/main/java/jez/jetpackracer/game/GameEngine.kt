package jez.jetpackracer.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield
import javax.inject.Inject

data class GameConfiguration(
    val worldSize: Vector2,
)

class GameEngine @Inject constructor() {

    private val mutableState: MutableStateFlow<GameEngineState> =
        MutableStateFlow(GameEngineState.Uninitialised)
    val engineState: StateFlow<GameEngineState> = mutableState.asStateFlow()

    suspend fun configure(config: GameConfiguration) {
        mutableState.value = GameEngineState.Initialising
        yield()
    }

    suspend fun update(deltaNanos: Long) {
        val currentState = mutableState.value
        mutableState.value = when (currentState) {
            is GameEngineState.Uninitialised,
            is GameEngineState.Initialising -> currentState
            is GameEngineState.Idling -> TODO()
            is GameEngineState.Running -> {
                // process input and update state accordingly (eg player velocity, pause command)

                // step game simulation to get updated state and events
                val updatedWorld = ProcessGameUpdate(currentState.worldState, deltaNanos)

                // process events (enqueue sound effects)

                // push updated state
                GameEngineState.Running(updatedWorld)
            }
        }
    }
}
