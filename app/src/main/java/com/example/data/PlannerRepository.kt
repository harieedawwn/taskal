package com.example.data

import com.example.domain.RecurrenceEngine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PlannerRepository(
    private val dao: TaskalDao,
    private val onDataChanged: suspend () -> Unit = {}
) {
    val activeTasksFlow: Flow<List<TaskWithDetails>> = dao.observeActiveTasksWithDetails()
    val trashTasksFlow: Flow<List<TaskWithDetails>> = dao.observeTrashTasksWithDetails()
    val listsFlow: Flow<List<TaskListEntity>> = dao.observeLists()
    val tagsFlow: Flow<List<TagEntity>> = dao.observeTags()
    val notesFlow: Flow<List<NoteEntity>> = dao.observeActiveNotes()
    val habitsFlow: Flow<List<HabitEntity>> = dao.observeHabits()
    val habitLogsFlow: Flow<List<HabitLogEntity>> = dao.observeHabitLogs()
    val focusSessionsFlow: Flow<List<FocusSessionEntity>> = dao.observeFocusSessions()
    val dashboardWidgetsFlow: Flow<List<DashboardWidgetEntity>> = dao.observeDashboardWidgets()

    suspend fun ensureDefaultsAndPurgeTrash() {
        // Purge trash older than 30 days
        val cutoff = System.currentTimeMillis() - 30L * 24L * 3600L * 1000L
        dao.purgeOldTrash(cutoff)

        val lists = dao.getAllListsRaw()
        if (lists.isEmpty()) {
            dao.insertList(TaskListEntity(id = 1L, name = "Inbox", colorHex = "#38BDF8", emoji = "📥", sortOrder = 0, isDefault = true))
            dao.insertList(TaskListEntity(id = 2L, name = "Work", colorHex = "#818CF8", emoji = "💼", sortOrder = 1))
            dao.insertList(TaskListEntity(id = 3L, name = "Personal", colorHex = "#34D399", emoji = "🌿", sortOrder = 2))
            dao.insertList(TaskListEntity(id = 4L, name = "Health", colorHex = "#FB7185", emoji = "🏃", sortOrder = 3))
        }

        val widgets = dao.getAllDashboardWidgetsRaw()
        if (widgets.isEmpty()) {
            dao.insertDashboardWidget(DashboardWidgetEntity(page = 0, widgetType = "PROGRESS_RING", spanX = 2, spanY = 2, sortOrder = 0))
            dao.insertDashboardWidget(DashboardWidgetEntity(page = 0, widgetType = "FOCUS_TIMER", spanX = 2, spanY = 2, sortOrder = 1))
            dao.insertDashboardWidget(DashboardWidgetEntity(page = 0, widgetType = "TODAY_LIST", spanX = 4, spanY = 2, sortOrder = 2))
            dao.insertDashboardWidget(DashboardWidgetEntity(page = 0, widgetType = "HABIT_STREAKS", spanX = 4, spanY = 1, sortOrder = 3))
            dao.insertDashboardWidget(DashboardWidgetEntity(page = 1, widgetType = "MINI_MONTH", spanX = 4, spanY = 2, sortOrder = 0))
            dao.insertDashboardWidget(DashboardWidgetEntity(page = 1, widgetType = "PINNED_NOTE", spanX = 4, spanY = 2, sortOrder = 1))
        }
    }

    suspend fun saveTaskWithDetails(
        task: TaskEntity,
        subtasks: List<String>,
        tagNames: List<String>,
        reminderOffsetsMinutes: List<Int>
    ): Long {
        val now = System.currentTimeMillis()
        val taskId = if (task.id == 0L) {
            dao.insertTask(task.copy(createdAt = now, updatedAt = now))
        } else {
            dao.updateTask(task.copy(updatedAt = now))
            task.id
        }

        dao.deleteSubtasksForTask(taskId)
        subtasks.filter { it.isNotBlank() }.forEachIndexed { idx, title ->
            dao.insertSubtask(SubtaskEntity(taskId = taskId, title = title.trim(), isCompleted = false, sortOrder = idx))
        }

        dao.clearTagsForTask(taskId)
        for (rawTag in tagNames.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct()) {
            val existing = dao.getTagByName(rawTag)
            val tagId = existing?.id ?: dao.insertTag(TagEntity(name = rawTag))
            dao.insertTaskTagCrossRef(TaskTagCrossRef(taskId = taskId, tagId = tagId))
        }

        dao.clearRemindersForTask(taskId)
        val epochDay = task.dueDateEpochDay
        val startMin = task.startTimeMinute ?: 540
        if (epochDay != null) {
            for (offset in reminderOffsetsMinutes.distinct()) {
                if (offset >= 0) {
                    val triggerMillis = RecurrenceEngine.toInstantMillisDstSafe(
                        epochDay = epochDay,
                        minuteOfDay = startMin,
                        offsetMinutesBefore = offset
                    )
                    dao.insertReminder(
                        ReminderEntity(
                            taskId = taskId,
                            offsetMinutesBefore = offset,
                            triggerEpochMillis = triggerMillis,
                            isFired = triggerMillis <= now
                        )
                    )
                }
            }
        }
        onDataChanged()
        return taskId
    }

    /**
     * Completes or uncompletes a task. If completing a repeating task, creates the next occurrence!
     */
    suspend fun toggleTaskCompleted(taskId: Long, completed: Boolean): Long? {
        val details = dao.getTaskWithDetailsById(taskId) ?: return null
        val task = details.task
        val now = System.currentTimeMillis()
        val updated = task.copy(
            isCompleted = completed,
            completedAt = if (completed) now else null,
            status = if (completed) TaskStatus.DONE.name else TaskStatus.TODO.name,
            updatedAt = now
        )
        dao.updateTask(updated)

        var spawnedNextId: Long? = null
        if (completed && task.recurrenceJson.isNotBlank()) {
            val next = RecurrenceEngine.computeNextOccurrence(task)
            if (next != null) {
                val nextTask = task.copy(
                    id = 0L,
                    dueDateEpochDay = next.nextDueEpochDay,
                    startDateEpochDay = next.nextDueEpochDay,
                    startTimeMinute = next.nextStartTimeMinute,
                    recurrenceJson = RecurrenceEngine.encodeRule(next.updatedRule),
                    isCompleted = false,
                    completedAt = null,
                    status = TaskStatus.TODO.name,
                    createdAt = now,
                    updatedAt = now
                )
                spawnedNextId = saveTaskWithDetails(
                    task = nextTask,
                    subtasks = details.subtasks.map { it.title },
                    tagNames = details.tags.map { it.name },
                    reminderOffsetsMinutes = details.reminders.map { it.offsetMinutesBefore }
                )
            }
        }
        onDataChanged()
        return spawnedNextId
    }

    suspend fun updateTaskDirect(task: TaskEntity) {
        dao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        onDataChanged()
    }

    suspend fun toggleSubtask(subtask: SubtaskEntity) {
        dao.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
        onDataChanged()
    }

    suspend fun addSubtask(taskId: Long, title: String, sortOrder: Int) {
        if (title.isBlank()) return
        dao.insertSubtask(SubtaskEntity(taskId = taskId, title = title.trim(), sortOrder = sortOrder))
        onDataChanged()
    }

    suspend fun deleteSubtask(subtaskId: Long) {
        dao.deleteSubtask(subtaskId)
        onDataChanged()
    }

    suspend fun softDeleteTask(taskId: Long) {
        val details = dao.getTaskWithDetailsById(taskId) ?: return
        val now = System.currentTimeMillis()
        dao.updateTask(details.task.copy(isDeleted = true, deletedAt = now, updatedAt = now))
        onDataChanged()
    }

    suspend fun restoreTask(taskId: Long) {
        val details = dao.getTaskWithDetailsById(taskId) ?: return
        dao.updateTask(details.task.copy(isDeleted = false, deletedAt = null, updatedAt = System.currentTimeMillis()))
        onDataChanged()
    }

    suspend fun hardDeleteTask(taskId: Long) {
        dao.hardDeleteTask(taskId)
        onDataChanged()
    }

    suspend fun emptyTrash() {
        dao.emptyTrash()
        onDataChanged()
    }

    suspend fun rescheduleTask(taskId: Long, newEpochDay: Long?, newStartMinute: Int?, newDurationMinutes: Int? = null) {
        val details = dao.getTaskWithDetailsById(taskId) ?: return
        val updated = details.task.copy(
            dueDateEpochDay = newEpochDay,
            startDateEpochDay = newEpochDay,
            startTimeMinute = newStartMinute,
            isAllDay = newStartMinute == null,
            durationMinutes = newDurationMinutes ?: details.task.durationMinutes,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateTask(updated)
        onDataChanged()
    }

    suspend fun addList(name: String, emoji: String, colorHex: String) {
        if (name.isBlank()) return
        val count = dao.getAllListsRaw().size
        dao.insertList(TaskListEntity(name = name.trim(), emoji = emoji.ifBlank { "📋" }, colorHex = colorHex, sortOrder = count))
        onDataChanged()
    }

    suspend fun deleteList(listId: Long) {
        dao.deleteList(listId)
        onDataChanged()
    }

    suspend fun saveNote(note: NoteEntity): Long {
        val now = System.currentTimeMillis()
        val id = if (note.id == 0L) {
            dao.insertNote(note.copy(createdAt = now, updatedAt = now))
        } else {
            dao.updateNote(note.copy(updatedAt = now))
            note.id
        }
        onDataChanged()
        return id
    }

    suspend fun saveDayNote(epochDay: Long, content: String) {
        val existing = dao.getDayNote(epochDay)
        val now = System.currentTimeMillis()
        if (existing != null) {
            if (content.isBlank()) {
                dao.hardDeleteNote(existing.id)
            } else {
                dao.updateNote(existing.copy(contentMarkdown = content, updatedAt = now))
            }
        } else if (content.isNotBlank()) {
            val dateStr = LocalDate.ofEpochDay(epochDay).toString()
            dao.insertNote(
                NoteEntity(
                    title = "Day Note · $dateStr",
                    contentMarkdown = content,
                    attachedDateEpochDay = epochDay,
                    isDayNote = true,
                    colorHex = "#38BDF8",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        onDataChanged()
    }

    suspend fun deleteNote(noteId: Long) {
        dao.hardDeleteNote(noteId)
        onDataChanged()
    }

    suspend fun saveHabit(habit: HabitEntity) {
        if (habit.id == 0L) dao.insertHabit(habit) else dao.updateHabit(habit)
        onDataChanged()
    }

    suspend fun deleteHabit(habitId: Long) {
        dao.deleteHabit(habitId)
        onDataChanged()
    }

    suspend fun toggleHabitCheckIn(habit: HabitEntity, epochDay: Long, existingLogs: List<HabitLogEntity>) {
        val alreadyDone = existingLogs.any { it.habitId == habit.id && it.epochDay == epochDay }
        if (alreadyDone) {
            dao.deleteHabitLog(habit.id, epochDay)
        } else {
            dao.insertHabitLog(HabitLogEntity(habitId = habit.id, epochDay = epochDay))
        }
        // Recompute streak
        val allLogs = dao.getAllHabitLogsRaw().filter { it.habitId == habit.id }.map { it.epochDay }.toSet()
        var streak = 0
        var cursor = LocalDate.now().toEpochDay()
        if (!allLogs.contains(cursor)) {
            cursor -= 1L
        }
        while (allLogs.contains(cursor)) {
            streak++
            cursor -= 1L
        }
        val best = maxOf(habit.bestStreak, streak)
        dao.updateHabit(habit.copy(currentStreak = streak, bestStreak = best))
        onDataChanged()
    }

    suspend fun logFocusSession(session: FocusSessionEntity) {
        dao.insertFocusSession(session)
        onDataChanged()
    }

    suspend fun saveDashboardWidget(widget: DashboardWidgetEntity) {
        if (widget.id == 0L) dao.insertDashboardWidget(widget) else dao.updateDashboardWidget(widget)
        onDataChanged()
    }

    suspend fun deleteDashboardWidget(widgetId: Long) {
        dao.deleteDashboardWidget(widgetId)
        onDataChanged()
    }

    suspend fun wipeAllData() {
        dao.clearAllCrossRefs()
        dao.clearAllReminders()
        dao.clearAllSubtasks()
        dao.clearAllTasks()
        dao.clearAllTags()
        dao.clearAllLists()
        dao.clearAllNotes()
        dao.clearAllHabitLogs()
        dao.clearAllHabits()
        dao.clearAllFocusSessions()
        dao.clearAllDashboardWidgets()
        ensureDefaultsAndPurgeTrash()
        onDataChanged()
    }

    suspend fun loadSampleData(isVi: Boolean = false) {
        ensureDefaultsAndPurgeTrash()
        val today = LocalDate.now().toEpochDay()
        val tomorrow = today + 1
        val yesterday = today - 1

        // Sample Tasks
        saveTaskWithDetails(
            task = TaskEntity(
                title = if (isVi) "Họp thiết kế sản phẩm Quý 4" else "Q4 Product Design Sync",
                description = if (isVi) "- Chốt hệ thống Liquid Glass\n- Kiểm tra hiệu năng 120fps" else "- Finalize Liquid Glass design tokens\n- Verify 120fps scroll performance",
                listId = 2L,
                priority = 3,
                dueDateEpochDay = today,
                startDateEpochDay = today,
                startTimeMinute = 9 * 60 + 30,
                durationMinutes = 60,
                isAllDay = false,
                colorHex = "#38BDF8",
                isPinned = true,
                status = TaskStatus.DOING.name
            ),
            subtasks = if (isVi) listOf("Chuẩn bị slide demo", "Gửi ghi chú cuộc họp") else listOf("Prepare interactive prototype", "Send meeting notes"),
            tagNames = listOf("work", "design"),
            reminderOffsetsMinutes = listOf(10)
        )

        saveTaskWithDetails(
            task = TaskEntity(
                title = if (isVi) "Tập gym & giãn cơ buổi chiều" else "Strength training & mobility session",
                description = if (isVi) "Lịch tập thân trên 45 phút" else "45-minute upper body + core routine",
                listId = 4L,
                priority = 2,
                dueDateEpochDay = today,
                startDateEpochDay = today,
                startTimeMinute = 17 * 60 + 30,
                durationMinutes = 45,
                isAllDay = false,
                recurrenceJson = RecurrenceEngine.encodeRule(
                    RecurrenceRuleModel(frequency = RecurrenceFrequency.WEEKLY, interval = 1, daysOfWeek = listOf(1, 3, 5))
                ),
                colorHex = "#FB7185",
                status = TaskStatus.TODO.name
            ),
            subtasks = if (isVi) listOf("Khởi động 10 phút", "Uống điện giải") else listOf("10m warm-up", "Electrolyte hydration"),
            tagNames = listOf("fitness"),
            reminderOffsetsMinutes = listOf(30)
        )

        saveTaskWithDetails(
            task = TaskEntity(
                title = if (isVi) "Đọc 20 trang sách kiến trúc phần mềm" else "Read 20 pages of Clean Architecture",
                listId = 3L,
                priority = 1,
                dueDateEpochDay = today,
                startDateEpochDay = today,
                startTimeMinute = 21 * 60,
                durationMinutes = 30,
                isAllDay = false,
                recurrenceJson = RecurrenceEngine.encodeRule(
                    RecurrenceRuleModel(frequency = RecurrenceFrequency.DAILY, interval = 1)
                ),
                colorHex = "#34D399",
                status = TaskStatus.TODO.name
            ),
            subtasks = emptyList(),
            tagNames = listOf("reading"),
            reminderOffsetsMinutes = emptyList()
        )

        saveTaskWithDetails(
            task = TaskEntity(
                title = if (isVi) "Nộp báo cáo tài chính tháng" else "Submit monthly expense summary",
                listId = 2L,
                priority = 3,
                dueDateEpochDay = tomorrow,
                startDateEpochDay = tomorrow,
                startTimeMinute = 14 * 60,
                durationMinutes = 45,
                isAllDay = false,
                colorHex = "#FBBF24",
                status = TaskStatus.TODO.name
            ),
            subtasks = if (isVi) listOf("Xuất hóa đơn PDF", "Đối chiếu ngân sách") else listOf("Export PDF receipts", "Reconcile budget sheet"),
            tagNames = listOf("finance", "work"),
            reminderOffsetsMinutes = listOf(60)
        )

        saveTaskWithDetails(
            task = TaskEntity(
                title = if (isVi) "Gọi điện hỏi thăm gia đình" else "Call family & plan weekend dinner",
                listId = 3L,
                priority = 2,
                dueDateEpochDay = yesterday,
                startDateEpochDay = yesterday,
                isAllDay = true,
                colorHex = "#C084FC",
                isCompleted = true,
                completedAt = System.currentTimeMillis() - 3600_000L,
                status = TaskStatus.DONE.name
            ),
            subtasks = emptyList(),
            tagNames = listOf("family"),
            reminderOffsetsMinutes = emptyList()
        )

        // Sample Notes & Day Note
        saveDayNote(
            epochDay = today,
            content = if (isVi) "🌟 Ưu tiên hôm nay: hoàn thành bản mẫu Liquid Glass và giữ nhịp tập trung 3 phiên Pomodoro."
            else "🌟 Main focus today: ship the Liquid Glass milestone and complete 3 deep Pomodoro sessions."
        )

        saveNote(
            NoteEntity(
                title = if (isVi) "Ý tưởng Sổ Kế Hoạch Taskal" else "Taskal Architecture & Manifesto",
                contentMarkdown = if (isVi) {
                    "# Triết lý Thiết kế\n- **Nhanh dưới 2 giây**: Nhập ngôn ngữ tự nhiên Tiếng Việt & Tiếng Anh.\n- **Kính lỏng & Ánh sáng sống**: Độ tương phản chuẩn WCAG AA.\n- [ ] Tối ưu hóa Widget màn hình chính\n- [x] Tích hợp Âm lịch Việt Nam"
                } else {
                    "# Product Principles\n- **Under 2 seconds capture**: Deterministic bilingual NLP.\n- **Liquid Glass x Living Light**: Atmospheric depth with WCAG AA legibility.\n- [ ] Customize Home Canvas widgets\n- [x] Vietnamese Lunar Calendar engine"
                },
                attachedDateEpochDay = today,
                isPinned = true,
                colorHex = "#38BDF8",
                tagsCsv = "architecture,ideas"
            )
        )

        // Sample Habits
        val h1 = dao.insertHabit(
            HabitEntity(
                title = if (isVi) "Uống đủ 2L nước" else "Drink 2L Hydration",
                emoji = "💧",
                colorHex = "#38BDF8",
                isDaily = true,
                weeklyTarget = 7,
                currentStreak = 5,
                bestStreak = 12
            )
        )
        val h2 = dao.insertHabit(
            HabitEntity(
                title = if (isVi) "Thiền định 10 phút" else "10m Mindful Breathing",
                emoji = "🧘",
                colorHex = "#34D399",
                isDaily = true,
                weeklyTarget = 7,
                currentStreak = 3,
                bestStreak = 9
            )
        )
        val h3 = dao.insertHabit(
            HabitEntity(
                title = if (isVi) "Viết nhật ký buổi tối" else "Evening Reflection Journal",
                emoji = "✍️",
                colorHex = "#C084FC",
                isDaily = false,
                weeklyTarget = 5,
                currentStreak = 4,
                bestStreak = 8
            )
        )
        for (offset in 0..4) {
            dao.insertHabitLog(HabitLogEntity(habitId = h1, epochDay = today - offset))
            if (offset <= 2) dao.insertHabitLog(HabitLogEntity(habitId = h2, epochDay = today - offset))
            if (offset in 1..4) dao.insertHabitLog(HabitLogEntity(habitId = h3, epochDay = today - offset))
        }

        // Sample Focus Sessions
        val now = System.currentTimeMillis()
        dao.insertFocusSession(FocusSessionEntity(taskTitleSnapshot = "Q4 Product Design Sync", durationMinutes = 25, startedAt = now - 7200_000L, completedAt = now - 5700_000L))
        dao.insertFocusSession(FocusSessionEntity(taskTitleSnapshot = "Clean Architecture Deep Work", durationMinutes = 50, startedAt = now - 86400_000L, completedAt = now - 83400_000L))
        onDataChanged()
    }
}
