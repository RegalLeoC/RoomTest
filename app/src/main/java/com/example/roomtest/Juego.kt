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

class Juego : AppCompatActivity() {

    //Layout
    private lateinit var buttonContainer: LinearLayout
    private lateinit var questionTextView: TextView
    private lateinit var topicImageView: ImageView
    private lateinit var questionNumberTextView: TextView
    private lateinit var hintTextView: TextView
    private lateinit var hintButton: Button


    // Maps (dictionaries) to follow
    private var questionOptions: MutableMap<Int, List<String>> = mutableMapOf() // List of all options we generate initially per question
    private var enabledWrongOptions: MutableMap<Int, MutableList<String>> = mutableMapOf() // List of wrong options that are enabled
    private var disabledWrongOptions: MutableMap<Int, List<String>> = mutableMapOf() // List of wrong options that were disabled by using hint
    private var answeredQuestions: MutableMap<Int, Boolean> = mutableMapOf() // Questions answered through manual selection
    private var answeredQuestionsHint:  MutableMap<Int, Boolean> = mutableMapOf() // Questions answered through hint
    private var userSelection: MutableMap<Int, String?> = mutableMapOf() //Right answers selected by the user
    private var hintSelection: MutableMap<Int, String?> = mutableMapOf() //Right answers selected by the hint

    //Question variables
    private var questionIndex: Int = 0;
    private lateinit var topics: Array<Topics>
    private lateinit var questions: List<Questions>

    // Hint variables
    private var hintsAvailable: Int = 3;
    private var hintStreak: Int = 0;


    // Score variables
    private var hintsUsed: Int = 0;
    private var finalScore: Int = 0;
    private var correctAnswers: Int = 0;
    private var difficultyMultiplier: Double = 1.0

    // Getting difficulty

    private val difficulty: String? by lazy {
        intent.getStringExtra("difficulty")
    }


    // Database variables
    private lateinit var db: MyAppDatabase
    private var numberOfQuestions: Int = 0;
    private var gameId: Int = 0;
    private var activeUserId: Int? = 0;
    private var settingsId: Int? = 0;
    private var uniqueId: Int? = 0;
    private var difficult: String? = null;
    private var cluesActive: Boolean? = null;



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_juego)

        // DB Stuff - start

        db = MyAppDatabase.getDatabase(applicationContext)

        uniqueId = generateUniqueId()

        lifecycleScope.launch(Dispatchers.IO) {


            gameId = generateRandomGameId()
            activeUserId = getActiveUserId()
            settingsId = activeUserId?.let { getSettingsIdForUser(it) }
            cluesActive = settingsId?.let { areCluesActive(it) }

            difficult = settingsId?.let { db.gameDao().getDifficultyById(it) }

            numberOfQuestions = settingsId?.let { db.gameDao().getNumberOfQuestions(it) } ?: 0

            val progress = Progress(
                gameId ?: 0, // Provide a default value for gameId if it's null
                settingsId ?: 0, // Provide a default value for settingsId if it's null
                activeUserId ?: 0, // Provide a default value for activeUserId if it's null
                0, // Score is always initialized to 0
                cluesActive ?: false, // Provide a default value for cluesActive if it's null
                uniqueId
            )

            db.progressDao().insertProgress(progress)


            activeUserId?.let {
                updatePendingGameStatus(it)
            }

        }

        // DB Stuff - end

        //Layout
        buttonContainer = findViewById(R.id.buttonContainer)
        questionTextView = findViewById(R.id.questionTextView)
        topicImageView = findViewById(R.id.topicImageView)
        questionNumberTextView = findViewById(R.id.questionNumberTextView)
        hintTextView = findViewById(R.id.hintTextView)
        hintButton = findViewById(R.id.hintButton)

        //Saving the topics
        topics = Topics.values()

        selectRandomQuestions()
        updateQuestion()

        //Initialize maps based on amount of questions (that were created in selectrandomquestions)
        for(i in 0 until questions.size){
            answeredQuestions[i] = false; // None of the questions are answered yet
            answeredQuestionsHint[i] = false; // None of the questions are answered yet
            userSelection[i] = null; // Putting a null for it not to be empty
            hintSelection[i] = null; // Putting a null for it not to be empty
        }


        //Exploration
        findViewById<Button>(R.id.nextButton).setOnClickListener {
            nextQuestion()
        }

        findViewById<Button>(R.id.prevButton).setOnClickListener {
            previousQuestion()
        }

        //Use hint
        hintButton.setOnClickListener {
            useHint()
        }


    }


    private fun generateUniqueId(): Int? {
        var uniqueIds: Int? = null

        lifecycleScope.launch(Dispatchers.IO) {
            do {
                uniqueId = (1..1000).random() // Generate a random ID
            } while (uniqueIds?.let { db.answerOptionDao().getExistingUniqueId(it) } != null)
        }

        return uniqueIds
    }





    //DB Functions - Start

    private fun generateRandomGameId(): Int {
        var randomId: Int
        do {
            randomId = (0..Int.MAX_VALUE).random() // Generate a random game ID
        } while (db.questionDao().getQuestionById(randomId) != null) // Check if ID already exists in the table
        return randomId
    }
    private suspend fun getActiveUserId(): Int? {
        var userId = db.userDao().getActiveUserId()
        return userId;
    }

    private suspend fun getSettingsIdForUser(userId: Int): Int? {
        return db.gameDao().getSettingsIdForUser(userId)
    }

    private suspend fun areCluesActive(settingsId: Int): Boolean? {
        return db.gameDao().areCluesActive(settingsId)
    }


    private suspend fun updatePendingGameStatus(userId: Int) {
        db.userDao().updatePendingGameStatus(userId)
    }


    private suspend fun getDifficultyById(db: MyAppDatabase, settingsId: Int): String? {
        return withContext(Dispatchers.IO) {
            db.gameDao().getDifficultyById(settingsId)
        }
    }


    //DB Funcitions - End


    private fun selectRandomQuestions(){
        val allQuestions = topics.flatMap {it.questions}.toMutableList()
        questions = mutableListOf()

        repeat(numberOfQuestions) {
            val randomQuestion = allQuestions.random()
            val randomId = (0..1000).random() // Generate a random ID for the question

            lifecycleScope.launch(Dispatchers.IO) {
                val difficultyLevel = settingsId?.let { getDifficultyById(db, settingsId!!) } ?: "Normal"  // Use the specified difficulty or default to "Normal"

                val questionInstance = Question(
                    id = randomId,
                    gameId = gameId, // Assuming `gameId` is accessible here
                    state = "Unanswered",
                    questionText = randomQuestion.text,
                    difficulty = difficultyLevel,
                    uniqueId = uniqueId
                )

                db.questionDao().insertQuestion(questionInstance)

            }

            (questions as MutableList<Questions>).add(randomQuestion)
            allQuestions.remove(randomQuestion)

        }

    }






    private fun updateQuestion() {
        // Establish which question we are on
        val currentQuestion = questions.getOrNull(questionIndex)

        if (currentQuestion != null) {
            // Look up the topic for the associated pic of the question
            val currentTopic = topics.find { it.questions.contains(currentQuestion) } ?: Topics.MATHEMATICS

            // Show the topic image
            topicImageView.setImageResource(currentTopic.imageResourceId)

            // Show the question text
            questionTextView.text = currentQuestion.text

            // Show the question number
            val questionNumberText = "${questionIndex + 1}/${questions.size}"
            questionNumberTextView.text = questionNumberText

            // If it's the first time visiting the question, generate options to choose from
            if (questionOptions[questionIndex] == null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val questionId = getQuestionIdByQuestionText(currentQuestion.text ?: "")

                    val options = generateQuestionsOptions(currentQuestion, questionId)
                    questionOptions[questionIndex] = options
                }
            }

            // Create buttons based on the generated options
            createButtons(questionOptions[questionIndex] ?: emptyList())

            // Create variables for navigation
            val navigationBar = findViewById<LinearLayout>(R.id.navigationBar)
            for (i in 1..numberOfQuestions) {
                val buttonId = resources.getIdentifier("bar$i", "id", packageName)
                val button = findViewById<Button>(buttonId)
                button.tag = i // Set the tag to the question number for each button
            }

            // Set listeners for navigation
            for (i in 1..numberOfQuestions) {
                val buttonId = resources.getIdentifier("bar$i", "id", packageName)
                val button = findViewById<Button>(buttonId)
                button.setOnClickListener { navigateToQuestion(button.tag as Int) }
            }
        }
    }



    private fun updateNavigationBar() {
        for (i in 0 until questions.size) {

            val isAnsweredHint = answeredQuestionsHint[i] ?: false
            val isCorrectAnswer = userSelection[i] == questions[i].correctAnswer
            val isIncorrectAnswer = userSelection[i] != questions[i].correctAnswer && answeredQuestions[i] ?: false


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


    //Aqui generamos las opciones segun la cantidad (dificultad)
    private fun generateQuestionsOptions(question: Questions, questionId: Int): MutableList<String> {
        val options = mutableListOf<String>()
        options.add(question.correctAnswer)

        var numWrongAnswers = 0;

        when (difficult) {
            "Easy" -> numWrongAnswers = 1
            "Normal" -> numWrongAnswers = 2
            "Hard" -> numWrongAnswers = 3
        }

        val wrongAnswers = question.wrongAnswers.shuffled().take(numWrongAnswers)
        options.addAll(wrongAnswers)
        options.shuffle()

        options.forEachIndexed { index, optionText ->
            val isCorrect = optionText == question.correctAnswer
            val optionInstance = AnswerOption(
                id = generateRandomId(),
                uniqueId = uniqueId,
                optionText = optionText,
                correct = isCorrect,
                questionId = questionId
            )

            insertAnswerOption(optionInstance)
        }

        return options
    }


    private suspend fun getQuestionIdByQuestionText(questionText: String): Int {



        return withContext(Dispatchers.IO) {

            lifecycleScope.launch {

            }

            db.questionDao().getQuestionIdByQuestionText(questionText) ?: error("Question not found")
        }


    }


    private fun insertAnswerOption(optionInstance: AnswerOption) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.answerOptionDao().insertAnswerOption(optionInstance)
        }

    }

    private fun generateRandomId(): Int? {
        var newId: Int? = null

        lifecycleScope.launch(Dispatchers.IO) {
            do {
                val randomId = (0 until 999).random()
                val existingOption = db.answerOptionDao().getAnswerOptionById(randomId)
                if (existingOption == null) {
                    newId = randomId
                }
            } while (newId == null)
        }

        return newId
    }

    private fun generateRandomHighScoresId(): Int? {
        var newId: Int
        do {
            newId = (0 until 1000).random()
        } while (db.highScoresDao().getHighScoresById(newId) != null)
        return newId
    }



    // Aqui mostramos las opciones segun la pregunta adecuada
    private fun createButtons(options: List<String>){
        buttonContainer.removeAllViews()


        val isAnswered = answeredQuestions[questionIndex]?: false
        val isAnsweredHint = answeredQuestionsHint[questionIndex]?: false


        for (option in options){
            val button = Button(this)
            button.text = option

            //Aqui poner la de navagacion de regreso para el hint (similar a lo dde arriba)
            val hintedButton = disabledWrongOptions[questionIndex]?.contains(option) ?: false

            val isSelected = userSelection[questionIndex] == option;

            //Checa si esta contestado
            // Si contestaste bien pues se pone verde, pero si contestaste mal, se pinta el rojo y se pone el verde mostrandote cual era el correcto
            if (isAnswered) {
                hintButton.isEnabled = false
                button.isEnabled = false
                val correctAnswer = questions[questionIndex].correctAnswer
                if (option == correctAnswer) {
                    button.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))

                } else if (isSelected) {
                    button.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))

                }

                if(hintedButton){
                    button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                }

            }

            //Permite usar hint y navegar
            if(!isAnswered){
                hintButton.isEnabled = true
                if(hintedButton){
                    button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                    button.isEnabled = false
                }
            }


            // Navegacion si se contesto usando hints
            if(isAnsweredHint){
                hintButton.isEnabled = false

                button.isEnabled = false
                val correctAnswer = questions[questionIndex].correctAnswer

                if(option == correctAnswer) {
                    button.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                }

                if(hintedButton){
                    button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                }

                disableButtons()

            }


            // Aqui es cuanddo no haz contestado y se guarda en la lista de contestado manual

            button.setOnClickListener {
                if (!isAnswered) {
                    hintButton.isEnabled = false
                    val correctAnswer = questions[questionIndex].correctAnswer
                    if (option == correctAnswer) {
                        button.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                        correctAnswers++ // Increment totalCorrectAnswers when the correct answer is selected

                        // Use a coroutine to update the question state to "Correct"
                        lifecycleScope.launch(Dispatchers.IO) {
                            val questionText = questions[questionIndex].text


                            activeUserId?.let { userId ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.progressDao().updateCorrectAnswers(userId, correctAnswers ?: 0)
                                }
                            }

                            uniqueId?.let { it1 ->
                                db.questionDao().updateQuestionState(questionText, "Correct",
                                    it1
                                )

                                uniqueId?.let { it1 ->
                                    db.answerOptionDao().updateAnswerOptionState(option, "Selected",
                                        it1
                                    )
                                }


                            }
                        }

                        // Add one more hint if in hint streak
                        if (hintStreak == 1) {
                            hintsAvailable++
                            hintTextView.text = hintsAvailable.toString()
                            hintStreak = 0
                        } else {
                            hintStreak++
                        }


                        lifecycleScope.launch(Dispatchers.IO) {
                            uniqueId?.let { it1 ->
                                db.answerOptionDao().updateAnswerOptionState(option, "Selected",
                                    it1
                                )

                            }
                        }



                    } else {
                        button.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                        hintStreak = 0

                        // Use a coroutine to update the question state to "Incorrect"
                        lifecycleScope.launch(Dispatchers.IO) {
                            val questionText = questions[questionIndex].text
                            uniqueId?.let { it1 ->
                                db.questionDao().updateQuestionState(questionText, "Incorrect",
                                    it1
                                )

                                uniqueId?.let { it1 ->
                                    db.answerOptionDao().updateAnswerOptionState(option, "Selected",
                                        it1
                                    )
                                }


                            }
                        }

                        // Show the correct answer by finding and coloring the button with the correct answer
                        for (i in 0 until buttonContainer.childCount) {
                            val child = buttonContainer.getChildAt(i)
                            if (child is Button && child.text.toString() == correctAnswer) {
                                child.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                            }
                        }

                        lifecycleScope.launch(Dispatchers.IO) {
                            uniqueId?.let { it1 ->
                                db.answerOptionDao().updateAnswerOptionState(option, "Selected",
                                    it1
                                )
                            }
                        }


                    }

                    answeredQuestions[questionIndex] = true
                    userSelection[questionIndex] = option
                    disableButtons() // Disable buttons after answering

                    // Check if we need to end the game
                    endGame()
                }
            }

            buttonContainer.addView(button)

        }

    }

    // Despues dded contestar se deshabilitan todos los botones para preevenir que el usuario conteste otra vez
    private fun disableButtons() {
        for (i in 0 until buttonContainer.childCount) {
            val child = buttonContainer.getChildAt(i)
            if (child is Button) {
                child.isEnabled = false
            }
        }
    }


    private fun useHint() {

        //check if we have hints remaining
        if(hintsAvailable > 0) {

            //Update variables
            hintsAvailable--
            hintStreak = 0
            hintsUsed++

            // Update text
            hintTextView.text = hintsAvailable.toString()


            activeUserId?.let { userId ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.progressDao().updateHintsUsed(userId, hintsUsed ?: 0)
                }
            }


            //Se inicilizan las variables para apuntar a la prgunta actual y sus opciones incorrectas
            val currentQuestion = questions[questionIndex]
            val enabledOptions = enabledWrongOptions[questionIndex]

            // Se selecciona al azar una opcion para deshabilitar con la pista
            if((enabledOptions?.size ?:0) > 1){
                val randomIndex = (0 until enabledOptions!!.size).random()
                val optionToDisable = enabledOptions[randomIndex]

                // Remove from enabled options and add it to disabled options
                enabledWrongOptions[questionIndex]?.remove(optionToDisable)
                disabledWrongOptions[questionIndex] = (disabledWrongOptions[questionIndex]?: emptyList()) + optionToDisable

                disableOption(optionToDisable)

            } else {
                // Si solo queda una opcion mala la pregunta se contesta sola
                val correctAnswer = currentQuestion.correctAnswer
                for(i in 0 until buttonContainer.childCount){
                    val child = buttonContainer.getChildAt(i)
                    if (child is Button) {
                        val option = child.text.toString()
                        if (option != correctAnswer && enabledOptions!!.contains(option)) {
                            disabledWrongOptions[questionIndex] = (disabledWrongOptions[questionIndex]?: emptyList()) + option
                            disableOption(option)
                        } else if (option == correctAnswer) {
                            correctAnswers++

                            activeUserId?.let { userId ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.progressDao().updateCorrectAnswers(userId, correctAnswers ?: 0)
                                }
                            }


                            // Necesito contar estos mas el userSelection para terminarlo
                            hintSelection[questionIndex] = option
                            answeredQuestionsHint[questionIndex] = true
                            child.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                            //Igual deshabilita el hint button????
                            hintButton.isEnabled = false
                            disableButtons()

                            //Checar si ya hay que terminar el juego
                            endGame()
                        }

                    }

                }


            }


        }


    }


    private fun disableOption(option: String) {
        for (i in 0 until buttonContainer.childCount) {
            val child = buttonContainer.getChildAt(i)
            if (child is Button && child.text.toString() == option) {
                child.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                child.isEnabled = false
            }
        }
    }


    // Navigation through buttons

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
        difficultyMultiplier = when (difficulty) {
            "Fácil" -> 1.0
            "Normal" -> 1.25
            "Difícil" -> 1.5
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


}