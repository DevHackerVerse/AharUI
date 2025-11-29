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

class MealReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MealReminderWorkerEntryPoint {
        fun preferencesManager(): PreferencesManager
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            MealReminderWorkerEntryPoint::class.java
        )

        val preferencesManager = hiltEntryPoint.preferencesManager()
        val mealType = inputData.getString(KEY_MEAL_TYPE) ?: "Meal"

        return try {
            if (preferencesManager.mealReminderEnabled) {
                NotificationHelper.sendMealReminder(appContext, mealType)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_MEAL_TYPE = "meal_type"
    }
}