package com.example.aharui.util

import android.content.Context
import androidx.work.*
import com.example.aharui.worker.MealReminderWorker
import com.example.aharui.worker.WaterReminderWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {

    fun scheduleWaterReminders(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            2, TimeUnit.HOURS
        )
            .setInitialDelay(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "water_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleMealReminders(context: Context) {
        // Breakfast reminder (8 AM)
        scheduleOneMealReminder(context, "Breakfast", 8, 0)

        // Lunch reminder (1 PM)
        scheduleOneMealReminder(context, "Lunch", 13, 0)

        // Dinner reminder (7 PM)
        scheduleOneMealReminder(context, "Dinner", 19, 0)
    }

    private fun scheduleOneMealReminder(
        context: Context,
        mealType: String,
        hour: Int,
        minute: Int
    ) {
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)

            if (timeInMillis < currentTime) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = calendar.timeInMillis - currentTime

        val data = workDataOf(MealReminderWorker.KEY_MEAL_TYPE to mealType)

        val workRequest = PeriodicWorkRequestBuilder<MealReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "meal_reminder_$mealType",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("water_reminder")
        WorkManager.getInstance(context).cancelUniqueWork("meal_reminder_Breakfast")
        WorkManager.getInstance(context).cancelUniqueWork("meal_reminder_Lunch")
        WorkManager.getInstance(context).cancelUniqueWork("meal_reminder_Dinner")
    }
}