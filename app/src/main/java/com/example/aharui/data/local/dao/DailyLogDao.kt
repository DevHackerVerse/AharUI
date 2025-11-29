package com.example.aharui.data.local.dao

import androidx.room.*
import com.example.aharui.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: DailyLogEntity)

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date = :date")
    fun getLogForDate(userId: String, date: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getLogsForDateRange(userId: String, startDate: String, endDate: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentLogs(userId: String, limit: Int): Flow<List<DailyLogEntity>>

    @Query("UPDATE daily_logs SET totalCalories = :calories WHERE userId = :userId AND date = :date")
    suspend fun updateCalories(userId: String, date: String, calories: Int)

    @Query("UPDATE daily_logs SET waterMl = :waterMl WHERE userId = :userId AND date = :date")
    suspend fun updateWater(userId: String, date: String, waterMl: Int)

    @Query("UPDATE daily_logs SET weightKg = :weightKg WHERE userId = :userId AND date = :date")
    suspend fun updateWeight(userId: String, date: String, weightKg: Double)

    @Query("DELETE FROM daily_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // NEW: Get a single log synchronously (not Flow)
    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getLogForDateSync(userId: String, date: String): DailyLogEntity?

    // NEW: Transaction method to upsert weight
    @Transaction
    suspend fun upsertWeight(userId: String, date: String, weightKg: Double) {
        val existingLog = getLogForDateSync(userId, date)
        if (existingLog != null) {
            // Update existing log
            updateWeight(userId, date, weightKg)
        } else {
            // Insert new log with weight
            insertOrUpdateLog(
                DailyLogEntity(
                    userId = userId,
                    date = date,
                    weightKg = weightKg,
                    totalCalories = 0,
                    waterMl = 0
                )
            )
        }
    }
}