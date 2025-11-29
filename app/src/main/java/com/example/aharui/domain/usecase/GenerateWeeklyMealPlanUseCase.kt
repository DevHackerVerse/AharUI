package com.example.aharui.domain.usecase

import com.example.aharui.data.api.GeminiService
import com.example.aharui.data.api.WeeklyMealPlan
import com.example.aharui.data.model.UserProfile
import com.example.aharui.data.repository.MealLogRepository
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import javax.inject.Inject

class GenerateWeeklyMealPlanUseCase @Inject constructor(
    private val geminiService: GeminiService,
    private val mealLogRepository: MealLogRepository
) {
    suspend operator fun invoke(userProfile: UserProfile, userId: String): Result<WeeklyMealPlan> {
        // Generate meal plan using Gemini
        val result = geminiService.generateWeeklyMealPlan(userProfile)

        if (result is Result.Success) {
            // Save all meals to database for the week
            val mealPlan = result.data

            mealPlan.days.forEachIndexed { index, dailyPlan ->
                val date = DateUtils.getDaysAgo(-index) // Today, tomorrow, day after, etc.
                dailyPlan.meals.forEach { meal ->
                    mealLogRepository.addMeal(userId, meal, date)
                }
            }
        }

        return result
    }
}