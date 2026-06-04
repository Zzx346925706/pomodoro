package com.pomodoro.timer.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pomodoro.timer.receiver.TimerReceiver
import java.time.LocalDateTime
import java.time.ZoneId

object TimerManager {
    private var startTime: LocalDateTime? = null
    private var isRunning = false
    private var isPaused = false
    private var remainingSeconds = 0
    private var currentMode = Mode.WORK

    enum class Mode { WORK, BREAK, LONG_BREAK }

    fun start(context: Context, durationMinutes: Int, mode: Mode) {
        startTime = LocalDateTime.now()
        isRunning = true
        isPaused = false
        remainingSeconds = durationMinutes * 60
        currentMode = mode
        scheduleAlarm(context, durationMinutes)
    }

    fun pause(context: Context) {
        if (isRunning && !isPaused) {
            isPaused = true
            cancelAlarm(context)
        }
    }

    fun resume(context: Context) {
        if (isRunning && isPaused) {
            isPaused = false
            scheduleAlarm(context, remainingSeconds / 60)
        }
    }

    fun stop(context: Context) {
        isRunning = false
        isPaused = false
        remainingSeconds = 0
        cancelAlarm(context)
    }

    fun getState(): Triple<Boolean, Boolean, Int> = Triple(isRunning, isPaused, remainingSeconds)

    fun getMode(): Mode = currentMode

    fun getStartTime(): LocalDateTime? = startTime

    fun tick() {
        if (isRunning && !isPaused && remainingSeconds > 0) {
            remainingSeconds--
        }
    }

    private fun scheduleAlarm(context: Context, durationMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + durationMinutes * 60 * 1000L
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
        )
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
