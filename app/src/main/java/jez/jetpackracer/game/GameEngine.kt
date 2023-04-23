package jez.jetpackracer.game

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield
import javax.inject.Inject

data class GameConfiguration(
    val worldSize: Vector2,
    val playerColor: Color,
    val playerSpeed: Vector2,
    val playerFriction: Vector2 = Vector2(0.75, 0.75),
    val playerRadius: Double = 5.0,
    val obstacleColor: Color,
)

class GameEngine @Inject constructor() {

    private val mutableState: MutableStateFlow<GameEngineState> =
        MutableStateFlow(GameEngineState.Uninitialised)
    val engineState: StateFlow<GameEngineState> = mutableState.asStateFlow()

    suspend fun configure(config: GameConfiguration) {
        mutableState.value = GameEngineState.Initialising
        yield()

        mutableState.value = GameEngineState.Idling(
            config = config,
            worldState = createInitialIdleWorldState(config)
        )
    }

    fun start() {
        val currentState = mutableState.value
        mutableState.value = when (currentState) {
            is GameEngineState.Uninitialised,
            is GameEngineState.Initialising -> currentState
            is GameEngineState.Idling ->
                GameEngineState.Running(
                    config = currentState.config,
                    input = GameEngineState.GameInput.Neutral,
                    worldState = initialiseIdleWorldState(currentState.worldState)
                )
            is GameEngineState.Running ->
                GameEngineState.Running(
                    config = currentState.config,
                    input = GameEngineState.GameInput.Neutral,
                    worldState = initialiseIdleWorldState(
                        createInitialIdleWorldState(currentState.config)
                    )
                )
        }
    }

    fun update(deltaNanos: Long) {
        val currentState = mutableState.value
        mutableState.value = when (currentState) {
            is GameEngineState.Uninitialised,
            is GameEngineState.Initialising -> currentState
            is GameEngineState.Idling -> currentState
            is GameEngineState.Running -> {
                // step game simulation to get updated state and events
                val updatedWorld =
                    ProcessGameUpdate(currentState.worldState, currentState.input, deltaNanos)

                // process events (enqueue sound effects for collisions, check for end game conditions)

                // push updated state
                currentState.copy(worldState = updatedWorld)
            }
        }
    }

    fun updateLeftInput(pressed: Boolean) {
        when (val currentState = mutableState.value) {
            is GameEngineState.Running -> mutableState.value = currentState.copy(
                input = currentState.input.copy(leftInput = if (pressed) 1.0 else .0)
            )
            else -> Unit
        }
    }

    fun updateRightInput(pressed: Boolean) {
        when (val currentState = mutableState.value) {
            is GameEngineState.Running -> mutableState.value = currentState.copy(
                input = currentState.input.copy(rightInput = if (pressed) 1.0 else .0)
            )
            else -> Unit
        }
    }

    private fun createInitialIdleWorldState(config: GameConfiguration): WorldState {
        val startingPos = Vector2(config.worldSize.x / 2.0, config.worldSize.y / 6.0)
        return WorldState(
            gameBounds = Bounds(
                left = .0,
                top = config.worldSize.y,
                right = config.worldSize.x,
                bottom = .0,
            ),
            worldMovementVector = Vector2(.0, 1.0),
            worldSpeed = LerpOverTime(
                durationNanos = .0.secondsToNanos(),
                startValue = .0,
                endValue = .0,
            ),
            worldOrigin = Vector2.Zero,
            viewOriginOffset = startingPos,
            viewUpdateSpeedFactor = 5.0,
            player = PlayerState(
                visuals = EntityVis.SolidColor(
                    drawBounds = Bounds(
                        left = -config.playerRadius,
                        top = config.playerRadius,
                        right = config.playerRadius,
                        bottom = -config.playerRadius,
                    ),
                    color = config.playerColor,
                ),
                position = startingPos,
                velocity = Vector2.Zero,
                collider = Collider.Circle(config.playerRadius),
                friction = config.playerFriction,
                baseAcceleration = Vector2.Zero,
                maxInputAcceleration = config.playerSpeed,
                collisionStatus = emptyList(),
            ),
            entities = emptyList(),
            enemySpawnConfig = WorldState.EnemySpawnConfig(
                spawnIntervalSeconds = 0.25,
                width = 20.0..100.0,
                height = 50.0..200.0,
                color = config.obstacleColor,
            ),
        )
    }

    private fun initialiseIdleWorldState(initialWorldState: WorldState) =
        initialWorldState.copy(
            worldOrigin = Vector2.Zero,
            worldSpeed = LerpOverTime(
                durationNanos = 60.0.secondsToNanos(),
                startValue = 100.0,
                endValue = 2000.0,
            ),
            secondsSinceLastEnemySpawn = Double.MAX_VALUE,
        )
}
