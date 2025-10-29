package com.example.flashcardquizapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.flashcardquizapp.databinding.ActivityAddEditCardBinding

class AddEditCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val question = intent.getStringExtra("question")
        val answer = intent.getStringExtra("answer")

        binding.questionEditText.setText(question)
        binding.answerEditText.setText(answer)

        binding.saveButton.setOnClickListener {
            val newQuestion = binding.questionEditText.text.toString()
            val newAnswer = binding.answerEditText.text.toString()

            val resultIntent = Intent()
            resultIntent.putExtra("question", newQuestion)
            resultIntent.putExtra("answer", newAnswer)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}