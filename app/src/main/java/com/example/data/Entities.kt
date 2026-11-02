package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus {
    TODO, DOING, DONE
}

@Serializable
enum class RecurrenceFrequency {
    NONE, DAILY, WEEKLY, MONTHLY_DATE, MONTHLY_NTH_WEEKDAY, MONTHLY_LAST_DAY, YEARLY, HOURS_INTERVAL
}

@Serializable
data class RecurrenceRuleModel(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(), // 1=Mon .. 7=Sun
    val nthWeek: Int = 1, // 1..4 or -1 for last
    val dayOfWeekInMonth: Int = 1,
    val repeatAfterCompletion: Boolean = false,
    val endEpochDay: Long? = null,
    val remainingOccurrences: Int? = null
)

@Serializable
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["listId"]),
        Index(value = ["isCompleted", "isDeleted"]),
        Index(value = ["status"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val listId: Long = 1L,
    val priority: Int = 0, // 0=None, 1=Low, 2=Medium, 3=High
    val startDateEpochDay: Long? = null,
    val dueDateEpochDay: Long? = null,
    val startTimeMinute: Int? = null, // 0..1439 (minutes from midnight)
    val durationMinutes: Int = 45,
    val isAllDay: Boolean = true,
    val recurrenceJson: String = "", // serialized RecurrenceRuleModel
    val colorHex: String = "#38BDF8",
    val isPinned: Boolean = false,
    val status: String = TaskStatus.TODO.name,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val sortOrder: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: String = "#38BDF8",
    val emoji: String = "📋",
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@Serializable
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: String = "#818CF8"
)

@Serializable
@Entity(
    tableName = "task_tag_cross_ref",
    primaryKeys = ["taskId", "tagId"],
    indices = [Index(value = ["taskId"]), Index(value = ["tagId"])]
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)

@Serializable
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"]), Index(value = ["triggerEpochMillis"])]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val offsetMinutesBefore: Int = 0, // 0, 5, 10, 30, 60, 1440, or -1 for custom
    val triggerEpochMillis: Long,
    val isFired: Boolean = false
)

@Serializable
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["attachedDateEpochDay"]),
        Index(value = ["isDayNote"]),
        Index(value = ["isDeleted", "isArchived"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val contentMarkdown: String = "",
    val attachedDateEpochDay: Long? = null,
    val isDayNote: Boolean = false,
    val linkedTaskId: Long? = null,
    val colorHex: String = "#38BDF8",
    val tagsCsv: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val sortOrder: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val emoji: String = "💧",
    val colorHex: String = "#34D399",
    val isDaily: Boolean = true,
    val weeklyTarget: Int = 7, // 1..7 days per week
    val reminderMinuteOfDay: Int? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "epochDay"],
    indices = [Index(value = ["habitId"]), Index(value = ["epochDay"])]
)
data class HabitLogEntity(
    val habitId: Long,
    val epochDay: Long,
    val completedCount: Int = 1,
    val completedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["startedAt"]), Index(value = ["taskId"])]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long? = null,
    val taskTitleSnapshot: String = "",
    val durationMinutes: Int = 25,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val wasCompleted: Boolean = true,
    val mode: String = "POMODORO" // POMODORO, STOPWATCH, BREAK
)

@Serializable
@Entity(
    tableName = "dashboard_widgets",
    indices = [Index(value = ["page", "sortOrder"])]
)
data class DashboardWidgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val page: Int = 0, // 0..2
    val widgetType: String, // TODAY_LIST, AGENDA, MINI_MONTH, WEEK_STRIP, QUICK_ADD, PINNED_NOTE, FOCUS_TIMER, HABIT_STREAKS, PROGRESS_RING, COUNTDOWN
    val spanX: Int = 2, // 1, 2, 4
    val spanY: Int = 2, // 1, 2, 3, 4
    val titleOverride: String = "",
    val configJson: String = "",
    val sortOrder: Int = 0
)

data class TaskWithDetails(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subtasks: List<SubtaskEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTagCrossRef::class,
            parentColumn = "taskId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val reminders: List<ReminderEntity>
)
