package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. Entities ---

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val username: String,
    val avatar: String, // String representation e.g. "cyber_green", "neon_pulse", "hologram_cat"
    val role: String = "Player", // "Player" or "Admin"
    val country: String = "US",
    val bio: String = "Survive, grow, dominate.",
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val highestScore: Int = 0,
    val isBanned: Boolean = false,
    val isMuted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerEmail: String,
    val matchDate: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val score: Int,
    val position: Int, // e.g. 1st, 2nd, 3rd, etc.
    val opponentsCount: Int,
    val winnerName: String
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val senderAvatar: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGlobal: Boolean = true,
    val roomId: String? = null
)

@Entity(tableName = "abuse_reports")
data class AbuseReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reporterName: String,
    val reportedName: String,
    val reason: String, // "Cheating", "Abuse", "Spam", "Botting"
    val status: String = "Pending", // "Pending" or "Resolved"
    val timestamp: Long = System.currentTimeMillis()
)

// --- 2. DAOs ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY highestScore DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'Player' ORDER BY highestScore DESC")
    fun getLeaderboard(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isBanned = :isBanned WHERE email = :email")
    suspend fun updateUserBanned(email: String, isBanned: Boolean)

    @Query("UPDATE users SET isMuted = :isMuted WHERE email = :email")
    suspend fun updateUserMuted(email: String, isMuted: Boolean)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)
}

@Dao
interface MatchHistoryDao {
    @Query("SELECT * FROM match_history WHERE playerEmail = :email ORDER BY matchDate DESC")
    fun getHistoryForPlayer(email: String): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistoryEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isGlobal = 1 ORDER BY timestamp ASC LIMIT 100")
    fun getGlobalChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isGlobal = 0 AND roomId = :roomId ORDER BY timestamp ASC")
    fun getRoomChats(roomId: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: Int)

    @Query("DELETE FROM chats")
    suspend fun clearAllChats()
}

@Dao
interface AbuseReportDao {
    @Query("SELECT * FROM abuse_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<AbuseReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: AbuseReportEntity)

    @Query("UPDATE abuse_reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: Int, status: String)

    @Query("DELETE FROM abuse_reports WHERE id = :reportId")
    suspend fun deleteReport(reportId: Int)
}

// --- 3. Database Base class ---

@Database(
    entities = [
        UserEntity::class,
        MatchHistoryEntity::class,
        ChatEntity::class,
        AbuseReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SnaikyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun matchHistoryDao(): MatchHistoryDao
    abstract fun chatDao(): ChatDao
    abstract fun abuseReportDao(): AbuseReportDao
}
