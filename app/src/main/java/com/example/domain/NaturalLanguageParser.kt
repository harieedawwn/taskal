package com.example.domain

import com.example.data.RecurrenceFrequency
import com.example.data.RecurrenceRuleModel
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class ParsedTaskDraft(
    val rawInput: String,
    val cleanTitle: String,
    val dueDate: LocalDate? = null,
    val startTimeMinute: Int? = null,
    val durationMinutes: Int? = null,
    val priority: Int? = null, // 0..3
    val tags: List<String> = emptyList(),
    val listName: String? = null,
    val recurrenceRule: RecurrenceRuleModel? = null,
    val extractedTokens: List<ExtractedChipToken> = emptyList()
)

enum class TokenKind {
    DATE, TIME, DURATION, PRIORITY, TAG, LIST, RECURRENCE
}

data class ExtractedChipToken(
    val kind: TokenKind,
    val matchedText: String,
    val label: String
)

/**
 * Deterministic, offline Natural Language Parser supporting both Vietnamese (with and without diacritics)
 * and English.
 *
 * Verified examples:
 * - "hop nhom mai 3pm #work !cao"
 * - "họp nhóm mai 3pm #work !cao"
 * - "nop bao cao thu 6 tuan sau 9h"
 * - "uong nuoc moi 2 tieng"
 * - "gym every mon wed fri 6am"
 * - "pay rent on the 1st monthly"
 */
object NaturalLanguageParser {

    fun stripVietnameseDiacritics(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
    }

    fun parse(input: String, referenceDate: LocalDate = LocalDate.now()): ParsedTaskDraft {
        if (input.isBlank()) {
            return ParsedTaskDraft(rawInput = input, cleanTitle = "")
        }

        var working = input
        val chips = mutableListOf<ExtractedChipToken>()
        var dueDate: LocalDate? = null
        var startTimeMinute: Int? = null
        var durationMinutes: Int? = null
        var priority: Int? = null
        val tags = mutableListOf<String>()
        var listName: String? = null
        var recurrence: RecurrenceRuleModel? = null

        fun consumeRegex(regex: Regex, onMatch: (MatchResult) -> Unit) {
            val matches = regex.findAll(working).toList()
            if (matches.isNotEmpty()) {
                for (m in matches.asReversed()) {
                    onMatch(m)
                    working = working.removeRange(m.range)
                }
            }
        }

        // 1. Tags: #tag
        consumeRegex(Regex("(?<=^|\\s)#([\\p{L}0-9_-]+)")) { m ->
            val tag = m.groupValues[1]
            tags.add(0, tag)
            chips.add(0, ExtractedChipToken(TokenKind.TAG, m.value.trim(), "#$tag"))
        }

        // 2. List: @list
        consumeRegex(Regex("(?<=^|\\s)@([\\p{L}0-9_-]+)")) { m ->
            val lName = m.groupValues[1]
            listName = lName
            chips.add(0, ExtractedChipToken(TokenKind.LIST, m.value.trim(), "@$lName"))
        }

        // 3. Priority: !high, !medium, !low, !cao, !tb, !thap, !1, !2, !3, !!!, !!, !
        consumeRegex(Regex("(?<=^|\\s)!(cao|thấp|thap|trungbinh|tb|high|med|medium|low|none|3|2|1|!!|!)(?=\\s|$)", RegexOption.IGNORE_CASE)) { m ->
            val raw = stripVietnameseDiacritics(m.groupValues[1].lowercase())
            val p = when (raw) {
                "cao", "high", "3", "!!" -> 3
                "tb", "trungbinh", "med", "medium", "2", "!" -> 2
                "thap", "low", "1" -> 1
                else -> 0
            }
            priority = p
            val pLabel = when (p) {
                3 -> "!High"
                2 -> "!Medium"
                1 -> "!Low"
                else -> "!None"
            }
            chips.add(ExtractedChipToken(TokenKind.PRIORITY, m.value.trim(), pLabel))
        }

        // Now match multi-word Vietnamese & English patterns using normalized fold mapping (1-to-1 length when NFD stripped)
        // Note: stripVietnameseDiacritics preserves exact character indices!
        fun findInFolded(pattern: String): MatchResult? {
            val folded = stripVietnameseDiacritics(working).lowercase()
            return Regex(pattern, RegexOption.IGNORE_CASE).find(folded)
        }

        fun removeRangeFromWorking(range: IntRange): String {
            val matched = working.substring(range)
            working = working.removeRange(range)
            return matched.trim()
        }

        // 4. Recurrence patterns:
        // 4a. "moi N tieng" / "every N hours"
        findInFolded("\\b(moi|every)\\s+(\\d{1,2})\\s*(tieng|gio|hours?|hrs?|h)\\b")?.let { m ->
            val hours = m.groupValues[2].toIntOrNull()?.coerceIn(1, 24) ?: 2
            val rawText = removeRangeFromWorking(m.range)
            recurrence = RecurrenceRuleModel(frequency = RecurrenceFrequency.HOURS_INTERVAL, interval = hours)
            if (dueDate == null) dueDate = referenceDate
            chips.add(ExtractedChipToken(TokenKind.RECURRENCE, rawText, "Every ${hours}h"))
        }

        // 4b. "every mon wed fri" / "moi thu 2 thu 4 thu 6"
        findInFolded("\\b(every|moi|hang)\\s+((?:(?:mon|monday|tue|tuesday|wed|wednesday|thu|thursday|fri|friday|sat|saturday|sun|sunday|t2|t3|t4|t5|t6|t7|cn|thu\\s*[2-7]|chu\\s*nhat)(?:\\s*,?\\s*|\\s+and\\s+|\\s+va\\s+)*)+)")?.let { m ->
            val daysPart = m.groupValues[2]
            val dows = parseWeekdaysList(daysPart)
            if (dows.isNotEmpty()) {
                val rawText = removeRangeFromWorking(m.range)
                recurrence = RecurrenceRuleModel(
                    frequency = RecurrenceFrequency.WEEKLY,
                    interval = 1,
                    daysOfWeek = dows
                )
                // Set initial dueDate to the next matching day of week (or today if matches)
                val todayDow = referenceDate.dayOfWeek.value
                val nextDow = dows.firstOrNull { it >= todayDow } ?: dows.first()
                val diff = if (nextDow >= todayDow) nextDow - todayDow else 7 - (todayDow - nextDow)
                dueDate = referenceDate.plusDays(diff.toLong())
                chips.add(ExtractedChipToken(TokenKind.RECURRENCE, rawText, "Weekly (${dows.joinToString(",") { dowShort(it) }})"))
            }
        }

        // 4c. "on the 1st monthly" / "monthly on the 15th" / "ngay 1 hang thang"
        findInFolded("\\b(?:on\\s+the\\s+(\\d{1,2})(?:st|nd|rd|th)?\\s+monthly|monthly\\s+on\\s+the\\s+(\\d{1,2})(?:st|nd|rd|th)?|ngay\\s+(\\d{1,2})\\s+(?:hang|moi)\\s+thang)\\b")?.let { m ->
            val dayNum = (m.groupValues[1].ifEmpty { m.groupValues[2] }.ifEmpty { m.groupValues[3] }).toIntOrNull()?.coerceIn(1, 31) ?: 1
            val rawText = removeRangeFromWorking(m.range)
            recurrence = RecurrenceRuleModel(frequency = RecurrenceFrequency.MONTHLY_DATE, interval = 1)
            val candidateThisMonth = referenceDate.withDayOfMonth(dayNum.coerceAtMost(referenceDate.lengthOfMonth()))
            dueDate = if (!candidateThisMonth.isBefore(referenceDate)) {
                candidateThisMonth
            } else {
                val nextMonth = referenceDate.plusMonths(1)
                nextMonth.withDayOfMonth(dayNum.coerceAtMost(nextMonth.lengthOfMonth()))
            }
            chips.add(ExtractedChipToken(TokenKind.RECURRENCE, rawText, "Monthly (Day $dayNum)"))
            chips.add(ExtractedChipToken(TokenKind.DATE, rawText, dueDate.toString()))
        }

        // 4d. Simple recurrence keywords: "hang ngay" / "moi ngay" / "daily" / "hang tuan" / "weekly" / "hang thang" / "monthly"
        if (recurrence == null) {
            findInFolded("\\b(hang\\s+ngay|moi\\s+ngay|every\\s+day|daily|hang\\s+tuan|moi\\s+tuan|every\\s+week|weekly|hang\\s+thang|moi\\s+thang|every\\s+month|monthly)\\b")?.let { m ->
                val token = m.groupValues[1]
                val rawText = removeRangeFromWorking(m.range)
                recurrence = when {
                    token.contains("ngay") || token.contains("day") || token == "daily" ->
                        RecurrenceRuleModel(frequency = RecurrenceFrequency.DAILY, interval = 1)
                    token.contains("tuan") || token.contains("week") ->
                        RecurrenceRuleModel(frequency = RecurrenceFrequency.WEEKLY, interval = 1, daysOfWeek = listOf(referenceDate.dayOfWeek.value))
                    else ->
                        RecurrenceRuleModel(frequency = RecurrenceFrequency.MONTHLY_DATE, interval = 1)
                }
                if (dueDate == null) dueDate = referenceDate
                chips.add(ExtractedChipToken(TokenKind.RECURRENCE, rawText, RecurrenceEngine.formatRuleSummary(recurrence!!, false)))
            }
        }

        // 5. Vietnamese & English Date expressions
        // 5a. "thu X tuan sau" / "chu nhat tuan sau" / "next mon..sun"
        findInFolded("\\b(?:(thu\\s*[2-7]|t[2-7]|chu\\s*nhat|cn)\\s+(tuan\\s+sau|tuan\\s+toi)|next\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun))\\b")?.let { m ->
            val rawText = removeRangeFromWorking(m.range)
            val targetDow = if (m.groupValues[1].isNotEmpty()) {
                parseSingleWeekday(m.groupValues[1])
            } else {
                parseSingleWeekday(m.groupValues[3])
            } ?: 1
            val daysUntilNextMonday = (8 - referenceDate.dayOfWeek.value) % 7
            val nextWeekMonday = referenceDate.plusDays((if (daysUntilNextMonday == 0) 7 else daysUntilNextMonday).toLong())
            val resolved = nextWeekMonday.plusDays((targetDow - 1).toLong())
            dueDate = resolved
            chips.add(ExtractedChipToken(TokenKind.DATE, rawText, resolved.toString()))
        }

        // 5b. "thu X" / "chu nhat" / "this mon..sun" / "on mon..sun"
        if (dueDate == null) {
            findInFolded("\\b(?:(thu\\s*[2-7]|chu\\s*nhat)|(?:this|on)\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun))\\b")?.let { m ->
                val rawText = removeRangeFromWorking(m.range)
                val targetDow = parseSingleWeekday(m.groupValues[1].ifEmpty { m.groupValues[2] }) ?: 1
                val resolved = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(targetDow)))
                dueDate = resolved
                chips.add(ExtractedChipToken(TokenKind.DATE, rawText, resolved.toString()))
            }
        }

        // 5c. "hom nay", "ngay mai", "mai", "ngay kia", "mot", "tuan sau", "today", "tomorrow", "next week", "this weekend", "cuoi tuan"
        if (dueDate == null) {
            findInFolded("\\b(hom\\s+nay|ngay\\s+mai|ngay\\s+kia|ngay\\s+mot|tuan\\s+sau|tuan\\s+toi|cuoi\\s+tuan|today|tomorrow|tmr|next\\s+week|this\\s+weekend|mai)\\b")?.let { m ->
                val kw = m.groupValues[1].replace(Regex("\\s+"), " ")
                val rawText = removeRangeFromWorking(m.range)
                val resolved = when (kw) {
                    "hom nay", "today" -> referenceDate
                    "ngay mai", "mai", "tomorrow", "tmr" -> referenceDate.plusDays(1)
                    "ngay kia", "ngay mot" -> referenceDate.plusDays(2)
                    "cuoi tuan", "this weekend" -> referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                    "tuan sau", "tuan toi", "next week" -> referenceDate.plusWeeks(1)
                    else -> referenceDate
                }
                dueDate = resolved
                chips.add(ExtractedChipToken(TokenKind.DATE, rawText, resolved.toString()))
            }
        }

        // 5d. Explicit dd/mm or dd/mm/yyyy
        if (dueDate == null) {
            findInFolded("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b")?.let { m ->
                val d = m.groupValues[1].toIntOrNull() ?: 1
                val mo = m.groupValues[2].toIntOrNull() ?: 1
                val yrRaw = m.groupValues[3].toIntOrNull()
                val yr = when {
                    yrRaw == null -> referenceDate.year
                    yrRaw < 100 -> 2000 + yrRaw
                    else -> yrRaw
                }
                runCatching { LocalDate.of(yr, mo.coerceIn(1, 12), d.coerceIn(1, 31)) }.getOrNull()?.let { validDate ->
                    val rawText = removeRangeFromWorking(m.range)
                    dueDate = validDate
                    chips.add(ExtractedChipToken(TokenKind.DATE, rawText, validDate.toString()))
                }
            }
        }

        // 6. Time expressions: "3pm", "3:30pm", "6am", "9h", "9h30", "15:30", "8 gio toi", "7h sang"
        findInFolded("\\b(\\d{1,2})(?:[:hg](\\d{2}))?\\s*(am|pm|h|gio)?(?:\\s*(sang|chieu|toi|dem))?\\b")?.let { m ->
            val hasExplicitMarker = m.groupValues[2].isNotEmpty() || m.groupValues[3].isNotEmpty() || m.groupValues[4].isNotEmpty()
            if (hasExplicitMarker) {
                var hour = m.groupValues[1].toIntOrNull() ?: 9
                val minute = m.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
                val ampm = m.groupValues[3]
                val periodVi = m.groupValues[4]

                if (ampm == "pm" || periodVi == "chieu" || periodVi == "toi" || periodVi == "dem") {
                    if (hour in 1..11) hour += 12
                } else if (ampm == "am" || periodVi == "sang") {
                    if (hour == 12) hour = 0
                }
                if (hour in 0..23) {
                    val rawText = removeRangeFromWorking(m.range)
                    startTimeMinute = hour * 60 + minute
                    if (dueDate == null) dueDate = referenceDate
                    val formattedTime = "%02d:%02d".format(hour, minute)
                    chips.add(ExtractedChipToken(TokenKind.TIME, rawText, formattedTime))
                }
            }
        }

        // 7. Duration expressions: "trong 45p", "for 45m", "90min"
        findInFolded("\\b(?:trong\\s+|for\\s+)?(\\d{1,3})\\s*(phut|mins?|minutes?)\\b")?.let { m ->
            val mins = m.groupValues[1].toIntOrNull()?.coerceIn(5, 720)
            if (mins != null) {
                val rawText = removeRangeFromWorking(m.range)
                durationMinutes = mins
                chips.add(ExtractedChipToken(TokenKind.DURATION, rawText, "${mins}m"))
            }
        }

        val cleanedTitle = working.replace(Regex("\\s{2,}"), " ").trim()
            .ifEmpty { input.trim() }

        return ParsedTaskDraft(
            rawInput = input,
            cleanTitle = cleanedTitle,
            dueDate = dueDate,
            startTimeMinute = startTimeMinute,
            durationMinutes = durationMinutes,
            priority = priority,
            tags = tags.distinct(),
            listName = listName,
            recurrenceRule = recurrence,
            extractedTokens = chips
        )
    }

    private fun parseWeekdaysList(raw: String): List<Int> {
        val result = mutableListOf<Int>()
        val tokenRegex = Regex("(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu\\s*[2-7]|thu|fri|sat|sun|t[2-7]|chu\\s*nhat|cn)", RegexOption.IGNORE_CASE)
        for (m in tokenRegex.findAll(raw)) {
            parseSingleWeekday(m.value)?.let { result.add(it) }
        }
        return result.distinct().sorted()
    }

    private fun parseSingleWeekday(token: String): Int? {
        val norm = token.lowercase().replace(Regex("\\s+"), "")
        return when (norm) {
            "mon", "monday", "t2", "thu2" -> 1
            "tue", "tuesday", "t3", "thu3" -> 2
            "wed", "wednesday", "t4", "thu4" -> 3
            "thu", "thursday", "t5", "thu5" -> 4
            "fri", "friday", "t6", "thu6" -> 5
            "sat", "saturday", "t7", "thu7" -> 6
            "sun", "sunday", "cn", "chunhat" -> 7
            else -> null
        }
    }

    private fun dowShort(dow: Int): String = when (dow) {
        1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"
        else -> "Mon"
    }
}
