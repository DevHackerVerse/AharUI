package com.example.aharui.data.local.dao

import androidx.room.*
import com.example.aharui.data.local.entity.MealLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MealLogEntity): Long

    @Query("SELECT * FROM meal_logs WHERE userId = :userId AND date = :date ORDER BY id DESC")
    fun getLogsForDate(userId: String, date: String): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getLogsForDateRange(userId: String, startDate: String, endDate: String): Flow<List<MealLogEntity>>

    @Query("SELECT SUM(calories) FROM meal_logs WHERE userId = :userId AND date = :date")
    suspend fun getTotalCaloriesForDate(userId: String, date: String): Int?

    @Query("SELECT COUNT(*) FROM meal_logs WHERE userId = :userId AND date = :date")
    suspend fun getMealCountForDate(userId: String, date: String): Int

    @Delete
    suspend fun deleteMeal(meal: MealLogEntity)

    @Query("DELETE FROM meal_logs WHERE userId = :userId AND date = :date")
    suspend fun deleteAllForDate(userId: String, date: String)

    @Query("DELETE FROM meal_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}