package com.example.ui.modes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.GlassThickness
import com.example.data.TaskEntity
import com.example.data.TaskListEntity
import com.example.data.TaskStatus
import com.example.data.TaskWithDetails
import com.example.ui.glass.DropletCheckbox
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.theme.parseColorHex
import com.example.util.S
import java.time.LocalDate
import kotlin.math.roundToInt

enum class KanbanGroupBy(val labelEn: String, val labelVi: String) {
    STATUS("By Status", "Theo trạng thái"),
    LIST("By List", "Theo danh sách"),
    PRIORITY("By Priority", "Theo ưu tiên")
}

private data class KanbanColumnSpec(
    val key: String,
    val title: String,
    val accent: Color,
    val items: List<TaskWithDetails>
)

@Composable
fun BoardModeScreen(
    tasks: List<TaskWithDetails>,
    lists: List<TaskListEntity>,
    onUpdateTask: (TaskEntity) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    var groupBy by remember { mutableStateOf(KanbanGroupBy.STATUS) }

    val columns = remember(tasks, lists, groupBy, s.isVi) {
        when (groupBy) {
            KanbanGroupBy.STATUS -> listOf(
                KanbanColumnSpec(
                    key = TaskStatus.TODO.name,
                    title = s.statusTodo,
                    accent = Color(0xFF38BDF8),
                    items = tasks.filter { it.task.status == TaskStatus.TODO.name && !it.task.isCompleted }
                ),
                KanbanColumnSpec(
                    key = TaskStatus.DOING.name,
                    title = s.statusDoing,
                    accent = Color(0xFFFBBF24),
                    items = tasks.filter { it.task.status == TaskStatus.DOING.name && !it.task.isCompleted }
                ),
                KanbanColumnSpec(
                    key = TaskStatus.DONE.name,
                    title = s.statusDone,
                    accent = Color(0xFF34D399),
                    items = tasks.filter { it.task.status == TaskStatus.DONE.name || it.task.isCompleted }
                )
            )
            KanbanGroupBy.PRIORITY -> listOf(
                KanbanColumnSpec("3", s.priorityHigh, Color(0xFFF43F5E), tasks.filter { it.task.priority == 3 }),
                KanbanColumnSpec("2", s.priorityMedium, Color(0xFFFBBF24), tasks.filter { it.task.priority == 2 }),
                KanbanColumnSpec("1", s.priorityLow, Color(0xFF38BDF8), tasks.filter { it.task.priority == 1 }),
                KanbanColumnSpec("0", s.priorityNone, Color(0xFF94A3B8), tasks.filter { it.task.priority == 0 })
            )
            KanbanGroupBy.LIST -> lists.map { list ->
                KanbanColumnSpec(
                    key = list.id.toString(),
                    title = "${list.emoji} ${list.name}",
                    accent = parseColorHex(list.colorHex, tokens.accentColor),
                    items = tasks.filter { it.task.listId == list.id }
                )
            }
        }
    }

    fun moveTaskToColumn(task: TaskEntity, targetKey: String) {
        when (groupBy) {
            KanbanGroupBy.STATUS -> {
                val isDone = targetKey == TaskStatus.DONE.name
                onUpdateTask(
                    task.copy(
                        status = targetKey,
                        isCompleted = isDone,
                        completedAt = if (isDone) System.currentTimeMillis() else null
                    )
                )
            }
            KanbanGroupBy.PRIORITY -> {
                val p = targetKey.toIntOrNull() ?: 0
                onUpdateTask(task.copy(priority = p))
            }
            KanbanGroupBy.LIST -> {
                val lId = targetKey.toLongOrNull() ?: task.listId
                onUpdateTask(task.copy(listId = lId))
            }
        }
    }

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
            Text(
                text = s.modeBoard,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KanbanGroupBy.entries.forEach { mode ->
                    GlassChip(
                        label = if (s.isVi) mode.labelVi else mode.labelEn,
                        selected = groupBy == mode,
                        onClick = { groupBy = mode }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 105.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            columns.forEachIndexed { colIndex, col ->
                GlassSurface(
                    modifier = Modifier
                        .width(285.dp)
                        .fillMaxHeight(),
                    thickness = GlassThickness.REGULAR,
                    cornerRadius = 24.dp,
                    accentTint = col.accent,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(col.accent)
                                )
                                Text(
                                    text = col.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${col.items.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = col.accent
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(col.items, key = { it.task.id }) { item ->
                                var dragX by remember { mutableFloatStateOf(0f) }
                                GlassSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(dragX.roundToInt(), 0) }
                                        .pointerInput(item.task.id, colIndex, columns.size) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    if (dragX > 95f && colIndex < columns.lastIndex) {
                                                        moveTaskToColumn(item.task, columns[colIndex + 1].key)
                                                    } else if (dragX < -95f && colIndex > 0) {
                                                        moveTaskToColumn(item.task, columns[colIndex - 1].key)
                                                    }
                                                    dragX = 0f
                                                },
                                                onDragCancel = { dragX = 0f },
                                                onDrag = { _, dragAmount ->
                                                    dragX = (dragX + dragAmount.x).coerceIn(-180f, 180f)
                                                }
                                            )
                                        },
                                    thickness = GlassThickness.THICK,
                                    cornerRadius = 16.dp,
                                    accentTint = parseColorHex(item.task.colorHex, col.accent),
                                    onClick = { onOpenTaskDetail(item) },
                                    contentPadding = PaddingValues(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = item.task.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            DropletCheckbox(
                                                checked = item.task.isCompleted,
                                                onCheckedChange = { onToggleComplete(item.task.id, it) },
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (colIndex > 0) moveTaskToColumn(item.task, columns[colIndex - 1].key)
                                                },
                                                enabled = colIndex > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move left", modifier = Modifier.size(15.dp))
                                            }
                                            Text(
                                                text = if (s.isVi) "Kéo ngang để chuyển cột" else "Drag horizontally",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (colIndex < columns.lastIndex) moveTaskToColumn(item.task, columns[colIndex + 1].key)
                                                },
                                                enabled = colIndex < columns.lastIndex,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move right", modifier = Modifier.size(15.dp))
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
    }
}

/**
 * EISENHOWER 2x2 MATRIX:
 * Q1: Urgent & Important (Priority >= 2, Due Today/Tomorrow/Overdue)
 * Q2: Not Urgent & Important (Priority >= 2, Due later or No Date)
 * Q3: Urgent & Less Important (Priority < 2, Due Today/Tomorrow/Overdue)
 * Q4: Neither (Priority < 2, Due later or No Date)
 * Dragging or tapping quadrant switcher moves tasks across quadrants and updates Priority + Due Date.
 */
@Composable
fun MatrixModeScreen(
    tasks: List<TaskWithDetails>,
    onUpdateTask: (TaskEntity) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    val urgentCutoff = todayEpoch + 1

    val active = remember(tasks) { tasks.filter { !it.task.isCompleted } }
    val q1 = remember(active, urgentCutoff) {
        active.filter { it.task.priority >= 2 && (it.task.dueDateEpochDay != null && it.task.dueDateEpochDay <= urgentCutoff) }
    }
    val q2 = remember(active, urgentCutoff) {
        active.filter { it.task.priority >= 2 && (it.task.dueDateEpochDay == null || it.task.dueDateEpochDay > urgentCutoff) }
    }
    val q3 = remember(active, urgentCutoff) {
        active.filter { it.task.priority < 2 && (it.task.dueDateEpochDay != null && it.task.dueDateEpochDay <= urgentCutoff) }
    }
    val q4 = remember(active, urgentCutoff) {
        active.filter { it.task.priority < 2 && (it.task.dueDateEpochDay == null || it.task.dueDateEpochDay > urgentCutoff) }
    }

    fun assignQuadrant(task: TaskEntity, targetQ: Int) {
        when (targetQ) {
            1 -> onUpdateTask(task.copy(priority = 3, dueDateEpochDay = todayEpoch))
            2 -> onUpdateTask(task.copy(priority = 2, dueDateEpochDay = todayEpoch + 4))
            3 -> onUpdateTask(task.copy(priority = 1, dueDateEpochDay = todayEpoch))
            4 -> onUpdateTask(task.copy(priority = 0, dueDateEpochDay = todayEpoch + 7))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 105.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = s.modeMatrix,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MatrixQuadrantCard(
                quadrantIndex = 1,
                title = if (s.isVi) "I · Khẩn cấp & Quan trọng" else "I · Do First (Urgent & Important)",
                accent = Color(0xFFF43F5E),
                items = q1,
                onMoveToQuadrant = ::assignQuadrant,
                onToggleComplete = onToggleComplete,
                onOpenTaskDetail = onOpenTaskDetail,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            MatrixQuadrantCard(
                quadrantIndex = 2,
                title = if (s.isVi) "II · Lên lịch (Quan trọng)" else "II · Schedule (Important)",
                accent = Color(0xFF38BDF8),
                items = q2,
                onMoveToQuadrant = ::assignQuadrant,
                onToggleComplete = onToggleComplete,
                onOpenTaskDetail = onOpenTaskDetail,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MatrixQuadrantCard(
                quadrantIndex = 3,
                title = if (s.isVi) "III · Ủy thác (Khẩn cấp)" else "III · Delegate (Urgent)",
                accent = Color(0xFFFBBF24),
                items = q3,
                onMoveToQuadrant = ::assignQuadrant,
                onToggleComplete = onToggleComplete,
                onOpenTaskDetail = onOpenTaskDetail,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            MatrixQuadrantCard(
                quadrantIndex = 4,
                title = if (s.isVi) "IV · Giảm thiểu / Để sau" else "IV · Eliminate / Later",
                accent = Color(0xFF94A3B8),
                items = q4,
                onMoveToQuadrant = ::assignQuadrant,
                onToggleComplete = onToggleComplete,
                onOpenTaskDetail = onOpenTaskDetail,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun MatrixQuadrantCard(
    quadrantIndex: Int,
    title: String,
    accent: Color,
    items: List<TaskWithDetails>,
    onMoveToQuadrant: (TaskEntity, Int) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        thickness = GlassThickness.REGULAR,
        cornerRadius = 22.dp,
        accentTint = accent,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${items.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.task.id }) { item ->
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = GlassThickness.THIN,
                        cornerRadius = 14.dp,
                        onClick = { onOpenTaskDetail(item) },
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.task.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                DropletCheckbox(
                                    checked = item.task.isCompleted,
                                    onCheckedChange = { onToggleComplete(item.task.id, it) },
                                    tint = accent,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (q in 1..4) {
                                    if (q != quadrantIndex) {
                                        Text(
                                            text = "→Q$q",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent,
                                            modifier = Modifier
                                                .clickable { onMoveToQuadrant(item.task, q) }
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
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
