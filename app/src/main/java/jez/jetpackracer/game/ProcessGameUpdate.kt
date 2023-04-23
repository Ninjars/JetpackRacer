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
            val playerVelocity = carriedPlayerVelocity + basePlayerVelocityChange + inputVelocity
            val prospectivePlayerPosition = player.position + playerVelocity * updateSeconds
            val playerPosition = Vector2(
                clamp(prospectivePlayerPosition.x, .0, gameBounds.width),
                clamp(prospectivePlayerPosition.y, .0, gameBounds.height),
            )
            val updatedViewOriginOffset =
                viewOriginOffset + (playerPosition - viewOriginOffset) * viewUpdateSpeedFactor * updateSeconds

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
            val collidingEntities = activeEntities.filter {
                it.isCollidingWith(playerPosition, player.collider)
            }
            val newOrOngoingCollisions = collidingEntities.map { collidingEntity ->
                val previousCollision = player.collisionStatus
                    .firstOrNull { it.collisionTarget == collidingEntity }
                CollisionStatus(
                    collisionTarget = collidingEntity,
                    didCollisionStartThisFrame = previousCollision == null,
                    collisionDurationNanos = previousCollision?.let { it.collisionDurationNanos + updateNanos }
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

            val secondsSinceLastSpawn = secondsSinceLastEnemySpawn + updateSeconds
            val spawnedEntities = spawnEntitiesIfPossible(
                gameBounds,
                enemySpawnConfig,
                secondsSinceLastSpawn,
            )

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


    private fun WorldEntity.isCollidingWith(
        playerPosition: Vector2,
        playerCollider: Collider.Circle
    ) =
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

private fun ClosedFloatingPointRange<Double>.random(): Double =
    start + Random.nextDouble() * (endInclusive - start)
