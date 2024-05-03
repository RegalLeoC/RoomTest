package com.example.roomtest

import  android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.roomtest.database.MyAppDatabase
import com.example.roomtest.dataclass.AnswerOption
import com.example.roomtest.dataclass.HighScores
import com.example.roomtest.dataclass.Progress
import com.example.roomtest.dataclass.Question
import com.example.roomtest.dataclass.Questions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JuegoReanudadoActivity : AppCompatActivity() {

    private lateinit var buttonContainer: LinearLayout
    private lateinit var questionTextView: TextView
    private lateinit var topicImageView: ImageView
    private lateinit var questionNumberTextView: TextView
    private lateinit var hintTextView: TextView
    private lateinit var hintButton: Button

    private var questionOptions: MutableMap<Int, List<String>> =
        mutableMapOf() // List of all options we generate initially per question
    private var enabledWrongOptions: MutableMap<Int, MutableList<String>> =
        mutableMapOf() // List of wrong options that are enabled
    private var disabledWrongOptions: MutableMap<Int, List<String>> =
        mutableMapOf() // List of wrong options that were disabled by using hint
    private var answeredQuestions: MutableMap<Int, Boolean> =
        mutableMapOf() // Questions answered through manual selection
    private var answeredQuestionsHint: MutableMap<Int, Boolean> =
        mutableMapOf() // Questions answered through hint
    private var userSelection: MutableMap<Int, String?> =
        mutableMapOf() //Right answers selected by the user
    private var hintSelection: MutableMap<Int, String?> =
        mutableMapOf() //Right answers selected by the hint

    //Question variables
    private var questionIndex: Int = 0;
    private lateinit var topics: Array<Topics>
    private lateinit var questions: List<Question>

    // Hint variables
    private var hintsAvailable: Int = 3;
    private var hintStreak: Int = 0;

    // Score variables
    private var hintsUsed: Int = 0;
    private var finalScore: Int = 0;
    private var correctAnswers: Int = 0;
    private var difficultyMultiplier: Double = 1.0



    // Database variables
    private lateinit var db: MyAppDatabase
    private var numberOfQuestions: Int = 0;
    private var gameId: Int = 0;
    private var activeUserId: Int? = 0;
    private var settingsId: Int? = 0;
    private var uniqueId: Int? = 0;
    private var difficult: String? = null;
    private var cluesActive: Boolean? = null;
    private var progressId: Int? = 0;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_juego_reanudado)

        db = MyAppDatabase.getDatabase(applicationContext)

        buttonContainer = findViewById(R.id.buttonContainer)
        questionTextView = findViewById(R.id.questionTextView)
        topicImageView = findViewById(R.id.topicImageView)
        questionNumberTextView = findViewById(R.id.questionNumberTextView)
        hintTextView = findViewById(R.id.hintTextView)
        hintButton = findViewById(R.id.hintButton)

        topics = Topics.values()


        lifecycleScope.launch(Dispatchers.IO) {
            activeUserId = db.userDao().getActiveUserId()
            difficult = settingsId?.let { db.gameDao().getDifficultyById(it) }

        }

        reestablishQuestions()
        updateQuestion()


    }


    private fun reestablishQuestions() {
        questions = mutableListOf()

        lifecycleScope.launch(Dispatchers.IO) {
            val activeUserId = db.userDao().getActiveUserId()
            activeUserId?.let { userId ->
                val progress = db.progressDao().getProgressByUserId(userId)
                val uniqueId = progress?.uniqueId

                uniqueId?.let { uniqueId ->
                    // Retrieve questions associated with the uniqueId from the database
                    val questionsFromDB = db.questionDao().getQuestionsByUniqueId(uniqueId)
                    questionsFromDB?.let {
                        // Add the fetched questions to the questions list
                        (questions as MutableList<Question>).addAll(it)
                        numberOfQuestions = questionsFromDB.size
                    }
                }
            }
        }
    }


    private fun updateQuestion() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Establish which question we are on
            val currentQuestion = questions.getOrNull(questionIndex)

            if (currentQuestion != null) {
                // Show the question text
                questionTextView.text = currentQuestion.questionText

                // Check if there are associated answer options for the current question
                val answerOptionsForQuestion =
                    db.answerOptionDao().getAnswerOptionsByQuestionId(currentQuestion.id)

                if (answerOptionsForQuestion.isEmpty()) {
                    // If no answer options exist for the current question, generate options
                    generateQuestionsOptions2(currentQuestion.id, questionIndex)
                } else {
                    // If answer options exist, create buttons for user selection
                    createButtons(currentQuestion.id)
                }
            }

            // Set listeners for navigation
            for (i in 1..numberOfQuestions) {
                val buttonId = resources.getIdentifier("bar$i", "id", packageName)
                val button = findViewById<Button>(buttonId)
                button.tag = i // Set the tag to the question number for each button
                button.setOnClickListener { navigateToQuestion(i) } // Update here
            }
        }
    }




    private fun generateQuestionsOptions(questionId: Int, Index: Int){

        var numWrongAnswers = 0;

        when (difficult) {
            "Easy" -> numWrongAnswers = 1
            "Normal" -> numWrongAnswers = 2
            "Hard" -> numWrongAnswers = 3
        }

    }

    private fun generateQuestionsOptions2(questionId: Int, index: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val options = mutableListOf<String>()

            // Retrieve answer options associated with the provided question ID
            val answerOptions = db.answerOptionDao().getAnswerOptionsByQuestionId(questionId)

            // Extract option text from AnswerOption instances
            answerOptions.forEach { option ->
                options.add(option.optionText)
            }

            // Store the extracted options in the questionOptions map
            questionOptions[index] = options
        }

    }


    private fun createButtons(questionId: Int) {
        buttonContainer.removeAllViews()

        lifecycleScope.launch(Dispatchers.IO) {
            // Retrieve answer options associated with the provided question ID
            val answerOptions = db.answerOptionDao().getAnswerOptionsByQuestionId(questionId)

            var isAnySelected = false

            withContext(Dispatchers.Main) {
                for (option in answerOptions) {
                    val button = Button(this@JuegoReanudadoActivity)
                    button.text = option.optionText

                    // Check the state of the answer option
                    when (option.state) {
                        "Unanswered" -> {
                            // Generate a gray button for unanswered options
                            button.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                            button.isEnabled = true // Enable the button for unanswered options
                        }
                        "Selected" -> {
                            // Color the button based on correctness
                            if (option.correct == true) {
                                // Green for correct answers
                                button.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                            } else {
                                // Red for incorrect answers
                                button.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                                isAnySelected = true // Set flag to indicate a selected option
                            }
                            button.isEnabled = false // Disable the button for selected options
                        }
                    }

                    // Set onClickListener for the button
                    button.setOnClickListener {
                        // Check if the option is correct
                        if (option.correct == true) {
                            // Green for correct answers
                            button.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                        } else {
                            // Red for incorrect answers
                            button.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                            // Disable all buttons
                            disableButtons()
                        }
                    }

                    buttonContainer.addView(button)
                }

                // Disable all buttons if any option is selected
                if (isAnySelected) {
                    disableButtons()
                }
            }
        }
    }


    private fun disableButtons() {
        // Disable all buttons
        for (i in 0 until buttonContainer.childCount) {
            val child = buttonContainer.getChildAt(i)
            if (child is Button) {
                child.isEnabled = false
            }
        }
    }

    private fun updateNavigationBar() {
        for (i in 0 until questions.size) {


            var questioncorrect = ""

            lifecycleScope.launch(Dispatchers.IO) {

                questioncorrect =
                    db.answerOptionDao().getCorrectAnswerByQuestionId(questions[i].id).toString()

            }

            val isAnsweredHint = answeredQuestionsHint[i] ?: false
            val isCorrectAnswer = userSelection[i] == questioncorrect
            val isIncorrectAnswer = userSelection[i] != questioncorrect && answeredQuestions[i] ?: false


            val buttonId = resources.getIdentifier("bar${i + 1}", "id", packageName)
            val button = findViewById<Button>(buttonId)
            if (i == questionIndex) {

                // Actualizar barra donde este seleccionado
                when {
                    isAnsweredHint -> button.background = ContextCompat.getDrawable(this, R.drawable.selected_button_background_hint)
                    isCorrectAnswer -> button.background = ContextCompat.getDrawable(this, R.drawable.selected_button_background_correct)
                    isIncorrectAnswer -> button.background = ContextCompat.getDrawable(this, R.drawable.selected_button_background_incorrect)
                    else -> button.background = ContextCompat.getDrawable(this, R.drawable.selected_button_background)
                }

            } else {

                //Aqui cuando no este seleccionado

                when {
                    isAnsweredHint -> button.background = ContextCompat.getDrawable(this, R.drawable.hint_button_background)
                    isCorrectAnswer -> button.background = ContextCompat.getDrawable(this, R.drawable.correct_button_background)
                    isIncorrectAnswer -> button.background = ContextCompat.getDrawable(this, R.drawable.incorrect_button_background)
                    else -> button.background = ContextCompat.getDrawable(this, R.drawable.button_background)
                }

            }
        }
    }


    private fun navigateToQuestion(questionNumber: Int) {
        questionIndex = questionNumber - 1 // Adjust question index (0-based)
        updateQuestion()
        updateNavigationBar()
    }

    private fun nextQuestion() {

        questionIndex = (questionIndex + 1) % questions.size
        updateQuestion()
        updateNavigationBar()
    }

    private fun previousQuestion() {
        questionIndex = (questionIndex - 1 + questions.size) % questions.size
        updateQuestion()
        updateNavigationBar()
    }

    private fun endGame() {
        val totalAnswers = hintSelection.count { it.value != null } + userSelection.count { it.value != null }
        if (totalAnswers == numberOfQuestions) {
            viewResults()
        }
    }

    private fun viewResults() {
        // Calculate the final result based on the specified formula
        difficultyMultiplier = when (difficult) {
            "Easy" -> 1.0
            "Normal" -> 1.25
            "Hard" -> 1.5
            else -> 1.0
        }

        // Deduct points for hints
        val deduction = hintsUsed * 25  // Deduct 25 points for each hint used
        val totalpoints = correctAnswers * 100
        val finalResult = ((totalpoints - deduction) * difficultyMultiplier)

        // Fetch active user name asynchronously
        lifecycleScope.launch(Dispatchers.IO) {
            val activeUserName = activeUserId?.let { db.userDao().getUserNameById(it) }

            // Generate a random ID for HighScores
            val highScoresId = generateRandomHighScoresId()

            // Wait for the highScoresId to be generated
            val id = highScoresId ?: return@launch

            // Create HighScores instance when active user name is available
            activeUserName?.let { userName ->
                val highScores = HighScores(
                    id = id,
                    userId = activeUserId ?: 0, // Assuming activeUserId is not null
                    name = userName,
                    score = finalResult,
                    clues = cluesActive ?: false
                )

                // Save the HighScores instance in the database
                db.highScoresDao().insertHighScores(highScores)
            }
        }

        // Pass the results to the FinPartida activity
        val intent = Intent(this, FinPartida::class.java)
        intent.putExtra("Deduction", deduction)
        intent.putExtra("totalScore", totalpoints)
        intent.putExtra("FinalResult", finalResult)
        intent.putExtra("difficultyMultiplier", difficultyMultiplier)

        startActivity(intent)
        finish()
    }

    private fun generateRandomHighScoresId(): Int? {
        var newId: Int
        do {
            newId = (0 until 1000).random()
        } while (db.highScoresDao().getHighScoresById(newId) != null)
        return newId
    }




}