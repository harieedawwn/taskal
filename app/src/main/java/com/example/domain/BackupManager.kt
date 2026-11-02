package com.example.domain

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.DashboardWidgetEntity
import com.example.data.FocusSessionEntity
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.NoteEntity
import com.example.data.ReminderEntity
import com.example.data.SubtaskEntity
import com.example.data.TagEntity
import com.example.data.TaskEntity
import com.example.data.TaskListEntity
import com.example.data.TaskTagCrossRef
import com.example.data.TaskalDatabase
import com.example.notifications.TaskalNotificationHelper
import com.example.widgets.TaskalGlanceWidget
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Serializable
data class TaskalBackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val tasks: List<TaskEntity> = emptyList(),
    val subtasks: List<SubtaskEntity> = emptyList(),
    val lists: List<TaskListEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val crossRefs: List<TaskTagCrossRef> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val dashboardWidgets: List<DashboardWidgetEntity> = emptyList()
)

object BackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportFullBackupJson(context: Context): String {
        val dao = TaskalDatabase.getInstance(context).dao()
        val payload = TaskalBackupPayload(
            tasks = dao.getAllTasksRaw(),
            subtasks = dao.getAllSubtasksRaw(),
            lists = dao.getAllListsRaw(),
            tags = dao.getAllTagsRaw(),
            crossRefs = dao.getAllCrossRefsRaw(),
            reminders = dao.getAllRemindersRaw(),
            notes = dao.getAllNotesRaw(),
            habits = dao.getAllHabitsRaw(),
            habitLogs = dao.getAllHabitLogsRaw(),
            focusSessions = dao.getAllFocusSessionsRaw(),
            dashboardWidgets = dao.getAllDashboardWidgetsRaw()
        )
        return json.encodeToString(payload)
    }

    suspend fun importFullBackupJson(context: Context, rawJson: String): Boolean {
        val payload = runCatching { json.decodeFromString<TaskalBackupPayload>(rawJson) }.getOrNull() ?: return false
        val dao = TaskalDatabase.getInstance(context).dao()

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

        payload.lists.forEach { dao.insertList(it) }
        payload.tags.forEach { dao.insertTag(it) }
        payload.tasks.forEach { dao.insertTask(it) }
        payload.subtasks.forEach { dao.insertSubtask(it) }
        payload.crossRefs.forEach { dao.insertTaskTagCrossRef(it) }
        payload.reminders.forEach { dao.insertReminder(it) }
        payload.notes.forEach { dao.insertNote(it) }
        payload.habits.forEach { dao.insertHabit(it) }
        payload.habitLogs.forEach { dao.insertHabitLog(it) }
        payload.focusSessions.forEach { dao.insertFocusSession(it) }
        payload.dashboardWidgets.forEach { dao.insertDashboardWidget(it) }

        TaskalNotificationHelper.rescheduleAllReminders(context)
        TaskalGlanceWidget.updateAllWidgets(context)
        return true
    }

    suspend fun exportTasksCsv(context: Context): String {
        val dao = TaskalDatabase.getInstance(context).dao()
        val tasks = dao.getAllTasksRaw().filter { !it.isDeleted }
        val sb = StringBuilder()
        sb.appendLine("id,title,description,priority,status,dueDate,startTime,durationMinutes,isCompleted")
        for (t in tasks) {
            val dueStr = t.dueDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()
            val timeStr = t.startTimeMinute?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()
            val escTitle = "\"" + t.title.replace("\"", "\"\"") + "\""
            val escDesc = "\"" + t.description.replace("\"", "\"\"").replace("\n", " ") + "\""
            sb.appendLine("${t.id},$escTitle,$escDesc,${t.priority},${t.status},$dueStr,$timeStr,${t.durationMinutes},${t.isCompleted}")
        }
        return sb.toString()
    }

    suspend fun exportTasksIcs(context: Context): String {
        val dao = TaskalDatabase.getInstance(context).dao()
        val tasks = dao.getAllTasksRaw().filter { !it.isDeleted && it.dueDateEpochDay != null }
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Taskal Local Planner//EN")
        val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
        for (t in tasks) {
            val date = LocalDate.ofEpochDay(t.dueDateEpochDay!!)
            val dStr = date.format(dateFmt)
            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:taskal-${t.id}@local")
            if (t.startTimeMinute != null) {
                val hh = t.startTimeMinute / 60
                val mm = t.startTimeMinute % 60
                sb.appendLine("DTSTART:${dStr}T%02d%02d00".format(hh, mm))
            } else {
                sb.appendLine("DTSTART;VALUE=DATE:$dStr")
            }
            sb.appendLine("SUMMARY:${t.title.replace("\n", " ")}")
            if (t.description.isNotBlank()) {
                sb.appendLine("DESCRIPTION:${t.description.replace("\n", "\\n")}")
            }
            sb.appendLine("END:VEVENT")
        }
        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    suspend fun importTasksIcs(context: Context, rawIcs: String): Int {
        val dao = TaskalDatabase.getInstance(context).dao()
        val blocks = rawIcs.split("BEGIN:VEVENT").drop(1)
        var importedCount = 0
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }
            val summary = lines.firstOrNull { it.startsWith("SUMMARY:") }?.removePrefix("SUMMARY:")?.trim()
            if (summary.isNullOrBlank()) continue
            val desc = lines.firstOrNull { it.startsWith("DESCRIPTION:") }?.removePrefix("DESCRIPTION:")?.replace("\\n", "\n").orEmpty()
            val dtStartLine = lines.firstOrNull { it.startsWith("DTSTART") }.orEmpty()
            val dtValue = dtStartLine.substringAfter(":", "").trim()
            var epochDay = LocalDate.now().toEpochDay()
            var startMin: Int? = null
            if (dtValue.length >= 8) {
                val yr = dtValue.substring(0, 4).toIntOrNull() ?: LocalDate.now().year
                val mo = dtValue.substring(4, 6).toIntOrNull() ?: 1
                val dy = dtValue.substring(6, 8).toIntOrNull() ?: 1
                runCatching { LocalDate.of(yr, mo.coerceIn(1, 12), dy.coerceIn(1, 31)) }.getOrNull()?.let {
                    epochDay = it.toEpochDay()
                }
                if (dtValue.contains("T") && dtValue.length >= 13) {
                    val tPart = dtValue.substringAfter("T")
                    val hh = tPart.substring(0, 2).toIntOrNull() ?: 9
                    val mm = tPart.substring(2, 4).toIntOrNull() ?: 0
                    startMin = (hh * 60 + mm).coerceIn(0, 1439)
                }
            }
            dao.insertTask(
                TaskEntity(
                    title = summary,
                    description = desc,
                    dueDateEpochDay = epochDay,
                    startDateEpochDay = epochDay,
                    startTimeMinute = startMin,
                    isAllDay = startMin == null
                )
            )
            importedCount++
        }
        TaskalGlanceWidget.updateAllWidgets(context)
        return importedCount
    }

    /**
     * Automatic daily local backup keeping the last 7 snapshots in filesDir/backups.
     */
    suspend fun performLocalRollingBackup(context: Context): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val jsonContent = exportFullBackupJson(context)
        val fileName = "taskal_auto_${LocalDate.now()}_${System.currentTimeMillis() / 1000}.json"
        val outFile = File(dir, fileName)
        outFile.writeText(jsonContent)

        val allBackups = dir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        if (allBackups.size > 7) {
            allBackups.drop(7).forEach { it.delete() }
        }
        return outFile
    }

    fun listLocalBackups(context: Context): List<File> {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    fun scheduleDailyAutoBackupWorker(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork("taskal_daily_auto_backup")
            return
        }
        val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(
            "taskal_daily_auto_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            BackupManager.performLocalRollingBackup(applicationContext)
            Result.success()
        }.getOrDefault(Result.retry())
    }
}
