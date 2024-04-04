package jez.jetpackracer.game

import androidx.compose.ui.graphics.Color
import androidx.core.math.MathUtils.clamp
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * [position]: position relative to game bounds
 * [velocity]: local velocity in addition to world velocity
 * [baseAcceleration]: constant acceleration applied in addition to input per axis.
 *      Could be used to simulate gravity or to represent a vehicle under constant thrust.
 * [maxInputAcceleration]: max velocity change per axis in response to player input.
 */
data class PlayerState(
    val visuals: EntityVis,
    val position: Vector2,
    val velocity: Vector2,
    val collider: Collider.Circle,
    val friction: Vector2,
    val baseAcceleration: Vector2,
    val maxInputAcceleration: Vector2,
    val collisionStatus: List<CollisionStatus>,
)

/**
 * [position]: position relative to game bounds
 * [velocity]: local velocity in addition to world velocity
 */
data class WorldEntity(
    val id: String,
    val position: Vector2,
    val visuals: EntityVis,
    val velocity: Vector2,
    val collider: Collider,
    val isStaticToGameBounds: Boolean,
) {
    val boundingBox = when (collider) {
        is Collider.Box -> collider.bounds.offset(position)
        is Collider.Circle -> Bounds(
            left = position.x - collider.radius,
            right = position.x + collider.radius,
            top = position.y + collider.radius,
            bottom = position.y - collider.radius,
        )
    }
}

data class CollisionStatus(
    val collisionTarget: WorldEntity?,
    val collisionPosition: Vector2?,
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

    fun clamp(box: Bounds) =
        Vector2(
            x.coerceIn(box.left, box.right),
            y.coerceIn(box.bottom, box.top)
        )

    fun invert() =
        Vector2(-x, -y)

    fun offset(vector: Vector2) =
        Vector2(x + vector.x, y + vector.y)

    fun snapToNormalisedOrthogonalDirection() =
        when {
            x == .0 && y == .0 -> Zero
            abs(x) > abs(y) -> if (x > 0) Left else Right
            else -> if (y < 0) Up else Down
        }

    companion object {
        val Zero = Vector2(.0, .0)
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

    constructor(width: Double, height: Double) : this(
        left = -width / 2.0,
        top = height / 2.0,
        right = width / 2.0,
        bottom = -height / 2.0,
    )

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

data class LerpOverTime(
    val durationNanos: Long,
    val startValue: Double,
    val endValue: Double,
    val accumulatedNanos: Long = 0,
    val easing: CubicBezierEasing = CubicBezierEasing(0.4, 0.0, 0.2, 1.0),
) {
    val value by lazy {
        if (durationNanos == 0L) endValue else {
            clamp(
                startValue + (endValue - startValue) *
                        easing.transform(accumulatedNanos.toDouble() / durationNanos.toDouble()),
                startValue,
                endValue,
            )
        }
    }
}

/**
 * Adapted from androidx.compose.animation.core.Easing
 */
data class CubicBezierEasing(
    private val a: Double,
    private val b: Double,
    private val c: Double,
    private val d: Double,
) {
    private fun evaluateCubic(a: Double, b: Double, m: Double): Double =
        3 * a * (1 - m) * (1 - m) * m +
                3 * b * (1 - m) * m * m +
                m * m * m

    fun transform(fraction: Double): Double {
        if (fraction > 0 && fraction < 1) {
            var start = 0.0
            var end = 1.0
            while (true) {
                val midpoint = (start + end) / 2
                val estimate = evaluateCubic(a, c, midpoint)
                if ((fraction - estimate).absoluteValue < CubicErrorBound)
                    return evaluateCubic(b, d, midpoint)
                if (estimate < fraction)
                    start = midpoint
                else
                    end = midpoint
            }
        } else {
            return fraction
        }
    }

    companion object {
        private const val CubicErrorBound: Double = 0.001
    }
}

fun Long.nanosToSeconds(): Double =
    this / 1000000000.0

fun Double.secondsToNanos(): Long =
    (this * 1000000000).toLong()
