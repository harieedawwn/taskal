package com.example

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppMode
import com.example.data.GlassThickness
import com.example.data.TaskWithDetails
import com.example.ui.TaskalViewModel
import com.example.ui.calendar.CalendarModeScreen
import com.example.ui.glass.AuroraBackdrop
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LiquidDock
import com.example.ui.glass.LiquidNavigationRail
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.glass.icon
import com.example.ui.glass.localizedTitle
import com.example.ui.modes.BoardModeScreen
import com.example.ui.modes.FocusModeScreen
import com.example.ui.modes.HabitsModeScreen
import com.example.ui.modes.MatrixModeScreen
import com.example.ui.modes.StatsModeScreen
import com.example.ui.notes.NotesModeScreen
import com.example.ui.settings.OnboardingOverlay
import com.example.ui.settings.SettingsModeScreen
import com.example.ui.tasks.QuickAddComposerSheet
import com.example.ui.tasks.TaskDetailDialog
import com.example.ui.tasks.TasksModeScreen
import com.example.ui.theme.TaskalTheme
import com.example.ui.widgets.HomeCanvasModeScreen
import com.example.ui.widgets.WidgetStudioScreen
import com.example.util.S
import com.example.widgets.TaskalGlanceWidget
import com.example.widgets.WidgetConfigStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TaskalViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isConfigureIntent = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (isConfigureIntent && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val cancelResult = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_CANCELED, cancelResult)
        }

        val deepLinkTaskId = intent?.getLongExtra("extra_task_id", 0L) ?: 0L
        val deepLinkMode = intent?.getStringExtra("open_mode")

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
            val scope = rememberCoroutineScope()

            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
            val trashTasks by viewModel.trashTasks.collectAsStateWithLifecycle()
            val lists by viewModel.lists.collectAsStateWithLifecycle()
            val tags by viewModel.tags.collectAsStateWithLifecycle()
            val notes by viewModel.notes.collectAsStateWithLifecycle()
            val habits by viewModel.habits.collectAsStateWithLifecycle()
            val habitLogs by viewModel.habitLogs.collectAsStateWithLifecycle()
            val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()
            val dashboardWidgets by viewModel.dashboardWidgets.collectAsStateWithLifecycle()
            val bloomCount by viewModel.dailyCompletionBloomCount.collectAsStateWithLifecycle()
            val lastDeletedTaskId by viewModel.lastDeletedTaskId.collectAsStateWithLifecycle()

            var currentModeId by rememberSaveable {
                mutableStateOf(
                    when {
                        isConfigureIntent && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID -> AppMode.WIDGETS.id
                        deepLinkMode != null -> deepLinkMode
                        else -> AppMode.CALENDAR.id
                    }
                )
            }
            val currentMode = AppMode.entries.find { it.id == currentModeId } ?: AppMode.CALENDAR

            var showQuickAdd by rememberSaveable { mutableStateOf(false) }
            var quickAddInitialDay by rememberSaveable { mutableStateOf<Long?>(null) }
            var quickAddInitialMinute by rememberSaveable { mutableStateOf<Int?>(null) }
            var showMoreModesSheet by rememberSaveable { mutableStateOf(false) }
            var selectedTaskForDetail by remember { mutableStateOf<TaskWithDetails?>(null) }

            LaunchedEffect(deepLinkTaskId, activeTasks) {
                if (deepLinkTaskId > 0L && selectedTaskForDetail == null) {
                    selectedTaskForDetail = activeTasks.firstOrNull { it.task.id == deepLinkTaskId }
                }
            }

            // Predictive Back / BackHandler returns to start mode when on secondary modes
            BackHandler(enabled = currentModeId != settings.startModeId) {
                currentModeId = settings.startModeId
            }

            TaskalTheme(settings = settings) {
                val s = S.current
                val tokens = LocalGlassTokens.current

                AuroraBackdrop(
                    settings = settings,
                    bloomTriggerCount = bloomCount
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        floatingActionButton = {
                            if (currentMode != AppMode.SETTINGS && currentMode != AppMode.WIDGETS) {
                                GlassSurface(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                                        .testTag("fab_quick_add"),
                                    thickness = GlassThickness.THICK,
                                    cornerRadius = 28.dp,
                                    accentTint = tokens.accentColor,
                                    elevated = true,
                                    onClick = {
                                        quickAddInitialDay = null
                                        quickAddInitialMinute = null
                                        showQuickAdd = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = s.quickAddPlaceholder,
                                            tint = tokens.accentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = if (s.isVi) "Thêm nhanh" else "Quick Add",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (!isExpandedTablet) {
                                LiquidDock(
                                    dockModes = settings.dockModes,
                                    currentMode = currentMode,
                                    floating = settings.floatingDock,
                                    showLabels = settings.showDockLabels,
                                    onSelectMode = { currentModeId = it.id },
                                    onOpenMoreSheet = { showMoreModesSheet = true }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isExpandedTablet) {
                                LiquidNavigationRail(
                                    allModes = AppMode.entries,
                                    currentMode = currentMode,
                                    onSelectMode = { currentModeId = it.id }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                when (currentMode) {
                                    AppMode.HOME -> HomeCanvasModeScreen(
                                        widgets = dashboardWidgets,
                                        tasks = activeTasks,
                                        notes = notes,
                                        habits = habits,
                                        onSaveWidget = viewModel::saveDashboardWidget,
                                        onDeleteWidget = viewModel::deleteDashboardWidget,
                                        onToggleTaskComplete = viewModel::toggleTaskCompleted,
                                        onOpenQuickAdd = { showQuickAdd = true },
                                        onNavigateMode = { currentModeId = it.id }
                                    )

                                    AppMode.CALENDAR -> CalendarModeScreen(
                                        tasks = activeTasks,
                                        notes = notes,
                                        settings = settings,
                                        isExpandedTablet = isExpandedTablet,
                                        onToggleTaskComplete = viewModel::toggleTaskCompleted,
                                        onOpenTaskDetail = { selectedTaskForDetail = it },
                                        onQuickAddAtSlot = { d, m ->
                                            quickAddInitialDay = d
                                            quickAddInitialMinute = m
                                            showQuickAdd = true
                                        },
                                        onRescheduleTask = viewModel::rescheduleTask,
                                        onSaveDayNote = viewModel::saveDayNote,
                                        onOpenNoteDetail = { currentModeId = AppMode.NOTES.id }
                                    )

                                    AppMode.TASKS -> TasksModeScreen(
                                        activeTasks = activeTasks,
                                        trashTasks = trashTasks,
                                        lists = lists,
                                        tags = tags,
                                        settings = settings,
                                        onToggleComplete = viewModel::toggleTaskCompleted,
                                        onOpenTaskDetail = { selectedTaskForDetail = it },
                                        onSwipeAction = viewModel::handleSwipeAction,
                                        onRestoreTask = viewModel::restoreTask,
                                        onEmptyTrash = viewModel::emptyTrash,
                                        onBulkComplete = viewModel::bulkComplete,
                                        onBulkDelete = viewModel::bulkDelete,
                                        onBulkSetToday = viewModel::bulkSetToday,
                                        onCreateList = viewModel::createList
                                    )

                                    AppMode.BOARD -> BoardModeScreen(
                                        tasks = activeTasks,
                                        lists = lists,
                                        onUpdateTask = viewModel::updateTaskDirect,
                                        onToggleComplete = viewModel::toggleTaskCompleted,
                                        onOpenTaskDetail = { selectedTaskForDetail = it }
                                    )

                                    AppMode.MATRIX -> MatrixModeScreen(
                                        tasks = activeTasks,
                                        onUpdateTask = viewModel::updateTaskDirect,
                                        onToggleComplete = viewModel::toggleTaskCompleted,
                                        onOpenTaskDetail = { selectedTaskForDetail = it }
                                    )

                                    AppMode.NOTES -> NotesModeScreen(
                                        notes = notes,
                                        tasks = activeTasks,
                                        onSaveNote = viewModel::saveNote,
                                        onDeleteNote = viewModel::deleteNote,
                                        onConvertChecklistToTasks = viewModel::convertChecklistLinesToTasks
                                    )

                                    AppMode.HABITS -> HabitsModeScreen(
                                        habits = habits,
                                        habitLogs = habitLogs,
                                        onToggleHabitDay = viewModel::toggleHabitDay,
                                        onSaveHabit = viewModel::saveHabit,
                                        onDeleteHabit = viewModel::deleteHabit
                                    )

                                    AppMode.FOCUS -> FocusModeScreen(
                                        tasks = activeTasks,
                                        sessions = focusSessions,
                                        settings = settings,
                                        onLogCompletedSession = viewModel::logFocusSession
                                    )

                                    AppMode.STATS -> StatsModeScreen(
                                        tasks = activeTasks,
                                        lists = lists,
                                        habits = habits,
                                        focusSessions = focusSessions
                                    )

                                    AppMode.WIDGETS -> WidgetStudioScreen(
                                        appWidgetIdToConfigure = if (isConfigureIntent && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetId else null,
                                        tasks = activeTasks,
                                        onConfirmConfigureWidget = { wId, cfg ->
                                            WidgetConfigStore.saveConfig(applicationContext, wId, cfg)
                                            scope.launch {
                                                TaskalGlanceWidget.updateAllWidgets(applicationContext)
                                                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wId)
                                                setResult(Activity.RESULT_OK, resultValue)
                                                finish()
                                            }
                                        }
                                    )

                                    AppMode.SETTINGS -> SettingsModeScreen(
                                        settings = settings,
                                        onUpdateSettings = viewModel::updateSettings,
                                        onResetSection = viewModel::resetSettingsSection,
                                        onLoadSampleData = { viewModel.loadSampleData(s.isVi) },
                                        onWipeAllData = viewModel::wipeAllData
                                    )
                                }

                                // Undo Destructive Action Glass Toast / Banner
                                if (lastDeletedTaskId != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 96.dp, start = 20.dp, end = 20.dp)
                                    ) {
                                        GlassSurface(
                                            modifier = Modifier.fillMaxWidth(),
                                            thickness = GlassThickness.THICK,
                                            cornerRadius = 20.dp,
                                            accentTint = tokens.accentColor,
                                            elevated = true,
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (s.isVi) "Đã chuyển vào Thùng rác" else "Moved to Trash",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Row {
                                                    GlassChip(
                                                        label = s.undo,
                                                        selected = true,
                                                        onClick = { viewModel.undoLastDelete() }
                                                    )
                                                    IconButton(
                                                        onClick = { viewModel.clearUndoState() },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = s.cancel, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick-Add Glass Composer Sheet
                    if (showQuickAdd) {
                        QuickAddComposerSheet(
                            lists = lists,
                            defaultListId = settings.defaultListId,
                            defaultPriority = settings.defaultPriority,
                            nlpEnabled = settings.nlpQuickAddEnabled,
                            initialEpochDay = quickAddInitialDay,
                            initialTimeMinute = quickAddInitialMinute,
                            onDismiss = { showQuickAdd = false },
                            onSubmitTask = { title, listId, priority, dueDate, startMin, dur, rec, tagNames, remOffsets ->
                                viewModel.createTaskFromComposer(
                                    title = title,
                                    listId = listId,
                                    priority = priority,
                                    dueDateEpochDay = dueDate,
                                    startTimeMinute = startMin,
                                    durationMinutes = dur,
                                    recurrenceRule = rec,
                                    tagNames = tagNames,
                                    reminderOffsets = remOffsets
                                )
                            }
                        )
                    }

                    // Full Task Detail Sheet
                    selectedTaskForDetail?.let { detailItem ->
                        TaskDetailDialog(
                            taskWithDetails = detailItem,
                            lists = lists,
                            onDismiss = { selectedTaskForDetail = null },
                            onSave = { updatedTask, subtasks, tagList, remOffsets, _ ->
                                viewModel.saveEditedTask(updatedTask, subtasks, tagList, remOffsets)
                            },
                            onDelete = { id ->
                                viewModel.softDeleteTask(id)
                            }
                        )
                    }

                    // "More" Modes Glass Sheet
                    if (showMoreModesSheet) {
                        MoreModesGlassDialog(
                            currentMode = currentMode,
                            onSelectMode = { mode ->
                                currentModeId = mode.id
                                showMoreModesSheet = false
                            },
                            onDismiss = { showMoreModesSheet = false }
                        )
                    }

                    // 3-Screen Skippable Onboarding on First Run
                    if (!settings.hasCompletedOnboarding && !isConfigureIntent) {
                        OnboardingOverlay(
                            onFinish = { loadSample ->
                                viewModel.completeOnboarding(loadSample, s.isVi)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreModesGlassDialog(
    currentMode: AppMode,
    onSelectMode: (AppMode) -> Unit,
    onDismiss: () -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 76.dp)
                    .testTag("more_modes_sheet"),
                thickness = GlassThickness.THICK,
                cornerRadius = 28.dp,
                elevated = true,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (s.isVi) "Chuyển chế độ làm việc" else "Switch Productivity Mode",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = s.cancel)
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppMode.entries.forEach { mode ->
                            val selected = mode == currentMode
                            GlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth(0.30f)
                                    .defaultMinSize(minHeight = 76.dp)
                                    .testTag("more_mode_${mode.id}"),
                                thickness = if (selected) GlassThickness.THICK else GlassThickness.REGULAR,
                                cornerRadius = 20.dp,
                                accentTint = if (selected) tokens.accentColor else null,
                                onClick = { onSelectMode(mode) },
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = mode.icon(),
                                        contentDescription = mode.localizedTitle(),
                                        tint = if (selected) tokens.accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = mode.localizedTitle(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) tokens.accentColor else MaterialTheme.colorScheme.onSurface
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
