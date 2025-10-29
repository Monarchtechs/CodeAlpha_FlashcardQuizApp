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

/**
 * The main screen of the app, responsible for displaying flashcards,
 * handling user navigation, and managing card operations like adding, editing, and deleting.
 */
class MainActivity : AppCompatActivity() {

    // View binding for safe and easy access to layout views.
    private lateinit var binding: ActivityMainBinding
    // The list of flashcard objects.
    private var flashcards: MutableList<Flashcard> = mutableListOf()
    // Index to keep track of the currently displayed card.
    private var currentCardIndex = 0
    // State to track if the answer side of the card is currently visible.
    private var isShowingAnswer = false

    /**
     * Handles the result from AddEditCardActivity when a new card is created.
     */
    private val addCardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val question = data?.getStringExtra("question")
            val answer = data?.getStringExtra("answer")

            if (question != null && answer != null) {
                // Add the new card, save, and update the view.
                flashcards.add(Flashcard(question, answer))
                saveFlashcards()
                currentCardIndex = flashcards.size - 1 // Navigate to the new card.
                updateCardView()
            }
        }
    }

    /**
     * Handles the result from AddEditCardActivity when a card is edited.
     */
    private val editCardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val question = data?.getStringExtra("question")
            val answer = data?.getStringExtra("answer")

            if (question != null && answer != null && flashcards.isNotEmpty()) {
                // Update the card at the current index, save, and refresh the view.
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

        // Initial setup
        loadFlashcards()
        updateCardView()
        setupClickListeners()
    }

    /**
     * Sets up all the click listeners for the buttons on the main screen.
     */
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
                // Pre-fill the fields with the current card's data.
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

    /**
     * Displays a confirmation dialog before deleting a card to prevent accidental deletion.
     */
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

    /**
     * Deletes the currently visible flashcard from the list.
     */
    private fun deleteCurrentCard() {
        flashcards.removeAt(currentCardIndex)
        if (flashcards.isEmpty()) {
            currentCardIndex = 0
        } else if (currentCardIndex >= flashcards.size) {
            // Adjust index if the last card was deleted.
            currentCardIndex = flashcards.size - 1
        }
        saveFlashcards()
        updateCardView()
    }

    /**
     * Loads flashcards from SharedPreferences. If none exist, loads default starter cards.
     */
    private fun loadFlashcards() {
        val sharedPreferences = getSharedPreferences("flashcards", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("flashcard_list", null)
        val type = object : TypeToken<MutableList<Flashcard>>() {}.type

        flashcards = gson.fromJson(json, type) ?: mutableListOf()

        if (flashcards.isEmpty()) {
            // If the user has no cards, provide a default set to get them started.
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
        // Shuffle the flashcards for a random order each time the app starts.
        flashcards.shuffle()
        saveFlashcards()
    }

    /**
     * Saves the current list of flashcards to SharedPreferences using Gson for JSON serialization.
     */
    private fun saveFlashcards() {
        val sharedPreferences = getSharedPreferences("flashcards", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(flashcards)
        editor.putString("flashcard_list", json)
        editor.apply() // apply() is asynchronous and preferred over commit().
    }

    /**
     * Updates the UI to display the current flashcard. Resets the view to the question side.
     */
    private fun updateCardView() {
        // Always reset the card to its question side when navigating.
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
            // Display a helpful message if there are no cards.
            binding.questionText.text = "Add a new card to start!"
            binding.answerText.visibility = View.INVISIBLE
        }
    }

    /**
     * Animates the card flip to show the opposite side (question or answer).
     */
    private fun flipCard() {
        val scale = applicationContext.resources.displayMetrics.density
        binding.cardView.cameraDistance = 8000 * scale

        val flipOut = AnimatorInflater.loadAnimator(this, R.animator.card_flip_part_1)
        val flipIn = AnimatorInflater.loadAnimator(this, R.animator.card_flip_part_2)

        flipOut.setTarget(binding.cardView)
        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // When the first half of the animation finishes, the card is edge-on.
                // Now, swap the text visibility and start the second half of the animation.
                if (isShowingAnswer) {
                    binding.questionText.visibility = View.VISIBLE
                    binding.answerText.visibility = View.INVISIBLE
                } else {
                    binding.questionText.visibility = View.INVISIBLE
                    binding.answerText.visibility = View.VISIBLE
                }
                isShowingAnswer = !isShowingAnswer

                // Apply the second half of the animation to complete the flip.
                flipIn.setTarget(binding.cardView)
                flipIn.start()
            }
        })
        flipOut.start()
    }
}
