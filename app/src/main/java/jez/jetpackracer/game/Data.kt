package jez.jetpackracer.game

import androidx.compose.ui.graphics.Color
import kotlin.math.abs


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
) {
    val boundingBox = when (collisionBounds) {
        is CollisionBounds.Box -> collisionBounds.bounds
        is CollisionBounds.Circle -> Bounds(
            left = localPosition.x - collisionBounds.radius,
            right = localPosition.x + collisionBounds.radius,
            top = localPosition.y + collisionBounds.radius,
            bottom = localPosition.y - collisionBounds.radius,
        )
    }
}

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
) {
    operator fun minus(other: Vector2) =
        Vector2(x - other.x, y - other.y)

    operator fun plus(other: Vector2) =
        Vector2(x + other.x, y + other.y)

    operator fun times(factor: Double) =
        Vector2(x * factor, y * factor)

    operator fun times(factor: Vector2) =
        Vector2(x * factor.x, y * factor.y)

    operator fun unaryMinus() =
        Vector2(-x, -y)

    fun dominantDirection() =
        if (abs(x) > abs(y)) {
            if (x > 0) Right else Left
        } else {
            if (y > 0) Up else Down
        }

    companion object {
        val Up = Vector2(.0, 1.0)
        val Down = Vector2(.0, -1.0)
        val Left = Vector2(-1.0, .0)
        val Right = Vector2(1.0, .0)
    }
}

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

    fun offset(offset: Vector2) =
        this.copy(
            left = left + offset.x,
            right = right + offset.x,
            top = top + offset.y,
            bottom = bottom + offset.y,
        )

    fun centerOn(vector2: Vector2) =
        this.copy(
            left = vector2.x - width / 2.0,
            right = vector2.x + width / 2.0,
            top = vector2.y + height / 2.0,
            bottom = vector2.y - height / 2.0,
        )
}
