package com.softrock.gesturesandwidgets

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log

object AppConfig {
    const val REMIND_ME_CHANNEL_ID = "remind_me_alarm"
}

class RemindMeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val channelName = "remind_me"
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConfig.REMIND_ME_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Remind Me Channel Description"
            }

            notificationManager.createNotificationChannel(channel)

            Log.d("REMIND_ME", "Channel Created with id: $channel")
        }
    }
}