package jez.jetpackracer.features.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import jez.jetpackracer.features.game.GameViewState.UiState
import jez.jetpackracer.game.Bounds
import jez.jetpackracer.game.EntityVis
import jez.jetpackracer.game.GameEngineState
import jez.jetpackracer.game.PlayerState
import jez.jetpackracer.game.Vector2
import jez.jetpackracer.game.WorldEntity

data class GameViewState(
    val uiState: UiState,
    val inGameState: InGameViewState?,
) {
    sealed class InGameViewState {
        object Uninitialised : InGameViewState()

        data class Running(
            val isPaused: Boolean,
            val player: Player,
            val entities: List<Entity>,
        ) : InGameViewState()
    }

    enum class UiState {
        Initialising,
        Idling,
        Running,
        Paused,
    }

    data class EntityVisuals(
        val bounds: Rect,
        val color: Color,
    )

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
            is GameVM.State.Loading -> GameViewState(
                uiState = UiState.Initialising,
                inGameState = null,
            )
            is GameVM.State.Running -> GameViewState(
                uiState = createUiViewState(vmState.isPaused, state.engineState),
                inGameState = createGameViewState(vmState, state.engineState)
            )
        }

    private fun createUiViewState(
        paused: Boolean,
        engineState: GameEngineState,
    ): UiState =
        when (engineState) {
            is GameEngineState.Uninitialised,
            is GameEngineState.Initialising -> UiState.Initialising
            is GameEngineState.Idling -> UiState.Idling
            is GameEngineState.Running ->
                if (paused) {
                    UiState.Paused
                } else {
                    UiState.Running
                }
        }

    private fun createGameViewState(
        vmState: GameVM.State.Running,
        engineState: GameEngineState
    ): GameViewState.InGameViewState =
        when (engineState) {
            is GameEngineState.Running -> {
                val worldState = engineState.worldState
                val viewportOffset = viewportToScreenOffset(
                    vmState.viewWidth,
                    vmState.viewHeight,
                )
                GameViewState.InGameViewState.Running(
                    isPaused = vmState.isPaused,
                    player = worldState.player.toScreenModel(
                        engineState.worldState.viewOriginOffset,
                        viewportOffset,
                        vmState.viewScale,
                    ),
                    entities = worldState.entities.toScreenModels(
                        engineState.worldState.viewOriginOffset,
                        viewportOffset,
                        vmState.viewScale
                    ),
                )
            }
            is GameEngineState.Idling -> {
                val viewportOffset = viewportToScreenOffset(
                    vmState.viewWidth,
                    vmState.viewHeight,
                )
                GameViewState.InGameViewState.Running(
                    isPaused = vmState.isPaused,
                    player = engineState.worldState.player.toScreenModel(
                        engineState.worldState.viewOriginOffset,
                        viewportOffset,
                        vmState.viewScale
                    ),
                    entities = engineState.worldState.entities.toScreenModels(
                        engineState.worldState.viewOriginOffset,
                        viewportOffset,
                        vmState.viewScale
                    ),
                )
            }
            is GameEngineState.Uninitialised,
            is GameEngineState.Initialising -> GameViewState.InGameViewState.Uninitialised
        }

    private fun viewportToScreenOffset(
        viewWidth: Float,
        viewHeight: Float,
    ) =
        Offset(
            viewWidth / 2f,
            viewHeight * 0.8f,
        )

    private fun Bounds.toScreenCoords(viewportScreenOffset: Offset, viewScale: Float) =
        Rect(
            left = left.toFloat() * viewScale + viewportScreenOffset.x,
            top = -top.toFloat() * viewScale + viewportScreenOffset.y,
            right = right.toFloat() * viewScale + viewportScreenOffset.x,
            bottom = -bottom.toFloat() * viewScale + viewportScreenOffset.y,
        )

    private fun PlayerState.toScreenModel(
        gameViewOrigin: Vector2,
        viewportScreenOffset: Offset,
        viewScale: Float
    ): GameViewState.Player =
        GameViewState.Player(
            visuals = GameViewState.EntityVisuals(
                bounds = visuals.drawBounds.offset(position - gameViewOrigin)
                    .toScreenCoords(viewportScreenOffset, viewScale),
                color = (visuals as EntityVis.SolidColor).color,
            )
        )

    private fun Iterable<WorldEntity>.toScreenModels(
        gameViewOrigin: Vector2,
        viewportScreenOffset: Offset,
        viewScale: Float
    ): List<GameViewState.Entity> =
        this.map {
            GameViewState.Entity(
                visuals = GameViewState.EntityVisuals(
                    bounds = it.boundingBox.offset(-gameViewOrigin)
                        .toScreenCoords(viewportScreenOffset, viewScale),
                    color = (it.visuals as EntityVis.SolidColor).color,
                )
            )
        }
}
