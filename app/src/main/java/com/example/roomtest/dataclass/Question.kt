package com.example.roomtest.dataclass

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName="Question")
data class Question(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "gameId")
    val gameId: Int, // Foreign key to Game_settings
    val state: String = "Unanswered",
    val questionText: String? = null,
    val difficulty: String = "Normal",
    val uniqueId: Int? = 0
    //Topic
)