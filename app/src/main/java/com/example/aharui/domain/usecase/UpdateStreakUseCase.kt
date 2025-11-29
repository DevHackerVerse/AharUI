package com.example.aharui.domain.usecase

import com.example.aharui.data.preferences.PreferencesManager
import com.example.aharui.data.repository.MealLogRepository
import com.example.aharui.data.repository.RewardRepository
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateStreakUseCase @Inject constructor(
    private val mealLogRepository: MealLogRepository,
    private val rewardRepository: RewardRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(userId: String): Result<Int> {
        val today = DateUtils.getTodayString()
        val yesterday = DateUtils.getDaysAgo(1)
        val lastCheckDate = preferencesManager.lastStreakCheckDate

        // Only check once per day
        if (lastCheckDate == today) {
            return Result.Success(0)
        }

        // Get current rewards
        val rewardData = rewardRepository.getRewards(userId).first()
        val currentStreak = rewardData?.streakDays ?: 0

        // Check if user logged meals yesterday
        val yesterdayMealCount = mealLogRepository.getMealCountForDate(userId, yesterday)

        val newStreak = if (yesterdayMealCount > 0) {
            currentStreak + 1
        } else {
            0
        }

        // Update streak
        rewardRepository.updateStreak(userId, newStreak)
        preferencesManager.lastStreakCheckDate = today

        // Award bonus points for milestones
        val bonusPoints = when (newStreak) {
            7 -> 100
            14 -> 200
            30 -> 500
            else -> 0
        }

        if (bonusPoints > 0) {
            rewardRepository.addPoints(userId, bonusPoints)
        }

        return Result.Success(newStreak)
    }
}