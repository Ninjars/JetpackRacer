package jez.jetpackracer.game

sealed class GameEngineState {
    object Uninitialised : GameEngineState()
    object Initialising : GameEngineState()
    data class Idling(
        val config: GameConfiguration,
        val worldState: WorldState,
    ) : GameEngineState()

    data class Running(
        val config: GameConfiguration,
        val worldState: WorldState,
        val input: GameInput,
    ) : GameEngineState()

    data class GameInput(
        val leftInput: Double,
        val rightInput: Double,
    ) {
        val movementVector by lazy { Vector2(rightInput - leftInput, .0) }
        companion object {
            val Neutral: GameInput = GameInput(leftInput = .0, rightInput = .0)
        }
    }
}

data class WorldState(
    val elapsedTimeNanos: Long = 0,
    /**
     * Defines the relative local size of the simulated game world.
     * Offset these bounds by baseWorldOffset to convert to world bounds.
     */
    val gameBounds: Bounds,

    /**
     * The world moves, taking all non-local entities with it.
     * Movement is managed by a current speed, target speed and acceleration lerp,
     * applied relative to a specific vector.
     */
    val worldMovementVector: Vector2,
    val worldSpeed: LerpOverTime,

    /**
     * Tracks the offset of all entities from the world's origin
     */
    val worldOrigin: Vector2,

    /**
     * Anchor point for the view port.
     *
     * Tracks after the player position.
     */
    val viewOriginOffset: Vector2,

    /**
     * Affects how fast the view tracks after the player.
     * Value of 1 means the view always snaps to the player's position
     * Value of 0.5 means the view will follow the player by half their
     * relative offset per second
     */
    val viewUpdateSpeedFactor: Double,

    /**
     * Player is a special entity that we are going to need to
     * often be referencing separately from other world entities.
     *
     * Position is relative to baseWorldOffset.
     * Velocity is in World space.
     */
    val player: PlayerState,

    /**
     * All non-player objects.
     *
     * Position is relative to baseWorldOffset.
     * Velocity is in World space.
     */
    val entities: List<WorldEntity>,
)
