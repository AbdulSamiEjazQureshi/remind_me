package com.softrock.gesturesandwidgets.ui

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.softrock.gesturesandwidgets.AppConfig
import com.softrock.gesturesandwidgets.MainActivity
import com.softrock.gesturesandwidgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                val database = AppDatabase.getAppDatabase(context)
                val reminderScheduler = ReminderScheduler(context)

                CoroutineScope(Dispatchers.IO).launch {
                    val reminders =
                        database.reminderDao().getAllRemindersByStatus(isReminded = false).first()

                    reminders.forEach { reminder ->
                        reminderScheduler.schedule(reminder.id, reminder.timeInMillis)
                    }

                    if (reminders.count() > 0) {
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        val pendingIntent = PendingIntent.getActivity(
                            context, 0, Intent(
                                context,
                                MainActivity::class.java
                            ), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        val notification = NotificationCompat.Builder(context, AppConfig.REMIND_ME_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("Reminders Rescheduled")
                            .setContentText("Your ${reminders.count()} reminders have been scheduled successfully")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)

                        notificationManager.notify(54, notification.build())
                    }
                }
            }
        }
    }
}