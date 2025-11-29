package com.example.aharui.domain.usecase

import com.example.aharui.data.repository.DailyLogRepository
import com.example.aharui.data.repository.RewardRepository
import com.example.aharui.data.repository.WaterLogRepository
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import com.example.aharui.util.isError
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

class LogWaterUseCase @Inject constructor(
    private val waterLogRepository: WaterLogRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val rewardRepository: RewardRepository
) {
    suspend operator fun invoke(userId: String, amountMl: Int): Result<Unit> {
        // 1. Log water intake
        val logResult = waterLogRepository.logWater(userId, amountMl)
        if (logResult.isError()) {
            return logResult as Result<Unit>
        }

        // 2. Update daily log
        val today = DateUtils.getTodayString()
        val totalWater = waterLogRepository.getTotalWaterForDay(userId, Date()).first()
        dailyLogRepository.updateWater(userId, today, totalWater)

        // 3. Award points
        val points = (amountMl / 250) * 10
        rewardRepository.addPoints(userId, points)

        return Result.Success(Unit)
    }
}