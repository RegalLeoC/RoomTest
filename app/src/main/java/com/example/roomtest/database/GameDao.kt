package com.example.roomtest.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roomtest.dataclass.Game_settings


@Dao
interface GameDao {
    @Query("SELECT * FROM Game_settings")
    fun getAllGames(): List<Game_settings>

    @Query("SELECT numQuestions FROM Game_settings WHERE id = :settingsId")
    suspend fun getNumberOfQuestions(settingsId: Int): Int

    @Query("SELECT * FROM Game_settings WHERE userId = :userId")
    fun getGamesForUser(userId: Int): List<Game_settings>

    @Query("SELECT clues FROM Game_settings WHERE id = :settingsId LIMIT 1")
    suspend fun areCluesActive(settingsId: Int): Boolean?

    @Query("SELECT * FROM Game_settings WHERE id = :gameId")
    fun getGameById(gameId: Long): Game_settings?

    @Query("SELECT * FROM Game_settings WHERE id = :settingsId LIMIT 1")
    suspend fun getGameSettingsById(settingsId: Int): Game_settings?

    @Query("SELECT * FROM Game_settings WHERE userId = :userId")
    suspend fun getGameSettingsByUserId(userId: Int): Game_settings?

    @Query("SELECT id FROM Game_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettingsIdForUser(userId: Int): Int?

    @Insert
    suspend fun insertGameSettings(gameSettings: Game_settings)

    @Insert
    fun insertGame(game: Game_settings)

    @Update
    fun updateGame(game: Game_settings)

    @Update
    suspend fun updateGameSettings(gameSettings: Game_settings)

    @Query("SELECT difficulty FROM Game_settings WHERE id = :settingsId")
    suspend fun getDifficultyById(settingsId: Int): String?

    // Add other methods for game-related database operations
}