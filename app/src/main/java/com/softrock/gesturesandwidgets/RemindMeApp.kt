package com.softrock.gesturesandwidgets

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log

class RemindMeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val channelId = "remind_me_alarm"
        val channelName = "remind_me"
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
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