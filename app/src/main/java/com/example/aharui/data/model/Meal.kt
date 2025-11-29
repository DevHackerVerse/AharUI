package com.example.aharui.data.model

data class Meal(
    val id: Long = 0,
    val name: String,
    val calories: Int,
    val mealType: MealType,
    val source: MealSource,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val quantity: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}

enum class MealSource {
    MANUAL, AI_PLAN, OCR
}