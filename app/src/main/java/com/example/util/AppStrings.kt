package com.example.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class AppLanguage(val code: String, val labelEn: String, val labelVi: String) {
    AUTO("auto", "System Auto", "Tự động theo hệ thống"),
    VI("vi", "Tiếng Việt", "Tiếng Việt"),
    EN("en", "English", "English")
}

data class BilingualStrings(
    val isVi: Boolean,
    val appName: String = "Taskal",
    // Modes
    val modeHome: String = if (isVi) "Trang chủ" else "Home",
    val modeCalendar: String = if (isVi) "Lịch" else "Calendar",
    val modeTasks: String = if (isVi) "Công việc" else "Tasks",
    val modeBoard: String = if (isVi) "Bảng Kanban" else "Board",
    val modeMatrix: String = if (isVi) "Ma trận" else "Matrix",
    val modeNotes: String = if (isVi) "Ghi chú" else "Notes",
    val modeHabits: String = if (isVi) "Thói quen" else "Habits",
    val modeFocus: String = if (isVi) "Tập trung" else "Focus",
    val modeStats: String = if (isVi) "Thống kê" else "Stats",
    val modeWidgets: String = if (isVi) "Tiện ích" else "Widget Studio",
    val modeSettings: String = if (isVi) "Cài đặt" else "Settings",
    val modeMore: String = if (isVi) "Thêm" else "More",
    // Calendar Views
    val calMonth: String = if (isVi) "Tháng" else "Month",
    val calWeek: String = if (isVi) "Tuần" else "Week",
    val calThreeDay: String = if (isVi) "3 Ngày" else "3-Day",
    val calDay: String = if (isVi) "Ngày" else "Day",
    val calAgenda: String = if (isVi) "Lịch trình" else "Agenda",
    val today: String = if (isVi) "Hôm nay" else "Today",
    val tomorrow: String = if (isVi) "Ngày mai" else "Tomorrow",
    val thisWeekend: String = if (isVi) "Cuối tuần này" else "This weekend",
    val nextWeek: String = if (isVi) "Tuần sau" else "Next week",
    val pickDate: String = if (isVi) "Chọn ngày" else "Pick date",
    val allDay: String = if (isVi) "Cả ngày" else "All-day",
    val dayNoteTitle: String = if (isVi) "Ghi chú ngày" else "Day Note",
    val dayNoteHint: String = if (isVi) "Viết ghi chú hoặc nhật ký nhanh cho ngày này…" else "Write a quick note or reflection for this day…",
    // Tasks & Smart Lists
    val quickAddPlaceholder: String = if (isVi) "Thêm việc… vd: \"họp nhóm mai 3pm #work !cao\"" else "Add task… e.g. \"hop nhom mai 3pm #work !cao\"",
    val smartToday: String = if (isVi) "Hôm nay" else "Today",
    val smartTomorrow: String = if (isVi) "Ngày mai" else "Tomorrow",
    val smartNext7: String = if (isVi) "7 ngày tới" else "Next 7 Days",
    val smartOverdue: String = if (isVi) "Quá hạn" else "Overdue",
    val smartAll: String = if (isVi) "Tất cả" else "All Tasks",
    val smartNoDate: String = if (isVi) "Chưa có ngày" else "No Date",
    val smartCompleted: String = if (isVi) "Đã xong" else "Completed",
    val smartTrash: String = if (isVi) "Thùng rác" else "Trash",
    val priorityNone: String = if (isVi) "Không" else "None",
    val priorityLow: String = if (isVi) "Thấp" else "Low",
    val priorityMedium: String = if (isVi) "Trung bình" else "Medium",
    val priorityHigh: String = if (isVi) "Cao" else "High",
    val statusTodo: String = if (isVi) "Cần làm" else "To Do",
    val statusDoing: String = if (isVi) "Đang làm" else "Doing",
    val statusDone: String = if (isVi) "Hoàn thành" else "Done",
    // Empty States
    val emptyTasks: String = if (isVi) "Lịch trình trong trẻo — chạm + để ghi lại công việc tiếp theo." else "Crystal clear schedule — tap + to capture your next thought.",
    val emptyNotes: String = if (isVi) "Sổ tay kính đã sẵn sàng — lưu lại ý tưởng hoặc ghi chú ngày của bạn." else "Your glass notebook is open — jot down an idea or daily reflection.",
    val emptyHabits: String = if (isVi) "Nước chảy đá mòn — tạo thói quen hàng ngày hoặc hàng tuần đầu tiên." else "Small droplets carve stone — add your first daily or weekly habit.",
    val emptySearch: String = if (isVi) "Không tìm thấy kết quả phù hợp trong bộ nhớ thiết bị." else "No matching items found in your local vault.",
    // Actions
    val save: String = if (isVi) "Lưu" else "Save",
    val cancel: String = if (isVi) "Hủy" else "Cancel",
    val delete: String = if (isVi) "Xóa" else "Delete",
    val undo: String = if (isVi) "Hoàn tác" else "Undo",
    val resetToDefault: String = if (isVi) "Khôi phục mặc định" else "Reset to default",
    val complete: String = if (isVi) "Hoàn thành" else "Complete",
    val snooze: String = if (isVi) "Hoãn 10p" else "Snooze 10m",
    val searchPlaceholder: String = if (isVi) "Tìm kiếm công việc, ghi chú, cài đặt…" else "Search tasks, notes, settings…"
)

val LocalAppStrings = staticCompositionLocalOf { BilingualStrings(isVi = false) }

@Composable
fun ProvideLocalizedResources(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val targetLocale = remember(language, baseConfig) {
        when (language) {
            AppLanguage.VI -> Locale.forLanguageTag("vi-VN")
            AppLanguage.EN -> Locale.ENGLISH
            AppLanguage.AUTO -> {
                val sys = baseConfig.locales[0] ?: Locale.getDefault()
                if (sys.language.lowercase().startsWith("vi")) Locale.forLanguageTag("vi-VN") else Locale.ENGLISH
            }
        }
    }
    val isVi = targetLocale.language.lowercase().startsWith("vi")
    val localizedContext = remember(baseContext, targetLocale) {
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(targetLocale)
        baseContext.createConfigurationContext(config)
    }
    val localizedConfig = remember(baseConfig, targetLocale) {
        Configuration(baseConfig).apply { setLocale(targetLocale) }
    }
    val strings = remember(isVi) { BilingualStrings(isVi = isVi) }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
        LocalAppStrings provides strings,
        content = content
    )
}

object S {
    val current: BilingualStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStrings.current
}

fun Context.isVietnameseLocale(langCode: String): Boolean {
    return when (langCode) {
        "vi" -> true
        "en" -> false
        else -> resources.configuration.locales[0]?.language?.lowercase()?.startsWith("vi") == true
    }
}
