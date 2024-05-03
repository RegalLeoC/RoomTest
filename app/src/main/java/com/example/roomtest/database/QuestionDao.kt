package com.example.roomtest.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roomtest.dataclass.Question
import com.example.roomtest.dataclass.Questions

@Dao
interface QuestionDao {
    @Query("SELECT * FROM Question")
    fun getAllQuestions(): List<Question>

    @Query("SELECT EXISTS (SELECT 1 FROM AnswerOption WHERE questionId = :questionId)")
    suspend fun hasAnswerOptions(questionId: Int): Boolean

    @Query("SELECT * FROM Question WHERE id = :questionId")
    fun getQuestionById(questionId: Int): Question?

    @Query("SELECT * FROM Question WHERE gameId = :gameId")
    fun getQuestionsForGame(gameId: Int): List<Question>

    @Insert
    fun insertQuestion(question: Question)

    @Update
    fun updateQuestion(question: Question)

    @Query("UPDATE Question SET state = :newState WHERE questionText = :questionText AND uniqueId = :uniqueId")
    suspend fun updateQuestionState(questionText: String, newState: String, uniqueId: Int)

    @Query("SELECT id FROM Question WHERE questionText = :questionText")
    suspend fun getQuestionIdByQuestionText(questionText: String): Int?

    @Query("SELECT * FROM Question WHERE uniqueId = :uniqueId")
    suspend fun getQuestionsByUniqueId(uniqueId: Int): List<Question>?

    // Add other methods for question-related database operations
}