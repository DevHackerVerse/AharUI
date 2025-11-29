package com.example.aharui.data.repository

import com.example.aharui.data.local.dao.DailyLogDao
import com.example.aharui.data.local.entity.DailyLogEntity
import com.example.aharui.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao
) {

    fun getLogForDate(userId: String, date: String): Flow<DailyLogEntity?> {
        return dailyLogDao.getLogForDate(userId, date)
    }

    fun getLogsForDateRange(userId: String, startDate: String, endDate: String): Flow<List<DailyLogEntity>> {
        return dailyLogDao.getLogsForDateRange(userId, startDate, endDate)
    }

    fun getRecentLogs(userId: String, limit: Int = 30): Flow<List<DailyLogEntity>> {
        return dailyLogDao.getRecentLogs(userId, limit)
    }

    suspend fun createOrUpdateLog(log: DailyLogEntity): Result<Unit> {
        return try {
            dailyLogDao.insertOrUpdateLog(log)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update daily log")
        }
    }

    suspend fun updateCalories(userId: String, date: String, calories: Int): Result<Unit> {
        return try {
            dailyLogDao.updateCalories(userId, date, calories)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update calories")
        }
    }

    suspend fun updateWater(userId: String, date: String, waterMl: Int): Result<Unit> {
        return try {
            dailyLogDao.updateWater(userId, date, waterMl)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update water")
        }
    }

    suspend fun updateWeight(userId: String, date: String, weightKg: Double): Result<Unit> {
        return try {
            // Use the new upsert method instead of plain update
            dailyLogDao.upsertWeight(userId, date, weightKg)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update weight")
        }
    }

    suspend fun ensureTodayLogExists(userId: String, date: String): Result<Unit> {
        return try {
            val log = DailyLogEntity(
                userId = userId,
                date = date,
                totalCalories = 0,
                waterMl = 0
            )
            dailyLogDao.insertOrUpdateLog(log)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create daily log")
        }
    }
}