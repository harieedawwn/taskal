package com.example.ui.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.AppSettings
import com.example.data.CalendarViewType
import com.example.data.GlassThickness
import com.example.data.NoteEntity
import com.example.data.TaskWithDetails
import com.example.domain.LunarCalendar
import com.example.ui.glass.DropletCheckbox
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassEmptyState
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.theme.parseColorHex
import com.example.util.S
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

data class DeviceCalendarEvent(
    val id: Long,
    val title: String,
    val epochDay: Long,
    val startMinute: Int,
    val durationMinutes: Int
)

fun queryDeviceCalendarEvents(context: Context, startEpochDay: Long, endEpochDay: Long): List<DeviceCalendarEvent> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    val result = mutableListOf<DeviceCalendarEvent>()
    val zone = ZoneId.systemDefault()
    val startMillis = LocalDate.ofEpochDay(startEpochDay).atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = LocalDate.ofEpochDay(endEpochDay + 1).atStartOfDay(zone).toInstant().toEpochMilli()

    val projection = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND
    )
    val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
    val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

    runCatching {
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
            val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "Event"
                val dtStart = if (startIdx >= 0) cursor.getLong(startIdx) else continue
                val dtEnd = if (endIdx >= 0) cursor.getLong(endIdx) else dtStart + 3600_000L
                val zdt = Instant.ofEpochMilli(dtStart).atZone(zone)
                val durMin = ((dtEnd - dtStart) / 60_000L).toInt().coerceIn(15, 720)
                result.add(
                    DeviceCalendarEvent(
                        id = id,
                        title = title.ifBlank { "Calendar Event" },
                        epochDay = zdt.toLocalDate().toEpochDay(),
                        startMinute = zdt.hour * 60 + zdt.minute,
                        durationMinutes = durMin
                    )
                )
            }
        }
    }
    return result
}

@Composable
fun CalendarModeScreen(
    tasks: List<TaskWithDetails>,
    notes: List<NoteEntity>,
    settings: AppSettings,
    isExpandedTablet: Boolean,
    onToggleTaskComplete: (Long, Boolean) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onQuickAddAtSlot: (epochDay: Long, startMinute: Int?) -> Unit,
    onRescheduleTask: (taskId: Long, newEpochDay: Long?, newStartMinute: Int?, newDurationMinutes: Int?) -> Unit,
    onSaveDayNote: (epochDay: Long, content: String) -> Unit,
    onOpenNoteDetail: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    var activeView by remember(settings.defaultCalendarView) { mutableStateOf(settings.defaultCalendarView) }
    var selectedDate by remember { mutableStateOf(today) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var showDaySheetModal by remember { mutableStateOf(false) }
    var showJumpMonthDialog by remember { mutableStateOf(false) }
    var swipeParallaxOffset by remember { mutableFloatStateOf(0f) }

    // Device calendar overlay permission & events
    var hasCalendarPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }
    val calPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCalendarPerm = granted
    }
    LaunchedEffect(settings.deviceCalendarOverlay) {
        if (settings.deviceCalendarOverlay && !hasCalendarPerm) {
            calPermLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }
    val deviceEvents = remember(settings.deviceCalendarOverlay, hasCalendarPerm, currentMonth) {
        if (settings.deviceCalendarOverlay && hasCalendarPerm) {
            queryDeviceCalendarEvents(
                context,
                currentMonth.atDay(1).minusDays(7).toEpochDay(),
                currentMonth.atEndOfMonth().plusDays(7).toEpochDay()
            )
        } else emptyList()
    }

    val visibleTasks by remember(tasks, settings.showCompletedInCalendar) {
        derivedStateOf {
            if (settings.showCompletedInCalendar) tasks else tasks.filter { !it.task.isCompleted }
        }
    }

    val tasksByEpochDay by remember(visibleTasks) {
        derivedStateOf {
            visibleTasks.filter { it.task.dueDateEpochDay != null }
                .groupBy { it.task.dueDateEpochDay!! }
        }
    }

    val notesByEpochDay by remember(notes) {
        derivedStateOf {
            notes.filter { it.attachedDateEpochDay != null && !it.isDeleted }
                .groupBy { it.attachedDateEpochDay!! }
        }
    }

    fun stepPeriod(direction: Int) {
        when (activeView) {
            CalendarViewType.MONTH -> {
                currentMonth = currentMonth.plusMonths(direction.toLong())
                selectedDate = currentMonth.atDay(selectedDate.dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth()))
            }
            CalendarViewType.WEEK -> {
                selectedDate = selectedDate.plusWeeks(direction.toLong())
                currentMonth = YearMonth.from(selectedDate)
            }
            CalendarViewType.THREE_DAY -> {
                selectedDate = selectedDate.plusDays((direction * 3).toLong())
                currentMonth = YearMonth.from(selectedDate)
            }
            CalendarViewType.DAY, CalendarViewType.AGENDA -> {
                selectedDate = selectedDate.plusDays(direction.toLong())
                currentMonth = YearMonth.from(selectedDate)
            }
        }
    }

    val locale = if (s.isVi) Locale.forLanguageTag("vi-VN") else Locale.ENGLISH
    val monthTitle = currentMonth.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(if (isExpandedTablet) 0.58f else 1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .pointerInput(activeView) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeParallaxOffset > 130f) stepPeriod(-1)
                            else if (swipeParallaxOffset < -130f) stepPeriod(1)
                            swipeParallaxOffset = 0f
                        },
                        onDragCancel = { swipeParallaxOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeParallaxOffset = (swipeParallaxOffset + dragAmount * 0.45f).coerceIn(-180f, 180f)
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Editorial Typography Header (Huge Expressive Numeral + Month Name + Lunar Year Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.clickable { showJumpMonthDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "%02d".format(selectedDate.dayOfMonth),
                            style = MaterialTheme.typography.displayLarge,
                            color = tokens.accentColor
                        )
                        Column {
                            Text(
                                text = "$monthTitle ${currentMonth.year}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (settings.showLunarCalendar) {
                                val lunar = remember(selectedDate) { LunarCalendar.solarToLunar(selectedDate) }
                                val holiday = remember(selectedDate, lunar) {
                                    if (settings.showVietnameseHolidays) LunarCalendar.getVietnameseHoliday(selectedDate, lunar) else null
                                }
                                val lunarLabel = if (s.isVi) {
                                    "Âm lịch: ${lunar.day}/${lunar.month} (${LunarCalendar.canChiOfYear(lunar.year)})"
                                } else {
                                    "Lunar: ${lunar.day}/${lunar.month} (${LunarCalendar.canChiOfYear(lunar.year)})"
                                }
                                Text(
                                    text = lunarLabel + (holiday?.let { " · ✦ ${if (s.isVi) it.nameVi else it.nameEn}" } ?: ""),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.secondaryAccent
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { stepPeriod(-1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous period")
                    }
                    GlassChip(
                        label = s.today,
                        selected = selectedDate == today,
                        onClick = {
                            selectedDate = today
                            currentMonth = YearMonth.from(today)
                        }
                    )
                    IconButton(onClick = { stepPeriod(1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next period")
                    }
                }
            }

            // View Switcher Bar (Month, Week, 3-Day, Day, Agenda)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    CalendarViewType.MONTH to s.calMonth,
                    CalendarViewType.WEEK to s.calWeek,
                    CalendarViewType.THREE_DAY to s.calThreeDay,
                    CalendarViewType.DAY to s.calDay,
                    CalendarViewType.AGENDA to s.calAgenda
                ).forEach { (viewType, label) ->
                    GlassChip(
                        label = label,
                        selected = activeView == viewType,
                        onClick = { activeView = viewType },
                        modifier = Modifier.testTag("cal_view_${viewType.id}")
                    )
                }
            }

            // Main Calendar Viewport with Parallax Offset
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset { IntOffset((swipeParallaxOffset * 0.35f).roundToInt(), 0) }
            ) {
                when (activeView) {
                    CalendarViewType.MONTH -> {
                        MonthCalendarGrid(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            today = today,
                            settings = settings,
                            tasksByEpochDay = tasksByEpochDay,
                            notesByEpochDay = notesByEpochDay,
                            onSelectDate = { clickedDate ->
                                selectedDate = clickedDate
                                if (!isExpandedTablet) {
                                    showDaySheetModal = true
                                }
                            },
                            onDropTaskOnDate = { taskId, targetDate ->
                                onRescheduleTask(taskId, targetDate.toEpochDay(), null, null)
                            }
                        )
                    }
                    CalendarViewType.WEEK -> {
                        val startOfWeek = remember(selectedDate, settings.firstDayMonday) {
                            val firstDow = if (settings.firstDayMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
                            var d = selectedDate
                            while (d.dayOfWeek != firstDow) d = d.minusDays(1)
                            d
                        }
                        MultiDayTimeGrid(
                            days = (0 until 7).map { startOfWeek.plusDays(it.toLong()) },
                            selectedDate = selectedDate,
                            settings = settings,
                            tasksByEpochDay = tasksByEpochDay,
                            deviceEvents = deviceEvents,
                            onSelectDate = { selectedDate = it },
                            onOpenTaskDetail = onOpenTaskDetail,
                            onQuickAddAtSlot = onQuickAddAtSlot,
                            onRescheduleTask = onRescheduleTask
                        )
                    }
                    CalendarViewType.THREE_DAY -> {
                        MultiDayTimeGrid(
                            days = (0 until 3).map { selectedDate.plusDays(it.toLong()) },
                            selectedDate = selectedDate,
                            settings = settings,
                            tasksByEpochDay = tasksByEpochDay,
                            deviceEvents = deviceEvents,
                            onSelectDate = { selectedDate = it },
                            onOpenTaskDetail = onOpenTaskDetail,
                            onQuickAddAtSlot = onQuickAddAtSlot,
                            onRescheduleTask = onRescheduleTask
                        )
                    }
                    CalendarViewType.DAY -> {
                        MultiDayTimeGrid(
                            days = listOf(selectedDate),
                            selectedDate = selectedDate,
                            settings = settings,
                            tasksByEpochDay = tasksByEpochDay,
                            deviceEvents = deviceEvents,
                            onSelectDate = { selectedDate = it },
                            onOpenTaskDetail = onOpenTaskDetail,
                            onQuickAddAtSlot = onQuickAddAtSlot,
                            onRescheduleTask = onRescheduleTask
                        )
                    }
                    CalendarViewType.AGENDA -> {
                        AgendaTimelineView(
                            selectedDate = selectedDate,
                            settings = settings,
                            tasksByEpochDay = tasksByEpochDay,
                            notesByEpochDay = notesByEpochDay,
                            onToggleTaskComplete = onToggleTaskComplete,
                            onOpenTaskDetail = onOpenTaskDetail,
                            onQuickAddForDay = { epochDay -> onQuickAddAtSlot(epochDay, null) },
                            onOpenNote = onOpenNoteDetail
                        )
                    }
                }
            }
        }

        // Tablet / Foldable Two-Pane Supporting Day Sheet
        if (isExpandedTablet) {
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(top = 12.dp, bottom = 24.dp, end = 16.dp)
            ) {
                DaySheetContent(
                    date = selectedDate,
                    settings = settings,
                    dayTasks = tasksByEpochDay[selectedDate.toEpochDay()].orEmpty(),
                    dayNotes = notesByEpochDay[selectedDate.toEpochDay()].orEmpty(),
                    onClose = null,
                    onToggleTaskComplete = onToggleTaskComplete,
                    onOpenTaskDetail = onOpenTaskDetail,
                    onQuickAddAtSlot = onQuickAddAtSlot,
                    onRescheduleTask = onRescheduleTask,
                    onSaveDayNote = onSaveDayNote
                )
            }
        }
    }

    // Phone Day Sheet Modal (Container Transform from selected Calendar Day)
    if (showDaySheetModal && !isExpandedTablet) {
        Dialog(
            onDismissRequest = { showDaySheetModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                DaySheetContent(
                    date = selectedDate,
                    settings = settings,
                    dayTasks = tasksByEpochDay[selectedDate.toEpochDay()].orEmpty(),
                    dayNotes = notesByEpochDay[selectedDate.toEpochDay()].orEmpty(),
                    onClose = { showDaySheetModal = false },
                    onToggleTaskComplete = onToggleTaskComplete,
                    onOpenTaskDetail = onOpenTaskDetail,
                    onQuickAddAtSlot = onQuickAddAtSlot,
                    onRescheduleTask = onRescheduleTask,
                    onSaveDayNote = onSaveDayNote
                )
            }
        }
    }

    // Jump-to-Month Picker Dialog
    if (showJumpMonthDialog) {
        var pickerYear by remember { mutableIntStateOf(currentMonth.year) }
        AlertDialog(
            onDismissRequest = { showJumpMonthDialog = false },
            title = { Text(if (s.isVi) "Chuyển nhanh tới tháng" else "Jump to Month") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { pickerYear-- }) { Icon(Icons.Filled.ChevronLeft, null) }
                        Text(pickerYear.toString(), style = MaterialTheme.typography.headlineMedium)
                        IconButton(onClick = { pickerYear++ }) { Icon(Icons.Filled.ChevronRight, null) }
                    }
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 1..3) {
                                val m = row * 3 + col
                                GlassChip(
                                    label = "T$m",
                                    selected = currentMonth.year == pickerYear && currentMonth.monthValue == m,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        currentMonth = YearMonth.of(pickerYear, m)
                                        selectedDate = currentMonth.atDay(1)
                                        showJumpMonthDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJumpMonthDialog = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun MonthCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    settings: AppSettings,
    tasksByEpochDay: Map<Long, List<TaskWithDetails>>,
    notesByEpochDay: Map<Long, List<NoteEntity>>,
    onSelectDate: (LocalDate) -> Unit,
    onDropTaskOnDate: (Long, LocalDate) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    var draggedTaskId by remember { mutableStateOf<Long?>(null) }

    val dayHeaders = remember(settings.firstDayMonday, s.isVi) {
        if (settings.firstDayMonday) {
            if (s.isVi) listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        } else {
            if (s.isVi) listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
            else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        }
    }

    val firstOfMonth = currentMonth.atDay(1)
    val leadingOffset = if (settings.firstDayMonday) {
        (firstOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
    } else {
        firstOfMonth.dayOfWeek.value % 7
    }
    val daysInMonth = currentMonth.lengthOfMonth()
    val totalCells = ((leadingOffset + daysInMonth + 6) / 7) * 7
    val rows = totalCells / 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showWeekNumbers) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.accentColor,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
            }
            dayHeaders.forEach { hdr ->
                Text(
                    text = hdr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (settings.showWeekNumbers) {
                    val sampleDayNum = (r * 7 - leadingOffset + 1).coerceIn(1, daysInMonth)
                    val weekNum = currentMonth.atDay(sampleDayNum).get(WeekFields.ISO.weekOfWeekBasedYear())
                    Text(
                        text = "$weekNum",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.secondaryAccent,
                        modifier = Modifier.width(20.dp),
                        textAlign = TextAlign.Center
                    )
                }

                for (c in 0 until 7) {
                    val cellIdx = r * 7 + c
                    val dayNum = cellIdx - leadingOffset + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNum)
                        val epoch = date.toEpochDay()
                        val isToday = date == today
                        val isSelected = date == selectedDate
                        val dayTasks = tasksByEpochDay[epoch].orEmpty()
                        val dayNotes = notesByEpochDay[epoch].orEmpty()
                        val lunar = remember(date, settings.showLunarCalendar) {
                            if (settings.showLunarCalendar) LunarCalendar.solarToLunar(date) else null
                        }
                        val holiday = remember(date, lunar, settings.showVietnameseHolidays) {
                            if (settings.showVietnameseHolidays && lunar != null) {
                                LunarCalendar.getVietnameseHoliday(date, lunar)
                            } else null
                        }

                        GlassSurface(
                            modifier = Modifier
                                .weight(1f)
                                .height(82.dp)
                                .testTag("month_cell_${date.dayOfMonth}"),
                            thickness = if (isSelected || isToday) GlassThickness.THICK else GlassThickness.THIN,
                            cornerRadius = 14.dp,
                            accentTint = when {
                                isSelected -> tokens.accentColor
                                isToday -> tokens.secondaryAccent
                                holiday != null -> Color(0xFFF43F5E)
                                else -> null
                            },
                            onClick = {
                                if (draggedTaskId != null) {
                                    onDropTaskOnDate(draggedTaskId!!, date)
                                    draggedTaskId = null
                                } else {
                                    onSelectDate(date)
                                }
                            },
                            contentPadding = PaddingValues(5.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top row: Solar Date + Note Glyph + Lunar Date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isToday -> tokens.accentColor
                                            holiday != null -> Color(0xFFFB7185)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (settings.showNoteMarkersInCalendar && dayNotes.isNotEmpty()) {
                                            Text(
                                                text = "✦",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.accentColor
                                            )
                                        }
                                        if (lunar != null) {
                                            Text(
                                                text = lunar.formatCompact(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (lunar.day == 1 || lunar.day == 15) Color(0xFFFBBF24)
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                            )
                                        }
                                    }
                                }

                                // Mini task bars (up to 2 + overflow dot row)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    dayTasks.take(2).forEach { tItem ->
                                        val barColor = parseColorHex(tItem.task.colorHex, tokens.accentColor)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(barColor.copy(alpha = if (tItem.task.isCompleted) 0.2f else 0.38f))
                                                .clickable {
                                                    draggedTaskId = tItem.task.id
                                                }
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = tItem.task.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    if (dayTasks.size > 2) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            dayTasks.drop(2).take(4).forEach { extra ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(parseColorHex(extra.task.colorHex, tokens.accentColor))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(82.dp))
                    }
                }
            }
        }

        if (draggedTaskId != null) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                thickness = GlassThickness.THICK,
                cornerRadius = 16.dp,
                accentTint = tokens.accentColor,
                contentPadding = PaddingValues(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (s.isVi) "Chạm vào một ngày trên lịch để dời việc tới ngày đó"
                        else "Tap any date cell to drop and reschedule task",
                        style = MaterialTheme.typography.labelMedium
                    )
                    TextButton(onClick = { draggedTaskId = null }) { Text(s.cancel) }
                }
            }
        }
    }
}

/**
 * Time-Grid View for Week, 3-Day, and Day views.
 * Supports long-pressing any empty hour slot to create a time-blocked task,
 * dragging a task vertically to reschedule its time, and resizing duration!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiDayTimeGrid(
    days: List<LocalDate>,
    selectedDate: LocalDate,
    settings: AppSettings,
    tasksByEpochDay: Map<Long, List<TaskWithDetails>>,
    deviceEvents: List<DeviceCalendarEvent>,
    onSelectDate: (LocalDate) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onQuickAddAtSlot: (epochDay: Long, startMinute: Int?) -> Unit,
    onRescheduleTask: (taskId: Long, newEpochDay: Long?, newStartMinute: Int?, newDurationMinutes: Int?) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val startHour = settings.timeGridStartHour.coerceIn(0, 12)
    val endHour = settings.timeGridEndHour.coerceIn(13, 23)
    val snapStep = settings.snapStepMinutes.coerceIn(5, 60)

    Column(modifier = Modifier.fillMaxSize()) {
        // Day column headers + All-day strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.forEach { d ->
                val isSel = d == selectedDate
                val allDayTasks = tasksByEpochDay[d.toEpochDay()].orEmpty().filter { it.task.startTimeMinute == null }
                GlassSurface(
                    modifier = Modifier.weight(1f),
                    thickness = if (isSel) GlassThickness.THICK else GlassThickness.THIN,
                    cornerRadius = 14.dp,
                    accentTint = if (isSel) tokens.accentColor else null,
                    onClick = { onSelectDate(d) },
                    contentPadding = PaddingValues(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${d.dayOfWeek.name.take(3)} ${d.dayOfMonth}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSel) tokens.accentColor else MaterialTheme.colorScheme.onSurface
                        )
                        allDayTasks.take(2).forEach { ad ->
                            Text(
                                text = "• ${ad.task.title}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Hourly scrollable timeline
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            for (hour in startHour..endHour) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "%02d:00".format(hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(48.dp)
                            .padding(top = 4.dp)
                    )

                    days.forEach { day ->
                        val epoch = day.toEpochDay()
                        val slotTasks = tasksByEpochDay[epoch].orEmpty().filter {
                            val sm = it.task.startTimeMinute ?: return@filter false
                            sm / 60 == hour
                        }
                        val slotDeviceEvents = deviceEvents.filter {
                            it.epochDay == epoch && (it.startMinute / 60) == hour
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .combinedClickable(
                                    onClick = { onQuickAddAtSlot(epoch, hour * 60) },
                                    onLongClick = { onQuickAddAtSlot(epoch, hour * 60) }
                                )
                                .padding(3.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                slotTasks.forEach { item ->
                                    val t = item.task
                                    var dragY by remember { mutableFloatStateOf(0f) }
                                    val taskColor = parseColorHex(t.colorHex, tokens.accentColor)
                                    GlassSurface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset { IntOffset(0, dragY.roundToInt()) }
                                            .pointerInput(t.id, t.startTimeMinute, t.durationMinutes) {
                                                detectVerticalDragGestures(
                                                    onDragEnd = {
                                                        val deltaSteps = (dragY / 16f).roundToInt()
                                                        if (deltaSteps != 0) {
                                                            val curMin = t.startTimeMinute ?: (hour * 60)
                                                            val newMin = (curMin + deltaSteps * snapStep).coerceIn(0, 23 * 60 + 45)
                                                            onRescheduleTask(t.id, epoch, newMin, t.durationMinutes)
                                                        }
                                                        dragY = 0f
                                                    },
                                                    onDragCancel = { dragY = 0f },
                                                    onVerticalDrag = { _, dragAmount ->
                                                        dragY = (dragY + dragAmount).coerceIn(-140f, 140f)
                                                    }
                                                )
                                            },
                                        thickness = GlassThickness.REGULAR,
                                        cornerRadius = 10.dp,
                                        accentTint = taskColor,
                                        onClick = { onOpenTaskDetail(item) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = t.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val sm = t.startTimeMinute ?: (hour * 60)
                                                Text(
                                                    text = "%02d:%02d · ${t.durationMinutes}m".format(sm / 60, sm % 60),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = taskColor
                                                )
                                            }
                                            // Quick duration resize pill (+15m)
                                            Text(
                                                text = "+${snapStep}m",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.accentColor,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        val nextDur = if (t.durationMinutes >= 180) 30 else t.durationMinutes + snapStep
                                                        onRescheduleTask(t.id, epoch, t.startTimeMinute, nextDur)
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                slotDeviceEvents.forEach { ev ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tokens.tertiaryAccent.copy(alpha = 0.22f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "📅 ${ev.title}",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaTimelineView(
    selectedDate: LocalDate,
    settings: AppSettings,
    tasksByEpochDay: Map<Long, List<TaskWithDetails>>,
    notesByEpochDay: Map<Long, List<NoteEntity>>,
    onToggleTaskComplete: (Long, Boolean) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onQuickAddForDay: (Long) -> Unit,
    onOpenNote: (NoteEntity) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val days = remember(selectedDate) { (0 until 14).map { selectedDate.plusDays(it.toLong()) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        items(days, key = { it.toEpochDay() }) { day ->
            val epoch = day.toEpochDay()
            val dayTasks = tasksByEpochDay[epoch].orEmpty()
            val dayNotes = notesByEpochDay[epoch].orEmpty()
            val lunar = remember(day, settings.showLunarCalendar) {
                if (settings.showLunarCalendar) LunarCalendar.solarToLunar(day) else null
            }

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                thickness = if (day == LocalDate.now()) GlassThickness.THICK else GlassThickness.REGULAR,
                cornerRadius = 20.dp,
                accentTint = if (day == LocalDate.now()) tokens.accentColor else null,
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "%02d/%02d".format(day.dayOfMonth, day.monthValue),
                                style = MaterialTheme.typography.headlineMedium,
                                color = tokens.accentColor
                            )
                            Text(
                                text = day.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (lunar != null) {
                                Text(
                                    text = "ÂL ${lunar.day}/${lunar.month}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.secondaryAccent
                                )
                            }
                        }
                        GlassChip(
                            label = "+",
                            onClick = { onQuickAddForDay(epoch) }
                        )
                    }

                    if (dayTasks.isEmpty() && dayNotes.isEmpty()) {
                        Text(
                            text = if (s.isVi) "Không có lịch trình" else "Free day — tap + to schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        dayTasks.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTaskDetail(item) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DropletCheckbox(
                                    checked = item.task.isCompleted,
                                    onCheckedChange = { onToggleTaskComplete(item.task.id, it) },
                                    tint = parseColorHex(item.task.colorHex, tokens.accentColor)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.task.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = if (item.task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    )
                                    item.task.startTimeMinute?.let { sm ->
                                        Text(
                                            text = "%02d:%02d (${item.task.durationMinutes}m)".format(sm / 60, sm % 60),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.accentColor
                                        )
                                    }
                                }
                            }
                        }
                        dayNotes.forEach { note ->
                            GlassChip(
                                label = "✦ ${note.title.ifBlank { note.contentMarkdown.take(30) }}",
                                selected = true,
                                accentColor = tokens.secondaryAccent,
                                onClick = { onOpenNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Day Sheet:
 * Shows the selected date's Lunar Date, Vietnamese Holiday, All-Day tasks,
 * Time-blocked schedule, and the inline Day Note editor.
 */
@Composable
fun DaySheetContent(
    date: LocalDate,
    settings: AppSettings,
    dayTasks: List<TaskWithDetails>,
    dayNotes: List<NoteEntity>,
    onClose: (() -> Unit)?,
    onToggleTaskComplete: (Long, Boolean) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onQuickAddAtSlot: (epochDay: Long, startMinute: Int?) -> Unit,
    onRescheduleTask: (taskId: Long, newEpochDay: Long?, newStartMinute: Int?, newDurationMinutes: Int?) -> Unit,
    onSaveDayNote: (epochDay: Long, content: String) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val epochDay = date.toEpochDay()
    val existingDayNote = dayNotes.firstOrNull { it.isDayNote }
    var dayNoteText by remember(epochDay, existingDayNote?.contentMarkdown) {
        mutableStateOf(existingDayNote?.contentMarkdown.orEmpty())
    }

    val lunar = remember(date) { LunarCalendar.solarToLunar(date) }
    val holiday = remember(date, lunar) { LunarCalendar.getVietnameseHoliday(date, lunar) }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_sheet_surface"),
        thickness = GlassThickness.THICK,
        cornerRadius = 28.dp,
        elevated = true,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${date.dayOfWeek.name} · ${date.dayOfMonth}/${date.monthValue}/${date.year}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (settings.showLunarCalendar) {
                        Text(
                            text = "Âm lịch ${lunar.day}/${lunar.month} (${LunarCalendar.canChiOfYear(lunar.year)})" +
                                (holiday?.let { " · ✦ ${if (s.isVi) it.nameVi else it.nameEn}" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.accentColor
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassChip(
                        label = "+ ${if (s.isVi) "Thêm việc" else "Add Task"}",
                        selected = true,
                        onClick = { onQuickAddAtSlot(epochDay, 9 * 60) }
                    )
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = s.cancel)
                        }
                    }
                }
            }

            // Inline Day Note
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                thickness = GlassThickness.THIN,
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.EditNote, contentDescription = null, tint = tokens.accentColor, modifier = Modifier.size(18.dp))
                            Text(s.dayNoteTitle, style = MaterialTheme.typography.titleSmall)
                        }
                        GlassChip(
                            label = s.save,
                            onClick = { onSaveDayNote(epochDay, dayNoteText) }
                        )
                    }
                    OutlinedTextField(
                        value = dayNoteText,
                        onValueChange = {
                            dayNoteText = it
                            onSaveDayNote(epochDay, it)
                        },
                        placeholder = { Text(s.dayNoteHint, style = MaterialTheme.typography.bodySmall) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("day_note_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // All-Day & Timed Tasks
            Text(
                text = if (s.isVi) "Công việc trong ngày (${dayTasks.size})" else "Day Schedule (${dayTasks.size})",
                style = MaterialTheme.typography.titleSmall
            )
            if (dayTasks.isEmpty()) {
                Text(
                    text = s.emptyTasks,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                dayTasks.sortedBy { it.task.startTimeMinute ?: -1 }.forEach { item ->
                    val t = item.task
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = GlassThickness.REGULAR,
                        cornerRadius = 16.dp,
                        accentTint = parseColorHex(t.colorHex, tokens.accentColor),
                        onClick = { onOpenTaskDetail(item) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                DropletCheckbox(
                                    checked = t.isCompleted,
                                    onCheckedChange = { onToggleTaskComplete(t.id, it) },
                                    tint = parseColorHex(t.colorHex, tokens.accentColor)
                                )
                                Column {
                                    Text(
                                        text = t.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            textDecoration = if (t.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    )
                                    val timeLabel = t.startTimeMinute?.let {
                                        "%02d:%02d · ${t.durationMinutes}m".format(it / 60, it % 60)
                                    } ?: s.allDay
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.accentColor
                                    )
                                }
                            }
                            // Quick move to Tomorrow button
                            GlassChip(
                                label = "→ +1d",
                                onClick = {
                                    onRescheduleTask(t.id, epochDay + 1, t.startTimeMinute, t.durationMinutes)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
