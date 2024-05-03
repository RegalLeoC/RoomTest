package com.example.roomtest.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roomtest.dataclass.Progress

@Dao
interface ProgressDao {
    @Query("SELECT * FROM Progress")
    fun getAllProgresses(): List<Progress>

    @Query("SELECT * FROM Progress WHERE userId = :userId")
    fun getProgressForUser(userId: Int): List<Progress>

    @Query("SELECT settingsId FROM Progress WHERE userId = :userId")
    fun getProgressId(userId: Int): Int?

    @Query("SELECT * FROM Progress WHERE userId = :userId")
    fun getProgress(userId: Int): Progress?


    @Insert
    suspend fun insertProgress(progress: Progress)

    @Update
    fun updateProgress(progress: Progress)

    @Query("UPDATE Progress SET hintsUsed = :hintsUsed WHERE userId = :userId")
    suspend fun updateHintsUsed(userId: Int, hintsUsed: Int)

    @Query("UPDATE Progress SET correctAnswers = :correctAnswers WHERE userId = :userId")
    suspend fun updateCorrectAnswers(userId: Int, correctAnswers: Int)

    @Query("SELECT * FROM Progress WHERE userId = :userId")
    suspend fun getProgressByUserId(userId: Int): Progress?


    // Add other methods for progress-related database operations
}