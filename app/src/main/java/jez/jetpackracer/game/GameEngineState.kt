package jez.jetpackracer.game

sealed class GameEngineState {
    object Uninitialised : GameEngineState()
    object Initialising : GameEngineState()
    data class Idling(
        val worldState: WorldState,
    ) : GameEngineState()

    data class Running(
        val worldState: WorldState,
    ) : GameEngineState()
}

data class WorldState(
    /**
     * Defines the relative local size of the simulated game world.
     * Offset these bounds by baseWorldOffset to convert to world bounds.
     */
    val localGameBounds: Bounds,

    /**
     * The velocity of the local coordinate origin.
     *
     * This establishes a base-line velocity of the game.
     */
    val baseWorldVelocity: Vector2,

    /**
     * Non-linear factor used to proportionally reduce velocity per axis.
     *
     * Value of 1.0 will negate all velocity from previous frame, 0.0 means no velocity reduction.
     */
    val friction: Vector2,

    /**
     * Tracks the offset of the relativeGameBounds vs the world origin.
     */
    val baseWorldOffset: Vector2,

    /**
     * Anchor point for the view port.
     *
     * Tracks after the player position.
     */
    val viewWorldOrigin: Vector2,

    /**
     * Affects how fast the view tracks after the player.
     */
    val viewVelocityFactor: Vector2,

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
