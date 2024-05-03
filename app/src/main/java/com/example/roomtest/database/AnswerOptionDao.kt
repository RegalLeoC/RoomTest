package com.example.roomtest.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roomtest.dataclass.AnswerOption


@Dao
interface AnswerOptionDao {
    @Query("SELECT * FROM AnswerOption")
    fun getAllAnswerOptions(): List<AnswerOption>

    @Query("SELECT * FROM AnswerOption WHERE id = :answerOptionId")
    fun getAnswerOptionById(answerOptionId: Long): AnswerOption?

    @Query("SELECT COUNT(*) FROM AnswerOption WHERE uniqueId = :uniqueId")
    suspend fun getAnswerOptionCountByUniqueId(uniqueId: Int): Int

    @Query("SELECT uniqueId FROM AnswerOption WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getExistingUniqueId(uniqueId: Int): Int?

    @Insert
    fun insertAnswerOption(answerOption: AnswerOption)

    @Query("SELECT * FROM AnswerOption WHERE questionId = :questionId")
    suspend fun getAnswerOptionsByQuestionId(questionId: Int): List<AnswerOption>

    @Update
    fun updateAnswerOption(answerOption: AnswerOption)

    @Query("SELECT * FROM AnswerOption WHERE id = :id LIMIT 1")
    suspend fun getAnswerOptionById(id: Int): AnswerOption?

    @Query("UPDATE AnswerOption SET state = :newState WHERE optionText = :optionText AND uniqueId = :uniqueId")
    suspend fun updateAnswerOptionState(optionText: String, newState: String, uniqueId: Int)

    @Query("SELECT optionText FROM AnswerOption WHERE questionId = :questionId AND correct = 1")
    suspend fun getCorrectAnswerByQuestionId(questionId: Int): String?

    // Add other methods for answerOption-related database operations
}