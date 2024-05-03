package com.example.roomtest.dataclass

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Progress",
    foreignKeys = [
        ForeignKey(
            entity = Game_settings::class,
            parentColumns = ["id"],
            childColumns = ["settingsId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Progress(
    @PrimaryKey val gameId: Int? = null,
    @ColumnInfo(name = "settingsId")
    val settingsId: Int? = null, // Foreign key to Game_settings
    @ColumnInfo(name = "userId")
    val userId: Int, // Foreign key to User
    val score: Int = 0,
    val clues: Boolean,
    val uniqueId: Int? = 0,
    var hintsUsed: Int? = 0,
    var correctAnswers: Int? = 0,
    var finalScore: Int? = 0,
)
