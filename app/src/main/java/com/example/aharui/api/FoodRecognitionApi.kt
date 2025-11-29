package com.example.aharui.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FoodRecognitionApi {

    @Multipart
    @POST("recognize")
    suspend fun recognizeFood(
        @Part image: MultipartBody.Part
    ): FoodRecognitionResponse
}

data class FoodRecognitionResponse(
    val foodName: String,
    val confidence: Double,
    val nutrition: NutritionData?
)

data class NutritionData(
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val servingSize: String
)