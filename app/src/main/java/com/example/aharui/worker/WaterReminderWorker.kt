package com.example.aharui.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aharui.data.preferences.PreferencesManager
import com.example.aharui.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class WaterReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WaterReminderWorkerEntryPoint {
        fun preferencesManager(): PreferencesManager
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WaterReminderWorkerEntryPoint::class.java
        )

        val preferencesManager = hiltEntryPoint.preferencesManager()

        return try {
            if (preferencesManager.waterReminderEnabled) {
                NotificationHelper.sendWaterReminder(appContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}