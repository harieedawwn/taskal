package com.example.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppMode
import com.example.data.AppSettings
import com.example.data.BackgroundStyle
import com.example.data.CalendarViewType
import com.example.data.FontStyleOption
import com.example.data.GlassThickness
import com.example.data.PerformanceMode
import com.example.data.SwipeActionType
import com.example.data.ThemeMode
import com.example.data.UiDensity
import com.example.domain.BackupManager
import com.example.notifications.TaskalNotificationHelper
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.glass.localizedTitle
import com.example.ui.theme.CuratedAccentPalettes
import com.example.util.AppLanguage
import com.example.util.S
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsModeScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onResetSection: (String) -> Unit,
    onLoadSampleData: () -> Unit,
    onWipeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showWipeConfirm by remember { mutableStateOf(false) }
    var localBackups by remember { mutableStateOf(BackupManager.listLocalBackups(context)) }

    // Photo Picker for Custom Background
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateSettings { it.copy(backgroundStyle = BackgroundStyle.PHOTO, photoBackgroundUri = uri.toString()) }
        }
    }

    // SAF Launchers for JSON, CSV, ICS
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val payload = BackupManager.exportFullBackupJson(context)
                context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
                statusMessage = if (s.isVi) "Đã xuất bản sao lưu JSON thành công!" else "Full JSON backup exported!"
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val ok = BackupManager.importFullBackupJson(context, raw)
                statusMessage = if (ok) {
                    if (s.isVi) "Khôi phục dữ liệu JSON thành công!" else "Restored full JSON backup!"
                } else {
                    if (s.isVi) "Tệp sao lưu không hợp lệ." else "Invalid JSON backup file."
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val csv = BackupManager.exportTasksCsv(context)
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                statusMessage = if (s.isVi) "Đã xuất CSV công việc!" else "Tasks exported to CSV!"
            }
        }
    }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ics = BackupManager.exportTasksIcs(context)
                context.contentResolver.openOutputStream(uri)?.use { it.write(ics.toByteArray()) }
                statusMessage = if (s.isVi) "Đã xuất lịch .ics!" else "Calendar exported to .ics!"
            }
        }
    }

    val importIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val count = BackupManager.importTasksIcs(context, raw)
                statusMessage = if (s.isVi) "Đã nhập $count sự kiện từ .ics!" else "Imported $count events from .ics!"
            }
        }
    }

    // Permission Launcher for Notifications
    var hasNotifPerm by remember { mutableStateOf(TaskalNotificationHelper.hasNotificationPermission(context)) }
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
    }

    fun matchesSection(keywords: String): Boolean {
        return searchQuery.isBlank() || keywords.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 115.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = s.modeSettings,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Search Bar
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.THIN,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(s.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_search_input")
            )
        }

        statusMessage?.let { msg ->
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                thickness = GlassThickness.THICK,
                cornerRadius = 16.dp,
                accentTint = Color(0xFF34D399),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(text = msg, style = MaterialTheme.typography.labelLarge, color = Color(0xFF34D399))
            }
        }

        // SECTION 1: APPEARANCE & LIQUID GLASS
        if (matchesSection("appearance theme dark light amoled aurora glass blur tint saturation corner font density animation dock")) {
            SettingsGlassSection(
                title = if (s.isVi) "1. Giao diện & Kính lỏng (Liquid Glass)" else "1. Appearance & Liquid Glass",
                onReset = { onResetSection("appearance") }
            ) {
                Text(if (s.isVi) "Chế độ màu" else "Theme Mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { tm ->
                        GlassChip(
                            label = if (s.isVi) tm.labelVi else tm.labelEn,
                            selected = settings.themeMode == tm,
                            onClick = { onUpdateSettings { it.copy(themeMode = tm) } }
                        )
                    }
                }

                SettingToggleRow(
                    label = if (s.isVi) "Màu động Material You (Android 12+)" else "Material You Dynamic Color",
                    checked = settings.dynamicColor,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(dynamicColor = v) } }
                )

                SettingToggleRow(
                    label = if (s.isVi) "Ánh sáng theo thời gian trong ngày" else "Time-Aware Ambience (Warm Golden Hour / Cool Night)",
                    checked = settings.timeAwareAmbience,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(timeAwareAmbience = v) } }
                )

                Text(if (s.isVi) "12 Bảng màu nhấn" else "12 Curated Accent Palettes", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CuratedAccentPalettes.forEach { pal ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(pal.primary)
                                .border(
                                    width = if (!settings.useCustomAccent && settings.accentPaletteIndex == pal.id) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateSettings { it.copy(accentPaletteIndex = pal.id, useCustomAccent = false) }
                                }
                        )
                    }
                }

                Text(if (s.isVi) "Nền sống (Living Backdrop)" else "Backdrop Style", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BackgroundStyle.entries.forEach { bg ->
                        GlassChip(
                            label = if (s.isVi) bg.labelVi else bg.labelEn,
                            selected = settings.backgroundStyle == bg,
                            onClick = {
                                if (bg == BackgroundStyle.PHOTO) {
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    onUpdateSettings { it.copy(backgroundStyle = bg) }
                                }
                            }
                        )
                    }
                }

                Text(if (s.isVi) "Độ dày kính" else "Glass Thickness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassThickness.entries.forEach { th ->
                        GlassChip(
                            label = if (s.isVi) th.labelVi else th.labelEn,
                            selected = settings.glassThickness == th,
                            onClick = { onUpdateSettings { it.copy(glassThickness = th) } }
                        )
                    }
                }

                SettingSliderRow(
                    label = if (s.isVi) "Độ mờ kính (Tint Opacity)" else "Glass Tint Opacity",
                    value = settings.glassTintOpacity,
                    range = 0.05f..0.45f,
                    onValueChange = { v -> onUpdateSettings { it.copy(glassTintOpacity = v) } }
                )

                SettingSliderRow(
                    label = if (s.isVi) "Viền sáng 1px (Edge Glow)" else "1px Luminous Edge Glow",
                    value = settings.glassEdgeGlow,
                    range = 0.1f..1.0f,
                    onValueChange = { v -> onUpdateSettings { it.copy(glassEdgeGlow = v) } }
                )

                SettingSliderRow(
                    label = if (s.isVi) "Ánh phản quang (Specular Sheen)" else "Specular Sheen",
                    value = settings.glassSpecularSheen,
                    range = 0f..1f,
                    onValueChange = { v -> onUpdateSettings { it.copy(glassSpecularSheen = v) } }
                )

                SettingToggleRow(
                    label = if (s.isVi) "Ánh kính nghiêng theo cảm biến góc máy" else "Tilt-Reactive Specular Reflection (Sensor)",
                    checked = settings.glassTiltSensor,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(glassTiltSensor = v) } }
                )

                SettingSliderRow(
                    label = if (s.isVi) "Độ bo góc (${settings.cornerRadiusDp}dp)" else "Corner Radius (${settings.cornerRadiusDp}dp)",
                    value = settings.cornerRadiusDp.toFloat(),
                    range = 6f..34f,
                    onValueChange = { v -> onUpdateSettings { it.copy(cornerRadiusDp = v.toInt()) } }
                )

                Text(if (s.isVi) "Kiểu chữ (Typography)" else "Font Family", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FontStyleOption.entries.forEach { f ->
                        GlassChip(
                            label = if (s.isVi) f.labelVi else f.labelEn,
                            selected = settings.fontStyle == f,
                            onClick = { onUpdateSettings { it.copy(fontStyle = f) } }
                        )
                    }
                }

                SettingSliderRow(
                    label = if (s.isVi) "Cỡ chữ (${(settings.textScale * 100).toInt()}%)" else "Text Scale (${(settings.textScale * 100).toInt()}%)",
                    value = settings.textScale,
                    range = 0.85f..1.35f,
                    onValueChange = { v -> onUpdateSettings { it.copy(textScale = v) } }
                )

                Text(if (s.isVi) "Mật độ giao diện" else "Layout Density", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UiDensity.entries.forEach { d ->
                        GlassChip(
                            label = if (s.isVi) d.labelVi else d.labelEn,
                            selected = settings.density == d,
                            onClick = { onUpdateSettings { it.copy(density = d) } }
                        )
                    }
                }

                SettingSliderRow(
                    label = if (s.isVi) "Tốc độ chuyển động (${"%.1f".format(settings.animationSpeed)}x)" else "Animation Speed (${"%.1f".format(settings.animationSpeed)}x)",
                    value = settings.animationSpeed,
                    range = 0f..1.5f,
                    onValueChange = { v -> onUpdateSettings { it.copy(animationSpeed = v) } }
                )

                Text(if (s.isVi) "Chế độ Hiệu năng" else "Performance Mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerformanceMode.entries.forEach { pm ->
                        GlassChip(
                            label = if (s.isVi) pm.labelVi else pm.labelEn,
                            selected = settings.performanceMode == pm,
                            onClick = { onUpdateSettings { it.copy(performanceMode = pm) } }
                        )
                    }
                }

                SettingToggleRow(
                    label = if (s.isVi) "Thanh điều hướng nổi (Floating Liquid Dock)" else "Floating Liquid Dock Pill",
                    checked = settings.floatingDock,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(floatingDock = v) } }
                )

                SettingToggleRow(
                    label = if (s.isVi) "Hiện nhãn chữ dưới thanh Dock" else "Show Dock Tab Labels",
                    checked = settings.showDockLabels,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showDockLabels = v) } }
                )

                Text(if (s.isVi) "Màn hình khởi động mặc định" else "Default Start Mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(AppMode.CALENDAR, AppMode.HOME, AppMode.TASKS, AppMode.NOTES, AppMode.FOCUS).forEach { m ->
                        GlassChip(
                            label = m.localizedTitle(),
                            selected = settings.startModeId == m.id,
                            onClick = { onUpdateSettings { it.copy(startModeId = m.id) } }
                        )
                    }
                }
            }
        }

        // SECTION 2: CALENDAR
        if (matchesSection("calendar month week lunar am lich holiday snap hour")) {
            SettingsGlassSection(
                title = if (s.isVi) "2. Lịch & Âm lịch Việt Nam" else "2. Calendar & Lunar Date",
                onReset = { onResetSection("calendar") }
            ) {
                SettingToggleRow(
                    label = if (s.isVi) "Bắt đầu tuần vào Thứ Hai" else "Start Week on Monday",
                    checked = settings.firstDayMonday,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(firstDayMonday = v) } }
                )
                SettingToggleRow(
                    label = if (s.isVi) "Hiện số thứ tự tuần" else "Show ISO Week Numbers",
                    checked = settings.showWeekNumbers,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showWeekNumbers = v) } }
                )
                SettingToggleRow(
                    label = if (s.isVi) "Hiện Âm lịch Việt Nam (Hồ Ngọc Đức)" else "Show Vietnamese Lunar Calendar (Âm lịch)",
                    checked = settings.showLunarCalendar,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showLunarCalendar = v) } }
                )
                SettingToggleRow(
                    label = if (s.isVi) "Đánh dấu ngày Lễ & Tết Việt Nam" else "Show Vietnamese Holiday Markers",
                    checked = settings.showVietnameseHolidays,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showVietnameseHolidays = v) } }
                )
                SettingToggleRow(
                    label = if (s.isVi) "Hiện lịch thiết bị (READ_CALENDAR)" else "Overlay Device Calendars (Read-Only)",
                    checked = settings.deviceCalendarOverlay,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(deviceCalendarOverlay = v) } }
                )
                Text(if (s.isVi) "Bước nhảy kéo thả khung giờ" else "Time-Grid Snap Step", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (step in listOf(5, 15, 30)) {
                        GlassChip(
                            label = "${step}m",
                            selected = settings.snapStepMinutes == step,
                            onClick = { onUpdateSettings { it.copy(snapStepMinutes = step) } }
                        )
                    }
                }
            }
        }

        // SECTION 3: TASKS & NLP
        if (matchesSection("tasks nlp quick add swipe priority completed archive")) {
            SettingsGlassSection(
                title = if (s.isVi) "3. Công việc & Ngôn ngữ tự nhiên" else "3. Tasks & Natural-Language Capture",
                onReset = { onResetSection("tasks") }
            ) {
                SettingToggleRow(
                    label = if (s.isVi) "Phân tích ngôn ngữ tự nhiên (Tiếng Việt & Tiếng Anh)" else "Natural-Language Quick Add Parser (EN + VI)",
                    checked = settings.nlpQuickAddEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(nlpQuickAddEnabled = v) } }
                )
                SettingToggleRow(
                    label = if (s.isVi) "Hiện công việc đã hoàn thành" else "Show Completed Tasks",
                    checked = settings.showCompletedTasks,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showCompletedTasks = v) } }
                )
                Text(if (s.isVi) "Vuốt sang phải để:" else "Swipe Right Action:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwipeActionType.entries.forEach { act ->
                        GlassChip(
                            label = if (s.isVi) act.labelVi else act.labelEn,
                            selected = settings.swipeRightAction == act,
                            onClick = { onUpdateSettings { it.copy(swipeRightAction = act) } }
                        )
                    }
                }
            }
        }

        // SECTION 4: NOTIFICATIONS & RELIABILITY CHECK
        if (matchesSection("notifications alarms reliability battery exact permission")) {
            SettingsGlassSection(
                title = if (s.isVi) "4. Thông báo & Kiểm tra Độ tin cậy" else "4. Notifications & Reliability Check",
                onReset = { onResetSection("notifications") }
            ) {
                SettingToggleRow(
                    label = if (s.isVi) "Bật thông báo nhắc việc" else "Enable Notifications",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(notificationsEnabled = v) } }
                )
                SettingToggleRow(
                    label = if (s.isVi) "Ẩn tiêu đề trên màn hình khóa" else "Lock-Screen Privacy (Hide Task Titles)",
                    checked = settings.lockScreenHideTitles,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(lockScreenHideTitles = v) } }
                )

                // Reliability Check Card
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = GlassThickness.THIN,
                    cornerRadius = 18.dp,
                    accentTint = tokens.accentColor,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    val exactOk = TaskalNotificationHelper.canScheduleExactAlarms(context)
                    val batteryOk = TaskalNotificationHelper.isIgnoringBatteryOptimizations(context)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (s.isVi) "Kiểm tra độ tin cậy chuông báo" else "Alarm & Notification Reliability Check",
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.accentColor
                        )
                        ReliabilityRow(
                            title = if (s.isVi) "Quyền Thông báo (POST_NOTIFICATIONS)" else "Notification Permission",
                            isOk = hasNotifPerm,
                            actionLabel = if (s.isVi) "Cấp quyền" else "Grant",
                            onAction = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                        ReliabilityRow(
                            title = if (s.isVi) "Báo thức chính xác (Exact Alarms)" else "Exact Alarms (Doze-Safe)",
                            isOk = exactOk,
                            actionLabel = if (s.isVi) "Mở cài đặt" else "Configure",
                            onAction = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    }
                                }
                            }
                        )
                        ReliabilityRow(
                            title = if (s.isVi) "Không bị giới hạn pin nền" else "Unrestricted Battery Usage",
                            isOk = batteryOk,
                            actionLabel = if (s.isVi) "Hướng dẫn" else "Optimize",
                            onAction = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }
                        )
                    }
                }
            }
        }

        // SECTION 5: FOCUS
        if (matchesSection("focus pomodoro timer break")) {
            SettingsGlassSection(
                title = if (s.isVi) "5. Đồng hồ Tập trung (Focus)" else "5. Focus Timer",
                onReset = { onResetSection("focus") }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (s.isVi) "Thời lượng Pomodoro:" else "Work Duration:")
                    for (m in listOf(15, 25, 45, 60)) {
                        GlassChip(
                            label = "${m}m",
                            selected = settings.focusWorkMinutes == m,
                            onClick = { onUpdateSettings { it.copy(focusWorkMinutes = m) } }
                        )
                    }
                }
            }
        }

        // SECTION 6: DATA, BACKUP & EXPORT
        if (matchesSection("data backup export import json csv ics sample wipe")) {
            SettingsGlassSection(
                title = if (s.isVi) "6. Sao lưu & Dữ liệu (JSON / CSV / .ics)" else "6. Data Vault & Backups",
                onReset = null
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(
                        label = if (s.isVi) "Xuất JSON đầy đủ" else "Export Full JSON",
                        selected = true,
                        onClick = { exportJsonLauncher.launch("taskal_backup.json") }
                    )
                    GlassChip(
                        label = if (s.isVi) "Nhập JSON" else "Import JSON",
                        onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) }
                    )
                    GlassChip(
                        label = if (s.isVi) "Xuất CSV" else "Export Tasks CSV",
                        onClick = { exportCsvLauncher.launch("taskal_tasks.csv") }
                    )
                    GlassChip(
                        label = if (s.isVi) "Xuất .ics" else "Export .ics",
                        onClick = { exportIcsLauncher.launch("taskal_calendar.ics") }
                    )
                    GlassChip(
                        label = if (s.isVi) "Nhập .ics" else "Import .ics",
                        onClick = { importIcsLauncher.launch(arrayOf("text/calendar", "*/*")) }
                    )
                }

                SettingToggleRow(
                    label = if (s.isVi) "Tự động sao lưu nội bộ hàng ngày (giữ 7 bản gần nhất)" else "Automatic Daily Local Backup (Keep Last 7)",
                    checked = settings.autoDailyBackup,
                    onCheckedChange = { v ->
                        onUpdateSettings { it.copy(autoDailyBackup = v) }
                        BackupManager.scheduleDailyAutoBackupWorker(context, v)
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(
                        label = if (s.isVi) "Tạo bản sao lưu nội bộ ngay" else "Create Local Snapshot Now",
                        onClick = {
                            scope.launch {
                                val f = BackupManager.performLocalRollingBackup(context)
                                localBackups = BackupManager.listLocalBackups(context)
                                statusMessage = "Saved snapshot: ${f.name}"
                            }
                        }
                    )
                    GlassChip(
                        label = if (s.isVi) "Tải dữ liệu mẫu" else "Load Sample Data",
                        onClick = {
                            onLoadSampleData()
                            statusMessage = if (s.isVi) "Đã tải dữ liệu mẫu!" else "Sample workspace loaded!"
                        }
                    )
                    GlassChip(
                        label = if (s.isVi) "Xóa toàn bộ dữ liệu" else "Wipe All Data",
                        accentColor = Color(0xFFF43F5E),
                        onClick = { showWipeConfirm = true }
                    )
                }

                if (localBackups.isNotEmpty()) {
                    Text(
                        text = if (s.isVi) "Các bản sao lưu tự động trên máy (${localBackups.size}/7):"
                        else "Rolling Local Snapshots (${localBackups.size}/7):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    localBackups.take(3).forEach { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(file.name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch {
                                    BackupManager.importFullBackupJson(context, file.readText())
                                    statusMessage = "Restored ${file.name}"
                                }
                            }) {
                                Text(if (s.isVi) "Khôi phục" else "Restore")
                            }
                        }
                    }
                }
            }
        }

        // SECTION 7: LANGUAGE & ABOUT
        if (matchesSection("language tieng viet english onboarding about")) {
            SettingsGlassSection(
                title = if (s.isVi) "7. Ngôn ngữ & Giới thiệu" else "7. Language & About",
                onReset = { onResetSection("language") }
            ) {
                Text(if (s.isVi) "Ngôn ngữ ứng dụng (Đổi trực tiếp)" else "App Language (Live Switch)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        GlassChip(
                            label = if (s.isVi) lang.labelVi else lang.labelEn,
                            selected = settings.language == lang,
                            onClick = { onUpdateSettings { it.copy(language = lang) } },
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }
                }

                SettingToggleRow(
                    label = if (s.isVi) "Định dạng 24 giờ" else "Use 24-Hour Time Format",
                    checked = settings.use24HourFormat,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(use24HourFormat = v) } }
                )

                GlassChip(
                    label = if (s.isVi) "Xem lại màn hình Giới thiệu (Onboarding)" else "Replay Onboarding",
                    onClick = { onUpdateSettings { it.copy(hasCompletedOnboarding = false) } }
                )

                Text(
                    text = "Taskal v1.0 · Liquid Glass × Living Light · 100% Local-First Offline Planner",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text(if (s.isVi) "Xác nhận xóa toàn bộ dữ liệu?" else "Wipe All Local Data?") },
            text = {
                Text(
                    if (s.isVi) "Tất cả công việc, ghi chú và thói quen sẽ bị xóa khỏi thiết bị."
                    else "All tasks, notes, habits, and focus sessions will be permanently removed."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onWipeAllData()
                    showWipeConfirm = false
                    statusMessage = if (s.isVi) "Đã xóa sạch dữ liệu." else "All data wiped."
                }) {
                    Text(s.delete, color = Color(0xFFF43F5E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun SettingsGlassSection(
    title: String,
    onReset: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val s = S.current
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        thickness = GlassThickness.REGULAR,
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (onReset != null) {
                    GlassChip(
                        label = s.resetToDefault,
                        leadingIcon = { Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        onClick = onReset
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ReliabilityRow(
    title: String,
    isOk: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isOk) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isOk) Color(0xFF34D399) else Color(0xFFFBBF24),
                modifier = Modifier.size(18.dp)
            )
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
        if (!isOk) {
            GlassChip(label = actionLabel, onClick = onAction)
        }
    }
}

/**
 * 3-Screen Skippable Glass Onboarding with 1-Tap Sample Data Loading.
 */
@Composable
fun OnboardingOverlay(
    onFinish: (loadSample: Boolean) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current
    var step by remember { mutableIntStateOf(0) }

    val titles = listOf(
        if (s.isVi) "Lịch Kính Lỏng & Ánh Sáng Sống" else "Calendar-Centric Liquid Glass",
        if (s.isVi) "Thêm Việc Dưới 2 Giây" else "Capture in Under 2 Seconds",
        if (s.isVi) "100% Ngoại Tuyến & Cá Nhân Hóa" else "100% Local-First & Customizable"
    )
    val descriptions = listOf(
        if (s.isVi) "Công việc, khung giờ, ghi chú và thói quen hội tụ ngay trong lịch kèm âm lịch Việt Nam."
        else "Tasks, time-blocks, notes, and habits live inside your calendar with Vietnamese Lunar dates.",
        if (s.isVi) "Nhập tự nhiên bằng Tiếng Việt hoặc Tiếng Anh: \"họp nhóm mai 3pm #work !cao\" hoặc \"gym every mon wed fri 6am\"."
        else "Type naturally in English or Vietnamese: \"hop nhom mai 3pm #work !cao\" or \"gym every mon wed fri 6am\".",
        if (s.isVi) "9 chế độ làm việc, tiện ích màn hình chính co giãn, và sao lưu JSON/CSV/ICS hoàn toàn trên máy."
        else "Switch between 9 productivity modes, resizable Glance widgets, and instant JSON/CSV/ICS local backups."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_card"),
            thickness = GlassThickness.THICK,
            cornerRadius = 32.dp,
            elevated = true,
            contentPadding = PaddingValues(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Taskal · ${step + 1}/3",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.accentColor
                    )
                    TextButton(
                        onClick = { onFinish(false) },
                        modifier = Modifier.testTag("onboarding_skip_btn")
                    ) {
                        Text(if (s.isVi) "Bỏ qua" else "Skip")
                    }
                }

                Text(
                    text = titles[step],
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = descriptions[step],
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step < 2) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassChip(
                            label = if (s.isVi) "Tiếp theo →" else "Next →",
                            selected = true,
                            onClick = { step++ }
                        )
                    } else {
                        GlassChip(
                            label = if (s.isVi) "Bắt đầu trống" else "Start Fresh",
                            onClick = { onFinish(false) }
                        )
                        GlassChip(
                            label = if (s.isVi) "✦ Tải dữ liệu mẫu & Bắt đầu" else "✦ Load Sample Data & Start",
                            selected = true,
                            onClick = { onFinish(true) },
                            modifier = Modifier.testTag("onboarding_sample_btn")
                        )
                    }
                }
            }
        }
    }
}
