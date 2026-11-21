package com.redcode.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.redcode.app.MainActivity
import com.redcode.app.R
import com.redcode.app.runtime.HealthMonitor
import com.redcode.app.runtime.OpenCodeServer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RedcodeForegroundService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "redcode_runtime_service"
        const val NOTIFICATION_ID = 49374
        const val ACTION_START = "com.redcode.app.action.START_SERVICE"
        const val ACTION_STOP = "com.redcode.app.action.STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, RedcodeForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RedcodeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var openCodeServer: OpenCodeServer
    @Inject lateinit var healthMonitor: HealthMonitor

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                openCodeServer.stopServer()
                healthMonitor.stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val notification = buildNotification("Opencode Runtime Active", "127.0.0.1:49374 · Loopback")
                startForeground(NOTIFICATION_ID, notification)
                openCodeServer.startServer()
                healthMonitor.startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Redcode::RuntimeWakeLock").apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24 hours
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Redcode Runtime Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the OpenCode proot server running in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        openCodeServer.stopServer()
        healthMonitor.stopMonitoring()
        super.onDestroy()
    }
}
