package com.example.aharui.ui.diet

import android.os.Bundle
import android.util.Log
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aharui.R
import com.example.aharui.data.model.*
import com.example.aharui.data.preferences.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DietFragment : Fragment() {

    private val viewModel: DietViewModel by viewModels()

    @Inject
    lateinit var preferencesManager: PreferencesManager

    // Views
    private lateinit var mealsRecyclerView: RecyclerView
    private lateinit var scanFoodFab: ExtendedFloatingActionButton
    private lateinit var generateShoppingListButton: Button
    private lateinit var generateMealPlanButton: Button
    private lateinit var addMealButton: Button
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var todaysMealsTitle: TextView
    private lateinit var emptyStateView: View
    private lateinit var mealsHeader: View

    private val mealAdapter = MealAdapter(
        onMealClick = { meal -> showMealDetailsDialog(meal) },
        onMealLongClick = { meal -> showDeleteMealDialog(meal) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        mealsRecyclerView = view.findViewById(R.id.meals_recyclerview)
        scanFoodFab = view.findViewById(R.id.scan_food_fab)
        generateShoppingListButton = view.findViewById(R.id.generate_shopping_list_button)
        generateMealPlanButton = view.findViewById(R.id.generate_meal_plan_button)
        addMealButton = view.findViewById(R.id.add_meal_button)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        todaysMealsTitle = view.findViewById(R.id.todays_meals_title)
        emptyStateView = view.findViewById(R.id.empty_state_text)
        mealsHeader = view.findViewById(R.id.meals_header)
    }

    private fun setupRecyclerView() {
        mealsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mealAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        scanFoodFab.setOnClickListener {
            navigateToScanFood()
        }

        generateMealPlanButton.setOnClickListener {
            showGenerateMealPlanDialog()
        }

        generateShoppingListButton.setOnClickListener {
            generateShoppingList()
        }

        addMealButton.setOnClickListener {
            showAddMealDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }

                launch {
                    viewModel.userProfile.collect { profile ->
                        Log.d("DietFragment", "Profile updated: $profile")
                    }
                }
            }
        }
    }

    private fun updateUI(state: DietUiState) {
        mealAdapter.submitList(state.meals)

        // Show/hide empty state and meals header
        if (state.meals.isEmpty() && !state.isLoading && !state.isGeneratingMealPlan) {
            emptyStateView.visibility = View.VISIBLE
            mealsRecyclerView.visibility = View.GONE
            mealsHeader.visibility = View.GONE
        } else {
            emptyStateView.visibility = View.GONE
            mealsRecyclerView.visibility = View.VISIBLE
            mealsHeader.visibility = if (state.meals.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Progress indicator
        progressIndicator.visibility = when {
            state.isLoading || state.isGeneratingList || state.isGeneratingMealPlan -> View.VISIBLE
            else -> View.GONE
        }

        // Button states with loading feedback
        generateShoppingListButton.apply {
            isEnabled = !state.isGeneratingList && !state.isGeneratingMealPlan
            text = if (state.isGeneratingList) "Generating..." else "Smart Shopping List"
        }

        generateMealPlanButton.apply {
            isEnabled = !state.isGeneratingMealPlan && !state.isGeneratingList
            text = if (state.isGeneratingMealPlan) "Generating..." else "Generate 7-Day Meal Plan"
        }

        addMealButton.isEnabled = !state.isLoading
        scanFoodFab.isEnabled = !state.isLoading

        // Success states
        if (state.mealPlanGenerated) {
            showMealPlanSuccessDialog(state.weeklyMealPlan)
            viewModel.clearSuccessState()
        }

        if (state.mealAddedSuccess) {
            Toast.makeText(requireContext(), "✅ Meal logged successfully!", Toast.LENGTH_SHORT).show()
            if (state.unlockedBadges.isNotEmpty()) {
                showBadgeUnlockedDialog(state.unlockedBadges)
            }
            viewModel.clearSuccessState()
        }

        if (state.shoppingListGenerated) {
            showShoppingListSuccessDialog()
            viewModel.clearSuccessState()
        }

        state.error?.let { error ->
            showErrorDialog(error)
            viewModel.clearError()
        }
    }

    private fun navigateToScanFood() {
        try {
            findNavController().navigate(R.id.action_diet_to_scanFood)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGenerateMealPlanDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userProfile = viewModel.userProfile.value

            Log.d("DietFragment", """
                Profile from Database:
                - UserId: ${userProfile?.userId}
                - Name: ${userProfile?.name}
                - Height: ${userProfile?.heightCm}
                - Weight: ${userProfile?.currentWeightKg}
                - Target: ${userProfile?.targetWeightKg}
                - Goal: ${userProfile?.goalType}
                - Activity: ${userProfile?.activityLevel}
            """.trimIndent())

            if (userProfile == null) {
                Log.e("DietFragment", "Profile is NULL - showing incomplete dialog")
                showIncompleteProfileDialog(listOf("Profile not found in database"))
                return@launch
            }

            val missingFields = mutableListOf<String>()

            if (userProfile.heightCm == null || userProfile.heightCm!! <= 0) {
                missingFields.add("Height")
            }
            if (userProfile.currentWeightKg == null || userProfile.currentWeightKg!! <= 0) {
                missingFields.add("Current Weight")
            }
            if (userProfile.targetWeightKg == null || userProfile.targetWeightKg!! <= 0) {
                missingFields.add("Target Weight")
            }

            if (missingFields.isNotEmpty()) {
                Log.e("DietFragment", "Profile incomplete. Missing: $missingFields")
                showIncompleteProfileDialog(missingFields)
                return@launch
            }

            Log.d("DietFragment", "Profile validated successfully - showing confirmation dialog")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("🤖 Generate AI Meal Plan")
                .setMessage(
                    """
                Generate a personalized 7-day meal plan:
                
                📊 Your Stats:
                • Height: ${userProfile.heightCm} cm
                • Weight: ${userProfile.currentWeightKg} kg → ${userProfile.targetWeightKg} kg
                • Goal: ${userProfile.goalType.name.replace("_", " ")}
                • Calories: ${userProfile.dailyCalorieTarget} kcal/day
                
                ⏱️ This takes 15-30 seconds.
                """.trimIndent()
                )
                .setPositiveButton("Generate") { dialog, _ ->
                    Log.d("DietFragment", "User confirmed - calling generateWeeklyMealPlan()")
                    viewModel.generateWeeklyMealPlan()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun generateShoppingList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userProfile = viewModel.userProfile.value

            if (userProfile == null) {
                Toast.makeText(
                    requireContext(),
                    "Please complete your profile first",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("🛒 Generate Shopping List")
                .setMessage(
                    """
                Create a smart shopping list from your meal plan for the next 7 days.
                
                The AI will:
                • Consolidate duplicate ingredients
                • Organize by category
                • Calculate proper quantities
                
                Continue?
                """.trimIndent()
                )
                .setPositiveButton("Generate") { dialog, _ ->
                    viewModel.generateShoppingList()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showMealPlanSuccessDialog(mealPlan: com.example.aharui.data.api.WeeklyMealPlan?) {
        val totalMeals = mealPlan?.days?.sumOf { it.meals.size } ?: 0
        val totalDays = mealPlan?.days?.size ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✅ Meal Plan Generated!")
            .setMessage(
                """
                Successfully generated your personalized meal plan!
                
                📅 Days: $totalDays
                🍽️ Total Meals: $totalMeals
                
                Your meals for the next week have been added to your calendar.
                
                Tip: Generate a shopping list to get all the ingredients you need!
                """.trimIndent()
            )
            .setPositiveButton("Generate Shopping List") { dialog, _ ->
                generateShoppingList()
                dialog.dismiss()
            }
            .setNegativeButton("Done") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showShoppingListSuccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✅ Shopping List Generated!")
            .setMessage(
                """
                Your AI-powered shopping list is ready!
                
                It includes all ingredients needed for your meal plan, organized by category.
                
                View it now?
                """.trimIndent()
            )
            .setPositiveButton("View List") { dialog, _ ->
                try {
                    Toast.makeText(requireContext(), "Shopping list feature coming soon!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Shopping list created successfully!", Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showAddMealDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_meal, null)
        val foodNameInput = dialogView.findViewById<TextInputEditText>(R.id.food_name_input)
        val caloriesInput = dialogView.findViewById<TextInputEditText>(R.id.calories_input)
        val proteinInput = dialogView.findViewById<TextInputEditText>(R.id.protein_input)
        val carbsInput = dialogView.findViewById<TextInputEditText>(R.id.carbs_input)
        val fatInput = dialogView.findViewById<TextInputEditText>(R.id.fat_input)

        val mealTypes = MealType.values().map {
            it.name.lowercase().replaceFirstChar { char -> char.uppercase() }
        }
        var selectedMealType = MealType.BREAKFAST

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Meal Manually")
            .setView(dialogView)
            .setSingleChoiceItems(mealTypes.toTypedArray(), 0) { _, which ->
                selectedMealType = MealType.values()[which]
            }
            .setPositiveButton("Add") { dialog, _ ->
                val foodName = foodNameInput.text.toString().trim()
                val calories = caloriesInput.text.toString().toIntOrNull() ?: 0
                val protein = proteinInput.text.toString().toDoubleOrNull() ?: 0.0
                val carbs = carbsInput.text.toString().toDoubleOrNull() ?: 0.0
                val fat = fatInput.text.toString().toDoubleOrNull() ?: 0.0

                if (foodName.isNotEmpty() && calories > 0) {
                    val meal = Meal(
                        name = foodName,
                        calories = calories,
                        mealType = selectedMealType,
                        source = MealSource.MANUAL,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat
                    )
                    viewModel.addMeal(meal)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Please enter valid food name and calories", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showMealDetailsDialog(meal: Meal) {
        val source = when (meal.source) {
            MealSource.MANUAL -> "Manually Entered"
            MealSource.AI_PLAN -> "AI Generated"
            MealSource.OCR -> "Scanned from Label"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(meal.name)
            .setMessage(
                """
                🍽️ Meal Type: ${meal.mealType.name.lowercase().replaceFirstChar { it.uppercase() }}
                📊 Source: $source
                
                Nutrition Facts:
                • Calories: ${meal.calories} kcal
                • Protein: ${meal.proteinG}g
                • Carbs: ${meal.carbsG}g
                • Fat: ${meal.fatG}g
                
                ${meal.quantity?.let { "Ingredients:\n$it" } ?: ""}
                """.trimIndent()
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Delete") { dialog, _ ->
                showDeleteMealDialog(meal)
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteMealDialog(meal: Meal) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Meal?")
            .setMessage("Are you sure you want to delete \"${meal.name}\"?")
            .setPositiveButton("Delete") { dialog, _ ->
                Toast.makeText(requireContext(), "Delete functionality coming soon", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showBadgeUnlockedDialog(badges: List<String>) {
        val message = if (badges.size == 1) {
            "🎉 Congratulations!\n\nYou unlocked:\n${badges[0]}"
        } else {
            "🎉 Congratulations!\n\nYou unlocked ${badges.size} badges:\n${badges.joinToString("\n")}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Badge Unlocked!")
            .setMessage(message)
            .setPositiveButton("View Rewards") { dialog, _ ->
                try {
                    findNavController().navigate(R.id.action_diet_to_rewards)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Rewards screen not available", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showErrorDialog(error: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("❌ Error")
            .setMessage(error)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showIncompleteProfileDialog(missingFields: List<String>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Incomplete Profile")
            .setMessage(
                """
                Please complete your profile first to generate a personalized meal plan.
                
                Missing information:
                ${missingFields.joinToString("\n") { "• $it" }}
                
                Go to Profile now?
                """.trimIndent()
            )
            .setPositiveButton("Go to Profile") { dialog, _ ->
                try {
                    findNavController().navigate(R.id.action_diet_to_profile)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Please update your profile from settings", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh meals when fragment resumes
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up if needed
    }
}