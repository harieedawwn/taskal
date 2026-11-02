package com.example.ui.modes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.FocusSessionEntity
import com.example.data.GlassThickness
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.TaskListEntity
import com.example.data.TaskWithDetails
import com.example.notifications.TaskalNotificationHelper
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassEmptyState
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.theme.parseColorHex
import com.example.util.S
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * HABITS MODE:
 * Daily & weekly-target habits, interactive check-ins, current/best streak, and Month Heat-Map.
 */
@Composable
fun HabitsModeScreen(
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>,
    onToggleHabitDay: (HabitEntity, Long) -> Unit,
    onSaveHabit: (HabitEntity) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.from(today) }
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.modeHabits,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            GlassChip(
                label = if (s.isVi) "+ Thói quen" else "+ Habit",
                selected = true,
                onClick = { showAddHabitDialog = true }
            )
        }

        if (habits.isEmpty()) {
            GlassEmptyState(
                message = s.emptyHabits,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    val habitColor = parseColorHex(habit.colorHex, tokens.accentColor)
                    val logsForHabit = remember(habitLogs, habit.id) {
                        habitLogs.filter { it.habitId == habit.id }.map { it.epochDay }.toSet()
                    }
                    val doneToday = logsForHabit.contains(today.toEpochDay())

                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = if (doneToday) GlassThickness.THICK else GlassThickness.REGULAR,
                        cornerRadius = 24.dp,
                        accentTint = habitColor,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = habit.emoji, style = MaterialTheme.typography.headlineMedium)
                                    Column {
                                        Text(
                                            text = habit.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (habit.isDaily) (if (s.isVi) "Mục tiêu hàng ngày" else "Daily habit")
                                            else "${habit.weeklyTarget}x / ${if (s.isVi) "tuần" else "week"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassChip(
                                        label = "🔥 ${habit.currentStreak}d (Best ${habit.bestStreak}d)",
                                        selected = habit.currentStreak > 0,
                                        accentColor = habitColor
                                    )
                                    GlassChip(
                                        label = if (doneToday) "✓" else (if (s.isVi) "Điểm danh" else "Check-in"),
                                        selected = doneToday,
                                        accentColor = habitColor,
                                        onClick = { onToggleHabitDay(habit, today.toEpochDay()) }
                                    )
                                    IconButton(
                                        onClick = { onDeleteHabit(habit.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = s.delete, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Last 7 days quick toggle strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (offset in 6 downTo 0) {
                                    val d = today.minusDays(offset.toLong())
                                    val ep = d.toEpochDay()
                                    val checked = logsForHabit.contains(ep)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onToggleHabitDay(habit, ep) }
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = d.dayOfWeek.name.take(2),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(if (checked) habitColor else Color.White.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = d.dayOfMonth.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (checked) Color(0xFF04101E) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Month Heat-Map Grid (1..lengthOfMonth)
                            Text(
                                text = if (s.isVi) "Bản đồ nhiệt tháng ${currentMonth.monthValue}"
                                else "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} Heat-Map",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (dayNum in 1..currentMonth.lengthOfMonth()) {
                                    val ep = currentMonth.atDay(dayNum).toEpochDay()
                                    val done = logsForHabit.contains(ep)
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (done) habitColor
                                                else habitColor.copy(alpha = 0.12f)
                                            )
                                            .clickable { onToggleHabitDay(habit, ep) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddHabitDialog) {
        var title by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("💧") }
        var weeklyTarget by remember { mutableIntStateOf(7) }
        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text(if (s.isVi) "Tạo thói quen mới" else "Create Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (s.isVi) "Tên thói quen" else "Habit title") },
                        singleLine = true
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (s.isVi) "Số ngày/tuần:" else "Days/week:")
                        for (t in listOf(3, 5, 7)) {
                            GlassChip(
                                label = "${t}d",
                                selected = weeklyTarget == t,
                                onClick = { weeklyTarget = t }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onSaveHabit(
                            HabitEntity(
                                title = title.trim(),
                                emoji = emoji.ifBlank { "💧" },
                                colorHex = "#34D399",
                                isDaily = weeklyTarget == 7,
                                weeklyTarget = weeklyTarget
                            )
                        )
                        showAddHabitDialog = false
                    }
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) { Text(s.cancel) }
            }
        )
    }
}

/**
 * FOCUS MODE:
 * Pomodoro & Stopwatch tied to a task, circular glass timer, presets + custom durations,
 * ongoing system notification with chronometer (no foreground service), and session history.
 */
@Composable
fun FocusModeScreen(
    tasks: List<TaskWithDetails>,
    sessions: List<FocusSessionEntity>,
    settings: AppSettings,
    onLogCompletedSession: (FocusSessionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current

    var isStopwatch by remember { mutableStateOf(false) }
    var targetMinutes by remember(settings.focusWorkMinutes) { mutableIntStateOf(settings.focusWorkMinutes) }
    var remainingSeconds by remember(targetMinutes) { mutableIntStateOf(targetMinutes * 60) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableLongStateOf(tasks.firstOrNull { !it.task.isCompleted }?.task?.id ?: 0L) }
    val selectedTask = tasks.firstOrNull { it.task.id == selectedTaskId }?.task

    LaunchedEffect(isRunning, isStopwatch, targetMinutes) {
        if (isRunning) {
            val endMillis = System.currentTimeMillis() + remainingSeconds * 1000L
            TaskalNotificationHelper.showFocusChronometerNotification(
                context = context,
                taskTitle = selectedTask?.title.orEmpty(),
                endEpochMillis = if (isStopwatch) System.currentTimeMillis() - elapsedSeconds * 1000L else endMillis,
                isCountdown = !isStopwatch
            )
            while (isRunning) {
                delay(1000L)
                if (isStopwatch) {
                    elapsedSeconds++
                } else {
                    if (remainingSeconds > 0) {
                        remainingSeconds--
                    } else {
                        isRunning = false
                        TaskalNotificationHelper.cancelFocusNotification(context)
                        onLogCompletedSession(
                            FocusSessionEntity(
                                taskId = selectedTask?.id,
                                taskTitleSnapshot = selectedTask?.title ?: "Deep Focus",
                                durationMinutes = targetMinutes,
                                startedAt = System.currentTimeMillis() - targetMinutes * 60_000L,
                                completedAt = System.currentTimeMillis(),
                                wasCompleted = true,
                                mode = "POMODORO"
                            )
                        )
                    }
                }
            }
        }
    }

    val progress = if (isStopwatch) {
        ((elapsedSeconds % 3600) / 3600f).coerceIn(0f, 1f)
    } else {
        val total = (targetMinutes * 60).coerceAtLeast(1)
        (1f - remainingSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.modeFocus,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassChip(
                    label = "Pomodoro",
                    selected = !isStopwatch,
                    onClick = { isRunning = false; isStopwatch = false }
                )
                GlassChip(
                    label = if (s.isVi) "Bấm giờ" else "Stopwatch",
                    selected = isStopwatch,
                    onClick = { isRunning = false; isStopwatch = true }
                )
            }
        }

        // Task Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.filter { !it.task.isCompleted }.take(8).forEach { item ->
                GlassChip(
                    label = "🎯 ${item.task.title.take(24)}",
                    selected = selectedTaskId == item.task.id,
                    onClick = { selectedTaskId = item.task.id }
                )
            }
        }

        // Circular Liquid Glass Timer
        GlassSurface(
            modifier = Modifier.size(268.dp),
            thickness = GlassThickness.THICK,
            cornerRadius = 134.dp,
            accentTint = tokens.accentColor,
            elevated = true
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                    val strokeW = 10.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.12f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(tokens.accentColor, tokens.secondaryAccent, tokens.accentColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val secsToShow = if (isStopwatch) elapsedSeconds else remainingSeconds
                    val mm = secsToShow / 60
                    val ss = secsToShow % 60
                    Text(
                        text = "%02d:%02d".format(mm, ss),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedTask?.title?.take(22) ?: (if (s.isVi) "Phiên tập trung sâu" else "Deep Focus Session"),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.accentColor
                    )
                }
            }
        }

        // Duration Presets
        if (!isStopwatch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    15 to "15m",
                    settings.focusWorkMinutes to "${settings.focusWorkMinutes}m Focus",
                    45 to "45m Deep",
                    60 to "60m Flow",
                    settings.focusShortBreakMinutes to "${settings.focusShortBreakMinutes}m Break",
                    settings.focusLongBreakMinutes to "${settings.focusLongBreakMinutes}m Rest"
                ).distinctBy { it.first }.forEach { (mins, label) ->
                    GlassChip(
                        label = label,
                        selected = targetMinutes == mins,
                        onClick = {
                            isRunning = false
                            targetMinutes = mins
                            remainingSeconds = mins * 60
                        }
                    )
                }
            }
        }

        // Play / Pause / Reset Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassSurface(
                modifier = Modifier.size(64.dp),
                thickness = GlassThickness.THICK,
                cornerRadius = 32.dp,
                accentTint = tokens.accentColor,
                onClick = { isRunning = !isRunning }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        tint = tokens.accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            GlassSurface(
                modifier = Modifier.size(52.dp),
                thickness = GlassThickness.REGULAR,
                cornerRadius = 26.dp,
                onClick = {
                    isRunning = false
                    TaskalNotificationHelper.cancelFocusNotification(context)
                    if (isStopwatch && elapsedSeconds >= 60) {
                        onLogCompletedSession(
                            FocusSessionEntity(
                                taskId = selectedTask?.id,
                                taskTitleSnapshot = selectedTask?.title ?: "Stopwatch Focus",
                                durationMinutes = (elapsedSeconds / 60).coerceAtLeast(1),
                                startedAt = System.currentTimeMillis() - elapsedSeconds * 1000L,
                                completedAt = System.currentTimeMillis(),
                                wasCompleted = true,
                                mode = "STOPWATCH"
                            )
                        )
                    }
                    elapsedSeconds = 0
                    remainingSeconds = targetMinutes * 60
                }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset")
                }
            }
        }

        // Session History
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.REGULAR,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (s.isVi) "Lịch sử phiên tập trung (${sessions.size})" else "Focus Session History (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                sessions.take(8).forEach { sess ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sess.taskTitleSnapshot.ifBlank { "Focus Session" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${sess.durationMinutes}m · ${sess.mode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.accentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * STATS MODE:
 * Completed per day/week, streak, focus time, by list/tag, completion rate, most productive hours.
 * All charts drawn with Jetpack Compose Canvas with animated draw-in.
 */
@Composable
fun StatsModeScreen(
    tasks: List<TaskWithDetails>,
    lists: List<TaskListEntity>,
    habits: List<HabitEntity>,
    focusSessions: List<FocusSessionEntity>,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }
    val drawProgress by animateFloatAsState(
        targetValue = if (mounted) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "stats_draw_in"
    )

    val completedTasks = remember(tasks) { tasks.filter { it.task.isCompleted } }
    val completionRate = remember(tasks, completedTasks) {
        if (tasks.isEmpty()) 0f else completedTasks.size.toFloat() / tasks.size.toFloat()
    }
    val totalFocusMinutes = remember(focusSessions) { focusSessions.sumOf { it.durationMinutes } }
    val bestHabitStreak = remember(habits) { habits.maxOfOrNull { it.bestStreak } ?: 0 }

    val today = remember { LocalDate.now() }
    val last7DaysCounts = remember(completedTasks, today) {
        (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong()).toEpochDay()
            val cnt = completedTasks.count { item ->
                val compDay = item.task.completedAt?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                } ?: item.task.dueDateEpochDay
                compDay == d
            }
            today.minusDays(offset.toLong()).dayOfWeek.name.take(2) to cnt
        }
    }

    val productiveHourBuckets = remember(completedTasks) {
        // 6 buckets of 4 hours: 0-4, 4-8, 8-12, 12-16, 16-20, 20-24
        val buckets = IntArray(6)
        completedTasks.forEach { item ->
            val hr = item.task.startTimeMinute?.div(60)
                ?: item.task.completedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).hour }
                ?: 10
            buckets[(hr / 4).coerceIn(0, 5)]++
        }
        buckets.toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = s.modeStats,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // KPI Summary Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassSurface(
                modifier = Modifier.weight(1f),
                thickness = GlassThickness.REGULAR,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(14.dp)
            ) {
                Column {
                    Text("${completedTasks.size}/${tasks.size}", style = MaterialTheme.typography.headlineMedium, color = tokens.accentColor)
                    Text(if (s.isVi) "Đã hoàn thành" else "Tasks Completed", style = MaterialTheme.typography.labelSmall)
                }
            }
            GlassSurface(
                modifier = Modifier.weight(1f),
                thickness = GlassThickness.REGULAR,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(14.dp)
            ) {
                Column {
                    Text("${totalFocusMinutes}m", style = MaterialTheme.typography.headlineMedium, color = tokens.secondaryAccent)
                    Text(if (s.isVi) "Phút tập trung" else "Focus Time", style = MaterialTheme.typography.labelSmall)
                }
            }
            GlassSurface(
                modifier = Modifier.weight(1f),
                thickness = GlassThickness.REGULAR,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(14.dp)
            ) {
                Column {
                    Text("${bestHabitStreak}d", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF34D399))
                    Text(if (s.isVi) "Chuỗi cao nhất" else "Best Streak", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 1. Canvas Animated Bar Chart: Completed Tasks Last 7 Days
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.REGULAR,
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (s.isVi) "Hoàn thành trong 7 ngày qua" else "Completed per Day (Last 7 Days)",
                    style = MaterialTheme.typography.titleMedium
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val maxVal = (last7DaysCounts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    val slotW = size.width / last7DaysCounts.size
                    last7DaysCounts.forEachIndexed { idx, (_, count) ->
                        val ratio = (count.toFloat() / maxVal.toFloat()) * drawProgress
                        val barH = (size.height * 0.78f * ratio).coerceAtLeast(6.dp.toPx())
                        val barW = slotW * 0.48f
                        val x = idx * slotW + (slotW - barW) / 2f
                        val y = size.height - barH
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(tokens.accentColor, tokens.secondaryAccent)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    last7DaysCounts.forEach { (dayLbl, cnt) ->
                        Text("$dayLbl ($cnt)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 2. Completion Rate Ring & Breakdown by List
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.REGULAR,
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sw = 12.dp.toPx()
                        drawArc(
                            color = Color.White.copy(alpha = 0.1f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(sw, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = tokens.accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * completionRate * drawProgress,
                            useCenter = false,
                            style = Stroke(sw, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(completionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (s.isVi) "Phân bổ theo Danh sách" else "Breakdown by List",
                        style = MaterialTheme.typography.titleMedium
                    )
                    lists.forEach { list ->
                        val count = tasks.count { it.task.listId == list.id }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${list.emoji} ${list.name}", style = MaterialTheme.typography.bodySmall)
                            Text("$count", style = MaterialTheme.typography.labelMedium, color = parseColorHex(list.colorHex))
                        }
                    }
                }
            }
        }

        // 3. Most Productive Hours Curve (Canvas Bezier / Polyline Chart)
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.REGULAR,
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (s.isVi) "Khung giờ năng suất nhất" else "Most Productive Hours",
                    style = MaterialTheme.typography.titleMedium
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val maxBucket = (productiveHourBuckets.maxOrNull() ?: 1).coerceAtLeast(1)
                    val stepX = size.width / (productiveHourBuckets.size - 1).coerceAtLeast(1)
                    val path = Path()
                    productiveHourBuckets.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - (v.toFloat() / maxBucket.toFloat()) * size.height * 0.8f * drawProgress - 8.dp.toPx()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(
                            color = tokens.accentColor,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(tokens.accentColor, tokens.secondaryAccent)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("00h", "04h", "08h", "12h", "16h", "20h").forEach { lbl ->
                        Text(lbl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
