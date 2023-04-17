package jez.jetpackracer.game

import jez.jetpackracer.game.Vector2.Companion.Down
import jez.jetpackracer.game.Vector2.Companion.Left
import jez.jetpackracer.game.Vector2.Companion.Right
import jez.jetpackracer.game.Vector2.Companion.Up

private fun Long.nanosToSeconds() =
    this / 1000000000.0

object ProcessGameUpdate : (WorldState, Long) -> GameUpdateResults {
    private val velocityBleedRate = Vector2(.25, 0.5)

    override fun invoke(initialState: WorldState, update: Long): GameUpdateResults {
        with(initialState) {
            // Bleed player velocity into world velocity
            val deltaV = player.velocity - baseWorldVelocity
            val bleedV = deltaV * velocityBleedRate * update.nanosToSeconds()

            val playerVelocity = player.velocity - bleedV
            val worldVelocity = baseWorldVelocity + bleedV

            val updatedWorldOffset = baseWorldOffset + worldVelocity

            // Remove old entities that are outside bounds
            val dominantVector = worldVelocity.dominantDirection()
            val activeEntities = entities.filter {
                val entityBounds = it.boundingBox.offset(-updatedWorldOffset)
                when (dominantVector) {
                    Up -> entityBounds.top < localGameBounds.bottom
                    Down -> entityBounds.bottom > localGameBounds.top
                    Left -> entityBounds.left > localGameBounds.right
                    Right -> entityBounds.right < localGameBounds.left
                    else -> true
                }
            }

            // Update player: velocity - worldVelocity

            // Update and collision-check entities
            //    apply velocity - worldVelocity
            //    perform collision detection at new positions
            //    consider interpolating collision detection
            //      if position change is sufficiently significant?
            //      Might be overkill for this project.
        }
        TODO("Not yet implemented")
    }
}

data class GameUpdateResults(
    val worldState: WorldState,
)
