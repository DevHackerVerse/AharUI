package com.example.aharui.domain.usecase

import com.example.aharui.data.model.Meal
import com.example.aharui.data.repository.DailyLogRepository
import com.example.aharui.data.repository.MealLogRepository
import com.example.aharui.data.repository.RewardRepository
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import com.example.aharui.util.isError
import javax.inject.Inject

class LogMealUseCase @Inject constructor(
    private val mealLogRepository: MealLogRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val rewardRepository: RewardRepository
) {
    suspend operator fun invoke(userId: String, meal: Meal): Result<Long> {
        val today = DateUtils.getTodayString()

        // 1. Log meal
        val result = mealLogRepository.addMeal(userId, meal, today)
        if (result.isError()) {
            return result
        }

        // 2. Update daily calories
        val totalCalories = mealLogRepository.getTotalCaloriesForDate(userId, today)
        dailyLogRepository.updateCalories(userId, today, totalCalories)

        // 3. Award points
        rewardRepository.addPoints(userId, 20)

        return result
    }
}