package com.example.flashcardquizapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorInflater
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.example.flashcardquizapp.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var flashcards: MutableList<Flashcard> = mutableListOf()
    private var currentCardIndex = 0
    private var isShowingAnswer = false

    // ActivityResultLauncher for adding a new card
    private val addCardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val question = data?.getStringExtra("question")
            val answer = data?.getStringExtra("answer")

            if (question != null && answer != null) {
                flashcards.add(Flashcard(question, answer))
                saveFlashcards()
                currentCardIndex = flashcards.size - 1
                updateCardView()
            }
        }
    }

    // ActivityResultLauncher for editing a card
    private val editCardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val question = data?.getStringExtra("question")
            val answer = data?.getStringExtra("answer")

            if (question != null && answer != null && flashcards.isNotEmpty()) {
                flashcards[currentCardIndex] = Flashcard(question, answer)
                saveFlashcards()
                updateCardView()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFlashcards()
        updateCardView()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.showAnswerButton.setOnClickListener {
            flipCard()
        }

        binding.nextButton.setOnClickListener {
            if (flashcards.isNotEmpty()) {
                currentCardIndex = (currentCardIndex + 1) % flashcards.size
                updateCardView()
            }
        }

        binding.prevButton.setOnClickListener {
            if (flashcards.isNotEmpty()) {
                currentCardIndex = (currentCardIndex - 1 + flashcards.size) % flashcards.size
                updateCardView()
            }
        }

        binding.addCardFab.setOnClickListener {
            val intent = Intent(this, AddEditCardActivity::class.java)
            addCardLauncher.launch(intent)
        }

        binding.editButton.setOnClickListener {
            if (flashcards.isNotEmpty()) {
                val intent = Intent(this, AddEditCardActivity::class.java)
                intent.putExtra("question", flashcards[currentCardIndex].question)
                intent.putExtra("answer", flashcards[currentCardIndex].answer)
                editCardLauncher.launch(intent)
            }
        }

        binding.deleteButton.setOnClickListener {
            if (flashcards.isNotEmpty()) {
                showDeleteConfirmationDialog()
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Flashcard")
            .setMessage("Are you sure you want to delete this flashcard?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCurrentCard()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCurrentCard() {
        flashcards.removeAt(currentCardIndex)
        if (flashcards.isEmpty()) {
            currentCardIndex = 0
        } else if (currentCardIndex >= flashcards.size) {
            currentCardIndex = flashcards.size - 1
        }
        saveFlashcards()
        updateCardView()
    }


    private fun loadFlashcards() {
        val sharedPreferences = getSharedPreferences("flashcards", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("flashcard_list", null)
        val type = object : TypeToken<MutableList<Flashcard>>() {}.type

        flashcards = gson.fromJson(json, type) ?: mutableListOf()

        if (flashcards.isEmpty()) {
            flashcards.addAll(listOf(
                Flashcard("What is the capital of France?", "Paris"),
                Flashcard("What is the highest mountain in the world?", "Mount Everest"),
                Flashcard("What is the largest country in the world by area?", "Russia"),
                Flashcard("What is the chemical symbol for water?", "H2O"),
                Flashcard("Who wrote 'To Kill a Mockingbird'?", "Harper Lee"),
                Flashcard("What planet is known as the Red Planet?", "Mars"),
                Flashcard("Who painted the Mona Lisa?", "Leonardo da Vinci"),
                Flashcard("What is the powerhouse of the cell?", "Mitochondria"),
                Flashcard("How many continents are there?", "7")
            ))
        }
        // Shuffle the flashcards for a random order each time
        flashcards.shuffle()
        saveFlashcards()
    }

    private fun saveFlashcards() {
        val sharedPreferences = getSharedPreferences("flashcards", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(flashcards)
        editor.putString("flashcard_list", json)
        editor.apply()
    }

    private fun updateCardView() {
        // Reset card state to show the question
        isShowingAnswer = false
        binding.cardView.rotationY = 0f
        binding.cardView.alpha = 1f

        if (flashcards.isNotEmpty()) {
            val flashcard = flashcards[currentCardIndex]
            binding.questionText.text = flashcard.question
            binding.answerText.text = flashcard.answer
            binding.questionText.visibility = View.VISIBLE
            binding.answerText.visibility = View.INVISIBLE
        } else {
            // Display a message when there are no cards
            binding.questionText.text = "Add a new card to start!"
            binding.answerText.visibility = View.INVISIBLE
        }
    }

    private fun flipCard() {
        val scale = applicationContext.resources.displayMetrics.density
        binding.cardView.cameraDistance = 8000 * scale

        val flipOut = AnimatorInflater.loadAnimator(this, R.animator.card_flip_part_1)
        val flipIn = AnimatorInflater.loadAnimator(this, R.animator.card_flip_part_2)

        flipOut.setTarget(binding.cardView)
        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // The first half of the animation is done (card is edge-on).
                // Now, switch the text and play the second half of the animation.
                if (isShowingAnswer) {
                    binding.questionText.visibility = View.VISIBLE
                    binding.answerText.visibility = View.INVISIBLE
                } else {
                    binding.questionText.visibility = View.INVISIBLE
                    binding.answerText.visibility = View.VISIBLE
                }
                isShowingAnswer = !isShowingAnswer
                
                // Correctly apply the second animation
                flipIn.setTarget(binding.cardView)
                flipIn.start()
            }
        })
        flipOut.start()
    }
}