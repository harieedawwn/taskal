package com.example.data

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

val Context.taskalDataStore: DataStore<Preferences> by preferencesDataStore(name = "taskal_settings")

enum class ThemeMode(val labelEn: String, val labelVi: String) {
    DARK("Dark", "Tối"),
    LIGHT("Light", "Sáng"),
    AMOLED("AMOLED Black", "Đen AMOLED"),
    AUTO("System Auto", "Tự động"),
    TIME_AWARE("Time-Aware", "Theo thời gian")
}

enum class BackgroundStyle(val labelEn: String, val labelVi: String) {
    AURORA("Living Aurora", "Cực quang sống"),
    GRADIENT("Soft Gradient", "Chuyển sắc dịu"),
    SOLID("Minimal Solid", "Màu đơn giản"),
    PHOTO("Custom Photo", "Ảnh nền riêng")
}

enum class GlassThickness(val labelEn: String, val labelVi: String, val alphaBoost: Float, val borderBoost: Float) {
    THIN("Thin", "Mỏng", 0.08f, 0.25f),
    REGULAR("Regular", "Vừa", 0.14f, 0.42f),
    THICK("Thick", "Dày", 0.22f, 0.60f)
}

enum class UiDensity(val labelEn: String, val labelVi: String, val scale: Float) {
    COMPACT("Compact", "Gọn", 0.85f),
    COMFORTABLE("Comfortable", "Tiêu chuẩn", 1.0f),
    SPACIOUS("Spacious", "Thoáng", 1.18f)
}

enum class FontStyleOption(val labelEn: String, val labelVi: String) {
    MODERN("Modern (Jakarta)", "Hiện đại (Jakarta)"),
    EDITORIAL("Editorial (Playfair)", "Báo chí (Playfair)"),
    MONO("Technical (JetBrains Mono)", "Kỹ thuật (Mono)"),
    SYSTEM("System Sans", "Hệ thống")
}

enum class PerformanceMode(val labelEn: String, val labelVi: String) {
    AUTO("Auto", "Tự động"),
    ON("Performance (Battery Saver)", "Tiết kiệm năng lượng"),
    OFF("High Fidelity", "Hiệu ứng tối đa")
}

enum class AppMode(val id: String) {
    HOME("home"),
    CALENDAR("calendar"),
    TASKS("tasks"),
    BOARD("board"),
    MATRIX("matrix"),
    NOTES("notes"),
    HABITS("habits"),
    FOCUS("focus"),
    STATS("stats"),
    WIDGETS("widgets"),
    SETTINGS("settings")
}

enum class CalendarViewType(val id: String) {
    MONTH("month"),
    WEEK("week"),
    THREE_DAY("three_day"),
    DAY("day"),
    AGENDA("agenda")
}

enum class SwipeActionType(val labelEn: String, val labelVi: String) {
    COMPLETE("Complete", "Hoàn thành"),
    SNOOZE_TOMORROW("Postpone Tomorrow", "Dời sang ngày mai"),
    PIN("Pin / Unpin", "Ghim / Bỏ ghim"),
    DELETE("Move to Trash", "Chuyển vào thùng rác")
}

@Serializable
data class AppSettings(
    // Onboarding & Navigation
    val hasCompletedOnboarding: Boolean = false,
    val dockModesCsv: String = "home,calendar,tasks,notes",
    val startModeId: String = AppMode.CALENDAR.id,
    val floatingDock: Boolean = true,
    val showDockLabels: Boolean = true,

    // Appearance & Glass
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val accentPaletteIndex: Int = 0,
    val customAccentHex: String = "#38BDF8",
    val useCustomAccent: Boolean = false,
    val timeAwareAmbience: Boolean = true,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.AURORA,
    val photoBackgroundUri: String = "",
    val photoBlurStrength: Float = 0.5f,
    val photoDimAlpha: Float = 0.45f,
    val glassBlurStrength: Float = 0.72f,
    val glassTintOpacity: Float = 0.15f,
    val glassSaturation: Float = 1.15f,
    val glassEdgeGlow: Float = 0.65f,
    val glassSpecularSheen: Float = 0.45f,
    val glassTiltSensor: Boolean = true,
    val glassNoise: Float = 0.18f,
    val glassThickness: GlassThickness = GlassThickness.REGULAR,
    val cornerRadiusDp: Int = 22,
    val density: UiDensity = UiDensity.COMFORTABLE,
    val fontStyle: FontStyleOption = FontStyleOption.MODERN,
    val textScale: Float = 1.0f,
    val animationSpeed: Float = 1.0f, // 0f = Off, 0.5f..1.5f
    val hapticsEnabled: Boolean = true,
    val performanceMode: PerformanceMode = PerformanceMode.AUTO,

    // Calendar
    val firstDayMonday: Boolean = true,
    val showWeekNumbers: Boolean = false,
    val defaultCalendarView: CalendarViewType = CalendarViewType.MONTH,
    val showLunarCalendar: Boolean = true,
    val showVietnameseHolidays: Boolean = true,
    val timeGridStartHour: Int = 6,
    val timeGridEndHour: Int = 23,
    val snapStepMinutes: Int = 15,
    val deviceCalendarOverlay: Boolean = false,
    val showCompletedInCalendar: Boolean = true,
    val showNoteMarkersInCalendar: Boolean = true,

    // Tasks
    val defaultListId: Long = 1L,
    val defaultPriority: Int = 0,
    val defaultReminderOffsetMin: Int = -1, // -1 = none, 0 = at time, 10, 30, 60
    val defaultTaskHour: Int = 9,
    val nlpQuickAddEnabled: Boolean = true,
    val swipeRightAction: SwipeActionType = SwipeActionType.COMPLETE,
    val swipeLeftAction: SwipeActionType = SwipeActionType.SNOOZE_TOMORROW,
    val showCompletedTasks: Boolean = true,
    val autoArchiveDays: Int = 30,
    val confirmBeforeDelete: Boolean = true,

    // Notifications
    val notificationsEnabled: Boolean = true,
    val reminderChannelEnabled: Boolean = true,
    val dailyAgendaEnabled: Boolean = true,
    val dailyAgendaHour: Int = 8,
    val eveningReviewEnabled: Boolean = true,
    val eveningReviewHour: Int = 20,
    val overdueNudgesEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val soundVibrationEnabled: Boolean = true,
    val lockScreenHideTitles: Boolean = false,

    // Focus
    val focusWorkMinutes: Int = 25,
    val focusShortBreakMinutes: Int = 5,
    val focusLongBreakMinutes: Int = 15,
    val focusAutoStartNext: Boolean = false,
    val focusTickSound: Boolean = true,

    // Data & Language
    val autoDailyBackup: Boolean = true,
    val lastAutoBackupEpochMillis: Long = 0L,
    val language: AppLanguage = AppLanguage.AUTO,
    val use24HourFormat: Boolean = true,
    val dateFormatPattern: String = "dd/MM/yyyy"
) {
    val dockModes: List<AppMode>
        get() = dockModesCsv.split(",")
            .mapNotNull { id -> AppMode.entries.find { it.id == id.trim() } }
            .distinct()
            .take(4)
            .ifEmpty { listOf(AppMode.HOME, AppMode.CALENDAR, AppMode.TASKS, AppMode.NOTES) }

    fun isLowRamOrPerformance(context: Context): Boolean {
        return when (performanceMode) {
            PerformanceMode.ON -> true
            PerformanceMode.OFF -> false
            PerformanceMode.AUTO -> {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.isLowRamDevice == true
            }
        }
    }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val DOCK_MODES = stringPreferencesKey("dock_modes_csv")
        val START_MODE = stringPreferencesKey("start_mode_id")
        val FLOATING_DOCK = booleanPreferencesKey("floating_dock")
        val SHOW_DOCK_LABELS = booleanPreferencesKey("show_dock_labels")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_INDEX = intPreferencesKey("accent_palette_index")
        val CUSTOM_ACCENT = stringPreferencesKey("custom_accent_hex")
        val USE_CUSTOM_ACCENT = booleanPreferencesKey("use_custom_accent")
        val TIME_AWARE_AMBIENCE = booleanPreferencesKey("time_aware_ambience")
        val BG_STYLE = stringPreferencesKey("background_style")
        val PHOTO_URI = stringPreferencesKey("photo_bg_uri")
        val PHOTO_BLUR = floatPreferencesKey("photo_blur_strength")
        val PHOTO_DIM = floatPreferencesKey("photo_dim_alpha")
        val GLASS_BLUR = floatPreferencesKey("glass_blur_strength")
        val GLASS_TINT = floatPreferencesKey("glass_tint_opacity")
        val GLASS_SAT = floatPreferencesKey("glass_saturation")
        val GLASS_EDGE = floatPreferencesKey("glass_edge_glow")
        val GLASS_SPECULAR = floatPreferencesKey("glass_specular_sheen")
        val GLASS_TILT = booleanPreferencesKey("glass_tilt_sensor")
        val GLASS_NOISE = floatPreferencesKey("glass_noise")
        val GLASS_THICKNESS = stringPreferencesKey("glass_thickness")
        val CORNER_RADIUS = intPreferencesKey("corner_radius_dp")
        val DENSITY = stringPreferencesKey("ui_density")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val ANIM_SPEED = floatPreferencesKey("animation_speed")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val PERF_MODE = stringPreferencesKey("performance_mode")

        val FIRST_DAY_MON = booleanPreferencesKey("first_day_monday")
        val SHOW_WEEK_NUM = booleanPreferencesKey("show_week_numbers")
        val DEFAULT_CAL_VIEW = stringPreferencesKey("default_calendar_view")
        val SHOW_LUNAR = booleanPreferencesKey("show_lunar_calendar")
        val SHOW_VN_HOLIDAYS = booleanPreferencesKey("show_vn_holidays")
        val GRID_START_HR = intPreferencesKey("time_grid_start_hour")
        val GRID_END_HR = intPreferencesKey("time_grid_end_hour")
        val SNAP_STEP = intPreferencesKey("snap_step_minutes")
        val DEVICE_CAL = booleanPreferencesKey("device_calendar_overlay")
        val SHOW_COMP_CAL = booleanPreferencesKey("show_completed_in_cal")
        val SHOW_NOTES_CAL = booleanPreferencesKey("show_notes_in_cal")

        val DEF_LIST_ID = intPreferencesKey("default_list_id")
        val DEF_PRIORITY = intPreferencesKey("default_priority")
        val DEF_REMINDER = intPreferencesKey("default_reminder_offset")
        val DEF_TASK_HR = intPreferencesKey("default_task_hour")
        val NLP_ENABLED = booleanPreferencesKey("nlp_quick_add_enabled")
        val SWIPE_RIGHT = stringPreferencesKey("swipe_right_action")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left_action")
        val SHOW_COMPLETED_TASKS = booleanPreferencesKey("show_completed_tasks")
        val AUTO_ARCHIVE_DAYS = intPreferencesKey("auto_archive_days")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_before_delete")

        val NOTIF_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_CH = booleanPreferencesKey("reminder_channel_enabled")
        val DAILY_AGENDA = booleanPreferencesKey("daily_agenda_enabled")
        val DAILY_AGENDA_HR = intPreferencesKey("daily_agenda_hour")
        val EVENING_REVIEW = booleanPreferencesKey("evening_review_enabled")
        val EVENING_REVIEW_HR = intPreferencesKey("evening_review_hour")
        val OVERDUE_NUDGES = booleanPreferencesKey("overdue_nudges_enabled")
        val SNOOZE_MIN = intPreferencesKey("default_snooze_minutes")
        val SOUND_VIB = booleanPreferencesKey("sound_vibration_enabled")
        val LOCK_PRIVACY = booleanPreferencesKey("lock_screen_hide_titles")

        val FOCUS_WORK = intPreferencesKey("focus_work_minutes")
        val FOCUS_SHORT = intPreferencesKey("focus_short_break_minutes")
        val FOCUS_LONG = intPreferencesKey("focus_long_break_minutes")
        val FOCUS_AUTO = booleanPreferencesKey("focus_auto_start_next")
        val FOCUS_SOUND = booleanPreferencesKey("focus_tick_sound")

        val AUTO_BACKUP = booleanPreferencesKey("auto_daily_backup")
        val LANGUAGE = stringPreferencesKey("app_language")
        val USE_24H = booleanPreferencesKey("use_24_hour_format")
        val DATE_FORMAT = stringPreferencesKey("date_format_pattern")
    }

    val settingsFlow: Flow<AppSettings> = context.taskalDataStore.data.map { prefs ->
        val def = AppSettings()
        AppSettings(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING] ?: def.hasCompletedOnboarding,
            dockModesCsv = prefs[Keys.DOCK_MODES] ?: def.dockModesCsv,
            startModeId = prefs[Keys.START_MODE] ?: def.startModeId,
            floatingDock = prefs[Keys.FLOATING_DOCK] ?: def.floatingDock,
            showDockLabels = prefs[Keys.SHOW_DOCK_LABELS] ?: def.showDockLabels,

            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: def.themeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: def.dynamicColor,
            accentPaletteIndex = prefs[Keys.ACCENT_INDEX] ?: def.accentPaletteIndex,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT] ?: def.customAccentHex,
            useCustomAccent = prefs[Keys.USE_CUSTOM_ACCENT] ?: def.useCustomAccent,
            timeAwareAmbience = prefs[Keys.TIME_AWARE_AMBIENCE] ?: def.timeAwareAmbience,
            backgroundStyle = prefs[Keys.BG_STYLE]?.let { runCatching { BackgroundStyle.valueOf(it) }.getOrNull() } ?: def.backgroundStyle,
            photoBackgroundUri = prefs[Keys.PHOTO_URI] ?: def.photoBackgroundUri,
            photoBlurStrength = prefs[Keys.PHOTO_BLUR] ?: def.photoBlurStrength,
            photoDimAlpha = prefs[Keys.PHOTO_DIM] ?: def.photoDimAlpha,
            glassBlurStrength = prefs[Keys.GLASS_BLUR] ?: def.glassBlurStrength,
            glassTintOpacity = prefs[Keys.GLASS_TINT] ?: def.glassTintOpacity,
            glassSaturation = prefs[Keys.GLASS_SAT] ?: def.glassSaturation,
            glassEdgeGlow = prefs[Keys.GLASS_EDGE] ?: def.glassEdgeGlow,
            glassSpecularSheen = prefs[Keys.GLASS_SPECULAR] ?: def.glassSpecularSheen,
            glassTiltSensor = prefs[Keys.GLASS_TILT] ?: def.glassTiltSensor,
            glassNoise = prefs[Keys.GLASS_NOISE] ?: def.glassNoise,
            glassThickness = prefs[Keys.GLASS_THICKNESS]?.let { runCatching { GlassThickness.valueOf(it) }.getOrNull() } ?: def.glassThickness,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: def.cornerRadiusDp,
            density = prefs[Keys.DENSITY]?.let { runCatching { UiDensity.valueOf(it) }.getOrNull() } ?: def.density,
            fontStyle = prefs[Keys.FONT_STYLE]?.let { runCatching { FontStyleOption.valueOf(it) }.getOrNull() } ?: def.fontStyle,
            textScale = prefs[Keys.TEXT_SCALE] ?: def.textScale,
            animationSpeed = prefs[Keys.ANIM_SPEED] ?: def.animationSpeed,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: def.hapticsEnabled,
            performanceMode = prefs[Keys.PERF_MODE]?.let { runCatching { PerformanceMode.valueOf(it) }.getOrNull() } ?: def.performanceMode,

            firstDayMonday = prefs[Keys.FIRST_DAY_MON] ?: def.firstDayMonday,
            showWeekNumbers = prefs[Keys.SHOW_WEEK_NUM] ?: def.showWeekNumbers,
            defaultCalendarView = prefs[Keys.DEFAULT_CAL_VIEW]?.let { runCatching { CalendarViewType.valueOf(it) }.getOrNull() } ?: def.defaultCalendarView,
            showLunarCalendar = prefs[Keys.SHOW_LUNAR] ?: def.showLunarCalendar,
            showVietnameseHolidays = prefs[Keys.SHOW_VN_HOLIDAYS] ?: def.showVietnameseHolidays,
            timeGridStartHour = prefs[Keys.GRID_START_HR] ?: def.timeGridStartHour,
            timeGridEndHour = prefs[Keys.GRID_END_HR] ?: def.timeGridEndHour,
            snapStepMinutes = prefs[Keys.SNAP_STEP] ?: def.snapStepMinutes,
            deviceCalendarOverlay = prefs[Keys.DEVICE_CAL] ?: def.deviceCalendarOverlay,
            showCompletedInCalendar = prefs[Keys.SHOW_COMP_CAL] ?: def.showCompletedInCalendar,
            showNoteMarkersInCalendar = prefs[Keys.SHOW_NOTES_CAL] ?: def.showNoteMarkersInCalendar,

            defaultListId = (prefs[Keys.DEF_LIST_ID] ?: 1).toLong(),
            defaultPriority = prefs[Keys.DEF_PRIORITY] ?: def.defaultPriority,
            defaultReminderOffsetMin = prefs[Keys.DEF_REMINDER] ?: def.defaultReminderOffsetMin,
            defaultTaskHour = prefs[Keys.DEF_TASK_HR] ?: def.defaultTaskHour,
            nlpQuickAddEnabled = prefs[Keys.NLP_ENABLED] ?: def.nlpQuickAddEnabled,
            swipeRightAction = prefs[Keys.SWIPE_RIGHT]?.let { runCatching { SwipeActionType.valueOf(it) }.getOrNull() } ?: def.swipeRightAction,
            swipeLeftAction = prefs[Keys.SWIPE_LEFT]?.let { runCatching { SwipeActionType.valueOf(it) }.getOrNull() } ?: def.swipeLeftAction,
            showCompletedTasks = prefs[Keys.SHOW_COMPLETED_TASKS] ?: def.showCompletedTasks,
            autoArchiveDays = prefs[Keys.AUTO_ARCHIVE_DAYS] ?: def.autoArchiveDays,
            confirmBeforeDelete = prefs[Keys.CONFIRM_DELETE] ?: def.confirmBeforeDelete,

            notificationsEnabled = prefs[Keys.NOTIF_ENABLED] ?: def.notificationsEnabled,
            reminderChannelEnabled = prefs[Keys.REMINDER_CH] ?: def.reminderChannelEnabled,
            dailyAgendaEnabled = prefs[Keys.DAILY_AGENDA] ?: def.dailyAgendaEnabled,
            dailyAgendaHour = prefs[Keys.DAILY_AGENDA_HR] ?: def.dailyAgendaHour,
            eveningReviewEnabled = prefs[Keys.EVENING_REVIEW] ?: def.eveningReviewEnabled,
            eveningReviewHour = prefs[Keys.EVENING_REVIEW_HR] ?: def.eveningReviewHour,
            overdueNudgesEnabled = prefs[Keys.OVERDUE_NUDGES] ?: def.overdueNudgesEnabled,
            defaultSnoozeMinutes = prefs[Keys.SNOOZE_MIN] ?: def.defaultSnoozeMinutes,
            soundVibrationEnabled = prefs[Keys.SOUND_VIB] ?: def.soundVibrationEnabled,
            lockScreenHideTitles = prefs[Keys.LOCK_PRIVACY] ?: def.lockScreenHideTitles,

            focusWorkMinutes = prefs[Keys.FOCUS_WORK] ?: def.focusWorkMinutes,
            focusShortBreakMinutes = prefs[Keys.FOCUS_SHORT] ?: def.focusShortBreakMinutes,
            focusLongBreakMinutes = prefs[Keys.FOCUS_LONG] ?: def.focusLongBreakMinutes,
            focusAutoStartNext = prefs[Keys.FOCUS_AUTO] ?: def.focusAutoStartNext,
            focusTickSound = prefs[Keys.FOCUS_SOUND] ?: def.focusTickSound,

            autoDailyBackup = prefs[Keys.AUTO_BACKUP] ?: def.autoDailyBackup,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: def.language,
            use24HourFormat = prefs[Keys.USE_24H] ?: def.use24HourFormat,
            dateFormatPattern = prefs[Keys.DATE_FORMAT] ?: def.dateFormatPattern
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.taskalDataStore.edit { prefs ->
            val current = readFromPrefs(prefs)
            val updated = transform(current)
            writeToPrefs(prefs, updated)
        }
    }

    suspend fun resetSection(section: String) {
        val def = AppSettings()
        update { cur ->
            when (section) {
                "appearance" -> cur.copy(
                    themeMode = def.themeMode,
                    dynamicColor = def.dynamicColor,
                    accentPaletteIndex = def.accentPaletteIndex,
                    useCustomAccent = def.useCustomAccent,
                    timeAwareAmbience = def.timeAwareAmbience,
                    backgroundStyle = def.backgroundStyle,
                    glassBlurStrength = def.glassBlurStrength,
                    glassTintOpacity = def.glassTintOpacity,
                    glassSaturation = def.glassSaturation,
                    glassEdgeGlow = def.glassEdgeGlow,
                    glassSpecularSheen = def.glassSpecularSheen,
                    glassTiltSensor = def.glassTiltSensor,
                    glassNoise = def.glassNoise,
                    glassThickness = def.glassThickness,
                    cornerRadiusDp = def.cornerRadiusDp,
                    density = def.density,
                    fontStyle = def.fontStyle,
                    textScale = def.textScale,
                    animationSpeed = def.animationSpeed,
                    hapticsEnabled = def.hapticsEnabled,
                    performanceMode = def.performanceMode,
                    floatingDock = def.floatingDock,
                    showDockLabels = def.showDockLabels,
                    dockModesCsv = def.dockModesCsv,
                    startModeId = def.startModeId
                )
                "calendar" -> cur.copy(
                    firstDayMonday = def.firstDayMonday,
                    showWeekNumbers = def.showWeekNumbers,
                    defaultCalendarView = def.defaultCalendarView,
                    showLunarCalendar = def.showLunarCalendar,
                    showVietnameseHolidays = def.showVietnameseHolidays,
                    timeGridStartHour = def.timeGridStartHour,
                    timeGridEndHour = def.timeGridEndHour,
                    snapStepMinutes = def.snapStepMinutes,
                    deviceCalendarOverlay = def.deviceCalendarOverlay,
                    showCompletedInCalendar = def.showCompletedInCalendar,
                    showNoteMarkersInCalendar = def.showNoteMarkersInCalendar
                )
                "tasks" -> cur.copy(
                    defaultListId = def.defaultListId,
                    defaultPriority = def.defaultPriority,
                    defaultReminderOffsetMin = def.defaultReminderOffsetMin,
                    defaultTaskHour = def.defaultTaskHour,
                    nlpQuickAddEnabled = def.nlpQuickAddEnabled,
                    swipeRightAction = def.swipeRightAction,
                    swipeLeftAction = def.swipeLeftAction,
                    showCompletedTasks = def.showCompletedTasks,
                    autoArchiveDays = def.autoArchiveDays,
                    confirmBeforeDelete = def.confirmBeforeDelete
                )
                "notifications" -> cur.copy(
                    notificationsEnabled = def.notificationsEnabled,
                    reminderChannelEnabled = def.reminderChannelEnabled,
                    dailyAgendaEnabled = def.dailyAgendaEnabled,
                    dailyAgendaHour = def.dailyAgendaHour,
                    eveningReviewEnabled = def.eveningReviewEnabled,
                    eveningReviewHour = def.eveningReviewHour,
                    overdueNudgesEnabled = def.overdueNudgesEnabled,
                    defaultSnoozeMinutes = def.defaultSnoozeMinutes,
                    soundVibrationEnabled = def.soundVibrationEnabled,
                    lockScreenHideTitles = def.lockScreenHideTitles
                )
                "focus" -> cur.copy(
                    focusWorkMinutes = def.focusWorkMinutes,
                    focusShortBreakMinutes = def.focusShortBreakMinutes,
                    focusLongBreakMinutes = def.focusLongBreakMinutes,
                    focusAutoStartNext = def.focusAutoStartNext,
                    focusTickSound = def.focusTickSound
                )
                "language" -> cur.copy(
                    language = def.language,
                    use24HourFormat = def.use24HourFormat,
                    dateFormatPattern = def.dateFormatPattern
                )
                else -> def
            }
        }
    }

    private fun readFromPrefs(prefs: Preferences): AppSettings {
        val def = AppSettings()
        return AppSettings(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING] ?: def.hasCompletedOnboarding,
            dockModesCsv = prefs[Keys.DOCK_MODES] ?: def.dockModesCsv,
            startModeId = prefs[Keys.START_MODE] ?: def.startModeId,
            floatingDock = prefs[Keys.FLOATING_DOCK] ?: def.floatingDock,
            showDockLabels = prefs[Keys.SHOW_DOCK_LABELS] ?: def.showDockLabels,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: def.themeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: def.dynamicColor,
            accentPaletteIndex = prefs[Keys.ACCENT_INDEX] ?: def.accentPaletteIndex,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT] ?: def.customAccentHex,
            useCustomAccent = prefs[Keys.USE_CUSTOM_ACCENT] ?: def.useCustomAccent,
            timeAwareAmbience = prefs[Keys.TIME_AWARE_AMBIENCE] ?: def.timeAwareAmbience,
            backgroundStyle = prefs[Keys.BG_STYLE]?.let { runCatching { BackgroundStyle.valueOf(it) }.getOrNull() } ?: def.backgroundStyle,
            photoBackgroundUri = prefs[Keys.PHOTO_URI] ?: def.photoBackgroundUri,
            photoBlurStrength = prefs[Keys.PHOTO_BLUR] ?: def.photoBlurStrength,
            photoDimAlpha = prefs[Keys.PHOTO_DIM] ?: def.photoDimAlpha,
            glassBlurStrength = prefs[Keys.GLASS_BLUR] ?: def.glassBlurStrength,
            glassTintOpacity = prefs[Keys.GLASS_TINT] ?: def.glassTintOpacity,
            glassSaturation = prefs[Keys.GLASS_SAT] ?: def.glassSaturation,
            glassEdgeGlow = prefs[Keys.GLASS_EDGE] ?: def.glassEdgeGlow,
            glassSpecularSheen = prefs[Keys.GLASS_SPECULAR] ?: def.glassSpecularSheen,
            glassTiltSensor = prefs[Keys.GLASS_TILT] ?: def.glassTiltSensor,
            glassNoise = prefs[Keys.GLASS_NOISE] ?: def.glassNoise,
            glassThickness = prefs[Keys.GLASS_THICKNESS]?.let { runCatching { GlassThickness.valueOf(it) }.getOrNull() } ?: def.glassThickness,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: def.cornerRadiusDp,
            density = prefs[Keys.DENSITY]?.let { runCatching { UiDensity.valueOf(it) }.getOrNull() } ?: def.density,
            fontStyle = prefs[Keys.FONT_STYLE]?.let { runCatching { FontStyleOption.valueOf(it) }.getOrNull() } ?: def.fontStyle,
            textScale = prefs[Keys.TEXT_SCALE] ?: def.textScale,
            animationSpeed = prefs[Keys.ANIM_SPEED] ?: def.animationSpeed,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: def.hapticsEnabled,
            performanceMode = prefs[Keys.PERF_MODE]?.let { runCatching { PerformanceMode.valueOf(it) }.getOrNull() } ?: def.performanceMode,
            firstDayMonday = prefs[Keys.FIRST_DAY_MON] ?: def.firstDayMonday,
            showWeekNumbers = prefs[Keys.SHOW_WEEK_NUM] ?: def.showWeekNumbers,
            defaultCalendarView = prefs[Keys.DEFAULT_CAL_VIEW]?.let { runCatching { CalendarViewType.valueOf(it) }.getOrNull() } ?: def.defaultCalendarView,
            showLunarCalendar = prefs[Keys.SHOW_LUNAR] ?: def.showLunarCalendar,
            showVietnameseHolidays = prefs[Keys.SHOW_VN_HOLIDAYS] ?: def.showVietnameseHolidays,
            timeGridStartHour = prefs[Keys.GRID_START_HR] ?: def.timeGridStartHour,
            timeGridEndHour = prefs[Keys.GRID_END_HR] ?: def.timeGridEndHour,
            snapStepMinutes = prefs[Keys.SNAP_STEP] ?: def.snapStepMinutes,
            deviceCalendarOverlay = prefs[Keys.DEVICE_CAL] ?: def.deviceCalendarOverlay,
            showCompletedInCalendar = prefs[Keys.SHOW_COMP_CAL] ?: def.showCompletedInCalendar,
            showNoteMarkersInCalendar = prefs[Keys.SHOW_NOTES_CAL] ?: def.showNoteMarkersInCalendar,
            defaultListId = (prefs[Keys.DEF_LIST_ID] ?: 1).toLong(),
            defaultPriority = prefs[Keys.DEF_PRIORITY] ?: def.defaultPriority,
            defaultReminderOffsetMin = prefs[Keys.DEF_REMINDER] ?: def.defaultReminderOffsetMin,
            defaultTaskHour = prefs[Keys.DEF_TASK_HR] ?: def.defaultTaskHour,
            nlpQuickAddEnabled = prefs[Keys.NLP_ENABLED] ?: def.nlpQuickAddEnabled,
            swipeRightAction = prefs[Keys.SWIPE_RIGHT]?.let { runCatching { SwipeActionType.valueOf(it) }.getOrNull() } ?: def.swipeRightAction,
            swipeLeftAction = prefs[Keys.SWIPE_LEFT]?.let { runCatching { SwipeActionType.valueOf(it) }.getOrNull() } ?: def.swipeLeftAction,
            showCompletedTasks = prefs[Keys.SHOW_COMPLETED_TASKS] ?: def.showCompletedTasks,
            autoArchiveDays = prefs[Keys.AUTO_ARCHIVE_DAYS] ?: def.autoArchiveDays,
            confirmBeforeDelete = prefs[Keys.CONFIRM_DELETE] ?: def.confirmBeforeDelete,
            notificationsEnabled = prefs[Keys.NOTIF_ENABLED] ?: def.notificationsEnabled,
            reminderChannelEnabled = prefs[Keys.REMINDER_CH] ?: def.reminderChannelEnabled,
            dailyAgendaEnabled = prefs[Keys.DAILY_AGENDA] ?: def.dailyAgendaEnabled,
            dailyAgendaHour = prefs[Keys.DAILY_AGENDA_HR] ?: def.dailyAgendaHour,
            eveningReviewEnabled = prefs[Keys.EVENING_REVIEW] ?: def.eveningReviewEnabled,
            eveningReviewHour = prefs[Keys.EVENING_REVIEW_HR] ?: def.eveningReviewHour,
            overdueNudgesEnabled = prefs[Keys.OVERDUE_NUDGES] ?: def.overdueNudgesEnabled,
            defaultSnoozeMinutes = prefs[Keys.SNOOZE_MIN] ?: def.defaultSnoozeMinutes,
            soundVibrationEnabled = prefs[Keys.SOUND_VIB] ?: def.soundVibrationEnabled,
            lockScreenHideTitles = prefs[Keys.LOCK_PRIVACY] ?: def.lockScreenHideTitles,
            focusWorkMinutes = prefs[Keys.FOCUS_WORK] ?: def.focusWorkMinutes,
            focusShortBreakMinutes = prefs[Keys.FOCUS_SHORT] ?: def.focusShortBreakMinutes,
            focusLongBreakMinutes = prefs[Keys.FOCUS_LONG] ?: def.focusLongBreakMinutes,
            focusAutoStartNext = prefs[Keys.FOCUS_AUTO] ?: def.focusAutoStartNext,
            focusTickSound = prefs[Keys.FOCUS_SOUND] ?: def.focusTickSound,
            autoDailyBackup = prefs[Keys.AUTO_BACKUP] ?: def.autoDailyBackup,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: def.language,
            use24HourFormat = prefs[Keys.USE_24H] ?: def.use24HourFormat,
            dateFormatPattern = prefs[Keys.DATE_FORMAT] ?: def.dateFormatPattern
        )
    }

    private fun writeToPrefs(prefs: androidx.datastore.preferences.core.MutablePreferences, s: AppSettings) {
        prefs[Keys.ONBOARDING] = s.hasCompletedOnboarding
        prefs[Keys.DOCK_MODES] = s.dockModesCsv
        prefs[Keys.START_MODE] = s.startModeId
        prefs[Keys.FLOATING_DOCK] = s.floatingDock
        prefs[Keys.SHOW_DOCK_LABELS] = s.showDockLabels
        prefs[Keys.THEME_MODE] = s.themeMode.name
        prefs[Keys.DYNAMIC_COLOR] = s.dynamicColor
        prefs[Keys.ACCENT_INDEX] = s.accentPaletteIndex
        prefs[Keys.CUSTOM_ACCENT] = s.customAccentHex
        prefs[Keys.USE_CUSTOM_ACCENT] = s.useCustomAccent
        prefs[Keys.TIME_AWARE_AMBIENCE] = s.timeAwareAmbience
        prefs[Keys.BG_STYLE] = s.backgroundStyle.name
        prefs[Keys.PHOTO_URI] = s.photoBackgroundUri
        prefs[Keys.PHOTO_BLUR] = s.photoBlurStrength
        prefs[Keys.PHOTO_DIM] = s.photoDimAlpha
        prefs[Keys.GLASS_BLUR] = s.glassBlurStrength
        prefs[Keys.GLASS_TINT] = s.glassTintOpacity
        prefs[Keys.GLASS_SAT] = s.glassSaturation
        prefs[Keys.GLASS_EDGE] = s.glassEdgeGlow
        prefs[Keys.GLASS_SPECULAR] = s.glassSpecularSheen
        prefs[Keys.GLASS_TILT] = s.glassTiltSensor
        prefs[Keys.GLASS_NOISE] = s.glassNoise
        prefs[Keys.GLASS_THICKNESS] = s.glassThickness.name
        prefs[Keys.CORNER_RADIUS] = s.cornerRadiusDp
        prefs[Keys.DENSITY] = s.density.name
        prefs[Keys.FONT_STYLE] = s.fontStyle.name
        prefs[Keys.TEXT_SCALE] = s.textScale
        prefs[Keys.ANIM_SPEED] = s.animationSpeed
        prefs[Keys.HAPTICS] = s.hapticsEnabled
        prefs[Keys.PERF_MODE] = s.performanceMode.name
        prefs[Keys.FIRST_DAY_MON] = s.firstDayMonday
        prefs[Keys.SHOW_WEEK_NUM] = s.showWeekNumbers
        prefs[Keys.DEFAULT_CAL_VIEW] = s.defaultCalendarView.name
        prefs[Keys.SHOW_LUNAR] = s.showLunarCalendar
        prefs[Keys.SHOW_VN_HOLIDAYS] = s.showVietnameseHolidays
        prefs[Keys.GRID_START_HR] = s.timeGridStartHour
        prefs[Keys.GRID_END_HR] = s.timeGridEndHour
        prefs[Keys.SNAP_STEP] = s.snapStepMinutes
        prefs[Keys.DEVICE_CAL] = s.deviceCalendarOverlay
        prefs[Keys.SHOW_COMP_CAL] = s.showCompletedInCalendar
        prefs[Keys.SHOW_NOTES_CAL] = s.showNoteMarkersInCalendar
        prefs[Keys.DEF_LIST_ID] = s.defaultListId.toInt()
        prefs[Keys.DEF_PRIORITY] = s.defaultPriority
        prefs[Keys.DEF_REMINDER] = s.defaultReminderOffsetMin
        prefs[Keys.DEF_TASK_HR] = s.defaultTaskHour
        prefs[Keys.NLP_ENABLED] = s.nlpQuickAddEnabled
        prefs[Keys.SWIPE_RIGHT] = s.swipeRightAction.name
        prefs[Keys.SWIPE_LEFT] = s.swipeLeftAction.name
        prefs[Keys.SHOW_COMPLETED_TASKS] = s.showCompletedTasks
        prefs[Keys.AUTO_ARCHIVE_DAYS] = s.autoArchiveDays
        prefs[Keys.CONFIRM_DELETE] = s.confirmBeforeDelete
        prefs[Keys.NOTIF_ENABLED] = s.notificationsEnabled
        prefs[Keys.REMINDER_CH] = s.reminderChannelEnabled
        prefs[Keys.DAILY_AGENDA] = s.dailyAgendaEnabled
        prefs[Keys.DAILY_AGENDA_HR] = s.dailyAgendaHour
        prefs[Keys.EVENING_REVIEW] = s.eveningReviewEnabled
        prefs[Keys.EVENING_REVIEW_HR] = s.eveningReviewHour
        prefs[Keys.OVERDUE_NUDGES] = s.overdueNudgesEnabled
        prefs[Keys.SNOOZE_MIN] = s.defaultSnoozeMinutes
        prefs[Keys.SOUND_VIB] = s.soundVibrationEnabled
        prefs[Keys.LOCK_PRIVACY] = s.lockScreenHideTitles
        prefs[Keys.FOCUS_WORK] = s.focusWorkMinutes
        prefs[Keys.FOCUS_SHORT] = s.focusShortBreakMinutes
        prefs[Keys.FOCUS_LONG] = s.focusLongBreakMinutes
        prefs[Keys.FOCUS_AUTO] = s.focusAutoStartNext
        prefs[Keys.FOCUS_SOUND] = s.focusTickSound
        prefs[Keys.AUTO_BACKUP] = s.autoDailyBackup
        prefs[Keys.LANGUAGE] = s.language.name
        prefs[Keys.USE_24H] = s.use24HourFormat
        prefs[Keys.DATE_FORMAT] = s.dateFormatPattern
    }
}
