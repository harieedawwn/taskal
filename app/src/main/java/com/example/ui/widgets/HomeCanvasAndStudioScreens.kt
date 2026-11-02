package com.example.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppMode
import com.example.data.DashboardWidgetEntity
import com.example.data.GlassThickness
import com.example.data.HabitEntity
import com.example.data.NoteEntity
import com.example.data.TaskWithDetails
import com.example.domain.LunarCalendar
import com.example.ui.glass.DropletCheckbox
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.theme.CuratedAccentPalettes
import com.example.ui.theme.parseColorHex
import com.example.util.S
import com.example.widgets.TaskalGlanceWidgetReceiver
import com.example.widgets.WidgetConfigStore
import com.example.widgets.WidgetInstanceConfig
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val SNAP_SIZES = listOf(
    1 to 1,
    2 to 1,
    2 to 2,
    4 to 1,
    4 to 2,
    4 to 3,
    4 to 4
)

private val WIDGET_TYPES = listOf(
    "TODAY_LIST" to "Today Tasks",
    "AGENDA" to "Agenda Timeline",
    "MINI_MONTH" to "Mini Month & Lunar",
    "WEEK_STRIP" to "Week Strip",
    "QUICK_ADD" to "Quick Add Launcher",
    "PINNED_NOTE" to "Pinned Note",
    "FOCUS_TIMER" to "Focus Ring",
    "HABIT_STREAKS" to "Habit Streaks",
    "PROGRESS_RING" to "Daily Progress Ring",
    "COUNTDOWN" to "Event Countdown"
)

/**
 * 5a) In-App Home Canvas:
 * 4-column grid dashboard across up to 3 swipeable pages.
 * User adds widget cards, resizes through snap sizes (1x1, 2x1, 2x2, 4x1, 4x2, 4x3, 4x4),
 * rearranges order, and long-presses to enter edit mode.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeCanvasModeScreen(
    widgets: List<DashboardWidgetEntity>,
    tasks: List<TaskWithDetails>,
    notes: List<NoteEntity>,
    habits: List<HabitEntity>,
    onSaveWidget: (DashboardWidgetEntity) -> Unit,
    onDeleteWidget: (Long) -> Unit,
    onToggleTaskComplete: (Long, Boolean) -> Unit,
    onOpenQuickAdd: () -> Unit,
    onNavigateMode: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    var editMode by remember { mutableStateOf(false) }
    var showAddWidgetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = s.modeHome,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (s.isVi) "Trang ${pagerState.currentPage + 1}/3 · Nhấn giữ thẻ để sửa bố cục"
                    else "Page ${pagerState.currentPage + 1}/3 · Long-press any widget to customize",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(
                    label = if (editMode) (if (s.isVi) "Xong" else "Done") else (if (s.isVi) "Sửa" else "Edit"),
                    selected = editMode,
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    onClick = { editMode = !editMode }
                )
                GlassChip(
                    label = "+ Widget",
                    selected = true,
                    onClick = { showAddWidgetDialog = true }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { pageIdx ->
            val pageWidgets = remember(widgets, pageIdx) {
                widgets.filter { it.page == pageIdx }.sortedBy { it.sortOrder }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pageWidgets.forEachIndexed { idx, w ->
                        val widthFraction = when (w.spanX) {
                            1 -> 0.22f
                            2 -> 0.47f
                            else -> 1.0f
                        }
                        val cardHeight = (w.spanY.coerceIn(1, 4) * 92).dp

                        GlassSurface(
                            modifier = Modifier
                                .fillMaxWidth(widthFraction)
                                .height(cardHeight)
                                .combinedClickable(
                                    onClick = {
                                        when (w.widgetType) {
                                            "QUICK_ADD" -> onOpenQuickAdd()
                                            "FOCUS_TIMER" -> onNavigateMode(AppMode.FOCUS)
                                            "MINI_MONTH", "WEEK_STRIP", "AGENDA" -> onNavigateMode(AppMode.CALENDAR)
                                            "PINNED_NOTE" -> onNavigateMode(AppMode.NOTES)
                                            "HABIT_STREAKS" -> onNavigateMode(AppMode.HABITS)
                                            else -> {}
                                        }
                                    },
                                    onLongClick = { editMode = !editMode }
                                ),
                            thickness = if (editMode) GlassThickness.THICK else GlassThickness.REGULAR,
                            cornerRadius = 22.dp,
                            accentTint = if (editMode) tokens.accentColor else null,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Edit mode header controls (Snap resize + Reorder + Delete)
                                if (editMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GlassChip(
                                            label = "${w.spanX}x${w.spanY} ⤢",
                                            selected = true,
                                            onClick = {
                                                val curIdx = SNAP_SIZES.indexOfFirst { it.first == w.spanX && it.second == w.spanY }
                                                val next = SNAP_SIZES[(curIdx + 1) % SNAP_SIZES.size]
                                                onSaveWidget(w.copy(spanX = next.first, spanY = next.second))
                                            }
                                        )
                                        Row {
                                            if (idx > 0) {
                                                IconButton(
                                                    onClick = { onSaveWidget(w.copy(sortOrder = (w.sortOrder - 1).coerceAtLeast(0))) },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move earlier", modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            IconButton(
                                                onClick = { onDeleteWidget(w.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = s.delete, tint = Color(0xFFF43F5E), modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }

                                // Widget Body Content
                                DashboardWidgetCardContent(
                                    widget = w,
                                    tasks = tasks,
                                    notes = notes,
                                    habits = habits,
                                    onToggleTaskComplete = onToggleTaskComplete,
                                    onOpenQuickAdd = onOpenQuickAdd
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddWidgetDialog = false },
            title = { Text(if (s.isVi) "Thêm Widget vào Trang ${pagerState.currentPage + 1}" else "Add Widget to Page ${pagerState.currentPage + 1}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WIDGET_TYPES.forEach { (typeKey, label) ->
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = GlassThickness.THIN,
                            cornerRadius = 14.dp,
                            onClick = {
                                onSaveWidget(
                                    DashboardWidgetEntity(
                                        page = pagerState.currentPage,
                                        widgetType = typeKey,
                                        spanX = 4,
                                        spanY = 2,
                                        sortOrder = widgets.size
                                    )
                                )
                                showAddWidgetDialog = false
                            },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(text = label, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddWidgetDialog = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun DashboardWidgetCardContent(
    widget: DashboardWidgetEntity,
    tasks: List<TaskWithDetails>,
    notes: List<NoteEntity>,
    habits: List<HabitEntity>,
    onToggleTaskComplete: (Long, Boolean) -> Unit,
    onOpenQuickAdd: () -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val today = remember { LocalDate.now() }
    val todayEpoch = today.toEpochDay()

    when (widget.widgetType) {
        "PROGRESS_RING" -> {
            val done = tasks.count { it.task.isCompleted }
            val total = tasks.size.coerceAtLeast(1)
            val pct = done.toFloat() / total.toFloat()
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.12f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = tokens.accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * pct,
                            useCenter = false,
                            style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(if (s.isVi) "Tiến độ" else "Progress", style = MaterialTheme.typography.titleSmall)
                    Text("$done/${tasks.size}", style = MaterialTheme.typography.labelSmall, color = tokens.accentColor)
                }
            }
        }

        "FOCUS_TIMER" -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏱ 25:00", style = MaterialTheme.typography.headlineMedium, color = tokens.accentColor)
                Text(if (s.isVi) "Chạm để bắt đầu Pomodoro" else "Tap to start Focus", style = MaterialTheme.typography.labelSmall)
            }
        }

        "TODAY_LIST", "AGENDA" -> {
            val todayItems = tasks.filter { !it.task.isCompleted }.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (widget.widgetType == "AGENDA") s.calAgenda else s.smartToday,
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.accentColor
                )
                if (todayItems.isEmpty()) {
                    Text(s.emptyTasks, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                } else {
                    todayItems.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DropletCheckbox(
                                checked = item.task.isCompleted,
                                onCheckedChange = { onToggleTaskComplete(item.task.id, it) },
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = item.task.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        "MINI_MONTH", "WEEK_STRIP" -> {
            val lunar = remember(today) { LunarCalendar.solarToLunar(today) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${today.dayOfWeek.name} · ${today.dayOfMonth}/${today.monthValue} (ÂL ${lunar.day}/${lunar.month})",
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.accentColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0..6) {
                        val d = today.plusDays(i.toLong() - 2)
                        val isToday = d == today
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d.dayOfWeek.name.take(2), style = MaterialTheme.typography.labelSmall)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) tokens.accentColor else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${d.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isToday) Color(0xFF04101E) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        "PINNED_NOTE" -> {
            val note = notes.firstOrNull { it.isPinned } ?: notes.firstOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("✦ ${note?.title ?: s.modeNotes}", style = MaterialTheme.typography.titleSmall, color = tokens.secondaryAccent)
                Text(
                    text = note?.contentMarkdown ?: s.emptyNotes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        "HABIT_STREAKS" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                habits.take(3).forEach { h ->
                    GlassChip(
                        label = "${h.emoji} ${h.currentStreak}d",
                        selected = h.currentStreak > 0
                    )
                }
            }
        }

        "COUNTDOWN" -> {
            val nextImportant = tasks.firstOrNull { !it.task.isCompleted && it.task.dueDateEpochDay != null && it.task.dueDateEpochDay >= todayEpoch }
            val daysLeft = nextImportant?.task?.dueDateEpochDay?.let { ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(it)) } ?: 0L
            Column {
                Text("⏳ D-$daysLeft", style = MaterialTheme.typography.headlineMedium, color = tokens.accentColor)
                Text(
                    text = nextImportant?.task?.title ?: "All caught up",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        else -> {
            // QUICK_ADD
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onOpenQuickAdd() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = tokens.accentColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(s.quickAddPlaceholder.take(28), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * 5b) Widget Studio & Single-Activity AppWidget Configure Screen:
 * Live preview of home-screen Glance widget with full customization (data source, style Glass/Solid/Transparent,
 * opacity, corner radius, accent color, text scale, show completed, header on/off, density)
 * + Pin via AppWidgetManager.requestPinAppWidget OR finish ACTION_APPWIDGET_CONFIGURE with RESULT_OK.
 */
@Composable
fun WidgetStudioScreen(
    appWidgetIdToConfigure: Int? = null,
    tasks: List<TaskWithDetails>,
    onConfirmConfigureWidget: (Int, WidgetInstanceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current

    var widgetType by remember { mutableStateOf("TODAY_LIST") }
    var style by remember { mutableStateOf("GLASS") }
    var opacity by remember { mutableFloatStateOf(0.82f) }
    var cornerRadiusDp by remember { mutableIntStateOf(22) }
    var accentHex by remember { mutableStateOf("#38BDF8") }
    var textScale by remember { mutableFloatStateOf(1.0f) }
    var showCompleted by remember { mutableStateOf(false) }
    var showHeader by remember { mutableStateOf(true) }
    var compactDensity by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val currentConfig = remember(
        widgetType, style, opacity, cornerRadiusDp, accentHex, textScale, showCompleted, showHeader, compactDensity
    ) {
        WidgetInstanceConfig(
            widgetType = widgetType,
            style = style,
            opacity = opacity,
            cornerRadiusDp = cornerRadiusDp,
            accentHex = accentHex,
            textScale = textScale,
            showCompleted = showCompleted,
            showHeader = showHeader,
            compactDensity = compactDensity
        )
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
            text = if (appWidgetIdToConfigure != null) {
                if (s.isVi) "Cấu hình Tiện ích Màn hình chính" else "Configure Home-Screen Widget"
            } else {
                s.modeWidgets
            },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Live Emulated Glass Widget Preview
        Text(if (s.isVi) "Xem trước trực tiếp:" else "Live Home-Screen Preview:", style = MaterialTheme.typography.labelLarge)
        val previewAccent = parseColorHex(accentHex, tokens.accentColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .background(
                    when (style) {
                        "TRANSPARENT" -> Color(0xFF0A1020).copy(alpha = 0.22f)
                        "SOLID" -> Color(0xFF0E172A)
                        else -> Color(0xFF101C34).copy(alpha = opacity)
                    }
                )
                .border(
                    width = 1.dp,
                    color = previewAccent.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(cornerRadiusDp.dp)
                )
                .padding(if (compactDensity) 10.dp else 16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showHeader) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Taskal · $widgetType",
                            style = MaterialTheme.typography.titleSmall,
                            color = previewAccent,
                            fontWeight = FontWeight.Bold
                        )
                        val lunar = remember { LunarCalendar.solarToLunar(LocalDate.now()) }
                        Text(
                            text = "ÂL ${lunar.day}/${lunar.month}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }
                tasks.filter { showCompleted || !it.task.isCompleted }.take(3).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = if (item.task.isCompleted) "💧" else "◯", color = previewAccent)
                        Text(
                            text = item.task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Studio Controls
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.REGULAR,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (s.isVi) "Kiểu hiển thị" else "Surface Style", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GLASS", "SOLID", "TRANSPARENT").forEach { st ->
                        GlassChip(
                            label = st,
                            selected = style == st,
                            onClick = { style = st }
                        )
                    }
                }

                Text("${if (s.isVi) "Độ mờ nền" else "Glass Opacity"}: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.25f..0.98f)

                Text("${if (s.isVi) "Bo góc" else "Corner Radius"}: ${cornerRadiusDp}dp", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = cornerRadiusDp.toFloat(),
                    onValueChange = { cornerRadiusDp = it.toInt() },
                    valueRange = 4f..36f
                )

                Text(if (s.isVi) "Màu nhấn" else "Accent Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CuratedAccentPalettes.forEach { pal ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(pal.primary)
                                .border(
                                    width = if (accentHex.equals(pal.hex, ignoreCase = true)) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { accentHex = pal.hex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (s.isVi) "Hiện tiêu đề tiện ích" else "Show Header")
                    Switch(checked = showHeader, onCheckedChange = { showHeader = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (s.isVi) "Hiện việc đã xong" else "Show Completed Tasks")
                    Switch(checked = showCompleted, onCheckedChange = { showCompleted = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (s.isVi) "Mật độ gọn" else "Compact Density")
                    Switch(checked = compactDensity, onCheckedChange = { compactDensity = it })
                }

                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = GlassThickness.THICK,
                    cornerRadius = 999.dp,
                    accentTint = previewAccent,
                    onClick = {
                        WidgetConfigStore.saveDefaultStudioConfig(context, currentConfig)
                        if (appWidgetIdToConfigure != null) {
                            onConfirmConfigureWidget(appWidgetIdToConfigure, currentConfig)
                        } else {
                            val awm = AppWidgetManager.getInstance(context)
                            val provider = ComponentName(context, TaskalGlanceWidgetReceiver::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && awm.isRequestPinAppWidgetSupported) {
                                awm.requestPinAppWidget(provider, null, null)
                                statusBanner = if (s.isVi) "Đã gửi yêu cầu ghim Widget ra màn hình chính!"
                                else "Requested launcher to pin widget to Home Screen!"
                            } else {
                                statusBanner = if (s.isVi) "Đã lưu cấu hình Widget mặc định!"
                                else "Saved default widget configuration!"
                            }
                        }
                    },
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PushPin, contentDescription = null, tint = previewAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appWidgetIdToConfigure != null) {
                                if (s.isVi) "Lưu & Hoàn tất Cấu hình Widget" else "Save & Finish Widget Setup"
                            } else {
                                if (s.isVi) "Áp dụng & Ghim ra Màn hình chính" else "Apply & Pin to Home Screen"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                statusBanner?.let { msg ->
                    Text(text = msg, style = MaterialTheme.typography.labelMedium, color = Color(0xFF34D399))
                }
            }
        }
    }
}
