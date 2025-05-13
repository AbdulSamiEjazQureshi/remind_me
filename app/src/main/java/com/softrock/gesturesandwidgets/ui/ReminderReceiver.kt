package com.softrock.gesturesandwidgets.ui

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.softrock.gesturesandwidgets.AppConfig
import com.softrock.gesturesandwidgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val id = intent.getIntExtra("REMINDER_ID", 0)
            if (id != 0) {
                val database = AppDatabase.getAppDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val entity = database.reminderDao().getReminder(id);

                    if (entity != null) {
                        val intent = Intent(context, AudioNoteForegroundService::class.java).apply {
                            action = "ACTION_START"
                            putExtra("TITLE", entity.title)
                            putExtra("FILE_PATH", entity.audioUri)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        database.reminderDao().updateReminderStatus(entity.id, true)
                        // showNotification(context, entity)
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, reminder: ReminderEntity) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(
            context,
            AppConfig.REMIND_ME_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(reminder.id, notification.build())
    }
}