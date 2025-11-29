package com.example.aharui.domain.usecase

import com.example.aharui.data.model.UserProfile
import com.example.aharui.data.repository.UserProfileRepository
import com.example.aharui.util.CalorieCalculator
import com.example.aharui.util.Result
import java.util.*
import javax.inject.Inject

class CalculateCalorieTargetUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(profile: UserProfile): Result<Int> {
        return try {
            val age = profile.dateOfBirth?.let { dob ->
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.timeInMillis = dob
                currentYear - calendar.get(Calendar.YEAR)
            } ?: 30

            val weightKg = profile.currentWeightKg ?: return Result.Error("Weight is required")
            val heightCm = profile.heightCm ?: return Result.Error("Height is required")
            val gender = profile.gender ?: return Result.Error("Gender is required")
            val targetWeightKg = profile.targetWeightKg ?: weightKg

            val bmr = CalorieCalculator.calculateBMR(weightKg, heightCm, age, gender)
            val tdee = CalorieCalculator.calculateTDEE(bmr, profile.activityLevel)
            val calorieTarget = CalorieCalculator.calculateDailyCalorieTarget(
                tdee,
                profile.goalType,
                targetWeightKg,
                weightKg
            )

            // Update profile with new target
            val updatedProfile = profile.copy(dailyCalorieTarget = calorieTarget)
            userProfileRepository.saveUserProfile(updatedProfile)

            Result.Success(calorieTarget)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to calculate calorie target")
        }
    }
}