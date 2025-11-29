package com.example.aharui.data.local.dao

import androidx.room.*
import com.example.aharui.data.local.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLogEntity): Long

    @Query("SELECT * FROM water_logs WHERE userId = :userId AND timestamp >= :startTimestamp AND timestamp < :endTimestamp ORDER BY timestamp DESC")
    fun getLogsForDay(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE userId = :userId AND timestamp >= :startTimestamp AND timestamp < :endTimestamp")
    fun getTotalWaterForDay(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM water_logs WHERE userId = :userId AND timestamp >= :startTimestamp AND timestamp < :endTimestamp")
    suspend fun getLogCountForDay(userId: String, startTimestamp: Long, endTimestamp: Long): Int

    @Delete
    suspend fun deleteLog(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}