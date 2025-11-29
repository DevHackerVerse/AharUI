package com.example.aharui.data.model

data class UserProfile(
    val userId: String,
    val name: String,
    val email: String?,
    val gender: Gender?,
    val dateOfBirth: Long?,
    val heightCm: Double?,
    val currentWeightKg: Double?,
    val targetWeightKg: Double?,
    val goalType: GoalType,
    val dailyCalorieTarget: Int,
    val dailyWaterTarget: Int = 2500,
    val activityLevel: ActivityLevel
)

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class GoalType {
    WEIGHT_LOSS, WEIGHT_GAIN, MUSCLE_GAIN, GENERAL_HEALTH
}

enum class ActivityLevel {
    SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTREMELY_ACTIVE
}