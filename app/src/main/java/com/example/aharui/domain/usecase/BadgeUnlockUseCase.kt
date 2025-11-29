package com.example.aharui.domain.usecase

import com.example.aharui.data.local.entity.DailyLogEntity
import com.example.aharui.data.model.Badge
import com.example.aharui.data.model.MealSource
import com.example.aharui.data.repository.DailyLogRepository
import com.example.aharui.data.repository.MealLogRepository
import com.example.aharui.data.repository.RewardRepository
import com.example.aharui.data.repository.UserProfileRepository
import com.example.aharui.data.repository.WaterLogRepository
import com.example.aharui.util.DateUtils
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

class BadgeUnlockUseCase @Inject constructor(
    private val rewardRepository: RewardRepository,
    private val mealLogRepository: MealLogRepository,
    private val waterLogRepository: WaterLogRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val userProfileRepository: UserProfileRepository
) {

    suspend operator fun invoke(userId: String): List<String> {
        val unlockedBadges = mutableListOf<String>()

        try {
            val rewardData = rewardRepository.getRewards(userId).first() ?: return emptyList()

            // Check each badge
            rewardData.badges.forEach { badge ->
                if (!badge.isUnlocked) {
                    val shouldUnlock = when (badge.id) {
                        "first_meal" -> checkFirstMeal(userId)
                        "hydration_hero" -> checkHydrationHero(userId)
                        "perfect_week" -> checkPerfectWeek(userId)
                        "scanner_pro" -> checkScannerPro(userId)
                        "weight_warrior" -> checkWeightWarrior(userId)
                        "calorie_master" -> checkCalorieMaster(userId)
                        "streak_rookie" -> checkStreakRookie(userId)
                        "streak_master" -> checkStreakMaster(userId)
                        "water_champion" -> checkWaterChampion(userId)
                        "nutrition_expert" -> checkNutritionExpert(userId)
                        else -> false
                    }

                    if (shouldUnlock) {
                        rewardRepository.unlockBadge(userId, badge.id)
                        unlockedBadges.add(badge.name)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }

        return unlockedBadges
    }

    // Badge Check Functions

    private suspend fun checkFirstMeal(userId: String): Boolean {
        // Check if user has logged at least 1 meal ever
        val startDate = DateUtils.getDaysAgo(365) // Check last year
        val endDate = DateUtils.getTodayString()
        val meals = mealLogRepository.getMealsForDateRange(userId, startDate, endDate).first()
        return meals.isNotEmpty()
    }

    private suspend fun checkHydrationHero(userId: String): Boolean {
        // Check if user met water goal for 3 consecutive days
        var consecutiveDays = 0
        val waterGoal = 2500 // Could get from preferences

        for (i in 0..6) {
            val date = DateUtils.getDaysAgo(i)
            val dateObj = DateUtils.parseDate(date) ?: continue
            val waterConsumed = waterLogRepository.getTotalWaterForDay(userId, dateObj).first()

            if (waterConsumed >= waterGoal) {
                consecutiveDays++
                if (consecutiveDays >= 3) return true
            } else {
                consecutiveDays = 0
            }
        }
        return false
    }

    private suspend fun checkPerfectWeek(userId: String): Boolean {
        // Check if user has 7-day streak
        val streakDays = rewardRepository.getRewards(userId).first()?.streakDays ?: 0
        return streakDays >= 7
    }

    private suspend fun checkScannerPro(userId: String): Boolean {
        // Count meals logged via OCR
        val startDate = DateUtils.getDaysAgo(365)
        val endDate = DateUtils.getTodayString()
        val meals = mealLogRepository.getMealsForDateRange(userId, startDate, endDate).first()
        val ocrMealCount = meals.count { it.source == MealSource.OCR }
        return ocrMealCount >= 10
    }

    private suspend fun checkWeightWarrior(userId: String): Boolean {
        // Check 7 consecutive days of weight logging
        var consecutiveDays = 0

        for (i in 0..13) { // Check last 2 weeks
            val date = DateUtils.getDaysAgo(i)
            val log = dailyLogRepository.getLogForDate(userId, date).first()

            if (log?.weightKg != null) {
                consecutiveDays++
                if (consecutiveDays >= 7) return true
            } else {
                consecutiveDays = 0
            }
        }
        return false
    }

    private suspend fun checkCalorieMaster(userId: String): Boolean {
        // Check if stayed within calorie target for 5 days
        val profile = userProfileRepository.getUserProfile(userId).first() ?: return false
        val calorieTarget = profile.dailyCalorieTarget
        var daysWithinTarget = 0

        for (i in 0..13) { // Check last 2 weeks
            val date = DateUtils.getDaysAgo(i)
            val log = dailyLogRepository.getLogForDate(userId, date).first()

            if (log != null) {
                val calories = log.totalCalories
                // Within 10% of target is acceptable
                if (calories in (calorieTarget * 0.9).toInt()..(calorieTarget * 1.1).toInt()) {
                    daysWithinTarget++
                    if (daysWithinTarget >= 5) return true
                }
            }
        }
        return false
    }

    private suspend fun checkStreakRookie(userId: String): Boolean {
        // Check if user has 3-day streak
        val streakDays = rewardRepository.getRewards(userId).first()?.streakDays ?: 0
        return streakDays >= 3
    }

    private suspend fun checkStreakMaster(userId: String): Boolean {
        // Check if user has 30-day streak
        val streakDays = rewardRepository.getRewards(userId).first()?.streakDays ?: 0
        return streakDays >= 30
    }

    private suspend fun checkWaterChampion(userId: String): Boolean {
        // Check total water logged (10 liters = 10,000 ml)
        var totalWater = 0

        for (i in 0..365) { // Check last year
            val date = DateUtils.getDaysAgo(i)
            val dateObj = DateUtils.parseDate(date) ?: continue
            totalWater += waterLogRepository.getTotalWaterForDay(userId, dateObj).first()

            if (totalWater >= 10000) return true
        }
        return false
    }

    private suspend fun checkNutritionExpert(userId: String): Boolean {
        // Check if user has logged 50 meals total
        val startDate = DateUtils.getDaysAgo(365)
        val endDate = DateUtils.getTodayString()
        val meals = mealLogRepository.getMealsForDateRange(userId, startDate, endDate).first()
        return meals.size >= 50
    }
}