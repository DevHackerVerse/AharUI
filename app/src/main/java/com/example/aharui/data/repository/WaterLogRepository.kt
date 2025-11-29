package com.example.aharui.data.repository

import com.example.aharui.data.local.dao.WaterLogDao
import com.example.aharui.data.local.entity.WaterLogEntity
import com.example.aharui.data.model.WaterLog
import com.example.aharui.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterLogRepository @Inject constructor(
    private val waterLogDao: WaterLogDao
) {

    fun getWaterLogsForDay(userId: String, date: Date): Flow<List<WaterLog>> {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTimestamp = calendar.timeInMillis
        val endTimestamp = startTimestamp + 24 * 60 * 60 * 1000

        return waterLogDao.getLogsForDay(userId, startTimestamp, endTimestamp).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getTotalWaterForDay(userId: String, date: Date): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTimestamp = calendar.timeInMillis
        val endTimestamp = startTimestamp + 24 * 60 * 60 * 1000

        return waterLogDao.getTotalWaterForDay(userId, startTimestamp, endTimestamp).map { it ?: 0 }
    }

    suspend fun logWater(userId: String, amountMl: Int): Result<Long> {
        return try {
            val entity = WaterLogEntity(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                amountMl = amountMl
            )
            val id = waterLogDao.insertLog(entity)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to log water")
        }
    }

    suspend fun deleteWaterLog(log: WaterLog): Result<Unit> {
        return try {
            val entity = WaterLogEntity(
                id = log.id,
                userId = "",
                timestamp = log.timestamp,
                amountMl = log.amountMl
            )
            waterLogDao.deleteLog(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete water log")
        }
    }

    private fun WaterLogEntity.toModel() = WaterLog(
        id = id,
        amountMl = amountMl,
        timestamp = timestamp
    )
}