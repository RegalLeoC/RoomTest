package com.example.roomtest

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roomtest.database.MyAppDatabase
import com.example.roomtest.dataclass.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var userAdapter: UserSelectionAdapter
    private lateinit var db: MyAppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_selection)

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
        recyclerViewUsers.layoutManager = LinearLayoutManager(this)

        db = MyAppDatabase.getDatabase(applicationContext)

        // Load users into RecyclerView
        loadUsers()

        // Set click listener for Add User button
        val buttonAddUser: Button = findViewById(R.id.buttonAddUser)
        buttonAddUser.setOnClickListener {
            showAddUserPrompt()
        }

        // Set click listener for Return button
        val buttonReturn: Button = findViewById(R.id.buttonReturn)
        buttonReturn.setOnClickListener {
            returnToMainActivity()
        }
    }

    private fun loadUsers() {
        GlobalScope.launch(Dispatchers.Main) {
            val users = withContext(Dispatchers.IO) {
                db.userDao().getAllUsers()
            }
            userAdapter = UserSelectionAdapter(users) { selectedUser ->
                switchToUser(selectedUser)
            }
            recyclerViewUsers.adapter = userAdapter
        }
    }

    private fun addUserAndReturn(userName: String) {
        GlobalScope.launch(Dispatchers.IO) {
            var userId: Int? = null
            do {
                val randomId = (1000..9999).random() // Generate random id
                val existingUser = db.userDao().getUserById(randomId)
                if (existingUser == null) {
                    // If user with this id doesn't exist, insert new user with this id
                    val newUser = User(id = randomId, userName)
                    db.userDao().insertUser(newUser)
                    userId = randomId
                }
            } while (userId == null) // Keep generating random ids until one is inserted successfully

            // Set new user as active (if user was inserted)
            userId?.let {
                db.userDao().setActiveUser(it)
            }
        }
        // Return to main activity
        returnToMainActivity()
    }

    private fun showAddUserPrompt() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New User")

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_add_user, null)
        val userInput = dialogLayout.findViewById<EditText>(R.id.editTextUserName)

        builder.setView(dialogLayout)

        builder.setPositiveButton("OK") { _, _ ->
            val userName = userInput.text.toString().trim()
            if (userName.isNotEmpty()) {
                addUserAndReturn(userName)
            } else {
                Toast.makeText(this, "Please enter a name for the new user", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun switchToUser(selectedUser: User) {
        // Implement logic to switch to the selected user
        // For example, you can set the selected user as active in the database and finish the activity
        GlobalScope.launch(Dispatchers.IO) {
            selectedUser.id?.let { db.userDao().deactivateAllUsersExcept(it) }
            selectedUser.id?.let { db.userDao().setActiveUser(it) }
        }

        returnToMainActivity()

    }



    private fun returnToMainActivity() {
        // Finish this activity and return to main activity
        setResult(Activity.RESULT_OK)
        finish()
    }
}
