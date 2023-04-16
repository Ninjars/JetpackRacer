package jez.jetpackracer.game

import androidx.compose.ui.graphics.Color


data class PlayerState(
    val visuals: EntityVis,
    val localPosition: Vector2,
    val velocity: Vector2,
    val collisionBounds: CollisionBounds.Circle,
)

data class WorldEntity(
    val visuals: EntityVis,
    val localPosition: Vector2,
    val velocity: Vector2,
    val collisionBounds: CollisionBounds,
)

sealed class CollisionBounds {
    data class Circle(val radius: Double) : CollisionBounds()
    data class Box(val bounds: Bounds) : CollisionBounds()
}

sealed class EntityVis {
    abstract val drawBounds: Bounds

    data class SolidColor(
        override val drawBounds: Bounds,
        val color: Color
    ) : EntityVis()
}

data class Vector2(
    val x: Double,
    val y: Double,
)

data class Bounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    val width = right - left
    val height = top - bottom

    val center = Vector2(
        x = left + width / 2.0,
        y = bottom + height / 2.0
    )

    fun offset(x: Double = .0, y: Double = .0) =
        this.copy(
            left = left + x,
            right = right + x,
            top = top + y,
            bottom = bottom + y,
        )

    fun centerOn(vector2: Vector2) =
        this.copy(
            left = vector2.x - width / 2.0,
            right = vector2.x + width / 2.0,
            top = vector2.y + height / 2.0,
            bottom = vector2.y - height / 2.0,
        )
}
