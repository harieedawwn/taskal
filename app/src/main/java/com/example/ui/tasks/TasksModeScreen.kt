package com.example.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.GlassThickness
import com.example.data.SwipeActionType
import com.example.data.TagEntity
import com.example.data.TaskListEntity
import com.example.data.TaskWithDetails
import com.example.domain.RecurrenceEngine
import com.example.ui.glass.DropletCheckbox
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassEmptyState
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.theme.parseColorHex
import com.example.util.S
import java.time.LocalDate
import kotlin.math.roundToInt

enum class SmartListType {
    TODAY, TOMORROW, NEXT_7_DAYS, OVERDUE, ALL, NO_DATE, COMPLETED, TRASH
}

enum class TaskSortOption(val labelEn: String, val labelVi: String) {
    DUE_DATE("By Date", "Theo ngày"),
    PRIORITY("By Priority", "Theo ưu tiên"),
    LIST("By List", "Theo danh sách"),
    MANUAL("Manual Order", "Thứ tự thủ công")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksModeScreen(
    activeTasks: List<TaskWithDetails>,
    trashTasks: List<TaskWithDetails>,
    lists: List<TaskListEntity>,
    tags: List<TagEntity>,
    settings: AppSettings,
    onToggleComplete: (Long, Boolean) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onSwipeAction: (TaskWithDetails, SwipeActionType) -> Unit,
    onRestoreTask: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    onBulkComplete: (List<Long>) -> Unit,
    onBulkDelete: (List<Long>) -> Unit,
    onBulkSetToday: (List<Long>) -> Unit,
    onCreateList: (name: String, emoji: String, colorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val todayEpoch = remember { LocalDate.now().toEpochDay() }

    var selectedSmartList by remember { mutableStateOf(SmartListType.ALL) }
    var selectedUserListId by remember { mutableLongStateOf(-1L) }
    var selectedTagName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(TaskSortOption.DUE_DATE) }
    var showAddListDialog by remember { mutableStateOf(false) }

    val selectedTaskIds = remember { mutableStateListOf<Long>() }
    val inMultiSelect = selectedTaskIds.isNotEmpty()

    val displayedTasks by remember(
        activeTasks, trashTasks, selectedSmartList, selectedUserListId, selectedTagName, searchQuery, sortOption, settings.showCompletedTasks
    ) {
        derivedStateOf {
            val basePool = if (selectedSmartList == SmartListType.TRASH) trashTasks else activeTasks
            basePool.filter { item ->
                val t = item.task
                val matchesSearch = searchQuery.isBlank() ||
                    t.title.contains(searchQuery, ignoreCase = true) ||
                    t.description.contains(searchQuery, ignoreCase = true) ||
                    item.tags.any { it.name.contains(searchQuery, ignoreCase = true) }

                val matchesList = selectedUserListId == -1L || t.listId == selectedUserListId
                val matchesTag = selectedTagName == null || item.tags.any { it.name.equals(selectedTagName, ignoreCase = true) }

                val matchesSmart = if (selectedUserListId != -1L) {
                    settings.showCompletedTasks || !t.isCompleted
                } else {
                    when (selectedSmartList) {
                        SmartListType.TODAY -> t.dueDateEpochDay == todayEpoch && (settings.showCompletedTasks || !t.isCompleted)
                        SmartListType.TOMORROW -> t.dueDateEpochDay == todayEpoch + 1 && (settings.showCompletedTasks || !t.isCompleted)
                        SmartListType.NEXT_7_DAYS -> t.dueDateEpochDay != null && t.dueDateEpochDay in todayEpoch..(todayEpoch + 7) && (settings.showCompletedTasks || !t.isCompleted)
                        SmartListType.OVERDUE -> t.dueDateEpochDay != null && t.dueDateEpochDay < todayEpoch && !t.isCompleted
                        SmartListType.ALL -> settings.showCompletedTasks || !t.isCompleted
                        SmartListType.NO_DATE -> t.dueDateEpochDay == null && !t.isCompleted
                        SmartListType.COMPLETED -> t.isCompleted
                        SmartListType.TRASH -> true
                    }
                }
                matchesSearch && matchesList && matchesTag && matchesSmart
            }.let { filtered ->
                when (sortOption) {
                    TaskSortOption.DUE_DATE -> filtered.sortedWith(
                        compareBy<TaskWithDetails> { it.task.isCompleted }
                            .thenByDescending { it.task.isPinned }
                            .thenBy { it.task.dueDateEpochDay ?: Long.MAX_VALUE }
                            .thenBy { it.task.startTimeMinute ?: 1440 }
                    )
                    TaskSortOption.PRIORITY -> filtered.sortedWith(
                        compareBy<TaskWithDetails> { it.task.isCompleted }
                            .thenByDescending { it.task.priority }
                            .thenBy { it.task.dueDateEpochDay ?: Long.MAX_VALUE }
                    )
                    TaskSortOption.LIST -> filtered.sortedBy { it.task.listId }
                    TaskSortOption.MANUAL -> filtered.sortedBy { it.task.sortOrder }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Search + Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.modeTasks,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(
                    label = if (s.isVi) sortOption.labelVi else sortOption.labelEn,
                    leadingIcon = { Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        val nextIdx = (TaskSortOption.entries.indexOf(sortOption) + 1) % TaskSortOption.entries.size
                        sortOption = TaskSortOption.entries[nextIdx]
                    }
                )
                GlassChip(
                    label = if (s.isVi) "+ Danh sách" else "+ List",
                    onClick = { showAddListDialog = true }
                )
            }
        }

        // Glass Search Bar
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.THIN,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(s.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Smart Lists Horizontal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SmartListType.ALL to s.smartAll,
                SmartListType.TODAY to s.smartToday,
                SmartListType.TOMORROW to s.smartTomorrow,
                SmartListType.NEXT_7_DAYS to s.smartNext7,
                SmartListType.OVERDUE to s.smartOverdue,
                SmartListType.NO_DATE to s.smartNoDate,
                SmartListType.COMPLETED to s.smartCompleted,
                SmartListType.TRASH to s.smartTrash
            ).forEach { (smart, label) ->
                GlassChip(
                    label = label,
                    selected = selectedUserListId == -1L && selectedSmartList == smart,
                    onClick = {
                        selectedUserListId = -1L
                        selectedSmartList = smart
                    }
                )
            }
        }

        // User Lists + Tags Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lists.forEach { list ->
                GlassChip(
                    label = "${list.emoji} ${list.name}",
                    selected = selectedUserListId == list.id,
                    accentColor = parseColorHex(list.colorHex),
                    onClick = {
                        selectedUserListId = if (selectedUserListId == list.id) -1L else list.id
                    }
                )
            }
            tags.forEach { tag ->
                GlassChip(
                    label = "#${tag.name}",
                    selected = selectedTagName == tag.name,
                    onClick = {
                        selectedTagName = if (selectedTagName == tag.name) null else tag.name
                    }
                )
            }
        }

        // Multi-Select Bulk Action Bar
        AnimatedVisibility(visible = inMultiSelect) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                thickness = GlassThickness.THICK,
                cornerRadius = 18.dp,
                accentTint = tokens.accentColor,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedTaskIds.size} selected",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = {
                            onBulkComplete(selectedTaskIds.toList())
                            selectedTaskIds.clear()
                        }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Complete selected")
                        }
                        IconButton(onClick = {
                            onBulkSetToday(selectedTaskIds.toList())
                            selectedTaskIds.clear()
                        }) {
                            Icon(Icons.Filled.Today, contentDescription = "Move to Today")
                        }
                        IconButton(onClick = {
                            onBulkDelete(selectedTaskIds.toList())
                            selectedTaskIds.clear()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = Color(0xFFF43F5E))
                        }
                        TextButton(onClick = { selectedTaskIds.clear() }) {
                            Text(s.cancel)
                        }
                    }
                }
            }
        }

        if (selectedSmartList == SmartListType.TRASH && trashTasks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (s.isVi) "Tự động xóa vĩnh viễn sau 30 ngày" else "Items in Trash are auto-purged after 30 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlassChip(
                    label = if (s.isVi) "Dọn sạch thùng rác" else "Empty Trash",
                    accentColor = Color(0xFFF43F5E),
                    leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = onEmptyTrash
                )
            }
        }

        if (displayedTasks.isEmpty()) {
            GlassEmptyState(
                message = if (searchQuery.isNotBlank()) s.emptySearch else s.emptyTasks,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("tasks_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(items = displayedTasks, key = { it.task.id }) { item ->
                    val isSelected = selectedTaskIds.contains(item.task.id)
                    SwipeableGlassTaskRow(
                        item = item,
                        lists = lists,
                        isSelected = isSelected,
                        isTrash = selectedSmartList == SmartListType.TRASH,
                        onToggleComplete = { completed -> onToggleComplete(item.task.id, completed) },
                        onClick = {
                            if (inMultiSelect) {
                                if (isSelected) selectedTaskIds.remove(item.task.id) else selectedTaskIds.add(item.task.id)
                            } else {
                                onOpenTaskDetail(item)
                            }
                        },
                        onLongClick = {
                            if (isSelected) selectedTaskIds.remove(item.task.id) else selectedTaskIds.add(item.task.id)
                        },
                        onSwipeRight = { onSwipeAction(item, settings.swipeRightAction) },
                        onSwipeLeft = { onSwipeAction(item, settings.swipeLeftAction) },
                        onRestore = { onRestoreTask(item.task.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showAddListDialog) {
        var newListName by remember { mutableStateOf("") }
        var newListEmoji by remember { mutableStateOf("🚀") }
        AlertDialog(
            onDismissRequest = { showAddListDialog = false },
            title = { Text(if (s.isVi) "Tạo danh sách mới" else "New Task List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newListEmoji,
                        onValueChange = { newListEmoji = it },
                        label = { Text("Emoji") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text(if (s.isVi) "Tên danh sách" else "List name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newListName.isNotBlank()) {
                        onCreateList(newListName, newListEmoji, "#38BDF8")
                        showAddListDialog = false
                    }
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddListDialog = false }) { Text(s.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableGlassTaskRow(
    item: TaskWithDetails,
    lists: List<TaskListEntity>,
    isSelected: Boolean,
    isTrash: Boolean,
    onToggleComplete: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    val task = item.task
    val taskColor = parseColorHex(task.colorHex, tokens.accentColor)
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val priorityColor = when (task.priority) {
        3 -> Color(0xFFF43F5E)
        2 -> Color(0xFFFBBF24)
        1 -> Color(0xFF38BDF8)
        else -> Color.Transparent
    }

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .pointerInput(task.id, isTrash) {
                if (!isTrash) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetX > 140f) {
                                onSwipeRight()
                            } else if (dragOffsetX < -140f) {
                                onSwipeLeft()
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = { dragOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-220f, 220f)
                        }
                    )
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        thickness = if (isSelected || task.isPinned) GlassThickness.THICK else GlassThickness.REGULAR,
        accentTint = if (isSelected) tokens.accentColor else taskColor,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTrash) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Restore", tint = tokens.accentColor)
                }
            } else {
                DropletCheckbox(
                    checked = task.isCompleted,
                    onCheckedChange = onToggleComplete,
                    tint = taskColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (task.isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = tokens.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (task.priority > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metadata badges row (Date, Time, Subtasks count, Recurrence, Tags)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val listObj = lists.firstOrNull { it.id == task.listId }
                    if (listObj != null) {
                        Text(
                            text = "${listObj.emoji} ${listObj.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    task.dueDateEpochDay?.let { epoch ->
                        val d = LocalDate.ofEpochDay(epoch)
                        val isOverdue = epoch < LocalDate.now().toEpochDay() && !task.isCompleted
                        val timeSuffix = task.startTimeMinute?.let { " %02d:%02d".format(it / 60, it % 60) }.orEmpty()
                        Text(
                            text = "${d.dayOfMonth}/${d.monthValue}$timeSuffix",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) Color(0xFFF43F5E) else tokens.accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (item.subtasks.isNotEmpty()) {
                        val doneCount = item.subtasks.count { it.isCompleted }
                        Text(
                            text = "☑ $doneCount/${item.subtasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.recurrenceJson.isNotBlank()) {
                        Icon(
                            Icons.Filled.Repeat,
                            contentDescription = "Repeating",
                            tint = tokens.secondaryAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    item.tags.take(2).forEach { tag ->
                        Text(
                            text = "#${tag.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.secondaryAccent
                        )
                    }
                }
            }
        }
    }
}
