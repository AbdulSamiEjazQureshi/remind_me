package com.softrock.gesturesandwidgets

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: return
        val channelId = "remind_me_alarm"
        context?.let { ctx ->
            val notificationManager =
                ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = NotificationCompat.Builder(
                ctx,
                channelId
            )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Remind Me")
                .setContentText("Notification sent with message $message")
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            Log.d("REMIND_ME", "Notification built with message $message")


            try {
                notificationManager.notify(1, builder.build())
                Log.d("REMIND_ME", "Notification Sent")
            } catch (e: Exception) {
                Log.d("REMIND_ME", e.message.toString())
            }
        }
    }
}