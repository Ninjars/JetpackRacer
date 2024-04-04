package jez.jetpackracer.game

import androidx.core.math.MathUtils.clamp
import jez.jetpackracer.game.GameEngineState.GameInput
import jez.jetpackracer.game.Vector2.Companion.Down
import jez.jetpackracer.game.Vector2.Companion.Left
import jez.jetpackracer.game.Vector2.Companion.Right
import jez.jetpackracer.game.Vector2.Companion.Up
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

object ProcessGameUpdate : (WorldState, GameInput, Long) -> WorldState {
    override fun invoke(initialState: WorldState, input: GameInput, updateNanos: Long): WorldState =
        with(initialState) {
            val updateSeconds: Double = updateNanos.nanosToSeconds()

            // Update world velocity with player velocity, acceleration and apply friction
            val updatedWorldSpeed =
                worldSpeed.copy(accumulatedNanos = worldSpeed.accumulatedNanos + updateNanos)
            val worldVelocity = worldMovementVector * updatedWorldSpeed.value
            val worldDistanceThisFrame = worldVelocity * updateSeconds

            // Update world offset
            val updatedWorldOrigin = worldOrigin + worldDistanceThisFrame

            // update player
            val carriedPlayerVelocity =
                player.velocity - player.velocity * player.friction * updateSeconds
            val basePlayerVelocityChange = player.baseAcceleration
            val inputVelocity = player.maxInputAcceleration * input.movementVector
            var playerVelocity = carriedPlayerVelocity + basePlayerVelocityChange + inputVelocity
            val prospectivePlayerPosition = player.position + playerVelocity * updateSeconds
            var playerPosition = Vector2(
                clamp(prospectivePlayerPosition.x, .0, gameBounds.width),
                clamp(prospectivePlayerPosition.y, .0, gameBounds.height),
            )

            // Update entities
            val dominantVector = worldVelocity.dominantDirection()
            val despawnBuffer = min(gameBounds.width, gameBounds.height) * 0.1f
            val activeEntities = entities.filterNot {
                // Remove old entities that are outside bounds
                val entityBounds = it.boundingBox
                when (dominantVector) {
                    Up -> entityBounds.top < gameBounds.bottom - despawnBuffer
                    Down -> entityBounds.bottom > gameBounds.top + despawnBuffer
                    Left -> entityBounds.left > gameBounds.right + despawnBuffer
                    Right -> entityBounds.right < gameBounds.left - despawnBuffer
                    else -> true
                }
            }.map { entity ->
                // Update entities position
                with(entity) {
                    copy(
                        position = (position + velocity * updateSeconds)
                            .let { if (isStaticToGameBounds) it else it - worldDistanceThisFrame }
                    )
                }
            }

            // check for player/entity collisions, tracking start, duration and end of collisions
            val collidingEntities = activeEntities.mapNotNull { entity ->
                entity.collisionPoint(playerPosition, player.collider)?.let { Pair(entity, it) }
            }
            val newOrOngoingCollisions = collidingEntities.map { (entity, collisionPos) ->
                val previousCollision = player.collisionStatus
                    .firstOrNull { it.collisionTarget == entity }
                CollisionStatus(
                    collisionTarget = entity,
                    collisionPosition = collisionPos,
                    didCollisionStartThisFrame = previousCollision == null,
                    collisionDurationNanos = previousCollision?.let { it.collisionDurationNanos + updateNanos }
                        ?: 0,
                )
            }
            val endingCollisions = player.collisionStatus
                .mapNotNull { collision ->
                    when {
                        collision.collisionTarget == null -> null
                        collidingEntities.any { it.first == collision.collisionTarget } -> null
                        else ->
                            CollisionStatus(
                                collisionTarget = null,
                                collisionPosition = null,
                                didCollisionStartThisFrame = false,
                                collisionDurationNanos = collision.collisionDurationNanos,
                            )
                    }
                }

            // update player velocity and position based on collisions
            val snappedFatalCollisionVector =
                worldMovementVector.snapToNormalisedOrthogonalDirection()
            var playerHasCrashed = false
            newOrOngoingCollisions.forEach {
                if (it.collisionPosition != null) {
                    val collisionVector = (playerPosition - it.collisionPosition).snapToNormalisedOrthogonalDirection()
                    // did the player just face-plant?
                    if (snappedFatalCollisionVector == collisionVector) {
                        playerHasCrashed = true
                        playerPosition = it.collisionPosition - collisionVector * player.collider.radius

                    // is player scraping their tail?
                    } else if (snappedFatalCollisionVector.invert() == collisionVector) {
                        playerPosition = it.collisionPosition - collisionVector * player.collider.radius

                    // player is side-swiping an obstacle
                    } else {
//                        val snappedPlayerVelocityVector = playerVelocity.snapToNormalisedOrthogonalDirection()
//                        if (snappedPlayerVelocityVector == collisionVector) {
//                            playerVelocity = Vector2.Zero//playerVelocity.invert() * 0.7
//                        }
                        playerPosition = it.collisionPosition - collisionVector * player.collider.radius
                    }
                }
            }

            val secondsSinceLastSpawn = secondsSinceLastEnemySpawn + updateSeconds
            val spawnedEntities = spawnEntitiesIfPossible(
                gameBounds,
                enemySpawnConfig,
                secondsSinceLastSpawn,
            )

            val updatedViewOriginOffset =
                viewOriginOffset + (playerPosition - viewOriginOffset) * viewUpdateSpeedFactor * updateSeconds

            return initialState.copy(
                elapsedTimeNanos = elapsedTimeNanos + updateNanos,
                worldSpeed = updatedWorldSpeed,
                worldOrigin = updatedWorldOrigin,
                viewOriginOffset = updatedViewOriginOffset,
                player = player.copy(
                    position = playerPosition,
                    velocity = playerVelocity,
                    collisionStatus = newOrOngoingCollisions + endingCollisions
                ),
                entities = spawnedEntities?.let { activeEntities + it } ?: activeEntities,
                secondsSinceLastEnemySpawn = spawnedEntities?.let { .0 } ?: secondsSinceLastSpawn
            )
        }

    private fun spawnEntitiesIfPossible(
        gameBounds: Bounds,
        enemySpawnConfig: WorldState.EnemySpawnConfig,
        secondsSinceLastSpawn: Double
    ): List<WorldEntity>? =
        with(enemySpawnConfig) {
            if (secondsSinceLastSpawn < spawnIntervalSeconds) {
                null
            } else {
                val bounds = Bounds(width.random(), height.random())
                listOf(
                    WorldEntity(
                        id = UUID.randomUUID().toString(),
                        position = Vector2(
                            (0.0..gameBounds.width).random(),
                            gameBounds.height + bounds.height * .5
                        ),
                        visuals = EntityVis.SolidColor(bounds, color),
                        velocity = Vector2.Zero,
                        collider = Collider.Box(bounds),
                        isStaticToGameBounds = false,
                    )
                )
            }
        }


    private fun WorldEntity.collisionPoint(
        playerPosition: Vector2,
        playerCollider: Collider.Circle
    ): Vector2? =
        when (collider) {
            is Collider.Circle ->
                circleCollision(
                    collider.radius + playerCollider.radius,
                    position,
                    playerPosition
                )
            is Collider.Box ->
                circleToBoxCollision(
                    playerPosition,
                    playerCollider.radius,
                    boundingBox,
                )
        }

    private fun circleCollision(distance: Double, position1: Vector2, position2: Vector2) =
        if (distance * distance <= Vector2.sqrMag(position1, position2)) {
            position1 + (position2 - position1) * 0.5
        } else {
            null
        }

    /**
     * Axis-aligned bounding box collision check.
     * Requires transforming the coordinate space to work with non-axis-aligned boxes.
     */
    private fun circleToBoxCollision(
        circlePos: Vector2,
        circleRadius: Double,
        box: Bounds,
    ): Vector2? {
        val distX = abs(circlePos.x - box.center.x)
        val distY = abs(circlePos.y - box.center.y)

        // proximity check
        if (distX > box.width / 2.0 + circleRadius || distY > box.height / 2.0 + circleRadius)
            return null

        // within box check
        if (distX <= box.width / 2.0 || distY <= box.height / 2.0)
            return (box.center + (circlePos - box.center)).clamp(box)

        val dx = distX - box.width / 2.0
        val dy = distY - box.height / 2.0
        return if (dx * dx + dy * dy <= circleRadius * circleRadius) {
            return (box.center + (circlePos - box.center)).clamp(box)
        } else {
            null
        }
    }

}

private fun ClosedFloatingPointRange<Double>.random(): Double =
    start + Random.nextDouble() * (endInclusive - start)
