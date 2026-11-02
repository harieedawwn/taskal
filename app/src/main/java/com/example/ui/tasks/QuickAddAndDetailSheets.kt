package com.example.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GlassThickness
import com.example.data.RecurrenceFrequency
import com.example.data.RecurrenceRuleModel
import com.example.data.TaskEntity
import com.example.data.TaskListEntity
import com.example.data.TaskStatus
import com.example.data.TaskWithDetails
import com.example.domain.NaturalLanguageParser
import com.example.domain.RecurrenceEngine
import com.example.ui.glass.DropletCheckbox
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.theme.CuratedAccentPalettes
import com.example.ui.theme.parseColorHex
import com.example.util.S
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Custom Glass Date & Time Picker with Presets (Today, Tomorrow, This weekend, Next week, Pick date)
 * and a tactile Time Wheel.
 */
@Composable
fun GlassDateTimePickerDialog(
    initialEpochDay: Long?,
    initialTimeMinute: Int?,
    initialDurationMinutes: Int = 45,
    onDismiss: () -> Unit,
    onConfirm: (epochDay: Long?, timeMinute: Int?, durationMinutes: Int) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val today = remember { LocalDate.now() }
    var selectedDate by remember {
        mutableStateOf(initialEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today)
    }
    var hasDate by remember { mutableStateOf(initialEpochDay != null) }
    var hasTime by remember { mutableStateOf(initialTimeMinute != null) }
    var selectedHour by remember { mutableIntStateOf((initialTimeMinute ?: 540) / 60) }
    var selectedMinute by remember { mutableIntStateOf(((initialTimeMinute ?: 540) % 60 / 5) * 5) }
    var duration by remember { mutableIntStateOf(initialDurationMinutes) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp),
            thickness = GlassThickness.THICK,
            cornerRadius = 28.dp,
            elevated = true,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = s.pickDate,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassChip(
                        label = s.today,
                        selected = hasDate && selectedDate == today,
                        onClick = {
                            hasDate = true
                            selectedDate = today
                            currentMonth = YearMonth.from(today)
                        }
                    )
                    GlassChip(
                        label = s.tomorrow,
                        selected = hasDate && selectedDate == today.plusDays(1),
                        onClick = {
                            hasDate = true
                            selectedDate = today.plusDays(1)
                            currentMonth = YearMonth.from(selectedDate)
                        }
                    )
                    GlassChip(
                        label = s.thisWeekend,
                        onClick = {
                            hasDate = true
                            selectedDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                            currentMonth = YearMonth.from(selectedDate)
                        }
                    )
                    GlassChip(
                        label = s.nextWeek,
                        onClick = {
                            hasDate = true
                            selectedDate = today.plusWeeks(1)
                            currentMonth = YearMonth.from(selectedDate)
                        }
                    )
                    GlassChip(
                        label = s.smartNoDate,
                        selected = !hasDate,
                        onClick = { hasDate = false }
                    )
                }

                // Mini Interactive Month Grid
                if (hasDate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Text("‹", style = MaterialTheme.typography.headlineMedium)
                        }
                        Text(
                            text = "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Text("›", style = MaterialTheme.typography.headlineMedium)
                        }
                    }

                    val firstOfMonth = currentMonth.atDay(1)
                    val leadingEmpty = (firstOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
                    val totalDays = currentMonth.lengthOfMonth()
                    val rows = (leadingEmpty + totalDays + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (c in 0 until 7) {
                                    val dayNum = r * 7 + c - leadingEmpty + 1
                                    if (dayNum in 1..totalDays) {
                                        val d = currentMonth.atDay(dayNum)
                                        val isSelected = d == selectedDate
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) tokens.accentColor
                                                    else Color.Transparent
                                                )
                                                .clickable { selectedDate = d },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) Color(0xFF04101E) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Time Wheel Toggle & Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (s.isVi) "Giờ cụ thể & Khung giờ" else "Specific Time & Time-Block",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Switch(checked = hasTime, onCheckedChange = { hasTime = it })
                }

                AnimatedVisibility(visible = hasTime) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Tactile Glass Time Wheel
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = GlassThickness.THIN,
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "%02d:%02d".format(selectedHour, selectedMinute),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = tokens.accentColor
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (h in 0..23) {
                                        GlassChip(
                                            label = "%02dh".format(h),
                                            selected = selectedHour == h,
                                            onClick = { selectedHour = h }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (m in listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)) {
                                        GlassChip(
                                            label = ":%02d".format(m),
                                            selected = selectedMinute == m,
                                            onClick = { selectedMinute = m }
                                        )
                                    }
                                }
                            }
                        }

                        // Duration chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (s.isVi) "Thời lượng:" else "Duration:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            for (dur in listOf(15, 30, 45, 60, 90, 120)) {
                                GlassChip(
                                    label = "${dur}m",
                                    selected = duration == dur,
                                    onClick = { duration = dur }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(s.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassChip(
                        label = s.save,
                        selected = true,
                        onClick = {
                            onConfirm(
                                if (hasDate) selectedDate.toEpochDay() else null,
                                if (hasTime) selectedHour * 60 + selectedMinute else null,
                                duration
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Quick-Add Glass Composer:
 * Single text field with live bilingual NLP token parsing and interactive chips.
 * Takes under 2 seconds to add a task.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddComposerSheet(
    lists: List<TaskListEntity>,
    defaultListId: Long,
    defaultPriority: Int,
    nlpEnabled: Boolean,
    initialEpochDay: Long? = null,
    initialTimeMinute: Int? = null,
    onDismiss: () -> Unit,
    onSubmitTask: (
        title: String,
        listId: Long,
        priority: Int,
        dueDateEpochDay: Long?,
        startTimeMinute: Int?,
        durationMinutes: Int,
        recurrenceRule: RecurrenceRuleModel,
        tags: List<String>,
        reminderOffsets: List<Int>
    ) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val focusRequester = remember { FocusRequester() }

    var rawText by remember { mutableStateOf("") }
    var manualEpochDay by remember { mutableStateOf(initialEpochDay) }
    var manualTimeMinute by remember { mutableStateOf(initialTimeMinute) }
    var manualDuration by remember { mutableIntStateOf(45) }
    var manualPriority by remember { mutableStateOf<Int?>(null) }
    var selectedListId by remember { mutableLongStateOf(defaultListId) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsed = remember(rawText, nlpEnabled) {
        if (nlpEnabled) NaturalLanguageParser.parse(rawText)
        else com.example.domain.ParsedTaskDraft(rawInput = rawText, cleanTitle = rawText.trim())
    }

    val effectiveEpochDay = manualEpochDay ?: parsed.dueDate?.toEpochDay() ?: LocalDate.now().toEpochDay()
    val effectiveTimeMinute = manualTimeMinute ?: parsed.startTimeMinute
    val effectiveDuration = parsed.durationMinutes ?: manualDuration
    val effectivePriority = manualPriority ?: parsed.priority ?: defaultPriority
    val effectiveRecurrence = parsed.recurrenceRule ?: RecurrenceRuleModel()

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    fun submitNow() {
        val finalTitle = parsed.cleanTitle.ifBlank { rawText.trim() }
        if (finalTitle.isBlank()) return
        val matchedListId = parsed.listName?.let { name ->
            lists.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
        } ?: selectedListId

        onSubmitTask(
            finalTitle,
            matchedListId,
            effectivePriority,
            effectiveEpochDay,
            effectiveTimeMinute,
            effectiveDuration,
            effectiveRecurrence,
            parsed.tags,
            if (effectiveTimeMinute != null) listOf(10) else emptyList()
        )
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_sheet"),
                thickness = GlassThickness.THICK,
                cornerRadius = 28.dp,
                elevated = true,
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (s.isVi) "Thêm nhanh công việc" else "Quick Add Task",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.accentColor
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = s.cancel)
                        }
                    }

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        placeholder = { Text(s.quickAddPlaceholder, style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitNow() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("quick_add_input")
                    )

                    // Live NLP Parsed Tokens Preview
                    if (parsed.extractedTokens.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            parsed.extractedTokens.forEach { chip ->
                                GlassChip(
                                    label = "✦ ${chip.label}",
                                    selected = true,
                                    accentColor = tokens.accentColor,
                                    onClick = {
                                        // Tap chip to remove matched text from raw input
                                        rawText = rawText.replaceFirst(chip.matchedText, "").replace(Regex("\\s{2,}"), " ").trim()
                                    }
                                )
                            }
                        }
                    }

                    // Action Chips Row (Date/Time, Priority, List, Submit)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateLabel = LocalDate.ofEpochDay(effectiveEpochDay).let { d ->
                            when (d) {
                                LocalDate.now() -> s.today
                                LocalDate.now().plusDays(1) -> s.tomorrow
                                else -> "${d.dayOfMonth}/${d.monthValue}"
                            }
                        } + (effectiveTimeMinute?.let { " %02d:%02d".format(it / 60, it % 60) } ?: "")

                        GlassChip(
                            label = dateLabel,
                            selected = true,
                            leadingIcon = {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp), tint = tokens.accentColor)
                            },
                            onClick = { showDatePicker = true }
                        )

                        GlassChip(
                            label = when (effectivePriority) {
                                3 -> "! ${s.priorityHigh}"
                                2 -> "! ${s.priorityMedium}"
                                1 -> "! ${s.priorityLow}"
                                else -> s.priorityNone
                            },
                            selected = effectivePriority > 0,
                            accentColor = when (effectivePriority) {
                                3 -> Color(0xFFF43F5E)
                                2 -> Color(0xFFFBBF24)
                                1 -> Color(0xFF38BDF8)
                                else -> tokens.accentColor
                            },
                            onClick = {
                                manualPriority = (effectivePriority + 1) % 4
                            }
                        )

                        lists.forEach { list ->
                            GlassChip(
                                label = "${list.emoji} ${list.name}",
                                selected = selectedListId == list.id,
                                onClick = { selectedListId = list.id }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        GlassSurface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("quick_add_submit_btn"),
                            thickness = GlassThickness.THICK,
                            cornerRadius = 999.dp,
                            accentTint = tokens.accentColor,
                            onClick = { submitNow() },
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = s.save, tint = tokens.accentColor, modifier = Modifier.size(18.dp))
                                Text(
                                    text = s.save,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        GlassDateTimePickerDialog(
            initialEpochDay = effectiveEpochDay,
            initialTimeMinute = effectiveTimeMinute,
            initialDurationMinutes = effectiveDuration,
            onDismiss = { showDatePicker = false },
            onConfirm = { d, t, dur ->
                manualEpochDay = d
                manualTimeMinute = t
                manualDuration = dur
                showDatePicker = false
            }
        )
    }
}

/**
 * Full Task Detail Sheet:
 * Supports editing title, markdown description, unlimited reorderable subtasks,
 * priority, status, list, tags, date/time/duration, recurrence rules (including
 * "this occurrence" vs "all occurrences" when repeating), multiple reminders, and color.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailDialog(
    taskWithDetails: TaskWithDetails,
    lists: List<TaskListEntity>,
    onDismiss: () -> Unit,
    onSave: (
        updatedTask: TaskEntity,
        subtasks: List<String>,
        tags: List<String>,
        reminderOffsets: List<Int>,
        applyToThisOccurrenceOnly: Boolean
    ) -> Unit,
    onDelete: (Long) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val base = taskWithDetails.task

    var title by remember { mutableStateOf(base.title) }
    var description by remember { mutableStateOf(base.description) }
    var listId by remember { mutableLongStateOf(base.listId) }
    var priority by remember { mutableIntStateOf(base.priority) }
    var status by remember { mutableStateOf(base.status) }
    var isPinned by remember { mutableStateOf(base.isPinned) }
    var colorHex by remember { mutableStateOf(base.colorHex) }
    var dueDateEpochDay by remember { mutableStateOf(base.dueDateEpochDay) }
    var startTimeMinute by remember { mutableStateOf(base.startTimeMinute) }
    var durationMinutes by remember { mutableIntStateOf(base.durationMinutes) }

    val initialRule = remember(base.recurrenceJson) { RecurrenceEngine.parseRule(base.recurrenceJson) }
    var recurrenceFreq by remember { mutableStateOf(initialRule.frequency) }
    var recurrenceInterval by remember { mutableIntStateOf(initialRule.interval) }
    var repeatAfterCompletion by remember { mutableStateOf(initialRule.repeatAfterCompletion) }
    var editThisOccurrenceOnly by remember { mutableStateOf(false) }

    val subtasks = remember {
        mutableStateListOf<String>().apply {
            addAll(taskWithDetails.subtasks.sortedBy { it.sortOrder }.map { it.title })
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }

    var tagsText by remember {
        mutableStateOf(taskWithDetails.tags.joinToString(", ") { it.name })
    }

    val reminderOffsets = remember {
        mutableStateListOf<Int>().apply {
            addAll(taskWithDetails.reminders.map { it.offsetMinutesBefore })
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp),
            thickness = GlassThickness.THICK,
            cornerRadius = 28.dp,
            elevated = true,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DropletCheckbox(
                            checked = base.isCompleted,
                            onCheckedChange = {},
                            tint = parseColorHex(colorHex)
                        )
                        Text(
                            text = if (s.isVi) "Chi tiết công việc" else "Task Details",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Row {
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Pin",
                                tint = if (isPinned) tokens.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(base.id); onDismiss() }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = s.delete,
                                tint = Color(0xFFF43F5E)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = s.cancel)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (s.isVi) "Tiêu đề" else "Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (s.isVi) "Mô tả (Markdown)" else "Description (Markdown)") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                // Date, Time & Duration Button
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = GlassThickness.THIN,
                    cornerRadius = 16.dp,
                    onClick = { showDatePicker = true },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = tokens.accentColor)
                            val dateStr = dueDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: s.smartNoDate
                            val timeStr = startTimeMinute?.let { "%02d:%02d (${durationMinutes}m)".format(it / 60, it % 60) } ?: s.allDay
                            Text("$dateStr · $timeStr", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(if (s.isVi) "Đổi" else "Edit", color = tokens.accentColor, style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Priority & Status Row
                Text(if (s.isVi) "Độ ưu tiên & Trạng thái" else "Priority & Status", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to s.priorityNone, 1 to s.priorityLow, 2 to s.priorityMedium, 3 to s.priorityHigh).forEach { (p, label) ->
                        GlassChip(
                            label = label,
                            selected = priority == p,
                            onClick = { priority = p }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TaskStatus.TODO.name to s.statusTodo,
                        TaskStatus.DOING.name to s.statusDoing,
                        TaskStatus.DONE.name to s.statusDone
                    ).forEach { (st, label) ->
                        GlassChip(
                            label = label,
                            selected = status == st,
                            onClick = { status = st }
                        )
                    }
                }

                // Subtasks (Unlimited & Reorderable)
                Text(
                    text = if (s.isVi) "Việc nhỏ (${subtasks.size})" else "Subtasks (${subtasks.size})",
                    style = MaterialTheme.typography.labelLarge
                )
                subtasks.forEachIndexed { idx, item ->
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = GlassThickness.THIN,
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                if (idx > 0) {
                                    IconButton(
                                        onClick = {
                                            val tmp = subtasks[idx - 1]
                                            subtasks[idx - 1] = subtasks[idx]
                                            subtasks[idx] = tmp
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (idx < subtasks.lastIndex) {
                                    IconButton(
                                        onClick = {
                                            val tmp = subtasks[idx + 1]
                                            subtasks[idx + 1] = subtasks[idx]
                                            subtasks[idx] = tmp
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { subtasks.removeAt(idx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text(if (s.isVi) "Thêm việc nhỏ…" else "Add subtask…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    GlassChip(
                        label = "+",
                        selected = true,
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks.add(newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        }
                    )
                }

                // Recurrence Selector
                Text(if (s.isVi) "Lặp lại" else "Repeat / Recurrence", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        RecurrenceFrequency.NONE to (if (s.isVi) "Không" else "None"),
                        RecurrenceFrequency.DAILY to (if (s.isVi) "Hàng ngày" else "Daily"),
                        RecurrenceFrequency.WEEKLY to (if (s.isVi) "Hàng tuần" else "Weekly"),
                        RecurrenceFrequency.MONTHLY_DATE to (if (s.isVi) "Hàng tháng" else "Monthly"),
                        RecurrenceFrequency.MONTHLY_LAST_DAY to (if (s.isVi) "Cuối tháng" else "Month-End"),
                        RecurrenceFrequency.YEARLY to (if (s.isVi) "Hàng năm" else "Yearly"),
                        RecurrenceFrequency.HOURS_INTERVAL to (if (s.isVi) "Mỗi N giờ" else "Every N hrs")
                    ).forEach { (freq, label) ->
                        GlassChip(
                            label = label,
                            selected = recurrenceFreq == freq,
                            onClick = { recurrenceFreq = freq }
                        )
                    }
                }
                if (recurrenceFreq != RecurrenceFrequency.NONE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (s.isVi) "Lặp sau khi hoàn thành" else "Repeat after completion", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = repeatAfterCompletion, onCheckedChange = { repeatAfterCompletion = it })
                    }
                    if (base.recurrenceJson.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (s.isVi) "Áp dụng sửa đổi:" else "Edit scope:", style = MaterialTheme.typography.bodySmall)
                            GlassChip(
                                label = if (s.isVi) "Tất cả chuỗi" else "All occurrences",
                                selected = !editThisOccurrenceOnly,
                                onClick = { editThisOccurrenceOnly = false }
                            )
                            GlassChip(
                                label = if (s.isVi) "Chỉ lần này" else "This occurrence",
                                selected = editThisOccurrenceOnly,
                                onClick = { editThisOccurrenceOnly = true }
                            )
                        }
                    }
                }

                // Multiple Reminders Selector
                Text(if (s.isVi) "Nhắc nhở" else "Reminders", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to (if (s.isVi) "Đúng giờ" else "At time"),
                        5 to "5m",
                        10 to "10m",
                        30 to "30m",
                        60 to "1h",
                        1440 to (if (s.isVi) "1 ngày trước" else "1d before")
                    ).forEach { (offset, label) ->
                        val active = reminderOffsets.contains(offset)
                        GlassChip(
                            label = label,
                            selected = active,
                            onClick = {
                                if (active) reminderOffsets.remove(offset) else reminderOffsets.add(offset)
                            }
                        )
                    }
                }

                // List & Tags & Color
                Text(if (s.isVi) "Danh sách & Thẻ (#tag)" else "List & Tags", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lists.forEach { l ->
                        GlassChip(
                            label = "${l.emoji} ${l.name}",
                            selected = listId == l.id,
                            onClick = { listId = l.id }
                        )
                    }
                }

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text(if (s.isVi) "Thẻ (cách nhau dấu phẩy)" else "Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Color Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CuratedAccentPalettes.forEach { pal ->
                        val selected = colorHex.equals(pal.hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(pal.primary)
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = pal.hex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(s.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassChip(
                        label = s.save,
                        selected = true,
                        onClick = {
                            val newRule = RecurrenceRuleModel(
                                frequency = recurrenceFreq,
                                interval = recurrenceInterval,
                                daysOfWeek = initialRule.daysOfWeek.ifEmpty { listOf(LocalDate.now().dayOfWeek.value) },
                                repeatAfterCompletion = repeatAfterCompletion
                            )
                            val updatedTask = base.copy(
                                title = title.trim().ifBlank { base.title },
                                description = description.trim(),
                                listId = listId,
                                priority = priority,
                                status = status,
                                isCompleted = status == TaskStatus.DONE.name,
                                isPinned = isPinned,
                                colorHex = colorHex,
                                dueDateEpochDay = dueDateEpochDay,
                                startDateEpochDay = dueDateEpochDay,
                                startTimeMinute = startTimeMinute,
                                isAllDay = startTimeMinute == null,
                                durationMinutes = durationMinutes,
                                recurrenceJson = if (editThisOccurrenceOnly) "" else RecurrenceEngine.encodeRule(newRule)
                            )
                            val parsedTags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onSave(updatedTask, subtasks.toList(), parsedTags, reminderOffsets.toList(), editThisOccurrenceOnly)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        GlassDateTimePickerDialog(
            initialEpochDay = dueDateEpochDay,
            initialTimeMinute = startTimeMinute,
            initialDurationMinutes = durationMinutes,
            onDismiss = { showDatePicker = false },
            onConfirm = { d, t, dur ->
                dueDateEpochDay = d
                startTimeMinute = t
                durationMinutes = dur
                showDatePicker = false
            }
        )
    }
}
