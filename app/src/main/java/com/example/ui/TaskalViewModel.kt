package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.DashboardWidgetEntity
import com.example.data.FocusSessionEntity
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.NoteEntity
import com.example.data.PlannerRepository
import com.example.data.RecurrenceRuleModel
import com.example.data.SettingsRepository
import com.example.data.SwipeActionType
import com.example.data.TagEntity
import com.example.data.TaskEntity
import com.example.data.TaskListEntity
import com.example.data.TaskStatus
import com.example.data.TaskWithDetails
import com.example.data.TaskalDatabase
import com.example.domain.BackupManager
import com.example.domain.RecurrenceEngine as DomainRecurrenceEngine
import com.example.notifications.TaskalNotificationHelper
import com.example.widgets.TaskalGlanceWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskalViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db = TaskalDatabase.getInstance(context)
    val settingsRepo = SettingsRepository(context)

    val repo = PlannerRepository(
        dao = db.dao(),
        onDataChanged = {
            TaskalNotificationHelper.rescheduleAllReminders(context)
            TaskalGlanceWidget.updateAllWidgets(context)
        }
    )

    val settingsState: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val activeTasks: StateFlow<List<TaskWithDetails>> = repo.activeTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashTasks: StateFlow<List<TaskWithDetails>> = repo.trashTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lists: StateFlow<List<TaskListEntity>> = repo.listsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repo.tagsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repo.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repo.habitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogs: StateFlow<List<HabitLogEntity>> = repo.habitLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = repo.focusSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardWidgets: StateFlow<List<DashboardWidgetEntity>> = repo.dashboardWidgetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dailyCompletionBloomCount = MutableStateFlow(0)
    val dailyCompletionBloomCount: StateFlow<Int> = _dailyCompletionBloomCount.asStateFlow()

    private val _lastDeletedTaskId = MutableStateFlow<Long?>(null)
    val lastDeletedTaskId: StateFlow<Long?> = _lastDeletedTaskId.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureDefaultsAndPurgeTrash()
            BackupManager.scheduleDailyAutoBackupWorker(context, true)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }

    fun resetSettingsSection(section: String) {
        viewModelScope.launch {
            settingsRepo.resetSection(section)
        }
    }

    fun completeOnboarding(loadSample: Boolean, isVi: Boolean) {
        viewModelScope.launch {
            if (loadSample) {
                repo.loadSampleData(isVi)
            }
            settingsRepo.update { it.copy(hasCompletedOnboarding = true) }
        }
    }

    fun createTaskFromComposer(
        title: String,
        listId: Long,
        priority: Int,
        dueDateEpochDay: Long?,
        startTimeMinute: Int?,
        durationMinutes: Int,
        recurrenceRule: RecurrenceRuleModel,
        tagNames: List<String>,
        reminderOffsets: List<Int>
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                listId = listId,
                priority = priority,
                startDateEpochDay = dueDateEpochDay,
                dueDateEpochDay = dueDateEpochDay,
                startTimeMinute = startTimeMinute,
                durationMinutes = durationMinutes,
                isAllDay = startTimeMinute == null,
                recurrenceJson = DomainRecurrenceEngine.encodeRule(recurrenceRule),
                status = TaskStatus.TODO.name
            )
            repo.saveTaskWithDetails(task, emptyList(), tagNames, reminderOffsets)
        }
    }

    fun saveEditedTask(
        updatedTask: TaskEntity,
        subtasks: List<String>,
        tags: List<String>,
        reminderOffsets: List<Int>
    ) {
        viewModelScope.launch {
            repo.saveTaskWithDetails(updatedTask, subtasks, tags, reminderOffsets)
        }
    }

    fun updateTaskDirect(task: TaskEntity) {
        viewModelScope.launch {
            repo.updateTaskDirect(task)
        }
    }

    fun toggleTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repo.toggleTaskCompleted(taskId, completed)
            if (completed) {
                val today = LocalDate.now().toEpochDay()
                val remainingToday = activeTasks.value.count {
                    it.task.id != taskId && it.task.dueDateEpochDay == today && !it.task.isCompleted
                }
                if (remainingToday == 0) {
                    _dailyCompletionBloomCount.value += 1
                }
            }
        }
    }

    fun handleSwipeAction(item: TaskWithDetails, action: SwipeActionType) {
        viewModelScope.launch {
            val t = item.task
            when (action) {
                SwipeActionType.COMPLETE -> toggleTaskCompleted(t.id, !t.isCompleted)
                SwipeActionType.SNOOZE_TOMORROW -> {
                    val nextDay = (t.dueDateEpochDay ?: LocalDate.now().toEpochDay()) + 1L
                    repo.rescheduleTask(t.id, nextDay, t.startTimeMinute, t.durationMinutes)
                }
                SwipeActionType.PIN -> repo.updateTaskDirect(t.copy(isPinned = !t.isPinned))
                SwipeActionType.DELETE -> {
                    repo.softDeleteTask(t.id)
                    _lastDeletedTaskId.value = t.id
                }
            }
        }
    }

    fun softDeleteTask(taskId: Long) {
        viewModelScope.launch {
            repo.softDeleteTask(taskId)
            _lastDeletedTaskId.value = taskId
        }
    }

    fun undoLastDelete() {
        val id = _lastDeletedTaskId.value ?: return
        viewModelScope.launch {
            repo.restoreTask(id)
            _lastDeletedTaskId.value = null
        }
    }

    fun clearUndoState() {
        _lastDeletedTaskId.value = null
    }

    fun restoreTask(taskId: Long) {
        viewModelScope.launch { repo.restoreTask(taskId) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repo.emptyTrash() }
    }

    fun bulkComplete(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { repo.toggleTaskCompleted(it, true) }
        }
    }

    fun bulkDelete(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { repo.softDeleteTask(it) }
            _lastDeletedTaskId.value = ids.lastOrNull()
        }
    }

    fun bulkSetToday(ids: List<Long>) {
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            ids.forEach { id ->
                val details = activeTasks.value.firstOrNull { it.task.id == id }
                repo.rescheduleTask(id, today, details?.task?.startTimeMinute, details?.task?.durationMinutes)
            }
        }
    }

    fun rescheduleTask(taskId: Long, newEpochDay: Long?, newStartMinute: Int?, newDurationMinutes: Int?) {
        viewModelScope.launch {
            repo.rescheduleTask(taskId, newEpochDay, newStartMinute, newDurationMinutes)
        }
    }

    fun createList(name: String, emoji: String, colorHex: String) {
        viewModelScope.launch { repo.addList(name, emoji, colorHex) }
    }

    fun saveDayNote(epochDay: Long, content: String) {
        viewModelScope.launch { repo.saveDayNote(epochDay, content) }
    }

    fun saveNote(note: NoteEntity) {
        viewModelScope.launch { repo.saveNote(note) }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { repo.deleteNote(noteId) }
    }

    fun convertChecklistLinesToTasks(lines: List<String>, dueDateEpochDay: Long?) {
        viewModelScope.launch {
            lines.forEach { title ->
                repo.saveTaskWithDetails(
                    task = TaskEntity(
                        title = title,
                        dueDateEpochDay = dueDateEpochDay ?: LocalDate.now().toEpochDay(),
                        startDateEpochDay = dueDateEpochDay ?: LocalDate.now().toEpochDay(),
                        isAllDay = true
                    ),
                    subtasks = emptyList(),
                    tagNames = listOf("from-note"),
                    reminderOffsetsMinutes = emptyList()
                )
            }
        }
    }

    fun toggleHabitDay(habit: HabitEntity, epochDay: Long) {
        viewModelScope.launch {
            repo.toggleHabitCheckIn(habit, epochDay, habitLogs.value)
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch { repo.saveHabit(habit) }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch { repo.deleteHabit(habitId) }
    }

    fun logFocusSession(session: FocusSessionEntity) {
        viewModelScope.launch { repo.logFocusSession(session) }
    }

    fun saveDashboardWidget(widget: DashboardWidgetEntity) {
        viewModelScope.launch { repo.saveDashboardWidget(widget) }
    }

    fun deleteDashboardWidget(widgetId: Long) {
        viewModelScope.launch { repo.deleteDashboardWidget(widgetId) }
    }

    fun loadSampleData(isVi: Boolean) {
        viewModelScope.launch { repo.loadSampleData(isVi) }
    }

    fun wipeAllData() {
        viewModelScope.launch { repo.wipeAllData() }
    }
}
