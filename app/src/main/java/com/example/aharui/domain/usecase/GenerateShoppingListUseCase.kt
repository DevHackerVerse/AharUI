package com.example.aharui.domain.usecase

import com.example.aharui.data.api.DailyMealPlan
import com.example.aharui.data.api.GeminiService
import com.example.aharui.data.api.WeeklyMealPlan
import com.example.aharui.data.model.UserProfile
import com.example.aharui.data.repository.MealLogRepository
import com.example.aharui.data.repository.ShoppingListRepository
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GenerateShoppingListUseCase @Inject constructor(
    private val mealLogRepository: MealLogRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(
        userId: String,
        userProfile: UserProfile,
        daysAhead: Int = 7
    ): Result<Long> {
        val today = DateUtils.getTodayString()
        val endDate = DateUtils.getDaysAgo(-daysAhead)

        // Get all planned meals for the next week
        val meals = mealLogRepository.getMealsForDateRange(userId, today, endDate).first()

        if (meals.isEmpty()) {
            return Result.Error("No meals found for the selected period. Please generate a meal plan first.")
        }

        // Group meals by day for proper structure
        val dailyPlans = mutableListOf<DailyMealPlan>()
        val mealsByDate = meals.groupBy { /* You'll need to add date field or use another way to group */ }

        // For now, create a simple structure
        dailyPlans.add(DailyMealPlan(1, meals))

        val weeklyPlan = WeeklyMealPlan(dailyPlans)

        // Generate smart shopping list using Gemini
        val geminiResult = geminiService.generateShoppingListFromMealPlan(weeklyPlan, userProfile)

        return when (geminiResult) {
            is Result.Success -> {
                val title = "AI Shopping List - Week of ${DateUtils.getTodayString()}"
                shoppingListRepository.createShoppingList(userId, title, geminiResult.data)
            }
            is Result.Error -> Result.Error(geminiResult.message)
            else -> Result.Error("Unknown error")
        }
    }
}