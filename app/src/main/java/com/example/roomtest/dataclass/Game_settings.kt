package com.example.roomtest.dataclass

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "Game_settings")
data class Game_settings(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "userId")
    val userId: Int?, // Foreign key to User
    var difficulty: String = "Normal",
    var numQuestions: Int = 10,
    var topic1: Boolean = true,
    var topic2: Boolean = true,
    var topic3: Boolean = true,
    var topic4: Boolean = true,
    var topic5: Boolean = true,
    var topic6: Boolean = true,
    var clues: Boolean = true
)