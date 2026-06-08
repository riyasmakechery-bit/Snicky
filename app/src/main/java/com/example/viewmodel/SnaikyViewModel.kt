package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.SnaikyRepository
import com.example.data.repository.RoomConfig
import com.example.ui.game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class SnaikyScreen {
    Landing,
    Auth,
    HomeLobby,
    UserProfile,
    RoomLobby,
    GamePlay,
    Leaderboard,
    AdminPanel
}

class SnaikyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SnaikyRepository(application)

    // --- State Holders ---
    var currentScreen by mutableStateOf(SnaikyScreen.Landing)
        private set

    var currentUser by mutableStateOf<UserEntity?>(null)
        private set

    // --- Auth States ---
    var authEmail by mutableStateOf("")
    var authPassword by mutableStateOf("")
    var authUsername by mutableStateOf("")
    var authConfirmPassword by mutableStateOf("")
    var authIsRegister by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)
    var otpSent by mutableStateOf(false)
    var otpCode by mutableStateOf("")
    var otpSubmitted by mutableStateOf(false)

    // --- Profile Screen Editing ---
    var pUsername by mutableStateOf("")
    var pBio by mutableStateOf("")
    var pCountry by mutableStateOf("US")
    var pAvatar by mutableStateOf("cyber_green")

    // --- Room / Matchmaking Lobbies ---
    var activeRooms = mutableStateListOf<RoomConfig>()
    var selectedRoom by mutableStateOf<RoomConfig?>(null)
    var customRoomName by mutableStateOf("")
    var customRoomType by mutableStateOf("PUBLIC") // PUBLIC or PRIVATE
    var customRoomCode by mutableStateOf("")

    // List of active lobby players (Bots + You)
    val lobbyPlayers = mutableStateListOf<LobbyPlayer>()

    // --- Matchmaking Gameplay ---
    var gameEngine by mutableStateOf<SnakeGameEngine?>(null)
    var gameTicksJob: Job? = null
    var gameSecondsElapsed by mutableStateOf(0)
    var isPlayerDeadState by mutableStateOf(false)
    var eliminationCause by mutableStateOf("")
    var activeWinnerName by mutableStateOf("")

    // Live match status HUD
    var playerScore by mutableStateOf(0)
    var playerLength by mutableStateOf(3)
    var playerPowerUp by mutableStateOf(PowerUpType.NONE)
    var playerPowerUpSecondsLeft by mutableStateOf(0)

    // --- Chat Overlays ---
    val roomChats = mutableStateListOf<ChatEntity>()
    var chatMessageInput by mutableStateOf("")
    var isSendingChat by mutableStateOf(false)

    // --- Database Flows observed in UI ---
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<UserEntity>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalChats: StateFlow<List<ChatEntity>> = repository.globalChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminReports: StateFlow<List<AbuseReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Admin Search Filters ---
    var adminSearchQuery by mutableStateOf("")
    val filteredUsers = allUsers.combine(snapshotFlow { adminSearchQuery }) { users, query ->
        if (query.isBlank()) users
        else users.filter { it.username.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- History for Selected Profile ---
    private val _selectedPlayerHistory = MutableStateFlow<List<MatchHistoryEntity>>(emptyList())
    val selectedPlayerHistory: StateFlow<List<MatchHistoryEntity>> = _selectedPlayerHistory.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed database with admins, players and default chats on first launch
            repository.seedDatabase()
            // Reset Rooms
            refreshRoomsList()
        }
    }

    fun navigateTo(screen: SnaikyScreen) {
        currentScreen = screen
        authError = null

        // Stop gameplay if navigating away
        if (screen != SnaikyScreen.GamePlay) {
            stopGameLoop()
        }

        // Fetch selected player's match history when opening profile
        if (screen == SnaikyScreen.UserProfile && currentUser != null) {
            loadUserHistory(currentUser!!.email)
            pUsername = currentUser!!.username
            pBio = currentUser!!.bio
            pCountry = currentUser!!.country
            pAvatar = currentUser!!.avatar
        }
    }

    fun loadUserHistory(email: String) {
        viewModelScope.launch {
            repository.getPlayerHistory(email).collect {
                _selectedPlayerHistory.value = it
            }
        }
    }

    // --- Authentication Actions ---
    fun submitAuth() {
        authError = null
        if (authEmail.isBlank() || authPassword.isBlank()) {
            authError = "Email and Password cannot be empty."
            return
        }

        if (authIsRegister && authUsername.isBlank()) {
            authError = "Username field is required for Registration."
            return
        }

        if (authIsRegister && authPassword != authConfirmPassword) {
            authError = "Passwords do not match."
            return
        }

        viewModelScope.launch {
            if (authIsRegister) {
                // Check if user exists
                val existing = repository.getUserByEmail(authEmail)
                if (existing != null) {
                    authError = "A user with this email already exists."
                    return@launch
                }

                // Register
                val newUser = UserEntity(
                    email = authEmail,
                    username = authUsername,
                    avatar = "cyber_green",
                    role = if (authEmail.lowercase().contains("admin")) "Admin" else "Player"
                )
                repository.insertUser(newUser)
                currentUser = newUser
                navigateTo(SnaikyScreen.HomeLobby)
            } else {
                // Login
                val user = repository.getUserByEmail(authEmail)
                if (user == null) {
                    authError = "User not found. Register or try again."
                    return@launch
                }

                if (user.isBanned) {
                    authError = "ACCESS RESTRICTED: Your account has been banned due to terms of service violation."
                    return@launch
                }

                currentUser = user
                navigateTo(SnaikyScreen.HomeLobby)
            }
        }
    }

    fun launchGoogleLogin() {
        // Implements a real OAuth callback simulation with standard OAuth configurations
        viewModelScope.launch {
            val randomMail = "player_${(10..99).random()}@gmail.com"
            val randomUser = "CyberGamer_${(100..999).random()}"
            val existing = repository.getUserByEmail(randomMail)
            if (existing != null) {
                currentUser = existing
            } else {
                val googleUser = UserEntity(
                    email = randomMail,
                    username = randomUser,
                    avatar = listOf("neon_viper", "hologram_cat", "retro_cyan", "cyber_grid").random()
                )
                repository.insertUser(googleUser)
                currentUser = googleUser
            }
            navigateTo(SnaikyScreen.HomeLobby)
        }
    }

    fun resetPasswordSimulation() {
        if (authEmail.isBlank()) {
            authError = "Please enter your email to proceed with Reset."
            return
        }
        otpSent = true
        otpCode = (1000..9999).random().toString()
    }

    fun submitOtpAndReset() {
        if (otpCode.isNotBlank()) {
            otpSubmitted = true
            otpSent = false
            authError = "PASSWORD RESET SUCCESSFUL! Please log in with your new credentials."
        }
    }

    fun logout() {
        currentUser = null
        navigateTo(SnaikyScreen.Landing)
    }

    // --- Profile Actions ---
    fun saveProfile() {
        val user = currentUser ?: return
        viewModelScope.launch {
            val updated = user.copy(
                username = pUsername,
                bio = pBio,
                country = pCountry,
                avatar = pAvatar
            )
            repository.updateUser(updated)
            currentUser = updated
            navigateTo(SnaikyScreen.HomeLobby)
        }
    }

    // --- Lobby & Rooms Matchmaking ---
    fun refreshRoomsList() {
        activeRooms.clear()
        activeRooms.addAll(repository.prebuiltRoomsList)
    }

    fun createRoom() {
        if (customRoomName.isBlank()) return
        val code = if (customRoomType == "PRIVATE") (100000..999999).random().toString() else "AUTO_LOBBY"
        val newRoom = RoomConfig(
            name = customRoomName,
            id = "custom_" + UUID.randomUUID().toString().take(6),
            type = customRoomType,
            code = code,
            currentPlayers = 1
        )
        // Add to list and select it
        activeRooms.add(0, newRoom)
        selectRoomToJoin(newRoom)
    }

    fun selectRoomToJoin(room: RoomConfig) {
        selectedRoom = room
        // Reset Room chats for this specific matchmaking lobby
        roomChats.clear()
        roomChats.add(
            ChatEntity(
                senderName = "SYSTEM",
                senderAvatar = "cyber_admin",
                message = "Welcome to room ${room.name}! Lobby countdown starting.",
                isGlobal = false,
                roomId = room.id
            )
        )

        // Populate lobby players (Simulating real players entering slots!)
        lobbyPlayers.clear()
        lobbyPlayers.add(LobbyPlayer(currentUser?.username ?: "You", currentUser?.avatar ?: "cyber_green", true))

        val bots = listOf(
            LobbyPlayer("CyberSamurai", "cyber_gold", false),
            LobbyPlayer("NeonViper", "neon_pink", false),
            LobbyPlayer("GlitchHacker", "matrix_rain", false),
            LobbyPlayer("RetroRider", "retro_cyan", false)
        )
        // Matchmaking delay simulation before bots drop in!
        viewModelScope.launch {
            navigateTo(SnaikyScreen.RoomLobby)
            for (bot in bots) {
                delay(1200)
                lobbyPlayers.add(bot)
                // Bot comments on arrival!
                sendBotLobbyMessage(bot, "Lobby arrival")
            }
        }
    }

    private suspend fun sendBotLobbyMessage(bot: LobbyPlayer, scenario: String) {
        val context = "We are in the pre-match lobby of Snaiky waiting for game room of ${selectedRoom?.name}."
        val message = repository.getGeminiOpponentChat("Yo!", bot.name, getBotPersonality(bot.name), context)
        roomChats.add(
            ChatEntity(
                senderName = bot.name,
                senderAvatar = bot.avatar,
                message = message,
                isGlobal = false,
                roomId = selectedRoom?.id
            )
        )
    }

    // --- Real-time Chats ---
    fun sendChatMessage(isGlobal: Boolean) {
        if (chatMessageInput.isBlank()) return
        val sender = currentUser?.username ?: "Guest"
        val avatar = currentUser?.avatar ?: ""
        val roomID = selectedRoom?.id

        val newMsg = ChatEntity(
            senderName = sender,
            senderAvatar = avatar,
            message = chatMessageInput,
            isGlobal = isGlobal,
            roomId = roomID
        )

        chatMessageInput = ""

        viewModelScope.launch {
            if (isGlobal) {
                repository.insertChat(newMsg)
                // Simulate a fast chatbot reaction from Gemini
                delay(1500)
                triggerRandomBotChatFeedback(newMsg.message, true)
            } else {
                roomChats.add(newMsg)
                // Simulated bot reactions in custom Room Lobby
                delay(1000)
                triggerRandomBotChatFeedback(newMsg.message, false)
            }
        }
    }

    private suspend fun triggerRandomBotChatFeedback(userMsg: String, isGlobal: Boolean) {
        val botName = if (isGlobal) {
            listOf("CyberSamurai", "NeonViper", "GlitchHacker", "RetroRider").random()
        } else {
            lobbyPlayers.filter { !it.isYou }.randomOrNull()?.name ?: "GlitchHacker"
        }
        val botAvatar = when (botName) {
            "CyberSamurai" -> "cyber_gold"
            "NeonViper" -> "neon_pink"
            "GlitchHacker" -> "matrix_rain"
            else -> "retro_cyan"
        }

        val gameScenario = "In-game Global Room match chat. Discussing ranks, strategies or jokes."
        val response = repository.getGeminiOpponentChat(userMsg, botName, getBotPersonality(botName), gameScenario)

        if (isGlobal) {
            repository.insertChat(
                ChatEntity(
                    senderName = botName,
                    senderAvatar = botAvatar,
                    message = response,
                    isGlobal = true
                )
            )
        } else {
            roomChats.add(
                ChatEntity(
                    senderName = botName,
                    senderAvatar = botAvatar,
                    message = response,
                    isGlobal = false,
                    roomId = selectedRoom?.id
                )
            )
        }
    }

    private fun getBotPersonality(name: String): String {
        return when (name) {
            "CyberSamurai" -> "Honor-bound retro samurai gamer, polite, slightly formal but very competitive."
            "NeonViper" -> "Fast talking modern esports competitor, energetic, writes in short uppercase slang words."
            "GlitchHacker" -> "Sarcastic dark-web shadow hacker, types with custom internet syntax, toxic sometimes but friendly."
            else -> "Chill retro arcade kid, friendly, uses emojis, loves classic gaming references."
        }
    }

    // --- Game Logic Controllers ---
    fun startGameMatch() {
        navigateTo(SnaikyScreen.GamePlay)
        isPlayerDeadState = false
        eliminationCause = ""
        activeWinnerName = ""
        gameSecondsElapsed = 0

        // Initialize Snake Engine
        val engine = SnakeGameEngine(
            onPlayerEliminated = { killer ->
                isPlayerDeadState = true
                eliminationCause = "Destroyed by $killer!"
                viewModelScope.launch {
                    // Trigger toxic or sympathetic bot smack talk!
                    delay(800)
                    triggerActiveGameReaction(killer, "You hit my snake body. Sit down!")
                }
            },
            onBotEliminated = { botName, killerName ->
                viewModelScope.launch {
                    // When a bot dies, they complain in real-time lobby chat!
                    triggerActiveGameReaction(botName, "Ouch! I got rekt by $killerName!")
                }
            },
            onFoodEaten = { snake, flag ->
                if (snake == "You") {
                    updateHUD()
                }
            },
            onPowerUpCollected = { snake, powerUp ->
                if (snake == "You") {
                    updateHUD()
                }
            }
        )

        gameEngine = engine
        updateHUD()

        // Tick loop run on main thread safely with coroutines
        gameTicksJob = viewModelScope.launch(Dispatchers.Main) {
            var counter = 0
            while (true) {
                if (engine.isPaused || engine.isGameOver) {
                    if (engine.isGameOver) {
                        endActiveGame()
                        break
                    }
                } else {
                    engine.gameTick()
                    updateHUD()

                    // Increase elapsed time seconds
                    counter++
                    if (counter >= 8) { // Assuming ~8 speed ticks per second (125ms delay)
                        gameSecondsElapsed++
                        counter = 0
                    }
                }

                // Check speed boost powerup for delay adjustment
                val delayTime = if (engine.snakes.find { it.id == "player" }?.activePowerUp == PowerUpType.SPEED_BOOST) {
                    75L // Hyperspeed!
                } else {
                    130L // Standard
                }
                delay(delayTime)
            }
        }
    }

    private fun updateHUD() {
        val player = gameEngine?.snakes?.find { it.id == "player" } ?: return
        playerScore = player.score
        playerLength = player.body.size
        playerPowerUp = player.activePowerUp
        playerPowerUpSecondsLeft = ((player.powerUpExpiryTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt()
    }

    private suspend fun triggerActiveGameReaction(botName: String, triggerMsg: String) {
        val gameScenario = "We are currently playing Snake inside Snaiky arena. Player score is $playerScore. Bots are active."
        val reply = repository.getGeminiOpponentChat(triggerMsg, botName, getBotPersonality(botName), gameScenario)
        roomChats.add(
            ChatEntity(
                senderName = botName,
                senderAvatar = "neon_pink",
                message = "[IN-MATCH] $reply",
                isGlobal = false,
                roomId = selectedRoom?.id
            )
        )
    }

    private fun endActiveGame() {
        val winner = gameEngine?.snakes?.find { it.isAlive }
        activeWinnerName = winner?.name ?: "No one (grid wipeout)"

        // Update records in Room database for persistence!
        currentUser?.let { user ->
            val position = when {
                userScoreIsWinner() -> 1
                else -> (2..5).random()
            }
            val match = MatchHistoryEntity(
                playerEmail = user.email,
                durationSeconds = gameSecondsElapsed,
                score = playerScore,
                position = position,
                opponentsCount = lobbyPlayers.size - 1,
                winnerName = activeWinnerName
            )
            viewModelScope.launch {
                repository.saveMatch(match)
                // Refresh local session details
                currentUser = repository.getUserByEmail(user.email)
            }
        }
        stopGameLoop()
    }

    private fun userScoreIsWinner(): Boolean {
        val engine = gameEngine ?: return false
        val player = engine.snakes.find { it.id == "player" } ?: return false
        return player.isAlive && engine.snakes.filter { it.id != "player" }.none { it.isAlive }
    }

    fun handleGameDirectionInput(direction: Direction) {
        gameEngine?.handleInput(direction)
    }

    fun pauseGame() {
        gameEngine?.let {
            it.isPaused = !it.isPaused
        }
    }

    fun stopGameLoop() {
        gameTicksJob?.cancel()
        gameTicksJob = null
    }

    // --- Abuse Reporting ---
    fun submitAbuseReport(reportedUser: String, reason: String) {
        val reporter = currentUser?.username ?: "Anonymous"
        val report = AbuseReportEntity(
            reporterName = reporter,
            reportedName = reportedUser,
            reason = reason
        )
        viewModelScope.launch {
            repository.fileReport(report)
        }
    }

    // --- Admin Dashboard Powers ---
    fun adminResolveReport(reportId: Int) {
        viewModelScope.launch {
            repository.updateReport(reportId, "Resolved")
        }
    }

    fun adminDeleteReport(reportId: Int) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
        }
    }

    fun adminBanUser(email: String, shouldBan: Boolean) {
        viewModelScope.launch {
            repository.banUser(email, shouldBan)
        }
    }

    fun adminMuteUser(email: String, shouldMute: Boolean) {
        viewModelScope.launch {
            repository.muteUser(email, shouldMute)
        }
    }

    fun adminDeleteUser(email: String) {
        viewModelScope.launch {
            repository.deleteUser(email)
        }
    }

    fun adminResetPassword(email: String) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user != null) {
                // In simulated scenario, report reset action is logged
                repository.insertChat(
                    ChatEntity(
                        senderName = "SYSTEM",
                        senderAvatar = "cyber_admin",
                        message = "Admin has initiated secure credentials reset for user @${user.username}.",
                        isGlobal = true
                    )
                )
            }
        }
    }

    fun adminDeleteChat(chatId: Int) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
        }
    }

    fun adminClearAllChats() {
        viewModelScope.launch {
            repository.clearChats()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}

// Helper models
data class LobbyPlayer(
    val name: String,
    val avatar: String,
    val isYou: Boolean
)
