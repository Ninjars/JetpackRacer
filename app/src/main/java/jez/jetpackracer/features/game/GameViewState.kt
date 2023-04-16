package jez.jetpackracer.features.game

data class GameViewState(
    val inGameState: InGameViewState?
) {
    data class InGameViewState(
        val isPaused: Boolean,
    )
}

object StateToViewState : (GameVM.State) -> GameViewState {
    override fun invoke(state: GameVM.State): GameViewState =
        when (state) {
            GameVM.State.Loading -> GameViewState(inGameState = null)
        }
}
