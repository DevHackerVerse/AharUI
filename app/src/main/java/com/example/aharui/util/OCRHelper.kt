package com.example.aharui.util

import android.graphics.Bitmap
import com.example.aharui.data.api.EnhancedNutritionInfo
import com.example.aharui.data.api.GeminiService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OCRHelper(private val geminiService: GeminiService) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(bitmap: Bitmap): Result<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val text = result.text
            Result.Success(text)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to extract text")
        }
    }

    /**
     * Enhanced parsing using Gemini AI for better accuracy
     */
    suspend fun parseNutritionInfoWithAI(text: String): Result<EnhancedNutritionInfo> {
        return geminiService.extractNutritionFromOCR(text)
    }

    /**
     * Legacy parsing method (fallback)
     */
    fun parseNutritionInfo(text: String): NutritionInfo {
        val caloriesRegex = """(\d+)\s*(kcal|cal|calories)""".toRegex(RegexOption.IGNORE_CASE)
        val proteinRegex = """protein[:\s]*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val carbsRegex = """carb[s|ohydrate]*[:\s]*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val fatRegex = """fat[s]*[:\s]*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)

        val calories = caloriesRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val protein = proteinRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val carbs = carbsRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val fat = fatRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        return NutritionInfo(calories, protein, carbs, fat)
    }

    data class NutritionInfo(
        val calories: Int,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double
    )
}