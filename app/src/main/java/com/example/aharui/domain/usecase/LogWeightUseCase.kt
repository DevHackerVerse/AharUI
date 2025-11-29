package com.example.aharui.domain.usecase

import com.example.aharui.data.repository.DailyLogRepository
import com.example.aharui.data.repository.RewardRepository
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import javax.inject.Inject

class LogWeightUseCase @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val rewardRepository: RewardRepository
) {
    suspend operator fun invoke(userId: String, weightKg: Double, date: String = DateUtils.getTodayString()): Result<Unit> {
        return try {
            // Update weight in daily log
            dailyLogRepository.updateWeight(userId, date, weightKg)

            // Award 15 points for logging weight
            rewardRepository.addPoints(userId, 15)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to log weight")
        }
    }
}