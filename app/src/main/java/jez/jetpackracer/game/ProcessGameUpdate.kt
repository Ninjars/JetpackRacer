package jez.jetpackracer.game

object ProcessGameUpdate : (WorldState, Long) -> GameUpdateResults {
    override fun invoke(initialState: WorldState, update: Long): GameUpdateResults {
        TODO("Not yet implemented")
        // Bleed player velocity into world velocity

        // Remove old entities that are outside bounds

        // Update player: velocity - worldVelocity

        // Update and collision-check entities
        //    apply velocity - worldVelocity
        //    perform collision detection at new positions
        //    consider interpolating collision detection
        //      if position change is sufficiently significant?
        //      Might be overkill for this project.
    }
}

data class GameUpdateResults(
    val worldState: WorldState,
)
