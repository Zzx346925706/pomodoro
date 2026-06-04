package com.pomodoro.timer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PomodoroApp : Application() {
    companion object {
        const val CHANNEL_ID = "pomodoro_timer"
        lateinit var instance: PomodoroApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "番茄钟提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "番茄钟计时完成提醒"
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
