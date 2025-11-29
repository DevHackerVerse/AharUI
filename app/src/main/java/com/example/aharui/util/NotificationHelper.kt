package com.example.aharui.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.aharui.MainActivity
import com.example.aharui.NeuroDietApplication
import com.example.aharui.R

object NotificationHelper {

    fun sendWaterReminder(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NeuroDietApplication.WATER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle("Time to Hydrate!")
            .setContentText("Don't forget to drink water 💧")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(WATER_NOTIFICATION_ID, notification)
    }

    fun sendMealReminder(context: Context, mealType: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NeuroDietApplication.MEAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_meal)
            .setContentTitle("Time for $mealType!")
            .setContentText("Log your meal to track your progress 🍽️")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(MEAL_NOTIFICATION_ID, notification)
    }

    fun sendStreakReminder(context: Context, streakDays: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NeuroDietApplication.MEAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rewards)
            .setContentTitle("Keep Your Streak Going! 🔥")
            .setContentText("You're on a $streakDays day streak! Log a meal today to continue.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(STREAK_NOTIFICATION_ID, notification)
    }

    private const val WATER_NOTIFICATION_ID = 1001
    private const val MEAL_NOTIFICATION_ID = 1002
    private const val STREAK_NOTIFICATION_ID = 1003
}