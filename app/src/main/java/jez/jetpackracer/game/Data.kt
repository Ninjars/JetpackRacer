package jez.jetpackracer.game

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * [friction]: factor used to proportionally reduce velocity per axis.
 *      Value of 1.0 will negate all velocity from previous frame, 0.0 means no velocity reduction.
 * [baseAcceleration]: constant acceleration applied in addition to input per axis.
 *      Could be used to simulate gravity or to represent a vehicle under constant thrust.
 * [maxInputAcceleration]: max velocity change per axis in response to player input.
 */
data class PlayerState(
    val visuals: EntityVis,
    val worldPosition: Vector2,
    val velocity: Vector2,
    val collider: Collider.Circle,
    val friction: Vector2,
    val baseAcceleration: Vector2,
    val maxInputAcceleration: Vector2,
    val collisionStatus: List<CollisionStatus>,
) {
    val boundingBox = Bounds(
        left = worldPosition.x - collider.radius,
        right = worldPosition.x + collider.radius,
        top = worldPosition.y + collider.radius,
        bottom = worldPosition.y - collider.radius,
    )
}

data class WorldEntity(
    val id: String,
    val visuals: EntityVis,
    val worldPosition: Vector2,
    val velocity: Vector2,
    val collider: Collider,
) {
    val boundingBox = when (collider) {
        is Collider.Box -> collider.bounds.offset(worldPosition)
        is Collider.Circle -> Bounds(
            left = worldPosition.x - collider.radius,
            right = worldPosition.x + collider.radius,
            top = worldPosition.y + collider.radius,
            bottom = worldPosition.y - collider.radius,
        )
    }
}

data class CollisionStatus(
    val collisionTarget: WorldEntity?,
    val didCollisionStartThisFrame: Boolean,
    val collisionDurationNanos: Long,
) {
    val didCollisionEndThisFrame = collisionTarget == null
}

sealed class Collider {
    data class Circle(val radius: Double) : Collider()
    data class Box(val bounds: Bounds) : Collider()
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

        fun sqrMag(a: Vector2, b: Vector2): Double =
            (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
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

    val center by lazy {
        Vector2(
            x = left + width / 2.0,
            y = bottom + height / 2.0
        )
    }

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
