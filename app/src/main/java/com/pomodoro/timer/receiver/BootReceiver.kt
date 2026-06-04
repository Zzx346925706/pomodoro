package com.pomodoro.timer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // 开机后不做任何操作，定时器会自然重置
    }
}
