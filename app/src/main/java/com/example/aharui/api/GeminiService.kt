package com.example.aharui.data.api

import android.util.Log
import com.example.aharui.data.model.*
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {

    // ✅ Get API key from BuildConfig (which reads from local.properties)
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 8192
        }
    )

    /**
     * Generate a 7-day personalized meal plan based on user profile
     */
    suspend fun generateWeeklyMealPlan(userProfile: UserProfile): com.example.aharui.util.Result<WeeklyMealPlan> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildMealPlanPrompt(userProfile)
                Log.d("GeminiService", "Sending meal plan request with gemini-2.5-flash...")

                val response = model.generateContent(prompt)
                val responseText = response.text ?: ""

                if (responseText.isEmpty()) {
                    Log.e("GeminiService", "Empty response received from API")
                    return@withContext com.example.aharui.util.Result.Error(
                        "Empty response from API. Please try again."
                    )
                }

                Log.d("GeminiService", "Raw API Response: $responseText")

                val jsonText = extractJsonFromResponse(responseText)
                Log.d("GeminiService", "Extracted JSON: $jsonText")

                val mealPlan = parseWeeklyMealPlan(jsonText)

                // ✅ LOG THE PARSED MEAL PLAN
                Log.d("GeminiService", "Parsed Meal Plan: $mealPlan")
                Log.d("GeminiService", "Total Days: ${mealPlan.days.size}")

                mealPlan.days.forEachIndexed { index, day ->
                    Log.d("GeminiService", "Day ${day.dayNumber}:")
                    day.meals.forEach { meal ->
                        Log.d("GeminiService",
                            "  - ${meal.mealType}: ${meal.name} | " +
                                    "Cals: ${meal.calories} | " +
                                    "P: ${meal.proteinG}g C: ${meal.carbsG}g F: ${meal.fatG}g"
                        )
                    }
                }

                com.example.aharui.util.Result.Success(mealPlan)
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating meal plan: ${e.message}", e)
                Log.e("GeminiService", "Stack trace: ${e.stackTraceToString()}")

                val errorMsg = when {
                    e.message?.contains("MAX_TOKENS", ignoreCase = true) == true ->
                        "Response too long. Retrying with shorter format..."
                    e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                        "API quota exceeded. Please try again in a few minutes."
                    e.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
                        "Service temporarily unavailable. Please try again."
                    else -> "Failed to generate meal plan: ${e.message}"
                }

                com.example.aharui.util.Result.Error(errorMsg)
            }
        }
    }

    /**
     * Generate shopping list from meal plan
     */
    suspend fun generateShoppingListFromMealPlan(
        mealPlan: WeeklyMealPlan,
        userProfile: UserProfile
    ): com.example.aharui.util.Result<List<ShoppingItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildShoppingListPrompt(mealPlan)
                Log.d("GeminiService", "Generating shopping list...")

                val response = model.generateContent(prompt)
                val responseText = response.text ?: ""

                val jsonText = extractJsonFromResponse(responseText)
                val shoppingList = parseShoppingList(jsonText)

                com.example.aharui.util.Result.Success(shoppingList)
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating shopping list", e)
                com.example.aharui.util.Result.Error("Failed to generate shopping list: ${e.message}")
            }
        }
    }

    /**
     * Enhanced OCR - Extract nutrition info from food label image text using Gemini
     */
    suspend fun extractNutritionFromOCR(ocrText: String): com.example.aharui.util.Result<EnhancedNutritionInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    You are a nutrition expert. Analyze this text extracted from a food label and extract accurate nutrition information.
                    
                    Text from food label:
                    $ocrText
                    
                    Extract and return ONLY a JSON object with this EXACT format (no markdown, no extra text):
                    {
                      "foodName": "name of the food product",
                      "calories": 0,
                      "protein": 0.0,
                      "carbs": 0.0,
                      "fat": 0.0,
                      "servingSize": "serving size description",
                      "confidence": "high"
                    }
                    
                    Rules:
                    - foodName should be the product name from the label
                    - If a value is not clearly visible, use 0
                    - All macros (protein, carbs, fat) should be in grams
                    - Calories should be kcal (kilocalories)
                    - confidence should be "high", "medium", or "low" based on text clarity
                    - Return ONLY valid JSON, no extra explanation
                """.trimIndent()

                Log.d("GeminiService", "Extracting nutrition from OCR...")
                val response = model.generateContent(prompt)
                val responseText = response.text ?: ""

                val jsonText = extractJsonFromResponse(responseText)
                val nutritionInfo = parseEnhancedNutritionInfo(jsonText)

                com.example.aharui.util.Result.Success(nutritionInfo)
            } catch (e: Exception) {
                Log.e("GeminiService", "Error extracting nutrition info", e)
                com.example.aharui.util.Result.Error("Failed to extract nutrition info: ${e.message}")
            }
        }
    }

    // ============= PRIVATE HELPER METHODS =============

    private fun buildMealPlanPrompt(userProfile: UserProfile): String {
        val age = userProfile.dateOfBirth?.let {
            val ageMillis = System.currentTimeMillis() - it
            (ageMillis / (1000L * 60 * 60 * 24 * 365)).toInt()
        } ?: 30

        val bmr = calculateBMR(
            userProfile.gender ?: Gender.OTHER,
            userProfile.currentWeightKg ?: 70.0,
            userProfile.heightCm ?: 170.0,
            age
        )

        val tdee = calculateTDEE(bmr, userProfile.activityLevel)
        val targetCalories = adjustCaloriesForGoal(tdee, userProfile.goalType)

        return """
            You are a professional nutritionist and meal planner. Create a detailed 7-day meal plan.
            
            User Profile:
            - Gender: ${userProfile.gender}
            - Age: $age years
            - Height: ${userProfile.heightCm} cm
            - Current Weight: ${userProfile.currentWeightKg} kg
            - Target Weight: ${userProfile.targetWeightKg} kg
            - Goal: ${userProfile.goalType}
            - Activity Level: ${userProfile.activityLevel}
            - Daily Calorie Target: $targetCalories kcal
            
            Requirements:
            - Create exactly 7 days of meals (Day 1 to Day 7)
            - Each day MUST have: Breakfast, Lunch, Dinner, and Snack
            - Total daily calories should be close to $targetCalories kcal (±100 kcal)
            - Meals should be realistic, easy to prepare, healthy, and diverse
            - Include accurate macronutrients (protein, carbs, fat) for each meal
            - For ${userProfile.goalType}: adjust protein/carb ratios accordingly
            - Include ingredient lists for shopping
            
            Return ONLY a valid JSON object (no markdown, no extra text) with this EXACT structure:
            {
              "weeklyPlan": [
                {
                  "day": 1,
                  "meals": [
                    {
                      "mealType": "BREAKFAST",
                      "name": "Oatmeal with Berries and Almonds",
                      "calories": 350,
                      "protein": 12.0,
                      "carbs": 55.0,
                      "fat": 10.0,
                      "ingredients": ["1 cup oats", "1/2 cup blueberries", "10 almonds", "1 tsp honey"]
                    },
                    {
                      "mealType": "LUNCH",
                      "name": "Grilled Chicken Salad",
                      "calories": 450,
                      "protein": 35.0,
                      "carbs": 30.0,
                      "fat": 18.0,
                      "ingredients": ["150g chicken breast", "2 cups mixed greens", "1/2 avocado", "olive oil dressing"]
                    },
                    {
                      "mealType": "DINNER",
                      "name": "Salmon with Quinoa and Broccoli",
                      "calories": 500,
                      "protein": 38.0,
                      "carbs": 45.0,
                      "fat": 20.0,
                      "ingredients": ["150g salmon fillet", "1 cup cooked quinoa", "1 cup steamed broccoli", "lemon"]
                    },
                    {
                      "mealType": "SNACK",
                      "name": "Greek Yogurt with Honey",
                      "calories": 150,
                      "protein": 15.0,
                      "carbs": 18.0,
                      "fat": 3.0,
                      "ingredients": ["200g Greek yogurt", "1 tbsp honey"]
                    }
                  ]
                }
              ]
            }
            
            Generate all 7 days following this exact format.
        """.trimIndent()
    }

    private fun buildShoppingListPrompt(mealPlan: WeeklyMealPlan): String {
        val allIngredients = mutableListOf<String>()
        mealPlan.days.forEach { day ->
            day.meals.forEach { meal ->
                meal.quantity?.split(",")?.forEach { ingredient ->
                    allIngredients.add(ingredient.trim())
                }
            }
        }

        return """
            Based on these ingredients from a 7-day meal plan, create a consolidated shopping list.
            
            All ingredients:
            ${allIngredients.joinToString("\n")}
            
            Requirements:
            - Combine duplicate ingredients and sum quantities intelligently
            - Organize by category: Proteins, Vegetables, Fruits, Grains, Dairy, Pantry, Other
            - Use practical quantities (e.g., "2 lbs chicken breast", "1 dozen eggs")
            - Round up to purchase-friendly amounts
            
            Return ONLY a valid JSON object (no markdown) with this structure:
            {
              "items": [
                {
                  "name": "Chicken Breast",
                  "quantity": "2 lbs",
                  "category": "Proteins"
                },
                {
                  "name": "Greek Yogurt",
                  "quantity": "3 containers (200g each)",
                  "category": "Dairy"
                },
                {
                  "name": "Oats",
                  "quantity": "1 large container",
                  "category": "Grains"
                }
              ]
            }
        """.trimIndent()
    }

    private fun calculateBMR(gender: Gender, weightKg: Double, heightCm: Double, age: Int): Double {
        return when (gender) {
            Gender.MALE -> 10 * weightKg + 6.25 * heightCm - 5 * age + 5
            Gender.FEMALE -> 10 * weightKg + 6.25 * heightCm - 5 * age - 161
            Gender.OTHER -> 10 * weightKg + 6.25 * heightCm - 5 * age - 78
        }
    }

    private fun calculateTDEE(bmr: Double, activityLevel: ActivityLevel): Double {
        val multiplier = when (activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHTLY_ACTIVE -> 1.375
            ActivityLevel.MODERATELY_ACTIVE -> 1.55
            ActivityLevel.VERY_ACTIVE -> 1.725
            ActivityLevel.EXTREMELY_ACTIVE -> 1.9
        }
        return bmr * multiplier
    }

    private fun adjustCaloriesForGoal(tdee: Double, goalType: GoalType): Int {
        return when (goalType) {
            GoalType.WEIGHT_LOSS -> (tdee - 500).toInt()
            GoalType.WEIGHT_GAIN -> (tdee + 300).toInt()
            GoalType.MUSCLE_GAIN -> (tdee + 250).toInt()
            GoalType.GENERAL_HEALTH -> tdee.toInt()
        }
    }

    private fun extractJsonFromResponse(text: String): String {
        var cleaned = text
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')

        return if (startIndex != -1 && endIndex != -1) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            cleaned
        }
    }

    private fun parseWeeklyMealPlan(jsonText: String): WeeklyMealPlan {
        val json = JSONObject(jsonText)
        val weeklyPlanArray = json.getJSONArray("weeklyPlan")

        val days = mutableListOf<DailyMealPlan>()

        for (i in 0 until weeklyPlanArray.length()) {
            val dayJson = weeklyPlanArray.getJSONObject(i)
            val dayNumber = dayJson.getInt("day")
            val mealsArray = dayJson.getJSONArray("meals")

            val meals = mutableListOf<Meal>()
            for (j in 0 until mealsArray.length()) {
                val mealJson = mealsArray.getJSONObject(j)

                val ingredientsArray = mealJson.optJSONArray("ingredients")
                val ingredientsList = mutableListOf<String>()
                if (ingredientsArray != null) {
                    for (k in 0 until ingredientsArray.length()) {
                        ingredientsList.add(ingredientsArray.getString(k))
                    }
                }

                val meal = Meal(
                    name = mealJson.getString("name"),
                    calories = mealJson.getInt("calories"),
                    mealType = MealType.valueOf(mealJson.getString("mealType")),
                    source = MealSource.AI_PLAN,
                    proteinG = mealJson.getDouble("protein"),
                    carbsG = mealJson.getDouble("carbs"),
                    fatG = mealJson.getDouble("fat"),
                    quantity = ingredientsList.joinToString(", ")
                )
                meals.add(meal)
            }

            days.add(DailyMealPlan(dayNumber, meals))
        }

        return WeeklyMealPlan(days)
    }

    private fun parseShoppingList(jsonText: String): List<ShoppingItem> {
        val json = JSONObject(jsonText)
        val itemsArray = json.getJSONArray("items")

        val items = mutableListOf<ShoppingItem>()
        for (i in 0 until itemsArray.length()) {
            val itemJson = itemsArray.getJSONObject(i)
            items.add(
                ShoppingItem(
                    name = itemJson.getString("name"),
                    quantity = itemJson.getString("quantity"),
                    isChecked = false
                )
            )
        }

        return items
    }

    private fun parseEnhancedNutritionInfo(jsonText: String): EnhancedNutritionInfo {
        val json = JSONObject(jsonText)
        return EnhancedNutritionInfo(
            foodName = json.optString("foodName", "Unknown Food"),
            calories = json.optInt("calories", 0),
            proteinG = json.optDouble("protein", 0.0),
            carbsG = json.optDouble("carbs", 0.0),
            fatG = json.optDouble("fat", 0.0),
            servingSize = json.optString("servingSize", "1 serving"),
            confidence = json.optString("confidence", "medium")
        )
    }
}

// Data classes for meal plan
data class WeeklyMealPlan(
    val days: List<DailyMealPlan>
)

data class DailyMealPlan(
    val dayNumber: Int,
    val meals: List<Meal>
)

data class EnhancedNutritionInfo(
    val foodName: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val servingSize: String,
    val confidence: String
)