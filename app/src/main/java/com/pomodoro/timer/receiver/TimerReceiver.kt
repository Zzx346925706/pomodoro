package com.pomodoro.timer.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pomodoro.timer.MainActivity
import com.pomodoro.timer.PomodoroApp
import com.pomodoro.timer.R
import com.pomodoro.timer.data.FocusStore
import com.pomodoro.timer.data.FocusRecord
import com.pomodoro.timer.data.TimerManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val store = FocusStore(context)
        val startTime = TimerManager.getStartTime()
        val mode = TimerManager.getMode()
        val now = LocalDateTime.now()

        // 记录完成的番茄
        if (mode == TimerManager.Mode.WORK && startTime != null) {
            val duration = java.time.Duration.between(startTime, now).toMinutes().toInt()
            store.addRecord(
                FocusRecord(
                    date = now.toLocalDate().toString(),
                    startTime = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    endTime = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    durationMinutes = duration.coerceAtLeast(1),
                    completed = true
                )
            )
        }

        TimerManager.stop(context)

        // 发送通知
        val title = if (mode == TimerManager.Mode.WORK) "🍅 番茄完成！" else "☕ 休息结束"
        val message = if (mode == TimerManager.Mode.WORK) "太棒了！休息一下吧" else "继续加油！"

        val clickIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PomodoroApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }
}
