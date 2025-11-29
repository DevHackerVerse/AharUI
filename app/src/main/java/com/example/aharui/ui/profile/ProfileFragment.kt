package com.example.aharui.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aharui.R
import com.example.aharui.ui.auth.OnboardingActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var nameTextView: TextView
    private lateinit var goalChip: Chip
    private lateinit var pointsTextView: TextView
    private lateinit var streakTextView: TextView
    private lateinit var editProfileCard: MaterialCardView
    private lateinit var notificationSettingsCard: MaterialCardView
    private lateinit var waterGoalCard: MaterialCardView
    private lateinit var rewardsCard: MaterialCardView
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        nameTextView = view.findViewById(R.id.name_textview)
        goalChip = view.findViewById(R.id.goal_chip)
        pointsTextView = view.findViewById(R.id.points_textview)
        streakTextView = view.findViewById(R.id.streak_textview)
        editProfileCard = view.findViewById(R.id.edit_profile_card)
        notificationSettingsCard = view.findViewById(R.id.notification_settings_card)
        waterGoalCard = view.findViewById(R.id.water_goal_card)
        rewardsCard = view.findViewById(R.id.rewards_card)
        logoutButton = view.findViewById(R.id.logout_button)
    }

    private fun setupListeners() {
        editProfileCard.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }

        rewardsCard.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_rewards)
        }

        notificationSettingsCard.setOnClickListener {
            showNotificationSettingsDialog()
        }

        waterGoalCard.setOnClickListener {
            showWaterGoalDialog()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: ProfileUiState) {
        state.profile?.let { profile ->
            nameTextView.text = profile.name
            val goalText = "Goal: ${profile.goalType.name.replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }}"
            goalChip.text = goalText
        }

        // Format points with comma separator
        pointsTextView.text = String.format("%,d", state.totalPoints)
        streakTextView.text = "${state.streakDays}"

        if (state.loggedOut) {
            navigateToOnboarding()
        }

        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun showNotificationSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_notification_settings, null)
        val waterReminderSwitch = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.water_reminder_switch)
        val mealReminderSwitch = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.meal_reminder_switch)

        // Load current settings
        val prefsManager = com.example.aharui.data.preferences.PreferencesManager(requireContext())
        waterReminderSwitch.isChecked = prefsManager.waterReminderEnabled
        mealReminderSwitch.isChecked = prefsManager.mealReminderEnabled

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Notification Settings")
            .setView(view)
            .setPositiveButton("Save") { dialog, _ ->
                prefsManager.waterReminderEnabled = waterReminderSwitch.isChecked
                prefsManager.mealReminderEnabled = mealReminderSwitch.isChecked

                // Update WorkManager based on settings
                if (waterReminderSwitch.isChecked) {
                    com.example.aharui.util.WorkManagerHelper.scheduleWaterReminders(requireContext())
                } else {
                    androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("water_reminder")
                }

                if (mealReminderSwitch.isChecked) {
                    com.example.aharui.util.WorkManagerHelper.scheduleMealReminders(requireContext())
                } else {
                    androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("meal_reminder_Breakfast")
                    androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("meal_reminder_Lunch")
                    androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("meal_reminder_Dinner")
                }

                Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showWaterGoalDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_water_goal, null)
        val waterGoalInput = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.water_goal_input)
        val preset2000 = view.findViewById<Button>(R.id.preset_2000)
        val preset2500 = view.findViewById<Button>(R.id.preset_2500)
        val preset3000 = view.findViewById<Button>(R.id.preset_3000)

        val prefsManager = com.example.aharui.data.preferences.PreferencesManager(requireContext())
        waterGoalInput.setText(prefsManager.dailyWaterGoal.toString())

        preset2000.setOnClickListener { waterGoalInput.setText("2000") }
        preset2500.setOnClickListener { waterGoalInput.setText("2500") }
        preset3000.setOnClickListener { waterGoalInput.setText("3000") }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Daily Water Goal")
            .setView(view)
            .setPositiveButton("Save") { dialog, _ ->
                val goal = waterGoalInput.text.toString().toIntOrNull()
                if (goal != null && goal > 0) {
                    prefsManager.dailyWaterGoal = goal
                    Toast.makeText(requireContext(), "Water goal updated to ${goal}ml", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid goal", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? Your data will be saved.")
            .setPositiveButton("Logout") { dialog, _ ->
                viewModel.logout()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun navigateToOnboarding() {
        val intent = Intent(requireActivity(), OnboardingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}