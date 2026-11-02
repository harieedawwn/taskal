package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskalDao {
    // Tasks
    @Transaction
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isPinned DESC, sortOrder ASC, dueDateEpochDay ASC, startTimeMinute ASC")
    fun observeActiveTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeTrashTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskWithDetailsById(taskId: Long): TaskWithDetails?

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksRaw(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun hardDeleteTask(taskId: Long)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffMillis")
    suspend fun purgeOldTrash(cutoffMillis: Long)

    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    suspend fun emptyTrash()

    // Subtasks
    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasksRaw(): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)

    // Lists
    @Query("SELECT * FROM task_lists ORDER BY sortOrder ASC, id ASC")
    fun observeLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllListsRaw(): List<TaskListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TaskListEntity): Long

    @Query("DELETE FROM task_lists WHERE id = :listId AND isDefault = 0")
    suspend fun deleteList(listId: Long)

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsRaw(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTagCrossRef(ref: TaskTagCrossRef)

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    suspend fun clearTagsForTask(taskId: Long)

    @Query("SELECT * FROM task_tag_cross_ref")
    suspend fun getAllCrossRefsRaw(): List<TaskTagCrossRef>

    // Reminders
    @Query("SELECT * FROM reminders WHERE isFired = 0 AND triggerEpochMillis > :nowMillis ORDER BY triggerEpochMillis ASC")
    suspend fun getUpcomingReminders(nowMillis: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersRaw(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun clearRemindersForTask(taskId: Long)

    @Query("UPDATE reminders SET isFired = 1 WHERE id = :reminderId")
    suspend fun markReminderFired(reminderId: Long)

    // Notes
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesRaw(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isDayNote = 1 AND attachedDateEpochDay = :epochDay AND isDeleted = 0 LIMIT 1")
    suspend fun getDayNote(epochDay: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun hardDeleteNote(noteId: Long)

    // Habits
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsRaw(): List<HabitEntity>

    @Query("SELECT * FROM habit_logs")
    fun observeHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllHabitLogsRaw(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteHabitLog(habitId: Long, epochDay: Long)

    // Focus Sessions
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions")
    suspend fun getAllFocusSessionsRaw(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity): Long

    // Dashboard Widgets
    @Query("SELECT * FROM dashboard_widgets ORDER BY page ASC, sortOrder ASC, id ASC")
    fun observeDashboardWidgets(): Flow<List<DashboardWidgetEntity>>

    @Query("SELECT * FROM dashboard_widgets")
    suspend fun getAllDashboardWidgetsRaw(): List<DashboardWidgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardWidget(widget: DashboardWidgetEntity): Long

    @Update
    suspend fun updateDashboardWidget(widget: DashboardWidgetEntity)

    @Query("DELETE FROM dashboard_widgets WHERE id = :widgetId")
    suspend fun deleteDashboardWidget(widgetId: Long)

    // Wipe All
    @Query("DELETE FROM task_tag_cross_ref")
    suspend fun clearAllCrossRefs()
    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()
    @Query("DELETE FROM subtasks")
    suspend fun clearAllSubtasks()
    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
    @Query("DELETE FROM tags")
    suspend fun clearAllTags()
    @Query("DELETE FROM task_lists")
    suspend fun clearAllLists()
    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
    @Query("DELETE FROM habit_logs")
    suspend fun clearAllHabitLogs()
    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()
    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllFocusSessions()
    @Query("DELETE FROM dashboard_widgets")
    suspend fun clearAllDashboardWidgets()
}
