package com.example.domain

import com.example.data.RecurrenceFrequency
import com.example.data.RecurrenceRuleModel
import com.example.data.TaskEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Pure Kotlin recurrence engine handling daily, weekly, monthly (date, nth weekday, last day),
 * yearly, every N hours/units, repeat-after-completion, Feb 29 leap-day, month-end clamping, and DST transitions.
 */
object RecurrenceEngine {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseRule(rawJson: String): RecurrenceRuleModel {
        if (rawJson.isBlank()) return RecurrenceRuleModel()
        return runCatching { json.decodeFromString<RecurrenceRuleModel>(rawJson) }
            .getOrDefault(RecurrenceRuleModel())
    }

    fun encodeRule(rule: RecurrenceRuleModel): String {
        if (rule.frequency == RecurrenceFrequency.NONE) return ""
        return json.encodeToString(rule)
    }

    data class NextOccurrenceResult(
        val nextDueEpochDay: Long,
        val nextStartTimeMinute: Int?,
        val updatedRule: RecurrenceRuleModel
    )

    /**
     * Computes the next occurrence when a task is completed.
     * Returns null if recurrence has ended (by endEpochDay or remainingOccurrences == 1).
     */
    fun computeNextOccurrence(
        task: TaskEntity,
        rule: RecurrenceRuleModel = parseRule(task.recurrenceJson),
        completionDate: LocalDate = LocalDate.now(),
        completionTimeMinute: Int = LocalTime.now().hour * 60 + LocalTime.now().minute
    ): NextOccurrenceResult? {
        if (rule.frequency == RecurrenceFrequency.NONE) return null

        val remaining = rule.remainingOccurrences
        if (remaining != null && remaining <= 1) return null

        val baseEpochDay = if (rule.repeatAfterCompletion) {
            completionDate.toEpochDay()
        } else {
            task.dueDateEpochDay ?: completionDate.toEpochDay()
        }
        val baseDate = LocalDate.ofEpochDay(baseEpochDay)
        val interval = rule.interval.coerceAtLeast(1)

        var nextDate = baseDate
        var nextTimeMinute = task.startTimeMinute

        when (rule.frequency) {
            RecurrenceFrequency.NONE -> return null
            RecurrenceFrequency.HOURS_INTERVAL -> {
                val startMin = if (rule.repeatAfterCompletion) completionTimeMinute else (task.startTimeMinute ?: 540)
                val totalMinutes = startMin + interval * 60
                val dayDaysToAdd = (totalMinutes / 1440).toLong()
                nextTimeMinute = totalMinutes % 1440
                nextDate = baseDate.plusDays(dayDaysToAdd)
            }
            RecurrenceFrequency.DAILY -> {
                nextDate = baseDate.plusDays(interval.toLong())
            }
            RecurrenceFrequency.WEEKLY -> {
                val sortedDays = rule.daysOfWeek.distinct().sorted()
                if (sortedDays.isEmpty() || rule.repeatAfterCompletion) {
                    nextDate = baseDate.plusWeeks(interval.toLong())
                } else {
                    val currentDow = baseDate.dayOfWeek.value // 1..7
                    val nextInSameWeek = sortedDays.firstOrNull { it > currentDow }
                    nextDate = if (nextInSameWeek != null) {
                        baseDate.plusDays((nextInSameWeek - currentDow).toLong())
                    } else {
                        val firstDow = sortedDays.first()
                        val startOfNextCycleWeek = baseDate.minusDays((currentDow - 1).toLong()).plusWeeks(interval.toLong())
                        startOfNextCycleWeek.plusDays((firstDow - 1).toLong())
                    }
                }
            }
            RecurrenceFrequency.MONTHLY_DATE -> {
                // Preserve target day-of-month safely across Feb 28/29 and 30/31 month-ends
                val targetMonth = YearMonth.from(baseDate).plusMonths(interval.toLong())
                val desiredDay = baseDate.dayOfMonth
                val clampedDay = desiredDay.coerceAtMost(targetMonth.lengthOfMonth())
                nextDate = targetMonth.atDay(clampedDay)
            }
            RecurrenceFrequency.MONTHLY_LAST_DAY -> {
                val targetMonth = YearMonth.from(baseDate).plusMonths(interval.toLong())
                nextDate = targetMonth.atEndOfMonth()
            }
            RecurrenceFrequency.MONTHLY_NTH_WEEKDAY -> {
                val targetMonth = YearMonth.from(baseDate).plusMonths(interval.toLong())
                val dow = DayOfWeek.of(rule.dayOfWeekInMonth.coerceIn(1, 7))
                nextDate = if (rule.nthWeek == -1) {
                    targetMonth.atEndOfMonth().with(TemporalAdjusters.previousOrSame(dow))
                } else {
                    val firstOfTarget = targetMonth.atDay(1)
                    val candidate = firstOfTarget.with(TemporalAdjusters.dayOfWeekInMonth(rule.nthWeek.coerceIn(1, 5), dow))
                    if (candidate.month == targetMonth.month) candidate
                    else targetMonth.atEndOfMonth().with(TemporalAdjusters.previousOrSame(dow))
                }
            }
            RecurrenceFrequency.YEARLY -> {
                // Safe for Feb 29 -> Feb 28 in non-leap years, or Feb 29 when target year is leap
                val targetYear = baseDate.year + interval
                val ym = YearMonth.of(targetYear, baseDate.month)
                val clampedDay = baseDate.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
                nextDate = ym.atDay(clampedDay)
            }
        }

        val nextEpochDay = nextDate.toEpochDay()
        if (rule.endEpochDay != null && nextEpochDay > rule.endEpochDay) {
            return null
        }

        val updatedRule = if (remaining != null) {
            rule.copy(remainingOccurrences = remaining - 1)
        } else {
            rule
        }

        return NextOccurrenceResult(
            nextDueEpochDay = nextEpochDay,
            nextStartTimeMinute = nextTimeMinute,
            updatedRule = updatedRule
        )
    }

    /**
     * Converts a floating local date + minute-of-day into a DST-safe epoch millis instant in the given ZoneId.
     * If the local time falls into a spring-forward DST gap, ZonedDateTime.of adjusts forward automatically.
     */
    fun toInstantMillisDstSafe(
        epochDay: Long,
        minuteOfDay: Int,
        offsetMinutesBefore: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        val clampedMinute = minuteOfDay.coerceIn(0, 1439)
        val time = LocalTime.of(clampedMinute / 60, clampedMinute % 60)
        val localDateTime = LocalDateTime.of(date, time)
        val zoned = ZonedDateTime.of(localDateTime, zoneId).minusMinutes(offsetMinutesBefore.toLong())
        return zoned.toInstant().toEpochMilli()
    }

    fun formatRuleSummary(rule: RecurrenceRuleModel, isVi: Boolean): String {
        if (rule.frequency == RecurrenceFrequency.NONE) return if (isVi) "Không lặp" else "Does not repeat"
        val everyStr = if (isVi) "Mỗi" else "Every"
        val base = when (rule.frequency) {
            RecurrenceFrequency.DAILY ->
                if (rule.interval == 1) (if (isVi) "Hàng ngày" else "Daily")
                else "$everyStr ${rule.interval} ${if (isVi) "ngày" else "days"}"
            RecurrenceFrequency.WEEKLY -> {
                val dayNames = rule.daysOfWeek.sorted().map { dowNameShort(it, isVi) }
                val daysSuffix = if (dayNames.isNotEmpty()) " (${dayNames.joinToString(", ")})" else ""
                if (rule.interval == 1) (if (isVi) "Hàng tuần$daysSuffix" else "Weekly$daysSuffix")
                else "$everyStr ${rule.interval} ${if (isVi) "tuần$daysSuffix" else "weeks$daysSuffix"}"
            }
            RecurrenceFrequency.MONTHLY_DATE ->
                if (rule.interval == 1) (if (isVi) "Hàng tháng" else "Monthly")
                else "$everyStr ${rule.interval} ${if (isVi) "tháng" else "months"}"
            RecurrenceFrequency.MONTHLY_LAST_DAY ->
                if (isVi) "Ngày cuối tháng" else "Last day of month"
            RecurrenceFrequency.MONTHLY_NTH_WEEKDAY ->
                if (isVi) "Theo thứ trong tháng" else "Monthly by weekday"
            RecurrenceFrequency.YEARLY ->
                if (rule.interval == 1) (if (isVi) "Hàng năm" else "Yearly")
                else "$everyStr ${rule.interval} ${if (isVi) "năm" else "years"}"
            RecurrenceFrequency.HOURS_INTERVAL ->
                "$everyStr ${rule.interval} ${if (isVi) "tiếng" else "hours"}"
            RecurrenceFrequency.NONE -> ""
        }
        return if (rule.repeatAfterCompletion) {
            "$base · ${if (isVi) "sau khi xong" else "after completion"}"
        } else base
    }

    private fun dowNameShort(dow: Int, isVi: Boolean): String {
        return if (isVi) {
            when (dow) {
                1 -> "T2"; 2 -> "T3"; 3 -> "T4"; 4 -> "T5"; 5 -> "T6"; 6 -> "T7"; 7 -> "CN"
                else -> "T2"
            }
        } else {
            when (dow) {
                1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"
                else -> "Mon"
            }
        }
    }
}
