package com.example.roomtest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roomtest.database.MyAppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FinPartida : AppCompatActivity() {

    private lateinit var highScoresRecyclerView: RecyclerView
    private lateinit var highScoresAdapter: HighScoresAdapter
    private lateinit var db: MyAppDatabase
    private lateinit var returnButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fin_partida)

        // Initialize RecyclerView
        highScoresRecyclerView = findViewById(R.id.highScoresRecyclerView)
        highScoresRecyclerView.layoutManager = LinearLayoutManager(this)
        highScoresAdapter = HighScoresAdapter(emptyList()) // Initialize with an empty list
        highScoresRecyclerView.adapter = highScoresAdapter

        // Initialize database
        db = MyAppDatabase.getDatabase(applicationContext)

        // Load high scores from database and update RecyclerView
        loadHighScores()

        returnButton = findViewById(R.id.returnButton)
        returnButton.setOnClickListener {
            finish()
        }

    }

    private fun loadHighScores() {
        GlobalScope.launch(Dispatchers.IO) {
            val highScoresList = db.highScoresDao().getTop5HighScores() // Get top 20 high scores from the database
            runOnUiThread {
                highScoresAdapter.setData(highScoresList) // Update RecyclerView with high scores data
            }
        }
    }
}
