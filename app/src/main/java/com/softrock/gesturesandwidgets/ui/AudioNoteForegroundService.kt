package com.softrock.gesturesandwidgets.ui

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.softrock.gesturesandwidgets.AppConfig
import com.softrock.gesturesandwidgets.MainActivity
import com.softrock.gesturesandwidgets.R
import java.io.File

class AudioNoteForegroundService : Service() {

    private var notificationId = 1
    private var isPlaying = false
    private lateinit var mediaPlayer: MediaPlayer

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val title = intent.getStringExtra("TITLE")
            val audioPath = intent.getStringExtra("FILE_PATH")

            when (intent.action) {
                "ACTION_PAUSE" -> pause()
                "ACTION_PLAY" -> {
                    if (audioPath != null) {
                        play(audioPath)
                    }
                }
                "ACTION_STOP" -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            if (title != null && audioPath != null) {
                startForeground(notificationId, buildNotification(title, audioPath))
            }
        }

        return START_STICKY
    }

    private fun buildNotification(
        title: String,
        audioPath: String
    ): android.app.Notification {

        val stopIntent = Intent(this, AudioNoteForegroundService::class.java).apply {
            action = "ACTION_STOP"
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_launcher_foreground,
                "Pause",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, AudioNoteForegroundService::class.java).apply {
                        action = "ACTION_PAUSE"
                        putExtra("TITLE", title)
                        putExtra("FILE_PATH", audioPath)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_launcher_foreground,
                "Play",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, AudioNoteForegroundService::class.java).apply {
                        action = "ACTION_PLAY"
                        putExtra("TITLE", title)
                        putExtra("FILE_PATH", audioPath)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }


        val appPendingIntent = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, AppConfig.REMIND_ME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing ${audioPath}..." else "Play $audioPath")
            .addAction(playPauseAction)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", pendingIntent)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun play(audioPath: String) {
        val file = File(this.cacheDir, audioPath)
        if (file.exists()) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
            isPlaying = true
        }
    }

    private fun pause() {
        isPlaying = false
        mediaPlayer.stop()
    }

//    private fun startForeground() {
//        try {
//            val notification = NotificationCompat.Builder(this, "remind_me_alarm")
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setContentTitle("Foreground Service")
//                .setContentText("This is the foreground service playing in background")
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//            ServiceCompat.startForeground(
//                this,
//                1,
//                notification.build(),
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
//                } else {
//                    0
//                }
//            )
//        } catch (e: Exception) {
//            Log.e("REMIND_ME", e.message.toString(), e)
//        }
//    }
}

// TODO: check condition if an audio is playing in app and user starts playing from notification