package com.example.aharui.util

import com.example.aharui.data.model.ActivityLevel
import com.example.aharui.data.model.Gender
import com.example.aharui.data.model.GoalType

object CalorieCalculator {

    /**
     * Calculate BMR (Basal Metabolic Rate) using Mifflin-St Jeor Equation
     */
    fun calculateBMR(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender
    ): Double {
        return when (gender) {
            Gender.MALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
            Gender.FEMALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
            Gender.OTHER -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 80 // Average
        }
    }

    /**
     * Calculate TDEE (Total Daily Energy Expenditure)
     */
    fun calculateTDEE(bmr: Double, activityLevel: ActivityLevel): Double {
        val multiplier = when (activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHTLY_ACTIVE -> 1.375
            ActivityLevel.MODERATELY_ACTIVE -> 1.55
            ActivityLevel.VERY_ACTIVE -> 1.725
            ActivityLevel.EXTREMELY_ACTIVE -> 1.9
        }
        return bmr * multiplier
    }

    /**
     * Calculate daily calorie target based on goal
     */
    fun calculateDailyCalorieTarget(
        tdee: Double,
        goalType: GoalType,
        targetWeightKg: Double,
        currentWeightKg: Double
    ): Int {
        return when (goalType) {
            GoalType.WEIGHT_LOSS -> {
                // Create 500-750 calorie deficit for 0.5-1 kg loss per week
                val deficit = if (currentWeightKg - targetWeightKg > 10) 750 else 500
                (tdee - deficit).toInt()
            }
            GoalType.WEIGHT_GAIN -> {
                // Create 300-500 calorie surplus
                (tdee + 400).toInt()
            }
            GoalType.MUSCLE_GAIN -> {
                // Slight surplus with high protein
                (tdee + 300).toInt()
            }
            GoalType.GENERAL_HEALTH -> {
                // Maintain current weight
                tdee.toInt()
            }
        }
    }

    /**
     * Calculate recommended macros (protein, carbs, fats)
     */
    fun calculateMacros(
        calorieTarget: Int,
        goalType: GoalType,
        weightKg: Double
    ): Triple<Int, Int, Int> {
        return when (goalType) {
            GoalType.WEIGHT_LOSS -> {
                val proteinG = (weightKg * 2.0).toInt() // High protein
                val fatG = (calorieTarget * 0.25 / 9).toInt()
                val carbsG = ((calorieTarget - (proteinG * 4) - (fatG * 9)) / 4).toInt()
                Triple(proteinG, carbsG, fatG)
            }
            GoalType.MUSCLE_GAIN -> {
                val proteinG = (weightKg * 2.2).toInt() // Very high protein
                val fatG = (calorieTarget * 0.25 / 9).toInt()
                val carbsG = ((calorieTarget - (proteinG * 4) - (fatG * 9)) / 4).toInt()
                Triple(proteinG, carbsG, fatG)
            }
            else -> {
                val proteinG = (weightKg * 1.6).toInt()
                val fatG = (calorieTarget * 0.30 / 9).toInt()
                val carbsG = ((calorieTarget - (proteinG * 4) - (fatG * 9)) / 4).toInt()
                Triple(proteinG, carbsG, fatG)
            }
        }
    }
}