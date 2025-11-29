package com.example.aharui.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aharui.R
import com.example.aharui.data.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var nameInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var heightInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var currentWeightInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var targetWeightInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var goalTypeDropdown: AutoCompleteTextView
    private lateinit var activityLevelDropdown: AutoCompleteTextView
    private lateinit var saveProfileButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupDropdowns()
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        nameInput = view.findViewById(R.id.name_input)
        heightInput = view.findViewById(R.id.height_input)
        currentWeightInput = view.findViewById(R.id.current_weight_input)
        targetWeightInput = view.findViewById(R.id.target_weight_input)
        genderDropdown = view.findViewById(R.id.gender_dropdown)
        goalTypeDropdown = view.findViewById(R.id.goal_type_dropdown)
        activityLevelDropdown = view.findViewById(R.id.activity_level_dropdown)
        saveProfileButton = view.findViewById(R.id.save_profile_button)
    }

    private fun setupDropdowns() {
        val genders = Gender.values().map { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        genderDropdown.setAdapter(genderAdapter)

        val goalTypes = GoalType.values().map { it.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() } }
        val goalAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, goalTypes)
        goalTypeDropdown.setAdapter(goalAdapter)

        val activityLevels = ActivityLevel.values().map { it.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() } }
        val activityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, activityLevels)
        activityLevelDropdown.setAdapter(activityAdapter)
    }

    private fun setupListeners() {
        saveProfileButton.setOnClickListener {
            saveProfile()
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
            Log.d("EditProfileFragment", "Updating UI with profile: $profile")
            nameInput.setText(profile.name)
            heightInput.setText(profile.heightCm?.toString() ?: "")
            currentWeightInput.setText(profile.currentWeightKg?.toString() ?: "")
            targetWeightInput.setText(profile.targetWeightKg?.toString() ?: "")

            profile.gender?.let {
                genderDropdown.setText(it.name.lowercase().replaceFirstChar { char -> char.uppercase() }, false)
            }

            goalTypeDropdown.setText(profile.goalType.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }, false)
            activityLevelDropdown.setText(profile.activityLevel.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }, false)
        }

        if (state.updateSuccess) {
            Toast.makeText(requireContext(), "✅ Profile saved to database successfully!", Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessState()
            findNavController().navigateUp()
        }

        state.error?.let { error ->
            Toast.makeText(requireContext(), "❌ Error: $error", Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }

        saveProfileButton.isEnabled = !state.isLoading
    }

    private fun saveProfile() {
        val name = nameInput.text.toString()
        val heightText = heightInput.text.toString()
        val currentWeightText = currentWeightInput.text.toString()
        val targetWeightText = targetWeightInput.text.toString()
        val genderText = genderDropdown.text.toString()
        val goalTypeText = goalTypeDropdown.text.toString()
        val activityLevelText = activityLevelDropdown.text.toString()

        Log.d("EditProfileFragment", "Attempting to save profile...")
        Log.d("EditProfileFragment", "Name: $name")
        Log.d("EditProfileFragment", "Height: $heightText")
        Log.d("EditProfileFragment", "Current Weight: $currentWeightText")
        Log.d("EditProfileFragment", "Target Weight: $targetWeightText")

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        val height = heightText.toDoubleOrNull()
        val currentWeight = currentWeightText.toDoubleOrNull()
        val targetWeight = targetWeightText.toDoubleOrNull()

        if (height == null || height <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid height", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentWeight == null || currentWeight <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid current weight", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetWeight == null || targetWeight <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid target weight", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = try {
            Gender.valueOf(genderText.uppercase().replace(" ", "_"))
        } catch (e: Exception) {
            null
        }

        val goalType = try {
            GoalType.valueOf(goalTypeText.uppercase().replace(" ", "_"))
        } catch (e: Exception) {
            GoalType.GENERAL_HEALTH
        }

        val activityLevel = try {
            ActivityLevel.valueOf(activityLevelText.uppercase().replace(" ", "_"))
        } catch (e: Exception) {
            ActivityLevel.MODERATELY_ACTIVE
        }

        val currentProfile = viewModel.uiState.value.profile
        val updatedProfile = UserProfile(
            userId = currentProfile?.userId ?: "default_user",
            name = name,
            email = currentProfile?.email,
            gender = gender,
            dateOfBirth = currentProfile?.dateOfBirth,
            heightCm = height,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            goalType = goalType,
            dailyCalorieTarget = currentProfile?.dailyCalorieTarget ?: 2000,
            dailyWaterTarget = currentProfile?.dailyWaterTarget ?: 2500,
            activityLevel = activityLevel
        )

        Log.d("EditProfileFragment", "Calling viewModel.updateProfile with: $updatedProfile")
        viewModel.updateProfile(updatedProfile)
    }
}