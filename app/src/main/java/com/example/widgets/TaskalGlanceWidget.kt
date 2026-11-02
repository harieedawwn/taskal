package com.example.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.PlannerRepository
import com.example.data.TaskEntity
import com.example.data.TaskalDatabase
import com.example.domain.LunarCalendar
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class WidgetInstanceConfig(
    val widgetType: String = "TODAY_LIST",
    val style: String = "GLASS", // GLASS, SOLID, TRANSPARENT
    val opacity: Float = 0.82f,
    val cornerRadiusDp: Int = 22,
    val accentHex: String = "#38BDF8",
    val textScale: Float = 1.0f,
    val showCompleted: Boolean = false,
    val showHeader: Boolean = true,
    val compactDensity: Boolean = false
)

object WidgetConfigStore {
    private const val PREF_NAME = "taskal_widget_configs"
    private val json = Json { ignoreUnknownKeys = true }

    fun saveConfig(context: Context, appWidgetId: Int, config: WidgetInstanceConfig) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("widget_$appWidgetId", json.encodeToString(config)).apply()
    }

    fun loadConfig(context: Context, appWidgetId: Int): WidgetInstanceConfig {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("widget_$appWidgetId", null)
            ?: prefs.getString("widget_default", null)
            ?: return WidgetInstanceConfig()
        return runCatching { json.decodeFromString<WidgetInstanceConfig>(raw) }
            .getOrDefault(WidgetInstanceConfig())
    }

    fun saveDefaultStudioConfig(context: Context, config: WidgetInstanceConfig) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("widget_default", json.encodeToString(config)).apply()
    }
}

private val TaskIdParamKey = ActionParameters.Key<Long>("task_id")

class CompleteTaskWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdParamKey] ?: return
        val dao = TaskalDatabase.getInstance(context).dao()
        val repo = PlannerRepository(dao)
        val existing = dao.getTaskWithDetailsById(taskId)?.task ?: return
        repo.toggleTaskCompleted(taskId, !existing.isCompleted)
        TaskalGlanceWidget().updateAll(context)
    }
}

class TaskalGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_BUCKET = DpSize(110.dp, 110.dp)
        private val MEDIUM_BUCKET = DpSize(220.dp, 130.dp)
        private val LARGE_BUCKET = DpSize(280.dp, 220.dp)

        suspend fun updateAllWidgets(context: Context) {
            runCatching {
                TaskalGlanceWidget().updateAll(context)
            }
        }
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_BUCKET, MEDIUM_BUCKET, LARGE_BUCKET)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = TaskalDatabase.getInstance(context).dao()
        val allTasks = dao.getAllTasksRaw().filter { !it.isDeleted }
        val config = WidgetConfigStore.loadConfig(context, 0)
        val today = LocalDate.now()
        val lunar = LunarCalendar.solarToLunar(today)

        provideContent {
            GlanceTheme {
                WidgetResponsiveContent(
                    context = context,
                    config = config,
                    tasks = allTasks,
                    today = today,
                    lunarText = "ÂL ${lunar.day}/${lunar.month}"
                )
            }
        }
    }

    @Composable
    private fun WidgetResponsiveContent(
        context: Context,
        config: WidgetInstanceConfig,
        tasks: List<TaskEntity>,
        today: LocalDate,
        lunarText: String
    ) {
        val size = LocalSize.current
        val isSmall = size.width < 160.dp
        val isLarge = size.height >= 200.dp

        val bgAlpha = when (config.style) {
            "TRANSPARENT" -> 0.15f
            "SOLID" -> 0.98f
            else -> config.opacity.coerceIn(0.25f, 0.95f)
        }
        val bgColor = Color(0xFF0D1526).copy(alpha = bgAlpha)
        val accentColor = Color(0xFF38BDF8)

        val visibleTasks = tasks.filter {
            (config.showCompleted || !it.isCompleted)
        }.sortedBy { it.dueDateEpochDay ?: Long.MAX_VALUE }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(config.cornerRadiusDp.dp)
                .background(ColorProvider(bgColor))
                .padding(if (config.compactDensity) 8.dp else 12.dp)
        ) {
            if (isSmall) {
                // Compact 1x1 / Small Layout: Date + Lunar + Pending Count
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(openAppIntent)),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = "%02d".format(today.dayOfMonth),
                        style = TextStyle(
                            color = ColorProvider(accentColor),
                            fontSize = (26f * config.textScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = lunarText,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFCBD5E1)),
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "${visibleTasks.size} tasks",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                // Medium & Large Layouts: Header + Interactive Task Checkboxes
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    if (config.showHeader) {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(openAppIntent))
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "Taskal · ${today.dayOfMonth}/${today.monthValue}",
                                style = TextStyle(
                                    color = ColorProvider(accentColor),
                                    fontSize = (14f * config.textScale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = lunarText,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF94A3B8)),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    val maxRows = if (isLarge) 10 else 4
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(visibleTasks.take(maxRows), itemId = { it.id }) { task ->
                            val detailIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("extra_task_id", task.id)
                            }
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Vertical.CenterVertically
                            ) {
                                // Tapping checkbox completes task in background without opening app
                                Text(
                                    text = if (task.isCompleted) "💧" else "◯",
                                    style = TextStyle(
                                        color = ColorProvider(accentColor),
                                        fontSize = 14.sp
                                    ),
                                    modifier = GlanceModifier
                                        .padding(end = 8.dp)
                                        .clickable(
                                            actionRunCallback<CompleteTaskWidgetCallback>(
                                                actionParametersOf(TaskIdParamKey to task.id)
                                            )
                                        )
                                )
                                Text(
                                    text = task.title,
                                    style = TextStyle(
                                        color = ColorProvider(if (task.isCompleted) Color(0xFF94A3B8) else Color.White),
                                        fontSize = (13f * config.textScale).sp
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .clickable(actionStartActivity(detailIntent))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class TaskalGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskalGlanceWidget()
}
