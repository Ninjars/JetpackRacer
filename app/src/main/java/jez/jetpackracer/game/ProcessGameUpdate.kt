package jez.jetpackracer.game

import jez.jetpackracer.game.Vector2.Companion.Down
import jez.jetpackracer.game.Vector2.Companion.Left
import jez.jetpackracer.game.Vector2.Companion.Right
import jez.jetpackracer.game.Vector2.Companion.Up
import kotlin.math.abs

private fun Long.nanosToSeconds() =
    this / 1000000000.0

object ProcessGameUpdate : (WorldState, Long) -> WorldState {
    private val velocityBleedRate = Vector2(.25, 0.5)

    override fun invoke(initialState: WorldState, update: Long): WorldState =
        with(initialState) {
            val updateSeconds: Double = update.nanosToSeconds()

            // Update player velocity
            val incomingPlayerVelocity =
                player.velocity + player.baseAcceleration * updateSeconds

            // Bleed player velocity into world velocity
            // TODO: better mechanism to adjust bleed rate to be proportional to position
            //   within desired player movement bounds, to keep player within a focused area
            //   of the screen and to move the world faster as they approach the limits of that space
            val deltaV = incomingPlayerVelocity - baseWorldVelocity
            val bleedV = deltaV * velocityBleedRate * updateSeconds

            val playerVelocity = incomingPlayerVelocity - bleedV
            val worldVelocity = (baseWorldVelocity + bleedV) * player.friction

            val updatedWorldOffset = baseWorldOffset + worldVelocity

            val dominantVector = worldVelocity.dominantDirection()
            val activeEntities = entities.filter {
                // Remove old entities that are outside bounds
                val entityBounds = it.boundingBox.offset(-updatedWorldOffset)
                when (dominantVector) {
                    Up -> entityBounds.top < localGameBounds.bottom
                    Down -> entityBounds.bottom > localGameBounds.top
                    Left -> entityBounds.left > localGameBounds.right
                    Right -> entityBounds.right < localGameBounds.left
                    else -> true
                }
            }.map {
                // Update entities position
                it.copy(
                    worldPosition = it.worldPosition + it.velocity * updateSeconds,
                )
            }

            // check for player/entity collisions, tracking start, duration and end of collisions
            val collidingEntities = activeEntities.filter {
                player.isCollidingWith(it)
            }
            val newOrOngoingCollisions = collidingEntities.map { collidingEntity ->
                val previousCollision = player.collisionStatus
                    .firstOrNull { it.collisionTarget == collidingEntity }
                CollisionStatus(
                    collisionTarget = collidingEntity,
                    didCollisionStartThisFrame = previousCollision == null,
                    collisionDurationNanos = previousCollision?.let { it.collisionDurationNanos + update }
                        ?: 0,
                )
            }
            val endingCollisions = player.collisionStatus
                .filterNot { it.collisionTarget == null }
                .filterNot { collidingEntities.contains(it.collisionTarget) }
                .map {
                    CollisionStatus(
                        collisionTarget = null,
                        didCollisionStartThisFrame = false,
                        collisionDurationNanos = it.collisionDurationNanos,
                    )
                }

            return initialState.copy(
                baseWorldVelocity = worldVelocity,
                baseWorldOffset = updatedWorldOffset,
                player = player.copy(
                    velocity = playerVelocity,
                    worldPosition = player.worldPosition + playerVelocity * updateSeconds,
                    collisionStatus = newOrOngoingCollisions + endingCollisions
                ),
                entities = activeEntities,
            )
        }

    private fun PlayerState.isCollidingWith(entity: WorldEntity) =
        when (entity.collider) {
            is Collider.Circle ->
                circleCollision(
                    collider.radius + entity.collider.radius,
                    worldPosition,
                    entity.worldPosition
                )
            is Collider.Box ->
                circleToBoxCollision(
                    worldPosition,
                    collider.radius,
                    entity.boundingBox,
                )
        }

    private fun circleCollision(distance: Double, position1: Vector2, position2: Vector2) =
        distance * distance <= Vector2.sqrMag(position1, position2)

    /**
     * Axis-aligned bounding box collision check.
     * Requires transforming the coordinate space to work with non-axis-aligned boxes.
     */
    private fun circleToBoxCollision(
        circlePos: Vector2,
        circleRadius: Double,
        box: Bounds,
    ): Boolean {
        val distX = abs(circlePos.x - box.center.x)
        val distY = abs(circlePos.y - box.center.y)

        // proximity check
        if (distX > box.width / 2.0 + circleRadius) return false
        if (distY > box.height / 2.0 + circleRadius) return false

        // within box check
        if (distX <= box.width / 2.0) return true
        if (distY <= box.height / 2.0) return true

        val dx = distX - box.width / 2.0
        val dy = distY - box.height / 2.0
        return dx * dx + dy * dy <= circleRadius * circleRadius
    }

}
