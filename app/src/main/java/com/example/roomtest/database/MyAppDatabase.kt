package com.example.roomtest.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.roomtest.dataclass.AnswerOption
import com.example.roomtest.dataclass.Game_settings
import com.example.roomtest.dataclass.HighScores
import com.example.roomtest.dataclass.Progress
import com.example.roomtest.dataclass.Question
import com.example.roomtest.dataclass.User

@Database(
    entities = [User::class, Game_settings::class, Question::class, Progress::class, HighScores::class, AnswerOption::class],
    version = 3, exportSchema = false
)
abstract class MyAppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
    abstract fun highScoresDao(): HighScoresDao
    abstract fun answerOptionDao(): AnswerOptionDao

    companion object {
        @Volatile
        private var INSTANCE: MyAppDatabase? = null

        fun getDatabase(context: Context): MyAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyAppDatabase::class.java,
                    "my_app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
