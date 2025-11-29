package com.example.aharui.ui.diet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aharui.data.api.WeeklyMealPlan
import com.example.aharui.data.model.Meal
import com.example.aharui.data.model.MealType
import com.example.aharui.data.model.UserProfile
import com.example.aharui.data.preferences.PreferencesManager
import com.example.aharui.data.repository.MealLogRepository
import com.example.aharui.data.repository.UserProfileRepository
import com.example.aharui.domain.usecase.BadgeUnlockUseCase
import com.example.aharui.domain.usecase.GenerateShoppingListUseCase
import com.example.aharui.domain.usecase.GenerateWeeklyMealPlanUseCase
import com.example.aharui.domain.usecase.LogMealUseCase
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DietViewModel @Inject constructor(
    private val mealLogRepository: MealLogRepository,
    private val logMealUseCase: LogMealUseCase,
    private val badgeUnlockUseCase: BadgeUnlockUseCase,
    private val generateShoppingListUseCase: GenerateShoppingListUseCase,
    private val generateWeeklyMealPlanUseCase: GenerateWeeklyMealPlanUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId: String
        get() = preferencesManager.currentUserId ?: "default_user"

    private val _uiState = MutableStateFlow(DietUiState())
    val uiState: StateFlow<DietUiState> = _uiState.asStateFlow()

    // User profile from database
    val userProfile: StateFlow<UserProfile?> = userProfileRepository
        .getUserProfile(userId)
        .onEach { profile ->
            Log.d("DietViewModel", "Profile flow emitted: $profile")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        Log.d("DietViewModel", "Initializing with userId: $userId")
        loadMeals()
        checkProfile()
    }

    private fun checkProfile() {
        viewModelScope.launch {
            val profile = userProfileRepository.getUserProfileSync(userId)
            Log.d("DietViewModel", "Initial profile check: $profile")

            if (profile == null) {
                Log.w("DietViewModel", "No profile found in database for userId: $userId")
                // Check if profile exists in preferences
                val prefProfile = preferencesManager.getUserProfile()
                Log.d("DietViewModel", "Profile from preferences: $prefProfile")

                if (prefProfile != null) {
                    Log.d("DietViewModel", "Migrating profile from preferences to database")
                    userProfileRepository.saveUserProfile(prefProfile)
                }
            }
        }
    }

    private fun loadMeals() {
        viewModelScope.launch {
            val today = DateUtils.getTodayString()
            mealLogRepository.getMealsForDate(userId, today)
                .collectLatest { meals ->
                    val groupedMeals = meals.groupBy { it.mealType }
                    _uiState.update {
                        it.copy(
                            meals = meals,
                            breakfastMeals = groupedMeals[MealType.BREAKFAST] ?: emptyList(),
                            lunchMeals = groupedMeals[MealType.LUNCH] ?: emptyList(),
                            dinnerMeals = groupedMeals[MealType.DINNER] ?: emptyList(),
                            snackMeals = groupedMeals[MealType.SNACK] ?: emptyList()
                        )
                    }
                }
        }
    }

    fun addMeal(meal: Meal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = logMealUseCase(userId, meal)) {
                is Result.Success -> {
                    val unlockedBadges = badgeUnlockUseCase(userId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mealAddedSuccess = true,
                            unlockedBadges = unlockedBadges
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Generate meal plan - automatically loads profile from database
     */
    fun generateWeeklyMealPlan() {
        viewModelScope.launch {
            Log.d("DietViewModel", "Starting meal plan generation...")
            _uiState.update { it.copy(isGeneratingMealPlan = true, error = null) }

            val profile = userProfileRepository.getUserProfileSync(userId)
            Log.d("DietViewModel", "Profile retrieved for meal plan: $profile")

            if (profile == null) {
                Log.e("DietViewModel", "Profile is null, cannot generate meal plan")
                _uiState.update {
                    it.copy(
                        isGeneratingMealPlan = false,
                        error = "Profile not found in database. Please save your profile first."
                    )
                }
                return@launch
            }

            Log.d("DietViewModel", "Profile validated successfully, calling use case...")
            when (val result = generateWeeklyMealPlanUseCase(profile, userId)) {
                is Result.Success -> {
                    Log.d("DietViewModel", "✅ Meal plan generated successfully!")
                    Log.d("DietViewModel", "Meal Plan Details:")
                    Log.d("DietViewModel", "  Days: ${result.data.days.size}")
                    result.data.days.forEachIndexed { index, day ->
                        Log.d("DietViewModel", "  Day ${day.dayNumber}: ${day.meals.size} meals")
                        day.meals.forEach { meal ->
                            Log.d("DietViewModel", "    - ${meal.name} (${meal.calories}kcal)")
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isGeneratingMealPlan = false,
                            mealPlanGenerated = true,
                            weeklyMealPlan = result.data
                        )
                    }
                }
                is Result.Error -> {
                    Log.e("DietViewModel", "❌ Meal plan generation failed: ${result.message}")
                    _uiState.update {
                        it.copy(isGeneratingMealPlan = false, error = result.message)
                    }
                }
                else -> {
                    Log.e("DietViewModel", "❌ Unknown result type")
                }
            }
        }
    }

    /**
     * Generate shopping list - automatically loads profile from database
     */
    fun generateShoppingList() {
        viewModelScope.launch {
            Log.d("DietViewModel", "Starting shopping list generation...")
            _uiState.update { it.copy(isGeneratingList = true, error = null) }

            val profile = userProfileRepository.getUserProfileSync(userId)
            Log.d("DietViewModel", "Profile retrieved for shopping list: $profile")

            if (profile == null) {
                Log.e("DietViewModel", "Profile is null, cannot generate shopping list")
                _uiState.update {
                    it.copy(
                        isGeneratingList = false,
                        error = "Profile not found in database. Please save your profile first."
                    )
                }
                return@launch
            }

            when (val result = generateShoppingListUseCase(userId, profile, 7)) {
                is Result.Success -> {
                    Log.d("DietViewModel", "Shopping list generated successfully")
                    _uiState.update {
                        it.copy(
                            isGeneratingList = false,
                            shoppingListGenerated = true,
                            generatedListId = result.data
                        )
                    }
                }
                is Result.Error -> {
                    Log.e("DietViewModel", "Shopping list generation failed: ${result.message}")
                    _uiState.update {
                        it.copy(isGeneratingList = false, error = result.message)
                    }
                }
                else -> {
                    Log.e("DietViewModel", "Unknown result type")
                }
            }
        }
    }

    fun clearSuccessState() {
        _uiState.update {
            it.copy(
                mealAddedSuccess = false,
                shoppingListGenerated = false,
                mealPlanGenerated = false,
                unlockedBadges = emptyList()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class DietUiState(
    val meals: List<Meal> = emptyList(),
    val breakfastMeals: List<Meal> = emptyList(),
    val lunchMeals: List<Meal> = emptyList(),
    val dinnerMeals: List<Meal> = emptyList(),
    val snackMeals: List<Meal> = emptyList(),
    val isLoading: Boolean = false,
    val isGeneratingList: Boolean = false,
    val isGeneratingMealPlan: Boolean = false,
    val mealAddedSuccess: Boolean = false,
    val shoppingListGenerated: Boolean = false,
    val mealPlanGenerated: Boolean = false,
    val generatedListId: Long? = null,
    val weeklyMealPlan: WeeklyMealPlan? = null,
    val unlockedBadges: List<String> = emptyList(),
    val error: String? = null
)