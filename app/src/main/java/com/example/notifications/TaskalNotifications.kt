package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PlannerRepository
import com.example.data.SettingsRepository
import com.example.data.TaskalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object TaskalNotificationHelper {
    const val CHANNEL_REMINDERS = "taskal_reminders"
    const val CHANNEL_FOCUS = "taskal_focus"
    const val CHANNEL_AGENDA = "taskal_agenda"

    const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_REMINDER"
    const val ACTION_COMPLETE_TASK = "com.example.action.COMPLETE_TASK"
    const val ACTION_SNOOZE_TASK = "com.example.action.SNOOZE_TASK"
    const val ACTION_FOCUS_FINISHED = "com.example.action.FOCUS_FINISHED"
    const val ACTION_STOP_FOCUS_CHRONO = "com.example.action.STOP_FOCUS_CHRONO"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"

    const val FOCUS_NOTIFICATION_ID = 900100

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Task and habit reminders"
            enableVibration(true)
        }
        val focusChannel = NotificationChannel(
            CHANNEL_FOCUS,
            context.getString(R.string.notif_channel_focus),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing focus timer chronometer"
        }
        val agendaChannel = NotificationChannel(
            CHANNEL_AGENDA,
            context.getString(R.string.notif_channel_agenda),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily agenda and evening review"
        }
        nm.createNotificationChannels(listOf(reminderChannel, focusChannel, agendaChannel))
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun scheduleDozeSafeAlarm(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Graceful fallback to windowed / inexact idle-safe alarm when exact alarm permission is denied
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }.onFailure {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    suspend fun rescheduleAllReminders(context: Context) {
        ensureChannels(context)
        val settings = SettingsRepository(context).settingsFlow.first()
        if (!settings.notificationsEnabled) return
        val dao = TaskalDatabase.getInstance(context).dao()
        val now = System.currentTimeMillis()
        val upcoming = dao.getUpcomingReminders(now)
        for (rem in upcoming) {
            val taskDetails = dao.getTaskWithDetailsById(rem.taskId) ?: continue
            if (taskDetails.task.isCompleted || taskDetails.task.isDeleted) continue
            val intent = Intent(context, TaskalAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
                putExtra(EXTRA_REMINDER_ID, rem.id)
                putExtra(EXTRA_TASK_ID, rem.taskId)
                putExtra(EXTRA_TASK_TITLE, taskDetails.task.title)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                rem.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleDozeSafeAlarm(context, rem.triggerEpochMillis, pi)
        }
    }

    /**
     * Posts or updates the ongoing Focus timer notification using NotificationCompat Chronometer
     * (NO foreground service used, complying with hard platform constraints).
     */
    fun showFocusChronometerNotification(
        context: Context,
        taskTitle: String,
        endEpochMillis: Long,
        isCountdown: Boolean = true
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_mode", "focus")
        }
        val openPi = PendingIntent.getActivity(
            context,
            9001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, TaskalAlarmReceiver::class.java).apply {
            action = ACTION_STOP_FOCUS_CHRONO
        }
        val stopPi = PendingIntent.getBroadcast(
            context,
            9002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (taskTitle.isNotBlank()) "Focus · $taskTitle" else "Taskal Focus Session")
            .setContentText(if (isCountdown) "Pomodoro session in progress" else "Stopwatch running")
            .setWhen(endEpochMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(isCountdown)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, context.getString(R.string.action_cancel), stopPi)

        runCatching {
            NotificationManagerCompat.from(context).notify(FOCUS_NOTIFICATION_ID, builder.build())
        }

        if (isCountdown && endEpochMillis > System.currentTimeMillis()) {
            val finishIntent = Intent(context, TaskalAlarmReceiver::class.java).apply {
                action = ACTION_FOCUS_FINISHED
                putExtra(EXTRA_TASK_TITLE, taskTitle)
            }
            val finishPi = PendingIntent.getBroadcast(
                context,
                9003,
                finishIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleDozeSafeAlarm(context, endEpochMillis, finishPi)
        }
    }

    fun cancelFocusNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(FOCUS_NOTIFICATION_ID)
    }
}

class TaskalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val db = TaskalDatabase.getInstance(context)
                val dao = db.dao()
                val settings = SettingsRepository(context).settingsFlow.first()
                val repo = PlannerRepository(dao)

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        TaskalNotificationHelper.rescheduleAllReminders(context)
                    }

                    TaskalNotificationHelper.ACTION_TRIGGER_REMINDER -> {
                        val reminderId = intent.getLongExtra(TaskalNotificationHelper.EXTRA_REMINDER_ID, 0L)
                        val taskId = intent.getLongExtra(TaskalNotificationHelper.EXTRA_TASK_ID, 0L)
                        val title = intent.getStringExtra(TaskalNotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
                        if (reminderId > 0L) dao.markReminderFired(reminderId)

                        if (settings.notificationsEnabled && settings.reminderChannelEnabled && TaskalNotificationHelper.hasNotificationPermission(context)) {
                            TaskalNotificationHelper.ensureChannels(context)
                            val openIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(TaskalNotificationHelper.EXTRA_TASK_ID, taskId)
                            }
                            val openPi = PendingIntent.getActivity(
                                context,
                                taskId.toInt(),
                                openIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            val completeIntent = Intent(context, TaskalAlarmReceiver::class.java).apply {
                                this.action = TaskalNotificationHelper.ACTION_COMPLETE_TASK
                                putExtra(TaskalNotificationHelper.EXTRA_TASK_ID, taskId)
                            }
                            val completePi = PendingIntent.getBroadcast(
                                context,
                                (taskId * 10 + 1).toInt(),
                                completeIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            val snoozeIntent = Intent(context, TaskalAlarmReceiver::class.java).apply {
                                this.action = TaskalNotificationHelper.ACTION_SNOOZE_TASK
                                putExtra(TaskalNotificationHelper.EXTRA_TASK_ID, taskId)
                                putExtra(TaskalNotificationHelper.EXTRA_TASK_TITLE, title)
                            }
                            val snoozePi = PendingIntent.getBroadcast(
                                context,
                                (taskId * 10 + 2).toInt(),
                                snoozeIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            val displayedTitle = if (settings.lockScreenHideTitles) "Taskal Reminder" else title
                            val visibility = if (settings.lockScreenHideTitles) {
                                NotificationCompat.VISIBILITY_PRIVATE
                            } else {
                                NotificationCompat.VISIBILITY_PUBLIC
                            }

                            val notif = NotificationCompat.Builder(context, TaskalNotificationHelper.CHANNEL_REMINDERS)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle(displayedTitle)
                                .setContentText(context.getString(R.string.app_tagline))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setVisibility(visibility)
                                .setAutoCancel(true)
                                .setContentIntent(openPi)
                                .addAction(0, context.getString(R.string.action_complete), completePi)
                                .addAction(0, context.getString(R.string.action_snooze), snoozePi)
                                .build()

                            runCatching {
                                NotificationManagerCompat.from(context).notify(taskId.toInt() + 10000, notif)
                            }
                        }
                    }

                    TaskalNotificationHelper.ACTION_COMPLETE_TASK -> {
                        val taskId = intent.getLongExtra(TaskalNotificationHelper.EXTRA_TASK_ID, 0L)
                        if (taskId > 0L) {
                            repo.toggleTaskCompleted(taskId, true)
                            NotificationManagerCompat.from(context).cancel(taskId.toInt() + 10000)
                        }
                    }

                    TaskalNotificationHelper.ACTION_SNOOZE_TASK -> {
                        val taskId = intent.getLongExtra(TaskalNotificationHelper.EXTRA_TASK_ID, 0L)
                        val title = intent.getStringExtra(TaskalNotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
                        NotificationManagerCompat.from(context).cancel(taskId.toInt() + 10000)
                        val snoozeMillis = System.currentTimeMillis() + settings.defaultSnoozeMinutes.coerceAtLeast(1) * 60_000L
                        val remId = dao.insertReminder(
                            com.example.data.ReminderEntity(
                                taskId = taskId,
                                offsetMinutesBefore = 0,
                                triggerEpochMillis = snoozeMillis,
                                isFired = false
                            )
                        )
                        val triggerIntent = Intent(context, TaskalAlarmReceiver::class.java).apply {
                            this.action = TaskalNotificationHelper.ACTION_TRIGGER_REMINDER
                            putExtra(TaskalNotificationHelper.EXTRA_REMINDER_ID, remId)
                            putExtra(TaskalNotificationHelper.EXTRA_TASK_ID, taskId)
                            putExtra(TaskalNotificationHelper.EXTRA_TASK_TITLE, title)
                        }
                        val pi = PendingIntent.getBroadcast(
                            context,
                            remId.toInt(),
                            triggerIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        TaskalNotificationHelper.scheduleDozeSafeAlarm(context, snoozeMillis, pi)
                    }

                    TaskalNotificationHelper.ACTION_STOP_FOCUS_CHRONO -> {
                        TaskalNotificationHelper.cancelFocusNotification(context)
                    }

                    TaskalNotificationHelper.ACTION_FOCUS_FINISHED -> {
                        TaskalNotificationHelper.cancelFocusNotification(context)
                        if (settings.notificationsEnabled && TaskalNotificationHelper.hasNotificationPermission(context)) {
                            val title = intent.getStringExtra(TaskalNotificationHelper.EXTRA_TASK_TITLE).orEmpty()
                            val notif = NotificationCompat.Builder(context, TaskalNotificationHelper.CHANNEL_REMINDERS)
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("Focus Session Complete!")
                                .setContentText(if (title.isNotBlank()) "Completed focus on: $title" else "Great deep work session — take a breather.")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)
                                .build()
                            runCatching {
                                NotificationManagerCompat.from(context).notify(TaskalNotificationHelper.FOCUS_NOTIFICATION_ID + 1, notif)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
