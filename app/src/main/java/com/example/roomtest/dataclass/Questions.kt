package com.example.roomtest.dataclass

data class Questions (
    val text: String,
    val correctAnswer: String,
    val wrongAnswers: List<String>
){
    var answered: Boolean = false
}
