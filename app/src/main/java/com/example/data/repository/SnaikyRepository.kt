package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.BuildConfig
import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- 1. Retrofit Client for Gemini API ---

data class Part(val text: String? = null)

data class Content(val parts: List<Part>)

data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

data class Candidate(val content: Content)

data class GenerateContentResponse(val candidates: List<Candidate>)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- 2. Snaiky Repository ---

class SnaikyRepository(context: Context) {

    private val database: SnaikyDatabase = Room.databaseBuilder(
        context.applicationContext,
        SnaikyDatabase::class.java,
        "snaiky_db"
    )
    .fallbackToDestructiveMigration()
    .build()

    val userDao: UserDao = database.userDao()
    val matchHistoryDao: MatchHistoryDao = database.matchHistoryDao()
    val chatDao: ChatDao = database.chatDao()
    val abuseReportDao: AbuseReportDao = database.abuseReportDao()

    // --- Observable Flows ---
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val leaderboard: Flow<List<UserEntity>> = userDao.getLeaderboard()
    val globalChats: Flow<List<ChatEntity>> = chatDao.getGlobalChats()
    val allReports: Flow<List<AbuseReportEntity>> = abuseReportDao.getAllReports()

    fun getRoomChats(roomId: String): Flow<List<ChatEntity>> = chatDao.getRoomChats(roomId)
    fun getPlayerHistory(email: String): Flow<List<MatchHistoryEntity>> = matchHistoryDao.getHistoryForPlayer(email)

    // --- Auth & Profile ---
    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun banUser(email: String, isBanned: Boolean) = withContext(Dispatchers.IO) {
        userDao.updateUserBanned(email, isBanned)
    }

    suspend fun muteUser(email: String, isMuted: Boolean) = withContext(Dispatchers.IO) {
        userDao.updateUserMuted(email, isMuted)
    }

    suspend fun deleteUser(email: String) = withContext(Dispatchers.IO) {
        userDao.deleteUserByEmail(email)
    }

    // --- Matches ---
    suspend fun saveMatch(match: MatchHistoryEntity) = withContext(Dispatchers.IO) {
        matchHistoryDao.insertMatch(match)
        // Also update user profile statistics
        val user = userDao.getUserByEmail(match.playerEmail)
        if (user != null) {
            val isWin = match.position == 1
            val updatedUser = user.copy(
                totalGames = user.totalGames + 1,
                wins = if (isWin) user.wins + 1 else user.wins,
                losses = if (!isWin) user.losses + 1 else user.losses,
                highestScore = if (match.score > user.highestScore) match.score else user.highestScore
            )
            userDao.insertUser(updatedUser)
        }
    }

    // --- Chat & Reports ---
    suspend fun insertChat(chat: ChatEntity) = withContext(Dispatchers.IO) {
        chatDao.insertChat(chat)
    }

    suspend fun deleteChat(chatId: Int) = withContext(Dispatchers.IO) {
        chatDao.deleteChatById(chatId)
    }

    suspend fun clearChats() = withContext(Dispatchers.IO) {
        chatDao.clearAllChats()
    }

    suspend fun fileReport(report: AbuseReportEntity) = withContext(Dispatchers.IO) {
        abuseReportDao.insertReport(report)
    }

    suspend fun updateReport(reportId: Int, status: String) = withContext(Dispatchers.IO) {
        abuseReportDao.updateReportStatus(reportId, status)
    }

    suspend fun deleteReport(reportId: Int) = withContext(Dispatchers.IO) {
        abuseReportDao.deleteReport(reportId)
    }

    // --- Matchmaking Room Simulation (In-Memory Helper) ---
    val prebuiltRoomsList = listOf(
        RoomConfig("Neon Plaza", "neon_room", "PUBLIC", "AUTO_MATCH", 5),
        RoomConfig("The Cyber Grid", "cyber_grid", "PUBLIC", "L289P9", 8),
        RoomConfig("Hacker Den", "hacker_den", "PUBLIC", "H4CK3R", 4),
        RoomConfig("Toxic Wasteland", "toxic_waste", "PUBLIC", "SLYTHR", 6)
    )

    // --- Gemini AI Conversation Simulator (Simulated Multiplayer Lobby Chat) ---
    suspend fun getGeminiOpponentChat(
        playerMessage: String,
        opponentName: String,
        opponentPersonality: String,
        gameContext: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            // Fallback mock responses if API key is not set up
            return@withContext getLocalBotReaction(playerMessage, opponentName, opponentPersonality, gameContext)
        }

        val prompt = """
            You are simulating an active online player named '$opponentName' in a competitive real-time multiplayer snake mobile game called Snaiky.
            Your personality profile: $opponentPersonality.
            The current match situation: $gameContext.
            The other player said: "$playerMessage".
            
            Generate a short, snappy, conversational reply representing yourself playing the game.
            Rules:
            1. Under 15 words.
            2. High energy, cyber/arcade gaming themed.
            3. Do NOT mention you are an AI or bot. Play along as a real human.
            4. Use casual gaming slang matching your personality (cyberpunk, retro, friendly, toxic, hyperactive, competitive).
            5. Return ONLY the chat message text, no quotation marks or meta-text.
        """.trimIndent()

        try {
            // We build the body manually or since we use Retrofit with Moshi, we can construct standard map-based content
            val textPart = mapOf("text" to prompt)
            val parts = listOf(textPart)
            val content = mapOf("parts" to parts)
            val requestBody = mapOf("contents" to listOf(content))

            // To support Moshi's Converter, let's create structural object
            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "You are $opponentName, a real gaming player in Snaiky.")))
            )

            val rawResponse = RetrofitClient.service.generateContent(apiKey, req)
            val output = rawResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!output.isNullOrEmpty()) {
                return@withContext output
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback default response
        return@withContext getLocalBotReaction(playerMessage, opponentName, opponentPersonality, gameContext)
    }

    private fun getLocalBotReaction(
        playerMsg: String,
        name: String,
        personality: String,
        context: String
    ): String {
        val msg = playerMsg.lowercase()
        return when {
            msg.contains("hi") || msg.contains("hello") || msg.contains("yo") -> {
                when (personality) {
                    "Toxic" -> "Yo trash, ready to get eaten? 🐍"
                    "Friendly" -> "Hey! Good luck, let's have a clean game!"
                    "Competitive" -> "Yo. Focus up, I'm taking that #1 spot."
                    else -> "Sup! Let's get eating. 🍏"
                }
            }
            msg.contains("noob") || msg.contains("rekt") || msg.contains("lol") -> {
                when (personality) {
                    "Toxic" -> "Talk when you reach my length, kid."
                    "Friendly" -> "Haha nice play! 😂"
                    "Competitive" -> "Focus on your tail, buddy."
                    else -> "LMAO let's see you try!"
                }
            }
            context.contains("eliminated") -> {
                when (personality) {
                    "Toxic" -> "EASY REKT! Sit down! 🗑️"
                    "Friendly" -> "Aww, almost! GG!"
                    "Competitive" -> "One down, more food for me."
                    else -> "Oof that wall hit was brutal!"
                }
            }
            context.contains("powerup") -> {
                when (personality) {
                    "Toxic" -> "Nice hacks buddy, still gonna devour you."
                    "Friendly" -> "Oh whoa, double points is active!"
                    "Competitive" -> "I need that speed boost immediately."
                    else -> "Power-ups are wild in this room!"
                }
            }
            else -> {
                when (personality) {
                    "Toxic" -> "Get out of my way or get crushed."
                    "Friendly" -> "This is super fun!"
                    "Competitive" -> "I'm 100 points away from breaking the record."
                    else -> "Ayo watch out for that gold coin!"
                }
            }
        }
    }

    // Populate default high scores to play against
    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val count = database.userDao().getUserByEmail("admin@snaiky.com")
        if (count == null) {
            // Seed Admin
            userDao.insertUser(
                UserEntity(
                    email = "admin@snaiky.com",
                    username = "SnaikyAdmin",
                    avatar = "cyber_admin",
                    role = "Admin",
                    country = "US",
                    bio = "System Administrator. Play fair, stay clean."
                )
            )

            // Seed Competitor Players
            val bots = listOf(
                UserEntity("cyber_samurai@cyber.net", "CyberSamurai", "cyber_gold", "Player", "JP", "Sharper than a laser sword. ⚔️", 45, 12, 33, 1450),
                UserEntity("neon_viper@retro.io", "NeonViper", "neon_pink", "Player", "KR", "Fast, silent, glowing. ⚡", 38, 8, 30, 980),
                UserEntity("glitch_hacker@dark.net", "GlitchHacker", "matrix_rain", "Player", "UA", "Zeroes and ones are my playground.", 62, 22, 40, 1820),
                UserEntity("retro_joystick@arcade.com", "RetroRider", "retro_cyan", "Player", "CA", "Insert coin to continue! 🕹️", 27, 4, 23, 760),
                UserEntity("hyper_active@fast.org", "HyperSnaik", "hyper_red", "Player", "GB", "Speed is my primary fuel! 🏁", 50, 15, 35, 1210)
            )
            bots.forEach { userDao.insertUser(it) }

            // Seed global chat history
            chatDao.insertChat(ChatEntity(senderName = "CyberSamurai", senderAvatar = "cyber_gold", message = "Who's ready for the weekend tournament?", isGlobal = true))
            chatDao.insertChat(ChatEntity(senderName = "NeonViper", senderAvatar = "neon_pink", message = "Tournament? count me in, bringing my custom skin!", isGlobal = true))
            chatDao.insertChat(ChatEntity(senderName = "GlitchHacker", senderAvatar = "matrix_rain", message = "Already hacked the leaderboards jk jk 😂", isGlobal = true))
            chatDao.insertChat(ChatEntity(senderName = "RetroRider", senderAvatar = "retro_cyan", message = "GG to whoever just beat me in Neon Plaza!", isGlobal = true))
        }
    }
}

data class RoomConfig(
    val name: String,
    val id: String,
    val type: String, // PUBLIC, PRIVATE
    val code: String,
    val currentPlayers: Int
)
