package com.pomodoro.timer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pomodoro.timer.data.FocusStore
import com.pomodoro.timer.data.TimerManager
import com.pomodoro.timer.data.TimerManager.Mode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val store = remember { FocusStore(context) }
    val todayStats by store.records.collectAsState()
    val dailyGoal by store.dailyGoal.collectAsState()
    val workMinutes by store.workMinutes.collectAsState()
    val breakMinutes by store.breakMinutes.collectAsState()
    val longBreakMinutes by store.longBreakMinutes.collectAsState()

    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(workMinutes * 60) }
    var currentMode by remember { mutableStateOf(Mode.WORK) }
    var completedCount by remember { mutableStateOf(store.getTodayStats().completedPomodoros) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    // 计时器
    LaunchedEffect(isRunning, isPaused) {
        while (isRunning && !isPaused) {
            delay(1000)
            remainingSeconds--
            TimerManager.tick()
            if (remainingSeconds <= 0) {
                isRunning = false
                if (currentMode == Mode.WORK) {
                    completedCount++
                    store.addRecord(
                        com.pomodoro.timer.data.FocusRecord(
                            date = LocalDate.now().toString(),
                            startTime = TimerManager.getStartTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                            endTime = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                            durationMinutes = workMinutes,
                            completed = true
                        )
                    )
                }
            }
        }
    }

    val tabTitles = listOf("🍅 计时", "📊 统计", "🏆 成就")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD32F2F),
                        Color(0xFFE53935),
                        Color(0xFFEF5350),
                        Color(0xFFFFCDD2)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Text(
                "🍅 番茄钟",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "专注 · 高效 · 成长",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> TimerPage(
                        isRunning = isRunning,
                        isPaused = isPaused,
                        remainingSeconds = remainingSeconds,
                        currentMode = currentMode,
                        workMinutes = workMinutes,
                        breakMinutes = breakMinutes,
                        longBreakMinutes = longBreakMinutes,
                        completedCount = completedCount,
                        dailyGoal = dailyGoal,
                        onStart = {
                            TimerManager.start(context, workMinutes, Mode.WORK)
                            remainingSeconds = workMinutes * 60
                            currentMode = Mode.WORK
                            isRunning = true
                            isPaused = false
                        },
                        onPause = {
                            TimerManager.pause(context)
                            isPaused = true
                        },
                        onResume = {
                            TimerManager.resume(context)
                            isPaused = false
                        },
                        onStop = {
                            TimerManager.stop(context)
                            isRunning = false
                            isPaused = false
                            remainingSeconds = workMinutes * 60
                        },
                        onBreak = {
                            val breakTime = if (completedCount % 4 == 0 && completedCount > 0) longBreakMinutes else breakMinutes
                            TimerManager.start(context, breakTime, if (completedCount % 4 == 0 && completedCount > 0) Mode.LONG_BREAK else Mode.BREAK)
                            remainingSeconds = breakTime * 60
                            currentMode = if (completedCount % 4 == 0 && completedCount > 0) Mode.LONG_BREAK else Mode.BREAK
                            isRunning = true
                            isPaused = false
                        }
                    )
                    1 -> StatsPage(store = store)
                    2 -> AchievementsPage(store = store)
                }
            }
        }
    }
}

// ==================== 计时器页面 ====================
@Composable
private fun TimerPage(
    isRunning: Boolean,
    isPaused: Boolean,
    remainingSeconds: Int,
    currentMode: Mode,
    workMinutes: Int,
    breakMinutes: Int,
    longBreakMinutes: Int,
    completedCount: Int,
    dailyGoal: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onBreak: () -> Unit
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val totalSeconds = when (currentMode) {
        Mode.WORK -> workMinutes * 60
        Mode.BREAK -> breakMinutes * 60
        Mode.LONG_BREAK -> longBreakMinutes * 60
    }
    val progress = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds else 0f

    val modeColor = when (currentMode) {
        Mode.WORK -> Color(0xFFD32F2F)
        Mode.BREAK -> Color(0xFF4CAF50)
        Mode.LONG_BREAK -> Color(0xFF2196F3)
    }
    val modeEmoji = when (currentMode) {
        Mode.WORK -> "🍅"
        Mode.BREAK -> "☕"
        Mode.LONG_BREAK -> "🌿"
    }
    val modeText = when (currentMode) {
        Mode.WORK -> "专注时间"
        Mode.BREAK -> "短休息"
        Mode.LONG_BREAK -> "长休息"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // 状态指示
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(modeEmoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(modeText, color = Color.White, fontWeight = FontWeight.Bold)
                if (isRunning) {
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPaused) "⏸ 暂停" else "▶ 运行中", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // 圆形计时器
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                // 背景圆环
                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(20f, 20f),
                    size = Size(size.width - 40f, size.height - 40f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )

                // 进度圆环
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(20f, 20f),
                    size = Size(size.width - 40f, size.height - 40f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d".format(minutes, seconds),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "已完成 $completedCount/$dailyGoal 个",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isRunning) {
                // 开始按钮
                Button(
                    onClick = onStart,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("▶", fontSize = 32.sp, color = modeColor)
                }
            } else {
                // 停止按钮
                Button(
                    onClick = onStop,
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("⏹", fontSize = 24.sp, color = Color.White)
                }

                // 暂停/继续按钮
                Button(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (isPaused) "▶" else "⏸", fontSize = 32.sp, color = modeColor)
                }

                // 跳过按钮
                if (currentMode != Mode.WORK) {
                    Button(
                        onClick = {
                            TimerManager.stop(PomodoroApp.instance)
                            onStop()
                        },
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("⏭", fontSize = 24.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 休息按钮（完成番茄后）
        if (!isRunning && currentMode == Mode.WORK && completedCount > 0) {
            Button(
                onClick = onBreak,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("☕ 休息一下", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 今日进度条
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("今日进度", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (completedCount.toFloat() / dailyGoal).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${completedCount} / $dailyGoal 个番茄",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==================== 统计页面 ====================
@Composable
private fun StatsPage(store: FocusStore) {
    val weeklyStats = store.getWeeklyStats()
    val todayStats = store.getTodayStats()
    val totalPomodoros = store.getTotalPomodoros()
    val totalMinutes = store.getTotalMinutes()
    val streak = store.getStreakDays()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 今日概览
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 今日统计", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("🍅", "完成", "${todayStats.completedPomodoros}")
                        StatItem("⏱️", "专注", "${todayStats.totalMinutes}分钟")
                        StatItem("🔥", "连续", "${streak}天")
                    }
                }
            }
        }

        // 累计统计
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏆 累计统计", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("🍅", "总番茄", "$totalPomodoros")
                        StatItem("⏱️", "总时长", "${totalMinutes / 60}小时")
                        StatItem("📅", "日均", "${if (weeklyStats.isNotEmpty()) todayStats.completedPomodoros else 0}个")
                    }
                }
            }
        }

        // 周趋势
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📈 本周趋势", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    val maxPomodoros = weeklyStats.maxOfOrNull { it.completedPomodoros } ?: 1

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyStats.forEach { stat ->
                            val dayName = try {
                                val date = LocalDate.parse(stat.date)
                                when (date.dayOfWeek.value) {
                                    1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"
                                    5 -> "五"; 6 -> "六"; 7 -> "日"; else -> ""
                                }
                            } catch (e: Exception) { "" }

                            val isToday = stat.date == LocalDate.now().toString()
                            val barHeight = if (maxPomodoros > 0) stat.completedPomodoros.toFloat() / maxPomodoros else 0f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 柱子
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(100.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height((barHeight * 100).dp.coerceAtLeast(2.dp))
                                            .background(
                                                if (isToday) Color(0xFFD32F2F) else Color(0xFFFFCDD2),
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    dayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) Color(0xFFD32F2F) else Color.Gray
                                )
                                Text(
                                    "${stat.completedPomodoros}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // 最近记录
        item {
            Text("📋 最近记录", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }

        val recentRecords = store.records.value.take(10)
        if (recentRecords.isEmpty()) {
            item {
                Text("还没有记录，开始专注吧！", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            items(recentRecords) { record ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (record.completed) "🍅" else "💀", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (record.completed) "完成一个番茄" else "中途放弃",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "${record.startTime} - ${record.endTime}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            "${record.durationMinutes}分钟",
                            fontSize = 13.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ==================== 成就页面 ====================
@Composable
private fun AchievementsPage(store: FocusStore) {
    val totalPomodoros = store.getTotalPomodoros()
    val totalMinutes = store.getTotalMinutes()
    val streak = store.getStreakDays()
    val todayStats = store.getTodayStats()
    val records by store.records.collectAsState()

    val achievements = listOf(
        Achievement("first", "🍅", "第一个番茄", "完成第一个番茄", totalPomodoros >= 1),
        Achievement("five", "🖐️", "五连击", "一天完成5个番茄", todayStats.completedPomodoros >= 5),
        Achievement("ten", "🔟", "十全十美", "一天完成10个番茄", todayStats.completedPomodoros >= 10),
        Achievement("streak3", "🔥", "三天打鱼", "连续3天有记录", streak >= 3),
        Achievement("streak7", "📅", "一周坚持", "连续7天有记录", streak >= 7),
        Achievement("streak30", "🏆", "月度之星", "连续30天有记录", streak >= 30),
        Achievement("total50", "⭐", "半个百", "累计完成50个番茄", totalPomodoros >= 50),
        Achievement("total100", "💯", "百发百中", "累计完成100个番茄", totalPomodoros >= 100),
        Achievement("total500", "👑", "番茄之王", "累计完成500个番茄", totalPomodoros >= 500),
        Achievement("hours10", "⏰", "时间旅人", "累计专注10小时", totalMinutes >= 600),
        Achievement("hours100", "⏳", "专注大师", "累计专注100小时", totalMinutes >= 6000),
        Achievement("noGiveUp", "💪", "永不放弃", "连续10个番茄不放弃",
            records.take(10).isNotEmpty() && records.take(10).all { it.completed }),
        Achievement("night", "🌙", "夜猫子", "晚上10点后完成番茄",
            records.any { it.completed && it.endTime >= "22:00" }),
        Achievement("early", "🌅", "早起鸟", "早上6点前完成番茄",
            records.any { it.completed && it.startTime < "06:00" }),
        Achievement("marathon", "🏃", "马拉松", "单次专注超过2小时",
            records.any { it.durationMinutes >= 120 }),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 进度概览
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏆 成就进度", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    val unlocked = achievements.count { it.unlocked }
                    Text(
                        "$unlocked / ${achievements.size} 已解锁",
                        fontSize = 14.sp,
                        color = Color(0xFFD32F2F)
                    )
                    LinearProgressIndicator(
                        progress = (unlocked.toFloat() / achievements.size).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFFD32F2F),
                        trackColor = Color(0xFFFFCDD2),
                    )
                }
            }
        }

        // 成就列表
        items(achievements) { achievement ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (achievement.unlocked) Color.White.copy(alpha = 0.95f)
                    else Color.White.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        achievement.emoji,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .background(
                                if (achievement.unlocked) Color(0xFFFFCDD2) else Color(0xFFF5F5F5),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(8.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            achievement.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (achievement.unlocked) Color(0xFF333333) else Color.Gray
                        )
                        Text(
                            achievement.desc,
                            fontSize = 12.sp,
                            color = if (achievement.unlocked) Color.Gray else Color.LightGray
                        )
                    }
                    if (achievement.unlocked) {
                        Text("✅", fontSize = 20.sp)
                    } else {
                        Text("🔒", fontSize = 16.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private data class Achievement(
    val id: String,
    val emoji: String,
    val name: String,
    val desc: String,
    val unlocked: Boolean
)

@Composable
private fun StatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

private object PomodoroApp {
    val instance get() = com.pomodoro.timer.PomodoroApp.instance
}
