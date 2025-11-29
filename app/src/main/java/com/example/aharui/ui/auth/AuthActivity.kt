package com.example.aharui.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aharui.MainActivity
import com.example.aharui.R
import com.example.aharui.data.local.entity.RewardEntity
import com.example.aharui.data.model.ActivityLevel
import com.example.aharui.data.model.Badge
import com.example.aharui.data.model.Gender
import com.example.aharui.data.model.GoalType
import com.example.aharui.data.model.UserProfile
import com.example.aharui.data.preferences.PreferencesManager
import com.example.aharui.data.repository.RewardRepository
import com.example.aharui.data.repository.UserProfileRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var rewardRepository: RewardRepository

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var continueButton: Button
    private lateinit var skipButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        continueButton = findViewById(R.id.continue_button)
        skipButton = findViewById(R.id.skip_button)
    }

    private fun setupListeners() {
        continueButton.setOnClickListener {
            handleSignup()
        }

        skipButton.setOnClickListener {
            handleSkip()
        }
    }

    private fun handleSignup() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        continueButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = UUID.randomUUID().toString()

                // Create default user profile
                val profile = UserProfile(
                    userId = userId,
                    name = name,
                    email = email.ifEmpty { null },
                    gender = null,
                    dateOfBirth = null,
                    heightCm = null,
                    currentWeightKg = null,
                    targetWeightKg = null,
                    goalType = GoalType.GENERAL_HEALTH,
                    dailyCalorieTarget = 2000,
                    dailyWaterTarget = 2500,
                    activityLevel = ActivityLevel.MODERATELY_ACTIVE
                )

                userProfileRepository.saveUserProfile(profile)

                // Initialize rewards
                rewardRepository.initializeRewards(userId)

                // Save user session
                preferencesManager.currentUserId = userId
                preferencesManager.isLoggedIn = true
                preferencesManager.isFirstLaunch = false

                withContext(Dispatchers.Main) {
                    navigateToMain()
                }
                // Save user session
                preferencesManager.currentUserId = userId
                preferencesManager.isLoggedIn = true
                preferencesManager.isFirstLaunch = false

                withContext(Dispatchers.Main) {
                    // Schedule notifications
                    com.example.aharui.util.WorkManagerHelper.scheduleWaterReminders(this@AuthActivity)
                    com.example.aharui.util.WorkManagerHelper.scheduleMealReminders(this@AuthActivity)

                    navigateToMain()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    continueButton.isEnabled = true
                    Toast.makeText(
                        this@AuthActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleSkip() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = "guest_${System.currentTimeMillis()}"

                // Create guest profile
                val profile = UserProfile(
                    userId = userId,
                    name = "Guest User",
                    email = null,
                    gender = null,
                    dateOfBirth = null,
                    heightCm = null,
                    currentWeightKg = null,
                    targetWeightKg = null,
                    goalType = GoalType.GENERAL_HEALTH,
                    dailyCalorieTarget = 2000,
                    dailyWaterTarget = 2500,
                    activityLevel = ActivityLevel.MODERATELY_ACTIVE
                )

                userProfileRepository.saveUserProfile(profile)
                rewardRepository.initializeRewards(userId)

                preferencesManager.currentUserId = userId
                preferencesManager.isLoggedIn = true
                preferencesManager.isFirstLaunch = false

                withContext(Dispatchers.Main) {
                    navigateToMain()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AuthActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}