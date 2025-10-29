package com.example.flashcardquizapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.flashcardquizapp.databinding.ActivityAddEditCardBinding

/**
 * This activity provides a user interface for creating a new flashcard or editing an existing one.
 * It returns the new or updated card data back to the calling activity (MainActivity).
 */
class AddEditCardActivity : AppCompatActivity() {

    // View binding to interact with the layout elements safely.
    private lateinit var binding: ActivityAddEditCardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if the activity was started for editing an existing card.
        // If so, the existing question and answer will be passed in the intent.
        val question = intent.getStringExtra("question")
        val answer = intent.getStringExtra("answer")

        // Pre-fill the input fields if editing.
        binding.questionEditText.setText(question)
        binding.answerEditText.setText(answer)

        // Set up the save button listener.
        binding.saveButton.setOnClickListener {
            // Get the text from the input fields.
            val newQuestion = binding.questionEditText.text.toString()
            val newAnswer = binding.answerEditText.text.toString()

            // Create an intent to send the data back.
            val resultIntent = Intent()
            resultIntent.putExtra("question", newQuestion)
            resultIntent.putExtra("answer", newAnswer)
            
            // Set the result to RESULT_OK to indicate success.
            setResult(Activity.RESULT_OK, resultIntent)
            
            // Close this activity and return to the previous screen.
            finish()
        }
    }
}
