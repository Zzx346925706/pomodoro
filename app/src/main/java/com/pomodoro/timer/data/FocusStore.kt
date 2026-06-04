package com.pomodoro.timer.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FocusRecord(
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val completed: Boolean // true=完成, false=中途放弃
)

data class DailyStats(
    val date: String,
    val totalMinutes: Int,
    val completedPomodoros: Int,
    val abandonedPomodoros: Int
)

class FocusStore(context: Context) {
    private val prefs = context.getSharedPreferences("focus_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _records = MutableStateFlow<List<FocusRecord>>(loadRecords())
    val records: StateFlow<List<FocusRecord>> = _records

    private val _dailyGoal = MutableStateFlow(prefs.getInt("daily_goal", 8))
    val dailyGoal: StateFlow<Int> = _dailyGoal

    private val _workMinutes = MutableStateFlow(prefs.getInt("work_minutes", 25))
    val workMinutes: StateFlow<Int> = _workMinutes

    private val _breakMinutes = MutableStateFlow(prefs.getInt("break_minutes", 5))
    val breakMinutes: StateFlow<Int> = _breakMinutes

    private val _longBreakMinutes = MutableStateFlow(prefs.getInt("long_break_minutes", 15))
    val longBreakMinutes: StateFlow<Int> = _longBreakMinutes

    fun addRecord(record: FocusRecord) {
        val current = _records.value.toMutableList()
        current.add(0, record)
        _records.value = current
        saveRecords(current)
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt("daily_goal", goal).apply()
        _dailyGoal.value = goal
    }

    fun setWorkMinutes(minutes: Int) {
        prefs.edit().putInt("work_minutes", minutes).apply()
        _workMinutes.value = minutes
    }

    fun setBreakMinutes(minutes: Int) {
        prefs.edit().putInt("break_minutes", minutes).apply()
        _breakMinutes.value = minutes
    }

    fun setLongBreakMinutes(minutes: Int) {
        prefs.edit().putInt("long_break_minutes", minutes).apply()
        _longBreakMinutes.value = minutes
    }

    fun getTodayStats(): DailyStats {
        val today = LocalDate.now().toString()
        val todayRecords = _records.value.filter { it.date == today }
        return DailyStats(
            date = today,
            totalMinutes = todayRecords.sumOf { it.durationMinutes },
            completedPomodoros = todayRecords.count { it.completed },
            abandonedPomodoros = todayRecords.count { !it.completed }
        )
    }

    fun getWeeklyStats(): List<DailyStats> {
        val today = LocalDate.now()
        return (0..6).map { offset ->
            val date = today.minusDays(offset.toLong()).toString()
            val dayRecords = _records.value.filter { it.date == date }
            DailyStats(
                date = date,
                totalMinutes = dayRecords.sumOf { it.durationMinutes },
                completedPomodoros = dayRecords.count { it.completed },
                abandonedPomodoros = dayRecords.count { !it.completed }
            )
        }.reversed()
    }

    fun getTotalPomodoros(): Int = _records.value.count { it.completed }

    fun getTotalMinutes(): Long = _records.value.sumOf { it.durationMinutes }.toLong()

    fun getStreakDays(): Int {
        val today = LocalDate.now()
        var streak = 0
        var date = today
        while (true) {
            val dayRecords = _records.value.filter { it.date == date.toString() && it.completed }
            if (dayRecords.isEmpty()) break
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    private fun loadRecords(): List<FocusRecord> {
        val json = prefs.getString("records", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<FocusRecord>>() {}.type)
    }

    private fun saveRecords(list: List<FocusRecord>) {
        prefs.edit().putString("records", gson.toJson(list)).apply()
    }
}
