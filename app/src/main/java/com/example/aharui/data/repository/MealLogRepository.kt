package com.example.aharui.data.repository

import com.example.aharui.data.local.dao.MealLogDao
import com.example.aharui.data.local.entity.MealLogEntity
import com.example.aharui.data.model.Meal
import com.example.aharui.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealLogRepository @Inject constructor(
    private val mealLogDao: MealLogDao
) {

    fun getMealsForDate(userId: String, date: String): Flow<List<Meal>> {
        return mealLogDao.getLogsForDate(userId, date).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getMealsForDateRange(userId: String, startDate: String, endDate: String): Flow<List<Meal>> {
        return mealLogDao.getLogsForDateRange(userId, startDate, endDate).map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addMeal(userId: String, meal: Meal, date: String): Result<Long> {
        return try {
            val id = mealLogDao.insertLog(meal.toEntity(userId, date))
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to add meal")
        }
    }

    suspend fun getTotalCaloriesForDate(userId: String, date: String): Int {
        return mealLogDao.getTotalCaloriesForDate(userId, date) ?: 0
    }

    suspend fun getMealCountForDate(userId: String, date: String): Int {
        return mealLogDao.getMealCountForDate(userId, date)
    }

    suspend fun deleteMeal(userId: String, mealId: Long, date: String): Result<Unit> {
        return try {
            val entity = MealLogEntity(
                id = mealId,
                userId = userId,
                date = date,
                mealType = "",
                foodName = "",
                source = "",
                calories = 0
            )
            mealLogDao.deleteMeal(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete meal")
        }
    }

    private fun MealLogEntity.toModel() = Meal(
        id = id,
        name = foodName,
        calories = calories,
        mealType = com.example.aharui.data.model.MealType.valueOf(mealType.uppercase()),
        source = com.example.aharui.data.model.MealSource.valueOf(source.uppercase()),
        proteinG = proteinG ?: 0.0,
        carbsG = carbsG ?: 0.0,
        fatG = fatG ?: 0.0,
        quantity = quantity
    )

    private fun Meal.toEntity(userId: String, date: String) = MealLogEntity(
        id = id,
        userId = userId,
        date = date,
        mealType = mealType.name.lowercase(),
        foodName = name,
        source = source.name.lowercase(),
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        quantity = quantity
    )
}