package com.example.ui.game

import androidx.compose.ui.graphics.Color
import java.util.Random
import kotlin.math.abs

// --- 1. Basic Type Definitions ---

data class Point(val x: Int, val y: Int)

enum class Direction { UP, DOWN, LEFT, RIGHT }

enum class PowerUpType {
    NONE,
    SPEED_BOOST,  // ⚡ Extra swiftness
    SHIELD,       // 🛡️ Immune to collisions
    DOUBLE_POINTS,// 💎 2x multiplier for points
    GHOST_MODE    // 👁️ Can pass through any obstacle/snake
}

data class FoodItem(
    val position: Point,
    val type: FoodType
)

enum class FoodType(val scoreValue: Int, val color: Color, val symbol: String) {
    NORMAL(1, Color(0xFFFF5555), "🍏"),
    GOLDEN(3, Color(0xFFFFD700), "🪙"),
    SPEED_BOOST(1, Color(0xFF55FFFF), "⚡"),
    SHIELD(1, Color(0xFF00FFCC), "🛡️"),
    DOUBLE_POINTS(1, Color(0xFFFF55FF), "💎"),
    GHOST(1, Color(0xFFAAAAAA), "👁️")
}

data class Snake(
    val id: String,
    val name: String,
    val body: List<Point>,
    val direction: Direction,
    val color: Color,
    val isAlive: Boolean = true,
    val score: Int = 0,
    val activePowerUp: PowerUpType = PowerUpType.NONE,
    val powerUpExpiryTime: Long = 0, // System.currentTimeMillis() when power expires
    val skinName: String = "Neon Solid"
)

// --- 2. Game Engine ---

class SnakeGameEngine(
    val gridWidth: Int = 26,
    val gridHeight: Int = 30,
    private val onPlayerEliminated: (killerName: String) -> Unit = { _ -> },
    private val onBotEliminated: (botName: String, killerName: String) -> Unit = { _, _ -> },
    private val onFoodEaten: (snakeName: String, foodSymbol: String) -> Unit = { _, _ -> },
    private val onPowerUpCollected: (snakeName: String, powerUp: PowerUpType) -> Unit = { _, _ -> }
) {
    private val random = Random()

    var snakes = mutableListOf<Snake>()
    var foods = mutableListOf<FoodItem>()
    var isGameOver = false
    var isPaused = false
    var matchStartTime = System.currentTimeMillis()

    init {
        resetGame()
    }

    fun resetGame() {
        snakes.clear()
        foods.clear()
        isGameOver = false
        isPaused = false
        matchStartTime = System.currentTimeMillis()

        // Spawn Player Snake centrally (bottom-half)
        snakes.add(
            Snake(
                id = "player",
                name = "You",
                body = listOf(Point(13, 20), Point(13, 21), Point(13, 22)),
                direction = Direction.UP,
                color = Color(0xFF39FF14) // Neon Cyber Green
            )
        )

        // Spawn 4 simulated network Bots in different corners
        val botNames = listOf("CyberSamurai", "NeonViper", "GlitchHacker", "RetroRider")
        val botColors = listOf(
            Color(0xFFFFD700), // Hologram Gold
            Color(0xFFFF007F), // Neon Pink
            Color(0xFF00FFFF), // Cyan Glitch
            Color(0xFFFF4500)  // Retro Rocket Orange
        )
        val botSkins = listOf("Laser Grid", "Hex Pulse", "Matrix Rain", "Cyber Steel")

        // Spawning positions
        val spawnPoints = listOf(
            listOf(Point(3, 4), Point(3, 3), Point(3, 2)),      // Top Left
            listOf(Point(22, 4), Point(22, 3), Point(22, 2)),    // Top Right
            listOf(Point(3, 25), Point(3, 26), Point(3, 27)),    // Bottom Left
            listOf(Point(22, 25), Point(22, 26), Point(22, 27))  // Bottom Right
        )

        val spawnDirs = listOf(Direction.DOWN, Direction.DOWN, Direction.UP, Direction.UP)

        for (i in botNames.indices) {
            snakes.add(
                Snake(
                    id = "bot_${i + 1}",
                    name = botNames[i],
                    body = spawnPoints[i],
                    direction = spawnDirs[i],
                    color = botColors[i],
                    skinName = botSkins[i]
                )
            )
        }

        // Spawn initial food items
        for (i in 0..4) {
            spawnFood()
        }
    }

    fun handleInput(direction: Direction) {
        val player = snakes.find { it.id == "player" } ?: return
        if (!player.isAlive) return

        // Prevent moving immediately backwards into oneself
        val isOpposite = when (direction) {
            Direction.UP -> player.direction == Direction.DOWN
            Direction.DOWN -> player.direction == Direction.UP
            Direction.LEFT -> player.direction == Direction.RIGHT
            Direction.RIGHT -> player.direction == Direction.LEFT
        }

        if (!isOpposite) {
            // Update player direction
            val index = snakes.indexOf(player)
            snakes[index] = player.copy(direction = direction)
        }
    }

    // Main Game Loop Tick
    fun gameTick() {
        if (isGameOver || isPaused) return

        // 1. Let Bots make decisions (Simple AI Pathfinding & Collision Avoidance)
        for (i in snakes.indices) {
            val snake = snakes[i]
            if (snake.id == "player" || !snake.isAlive) continue
            runBotAI(i)
        }

        val now = System.currentTimeMillis()
        val nextSnakesState = mutableListOf<Snake>()

        // 2. Move Snakes & check food eating
        for (snake in snakes) {
            if (!snake.isAlive) {
                nextSnakesState.add(snake)
                continue
            }

            // Clean expired powerups
            var activePower = snake.activePowerUp
            if (activePower != PowerUpType.NONE && snake.powerUpExpiryTime < now) {
                activePower = PowerUpType.NONE
            }

            val head = snake.body.first()
            val nextHead = getNextCell(head, snake.direction)

            // Check if food is eaten
            val eatenFoodIndex = foods.indexOfFirst { it.position == nextHead }
            val newBody = mutableListOf<Point>()
            newBody.add(nextHead)

            var updatedScore = snake.score
            var growthLength = 0

            if (eatenFoodIndex != -1) {
                val food = foods[eatenFoodIndex]
                foods.removeAt(eatenFoodIndex)

                // Power up handler
                var newPower = activePower
                var newExpiry = snake.powerUpExpiryTime

                val multiplier = if (activePower == PowerUpType.DOUBLE_POINTS) 2 else 1
                val addedPoints = food.type.scoreValue * multiplier
                updatedScore += addedPoints

                onFoodEaten(snake.name, food.type.symbol)

                when (food.type) {
                    FoodType.NORMAL -> { growthLength = 1 }
                    FoodType.GOLDEN -> { growthLength = 2 }
                    FoodType.SPEED_BOOST -> {
                        newPower = PowerUpType.SPEED_BOOST
                        newExpiry = now + 8000
                        onPowerUpCollected(snake.name, PowerUpType.SPEED_BOOST)
                    }
                    FoodType.SHIELD -> {
                        newPower = PowerUpType.SHIELD
                        newExpiry = now + 9000
                        onPowerUpCollected(snake.name, PowerUpType.SHIELD)
                    }
                    FoodType.DOUBLE_POINTS -> {
                        newPower = PowerUpType.DOUBLE_POINTS
                        newExpiry = now + 10000
                        onPowerUpCollected(snake.name, PowerUpType.DOUBLE_POINTS)
                    }
                    FoodType.GHOST -> {
                        newPower = PowerUpType.GHOST_MODE
                        newExpiry = now + 8000
                        onPowerUpCollected(snake.name, PowerUpType.GHOST_MODE)
                    }
                }

                // Add to standard body length with extra growth
                newBody.addAll(snake.body)
                for (g in 0 until growthLength) {
                    newBody.add(snake.body.last())
                }

                // Spawn replacements
                spawnFood()

                nextSnakesState.add(
                    snake.copy(
                        body = newBody,
                        score = updatedScore,
                        activePowerUp = newPower,
                        powerUpExpiryTime = newExpiry
                    )
                )
            } else {
                // Regular move, remove last element
                newBody.addAll(snake.body.dropLast(1))
                nextSnakesState.add(snake.copy(body = newBody))
            }
        }

        snakes = nextSnakesState

        // 3. Handle Obstacle & Multiplayer Battles Collisions
        val survivalSnakes = mutableListOf<Snake>()
        for (snake in snakes) {
            if (!snake.isAlive) {
                survivalSnakes.add(snake)
                continue
            }

            val head = snake.body.first()
            var survived = true
            var killerName = "the grid"

            // A. Wall collisions
            val isOOB = head.x < 0 || head.x >= gridWidth || head.y < 0 || head.y >= gridHeight
            if (isOOB) {
                if (snake.activePowerUp == PowerUpType.GHOST_MODE) {
                    // Wrap around in ghost mode
                    val wrappedX = (head.x + gridWidth) % gridWidth
                    val wrappedY = (head.y + gridHeight) % gridHeight
                    val wrappedBody = listOf(Point(wrappedX, wrappedY)) + snake.body.drop(1)
                    survivalSnakes.add(snake.copy(body = wrappedBody))
                    continue
                } else if (snake.activePowerUp == PowerUpType.SHIELD) {
                    // Shield bounces off at screen borders, turns snake around!
                    val bouncedDir = when (snake.direction) {
                        Direction.UP -> Direction.DOWN
                        Direction.DOWN -> Direction.UP
                        Direction.LEFT -> Direction.RIGHT
                        Direction.RIGHT -> Direction.LEFT
                    }
                    val currentHead = snake.body.first()
                    // Revert to inside borders
                    val safeX = currentHead.x.coerceIn(0, gridWidth - 1)
                    val safeY = currentHead.y.coerceIn(0, gridHeight - 1)
                    val correctedHead = Point(safeX, safeY)
                    val bouncedBody = listOf(correctedHead) + snake.body.drop(1)
                    survivalSnakes.add(snake.copy(body = bouncedBody, direction = bouncedDir))
                    continue
                } else {
                    survived = false
                    killerName = "Zone Border"
                }
            }

            // B. Collide with other snakes (including self!)
            if (survived) {
                for (otherSnake in snakes) {
                    if (!otherSnake.isAlive) continue

                    // Ignore if this snake has Ghost Mode active
                    if (snake.activePowerUp == PowerUpType.GHOST_MODE) continue

                    val isSelf = otherSnake.id == snake.id
                    val startsAt = if (isSelf) 1 else 0 // Skip head check for self

                    for (b in startsAt until otherSnake.body.size) {
                        val part = otherSnake.body[b]
                        if (head == part) {
                            // Hit something! Check if shielded
                            if (snake.activePowerUp == PowerUpType.SHIELD) {
                                // Shield survives!
                                break
                            }

                            // Multiplayer Rule check! Head-to-head collision
                            val isHeadCollision = !isSelf && b == 0
                            if (isHeadCollision) {
                                if (snake.score > otherSnake.score) {
                                    // I am larger, I survive! Other snake is dead.
                                    break
                                } else if (snake.score < otherSnake.score) {
                                    // Let other snake survive, eliminate me!
                                    survived = false
                                    killerName = otherSnake.name
                                    break
                                } else {
                                    // Equal size, crash BOTH or random coin toss
                                    survived = false
                                    killerName = otherSnake.name
                                    break
                                }
                            } else {
                                // Hit body of self or someone else, instant death
                                survived = false
                                killerName = otherSnake.name
                                break
                            }
                        }
                    }
                    if (!survived) break
                }
            }

            if (survived) {
                survivalSnakes.add(snake)
            } else {
                // SNAKE ELIMINATED!
                survivalSnakes.add(snake.copy(isAlive = false, body = emptyList()))

                // Turn eliminated snake cells into golden food scatter!
                for (cell in snake.body.take(5)) {
                    if (random.nextBoolean() && foods.size < 12) {
                        foods.add(FoodItem(cell, FoodType.NORMAL))
                    }
                }

                if (snake.id == "player") {
                    onPlayerEliminated(killerName)
                } else {
                    onBotEliminated(snake.name, killerName)
                }
            }
        }

        snakes = survivalSnakes

        // 4. Check if the active match has ended (All bots or players dead)
        val aliveCount = snakes.count { it.isAlive }
        val playerAlive = snakes.find { it.id == "player" }?.isAlive ?: false

        if (aliveCount <= 1) {
            isGameOver = true
        } else if (!playerAlive) {
            // Player is dead, but game continues in Spectator Mode among remaining bots
            // until only 1 remains!
        }
    }

    private fun getNextCell(head: Point, dir: Direction): Point {
        return when (dir) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }
    }

    // Spawn random foods with realistic weights
    private fun spawnFood() {
        var spawned = false
        var attempts = 0
        while (!spawned && attempts < 100) {
            attempts++
            val x = random.nextInt(gridWidth)
            val y = random.nextInt(gridHeight)
            val pos = Point(x, y)

            // Ensure not on top of any active snake
            val isOccupied = snakes.any { it.isAlive && it.body.contains(pos) } || foods.any { it.position == pos }
            if (!isOccupied) {
                // Deciding type
                val roll = random.nextInt(100)
                val type = when {
                    roll < 60 -> FoodType.NORMAL
                    roll < 75 -> FoodType.GOLDEN
                    roll < 82 -> FoodType.SPEED_BOOST
                    roll < 88 -> FoodType.SHIELD
                    roll < 94 -> FoodType.DOUBLE_POINTS
                    else -> FoodType.GHOST
                }
                foods.add(FoodItem(pos, type))
                spawned = true
            }
        }
    }

    // BOT AI MOVEMENT DECISIONS
    private fun runBotAI(index: Int) {
        val bot = snakes[index]
        val head = bot.body.first()

        // 1. Find nearest food
        val targetFood = foods.minByOrNull { getManhattanDistance(head, it.position) }
        val targetPos = targetFood?.position ?: Point(gridWidth / 2, gridHeight / 2)

        // 2. Evaluate all 4 possible direction nodes
        val candidates = listOf(
            Pair(Direction.UP, getNextCell(head, Direction.UP)),
            Pair(Direction.DOWN, getNextCell(head, Direction.DOWN)),
            Pair(Direction.LEFT, getNextCell(head, Direction.LEFT)),
            Pair(Direction.RIGHT, getNextCell(head, Direction.RIGHT))
        )

        // Filter out immediate death cells
        val safeCandidates = candidates.filter { (_, cell) ->
            // Inbounds check
            val inBounds = cell.x in 0 until gridWidth && cell.y in 0 until gridHeight
            if (!inBounds && bot.activePowerUp != PowerUpType.GHOST_MODE) return@filter false

            // Obstacle collision check
            var hitObstacle = false
            for (s in snakes) {
                if (!s.isAlive) continue
                if (bot.activePowerUp == PowerUpType.GHOST_MODE) continue

                // Can pass through if shielded, but prefer not to walk into bodies to be safe
                val startsAt = if (s.id == bot.id) 1 else 0
                for (b in startsAt until s.body.size) {
                    if (s.body[b] == cell) {
                        hitObstacle = true
                        break
                    }
                }
                if (hitObstacle) break
            }
            !hitObstacle
        }

        if (safeCandidates.isEmpty()) return // Bot accepts death, no safe moves left!

        // 3. Pick the safe move that minimizes distance to the food
        val bestMove = safeCandidates.minByOrNull { (_, cell) ->
            getManhattanDistance(cell, targetPos)
        }

        if (bestMove != null) {
            snakes[index] = bot.copy(direction = bestMove.first)
        }
    }

    private fun getManhattanDistance(p1: Point, p2: Point): Int {
        return abs(p1.x - p2.x) + abs(p1.y - p2.y)
    }
}
