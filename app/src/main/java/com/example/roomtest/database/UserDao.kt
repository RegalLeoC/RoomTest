package com.example.roomtest.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roomtest.dataclass.User

@Dao
interface UserDao {
    @Query("SELECT * FROM User")
    fun getAllUsers(): List<User>

    @Query("SELECT * FROM User WHERE id = :userId")
    fun getUserById(userId: Int): User?

    @Insert
    fun insertUser(user: User)

    @Update
    fun updateUser(user: User)

    @Delete
    fun deleteUser(user: User)

    @Query("UPDATE User SET active = CASE WHEN id = :userId THEN 1 ELSE 0 END")
    suspend fun setActiveUser(userId: Int)

    @Query("UPDATE User SET active = 0 WHERE id != :userId")
    suspend fun deactivateAllUsersExcept(userId: Int)

    @Query("SELECT id FROM User WHERE active = 1 LIMIT 1")
    suspend fun getActiveUserId(): Int?

    @Query("UPDATE User SET pendingGame = 1 WHERE id = :userId")
    suspend fun updatePendingGameStatus(userId: Int)

    @Query("UPDATE User SET pendingGame = 0 WHERE id = :userId")
    suspend fun disablePendingGameStatus(userId: Int)


    @Query("SELECT * FROM User WHERE active = 1 LIMIT 1")
    suspend fun getActiveUser(): User?

    @Query("DELETE FROM Progress WHERE userId = :userId")
    suspend fun deleteProgressByUserId(userId: Int)

    @Query("DELETE FROM Question WHERE uniqueId IN (SELECT uniqueId FROM Game_settings WHERE userId = :userId)")
    suspend fun deleteQuestionsByUserId(userId: Int)

    @Query("DELETE FROM AnswerOption WHERE uniqueId IN (SELECT uniqueId FROM Question WHERE uniqueId IN (SELECT id FROM Game_settings WHERE userId = :userId))")
    suspend fun deleteAnswerOptionsByUserId(userId: Int)

    @Query("SELECT name FROM User WHERE id = :userId")
    suspend fun getUserNameById(userId: Int): String?

    // Add other methods for user-related database operations
}