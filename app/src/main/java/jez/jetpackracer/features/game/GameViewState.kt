package jez.jetpackracer.features.game

import androidx.compose.ui.geometry.Rect
import jez.jetpackracer.features.game.GameViewState.EntityVisuals.EntityColor
import jez.jetpackracer.game.GameEngineState
import jez.jetpackracer.game.PlayerState

data class GameViewState(
    val inGameState: InGameViewState?
) {
    sealed class InGameViewState {
        object Uninitialised : InGameViewState()

        data class Running(
            val isPaused: Boolean,
            val player: Player,
            val entities: List<Entity>,
        ) : InGameViewState()
    }

    data class EntityVisuals(
        val bounds: Rect,
        val color: EntityColor,
    ) {
        enum class EntityColor {
            Player,
            Obstacle,
        }
    }

    data class Player(
        val visuals: EntityVisuals,
    )

    data class Entity(
        val visuals: EntityVisuals,
    )
}

object StateToViewState : (GameVM.CombinedState) -> GameViewState {
    override fun invoke(state: GameVM.CombinedState): GameViewState =
        when (val vmState = state.vmState) {
            is GameVM.State.Loading -> GameViewState(inGameState = null)
            is GameVM.State.Running -> GameViewState(createViewState(vmState, state.engineState))
        }

    private fun createViewState(
        vmState: GameVM.State.Running,
        engineState: GameEngineState
    ): GameViewState.InGameViewState? =
        when (engineState) {
            is GameEngineState.Running ->
                GameViewState.InGameViewState.Running(
                    isPaused = vmState.isPaused,
                    player = engineState.worldState.player.toScreenModel(),
                    entities = engineState.worldState.entities.toScreenModels(),
                )
            is GameEngineState.Idling ->
                GameViewState.InGameViewState.Running(
                    isPaused = false,
                    player = engineState.worldState.player.toScreenModel(),
                    entities = engineState.worldState.entities.toScreenModels(),
                )
            is GameEngineState.Uninitialised,
            is GameEngineState.Initialising -> GameViewState.InGameViewState.Uninitialised
        }

    private fun PlayerState.toScreenModel(): GameViewState.Player =
        GameViewState.Player(
            visuals = GameViewState.EntityVisuals(
                bounds = TODO("calculate position relative to viewWorldOrigin and convert to position relative to viewport dims"),
                color = EntityColor.Player,
            )
        )

    private fun <T> Iterable<T>.toScreenModels(): List<GameViewState.Entity> =
        this.map {
            GameViewState.Entity(
                visuals = GameViewState.EntityVisuals(
                    bounds =,
                    color = EntityColor.Obstacle,
                )
            )
        }
}
