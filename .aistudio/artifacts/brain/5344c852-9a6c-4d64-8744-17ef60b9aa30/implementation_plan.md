# Taskal — Calendar-Centric Local-First Planner

Taskal is a fast, offline-first Android planner built around a calendar-centric workflow where tasks, time-blocks, notes, habits, and focus sessions live seamlessly inside the calendar and across nine switchable productivity modes, styled with the signature **"Liquid Glass × Living Light"** visual system.

## User Review & Critical Decisions

> [!IMPORTANT]
> Per your instructions ("Do not ask me questions; make decisions yourself and list your assumptions at the end"), all architectural and product decisions have been finalized with sensible defaults and documented below so you can review and approve the implementation plan directly.

- **Confirmed Decision 1 — 100% Local-First Persistence (Room + DataStore)**: All structured entities (Tasks, Subtasks, TaskLists, Tags, CrossRefs, Reminders, Notes, Habits, HabitLogs, FocusSessions, DashboardWidgets) are stored locally in Room with reactive `Flow` queries, indices, soft-delete trash (30-day auto-purge), and full JSON/CSV/ICS import-export. User preferences and live customization controls are persisted in Jetpack DataStore Preferences.
- **Confirmed Decision 2 — Liquid Glass & Living Light Rendering Strategy**: Uses an adaptive rendering pipeline: real-time AGSL `RuntimeShader` aurora mesh-gradient on API 33+ (with layered radial gradient fallback on API 26–32), `graphicsLayer` + `RenderEffect.createBlurEffect` on API 31+ (with high-contrast translucent gradient tint, 1px luminous border, specular tilt sheen, and procedural noise on API <31 or in Performance Mode), capped at 2–3 simultaneous blur layers for 60/120fps stability.
- **Confirmed Decision 3 — Glance App Widgets & Single-Activity Configuration**: Uses Jetpack Glance (`glance-appwidget` + `glance-material3`) for responsive resizable Android home-screen widgets (`SizeMode.Responsive`), `ActionCallback` for background task completion without launching the app, and handles `ACTION_APPWIDGET_CONFIGURE` directly inside `MainActivity` alongside the in-app **Widget Studio** (`requestPinAppWidget`).
- **Confirmed Decision 4 — Bilingual Live Localization (English & Tiếng Việt)**: Full string resources in both English and Vietnamese (`values/strings.xml` and `values-vi/strings.xml`), coupled with an in-app reactive localization provider and `AppCompatDelegate` / `LocaleListCompat` / context wrapper so switching between Auto, Tiếng Việt, and English updates the entire UI immediately without restarting.

---

## 1. Overview & Core Concept

- **What It Does**: Taskal unifies calendar scheduling, GTD task management, Kanban boards, Eisenhower matrix prioritization, Markdown-lite notes (including daily "Day Notes"), habit tracking with monthly heat-maps, Pomodoro/stopwatch focus timers, canvas-drawn productivity analytics, and a customizable 4-column widget canvas into one cohesive, zero-latency offline app.
- **Target Audience / Persona**: Power planners, students, and professionals who find traditional to-do apps disconnected from their daily schedule and want a sub-2-second task capture flow with deep progressive disclosure and tactile visual craft.
- **Key Value**: Sub-2-second bilingual natural-language task capture (`"hop nhom mai 3pm #work !cao"`, `"gym every mon wed fri 6am"`), calendar-native time-blocking with Vietnamese Lunar Calendar (*Âm lịch*) support, and complete offline privacy with deterministic alarm reliability.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **First Launch & Onboarding**: Animated splash screen transitions into a 3-card skippable glass onboarding flow introducing Liquid Glass & Calendar-Centric planning, Natural Language Quick-Add, and One-Tap Sample Data loading.
2. **Sub-2-Second Quick Add**: Tapping the floating glass FAB morphs into the Quick-Add Composer. As the user types in English or Vietnamese, the deterministic `NaturalLanguageParser` extracts dates, times, recurrence rules, `#tags`, `@lists`, and `!priority` tokens into interactive live glass chips with a custom glass Date/Time wheel & preset picker.
3. **Calendar-Centric Planning**: Default start mode is **Calendar** (switchable between Month, Week, 3-Day, Day time-grid, and Agenda). Month cells render task indicator dots/bars, note glyphs, optional week numbers, and Vietnamese lunar dates (*Âm lịch* + holiday markers). Tapping any day expands into the **Day Sheet** showing the hourly timeline, all-day banner, scheduled tasks, and an inline **Day Note**. Users can long-press empty slots to create time-blocked tasks or drag/resize tasks across days and hours.
4. **Switchable Modes via Liquid Dock**: A floating glass pill dock at the bottom holds up to 5 customizable tabs with a spring-animated metaball active lens; remaining modes live in the **More** glass sheet (Home, Calendar, Tasks, Board, Matrix, Notes, Habits, Focus, Stats, Widget Studio, Settings).
5. **Droplet Check & Ambient Celebration**: Checking off a task animates the checkbox into a filled liquid droplet, sends a soft light ripple across the row, dissolves particles, and springs the list closed. Completing the final task of the day triggers a gentle wave of light across the living aurora backdrop.

### Visual Identity & Theme ("Liquid Glass × Living Light")
- **Aesthetic Direction**: Atmospheric luxury dark-first design with frosted glass surfaces floating over a slow, breathing aurora mesh backdrop that shifts subtly with the time of day (Dawn, Day, Golden Hour, Night).
- **Color Palette & Mood**:
  - `--background` (Dark default): Deep Obsidian (`#080B12`) / AMOLED (`#000000`) / Light (`#F4F7FB`)
  - `--surface-glass`: Translucent layered white/obsidian tint with 1px diagonal luminous border (top-left `#55FFFFFF` to bottom-right `#0DFFFFFF`) and auto-adjusted WCAG AA scrim
  - `--primary` / `--accent`: 12 curated palettes (Aurora Cyan `#38BDF8`, Electric Indigo `#818CF8`, Emerald Mist `#34D399`, Amber Glow `#FBBF24`, Rose Quartz `#FB7185`, Violet Pulse `#C084FC`, Coral Sun `#FB923C`, Teal Lagoon `#2DD4BF`, Crimson Velvet `#F43F5E`, Lime Spark `#A3E635`, Cobalt Sky `#60A5FA`, Monochrome Silver `#E2E8F0`) plus a custom HSV/Hex picker and optional Material You dynamic color.
- **Typography & Hierarchy**:
  - *Display / Editorial*: Locally bundled Google Fonts (`Playfair Display` for Editorial mode, `Plus Jakarta Sans` / `Inter` for Modern UI, `JetBrains Mono` for Mono/tabular numerals). Huge expressive date numerals and month headers with tight tracking (`-0.5sp`), paired with crisp body typography and `tnum` tabular numbers for times and countdowns.
- **Interactive Feedback & Motion**:
  - Spring physics (`dampingRatio = 0.78f`, `stiffness = 380f`) for dock lens, sheet expansions, and drag-and-drop cards.
  - Staggered list item reveals (`<= 35ms` step) drawn in `graphicsLayer`.
  - Global animation speed multiplier in Settings (`Off`, `0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`) and automatic respect for system Reduce Motion and low-RAM Performance Mode.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Single-Module Manual DI & Reactive State Container**:
  - *Chosen Approach*: A clean `AppContainer` instantiated in `TaskalApplication` providing singletons for `TaskalDatabase`, `SettingsDataStore`, repositories, domain engines (`RecurrenceEngine`, `NaturalLanguageParser`, `LunarCalendar`, `BackupManager`), and `AlarmScheduler`.
  - *Why*: Eliminates annotation processing overhead and build fragility from Hilt/Dagger while keeping startup instantaneous and tests trivial to wire.
- **Decision 2 — Deterministic Bilingual Natural Language Parser**:
  - *Chosen Approach*: Pure Kotlin tokenizing parser supporting English and Vietnamese (both with and without diacritics: e.g., `"mai"`, `"ngay mai"`, `"thu 2"`..`"chu nhat"`, `"tuan sau"`, `"moi 2 tieng"`, `"hang ngay"`, `"!cao"`, `"!tb"`, `"!thap"`, `"3pm"`, `"15h30"`).
  - *Why*: Zero network latency, works 100% offline, and allows users to edit or remove any extracted token chip before saving.
- **Decision 3 — Doze-Safe Exact Alarms & Ongoing Chronometer Notification for Focus**:
  - *Chosen Approach*: Uses `AlarmManager.setExactAndAllowWhileIdle` (falling back gracefully to windowed alarms if `canScheduleExactAlarms()` is false on Android 12+) and `NotificationCompat.Builder.setUsesChronometer(true).setChronometerCountDown(true)` for Focus sessions instead of a foreground service.
  - *Why*: Meets the hard constraint against foreground services while delivering a live countdown in the system notification shade and reliable completion alarms even across Doze mode and device reboots.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Single Activity)                      │
│   Edge-to-Edge │ SplashScreen │ AppWidgetConfigure │ DeepLink & BackHandler│
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ observes StateFlow
┌─────────────────────────────────────▼──────────────────────────────────────┐
│                     TaskalViewModel & Mode State Holders                   │
│  CalendarState │ TasksState │ BoardMatrixState │ NotesHabitsFocus │ Studio │
└───────┬─────────────────────────────┬──────────────────────────────┬───────┘
        │                             │                              │
┌───────▼───────────┐       ┌─────────▼──────────┐        ┌──────────▼───────┐
│   Domain Layer    │       │  Repository Layer  │        │ System Services  │
│ ───────────────── │       │ ────────────────── │        │ ──────────────── │
│ • NaturalLanguage │       │ • PlannerRepo      │        │ • AlarmScheduler │
│   Parser (EN/VI)  │       │ • SettingsRepo     │        │ • GlanceWidget   │
│ • RecurrenceEngine│       │ • BackupManager    │        │   Updater        │
│ • LunarCalendar   │       │   (JSON/CSV/ICS)   │        │ • TiltSensor     │
│   (Hồ Ngọc Đức)   │       │ • DeviceCalendar   │        │   Manager        │
└───────────────────┘       └─────────┬──────────┘        └──────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
      ┌───────────────────────────┐       ┌───────────────────────────┐
      │   Room (TaskalDatabase)   │       │  DataStore (Preferences)  │
      │ ───────────────────────── │       │ ───────────────────────── │
      │ • tasks, subtasks, lists  │       │ • Theme, GlassTokens      │
      │ • tags, task_tag_xref     │       │ • Dock order, Start mode  │
      │ • reminders, notes        │       │ • Calendar & NLP prefs    │
      │ • habits, habit_logs      │       │ • Notification & Focus    │
      │ • focus_sessions, widgets │       │ • Language (Auto/VI/EN)   │
      └───────────────────────────┘       └───────────────────────────┘
```

### Data Model & State
- **Room Entities**:
  - `TaskEntity`: `id`, `title`, `description`, `listId`, `priority` (0..3), `startDateEpochDay`, `dueDateEpochDay`, `startTimeMinute` (nullable), `durationMinutes`, `isAllDay`, `recurrenceRule` (serialized model), `colorHex`, `isPinned`, `status` (`TODO`, `DOING`, `DONE`), `isCompleted`, `completedAt`, `isDeleted`, `deletedAt`, `sortOrder`, `createdAt`, `updatedAt`.
  - `SubtaskEntity`: `id`, `taskId` (FK cascade), `title`, `isCompleted`, `sortOrder`.
  - `TaskListEntity`: `id`, `name`, `colorHex`, `emoji`, `sortOrder`, `isDefault`.
  - `TagEntity` & `TaskTagCrossRef`: many-to-many tagging with color and fast indexed lookup.
  - `ReminderEntity`: `id`, `taskId`, `offsetMinutes`, `customTriggerEpochMillis`, `isFired`.
  - `NoteEntity`: `id`, `title`, `contentMarkdown`, `attachedDateEpochDay` (nullable; `isDayNote` flag for quick Day Notes), `linkedTaskId` (nullable), `colorHex`, `tagsCsv`, `isPinned`, `isArchived`, `isDeleted`, `deletedAt`, `updatedAt`.
  - `HabitEntity` & `HabitLogEntity`: `id`, `title`, `emoji`, `colorHex`, `frequencyType` (DAILY / WEEKLY_TARGET), `weeklyTarget`, `reminderTimeMinute`, `currentStreak`, `bestStreak`, `createdAt`; logs indexed by `(habitId, epochDay)`.
  - `FocusSessionEntity`: `id`, `taskId` (nullable), `durationMinutes`, `startedAt`, `completedAt`, `wasCompleted`, `sessionType` (POMODORO / STOPWATCH / BREAK).
  - `DashboardWidgetEntity`: `id`, `page` (0..2), `type`, `gridX`, `gridY`, `spanX`, `spanY`, `configJson`, `sortOrder`.

### Interactive Component & State Mapping
- **GlassSurface & AuroraBackdrop**: Observes `GlassSettings` from DataStore live. Every slider tweak in Settings (blur radius, tint alpha, saturation, edge glow, specular intensity, noise, corner radius, thickness) recomposes `LocalGlassTokens` immediately across cards, sheets, dialogs, and the Liquid Dock.
- **Calendar & Day Sheet**: Swiping horizontally updates the current `LocalDate` anchor; tapping a day cell opens the Day Sheet with shared date context; dragging a task chip across time slots or month days invokes `PlannerRepository.rescheduleTask()` and triggers `AlarmScheduler` + Glance widget refresh.
- **Tasks, Board & Matrix**: Unified filter/sort pipeline supports smart lists, custom lists, tags, swipe actions, multi-select bulk actions, and undo snackbars. Dragging cards between Board columns or Eisenhower Matrix quadrants updates `status`, `listId`, or `(priority, dueDateEpochDay)` atomically.
- **Reliability & Backups**: `BootAndTimeReceiver` listens for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, and `TIMEZONE_CHANGED` to reschedule all active reminders and daily auto-backup workers (`WorkManager`).
