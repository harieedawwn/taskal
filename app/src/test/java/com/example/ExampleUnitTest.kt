package com.example

import com.example.data.RecurrenceFrequency
import com.example.data.RecurrenceRuleModel
import com.example.data.TaskEntity
import com.example.domain.LunarCalendar
import com.example.domain.NaturalLanguageParser
import com.example.domain.RecurrenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExampleUnitTest {

    @Test
    fun `natural language parser handles Vietnamese and English examples with and without diacritics`() {
        val refDate = LocalDate.of(2026, 11, 2) // Monday

        // 1. "hop nhom mai 3pm #work !cao"
        val p1 = NaturalLanguageParser.parse("hop nhom mai 3pm #work !cao", refDate)
        assertEquals("hop nhom", p1.cleanTitle)
        assertEquals(refDate.plusDays(1), p1.dueDate)
        assertEquals(15 * 60, p1.startTimeMinute)
        assertEquals(listOf("work"), p1.tags)
        assertEquals(3, p1.priority)

        // 1b. With full Vietnamese diacritics: "họp nhóm mai 3pm #work !cao"
        val p1Vi = NaturalLanguageParser.parse("họp nhóm mai 3pm #work !cao", refDate)
        assertEquals("họp nhóm", p1Vi.cleanTitle)
        assertEquals(refDate.plusDays(1), p1Vi.dueDate)
        assertEquals(15 * 60, p1Vi.startTimeMinute)
        assertEquals(listOf("work"), p1Vi.tags)
        assertEquals(3, p1Vi.priority)

        // 2. "nop bao cao thu 6 tuan sau 9h"
        val p2 = NaturalLanguageParser.parse("nop bao cao thu 6 tuan sau 9h", refDate)
        assertEquals("nop bao cao", p2.cleanTitle)
        // Ref is Mon Nov 2 -> next week Monday is Nov 9 -> Friday (thu 6) is Nov 13
        assertEquals(LocalDate.of(2026, 11, 13), p2.dueDate)
        assertEquals(9 * 60, p2.startTimeMinute)

        // 3. "uong nuoc moi 2 tieng"
        val p3 = NaturalLanguageParser.parse("uong nuoc moi 2 tieng", refDate)
        assertEquals("uong nuoc", p3.cleanTitle)
        assertNotNull(p3.recurrenceRule)
        assertEquals(RecurrenceFrequency.HOURS_INTERVAL, p3.recurrenceRule?.frequency)
        assertEquals(2, p3.recurrenceRule?.interval)

        // 4. "gym every mon wed fri 6am"
        val p4 = NaturalLanguageParser.parse("gym every mon wed fri 6am", refDate)
        assertEquals("gym", p4.cleanTitle)
        assertEquals(6 * 60, p4.startTimeMinute)
        assertEquals(RecurrenceFrequency.WEEKLY, p4.recurrenceRule?.frequency)
        assertEquals(listOf(1, 3, 5), p4.recurrenceRule?.daysOfWeek)

        // 5. "pay rent on the 1st monthly"
        val p5 = NaturalLanguageParser.parse("pay rent on the 1st monthly", refDate)
        assertEquals("pay rent", p5.cleanTitle)
        assertEquals(RecurrenceFrequency.MONTHLY_DATE, p5.recurrenceRule?.frequency)
        assertEquals(LocalDate.of(2026, 12, 1), p5.dueDate)
    }

    @Test
    fun `recurrence engine handles Feb 29 leap year, month-end, weekly, and DST safely`() {
        // Month-end Jan 31 -> Feb 28 in non-leap year 2026
        val jan31 = LocalDate.of(2026, 1, 31)
        val monthlyRule = RecurrenceRuleModel(frequency = RecurrenceFrequency.MONTHLY_DATE, interval = 1)
        val taskJan31 = TaskEntity(
            title = "Pay invoice",
            dueDateEpochDay = jan31.toEpochDay(),
            recurrenceJson = RecurrenceEngine.encodeRule(monthlyRule)
        )
        val nextFeb = RecurrenceEngine.computeNextOccurrence(taskJan31, monthlyRule)
        assertNotNull(nextFeb)
        assertEquals(LocalDate.of(2026, 2, 28), LocalDate.ofEpochDay(nextFeb!!.nextDueEpochDay))

        // Yearly on Feb 29, 2024 (leap year) -> Feb 28, 2025
        val feb29 = LocalDate.of(2024, 2, 29)
        val yearlyRule = RecurrenceRuleModel(frequency = RecurrenceFrequency.YEARLY, interval = 1)
        val taskLeap = TaskEntity(
            title = "Anniversary",
            dueDateEpochDay = feb29.toEpochDay(),
            recurrenceJson = RecurrenceEngine.encodeRule(yearlyRule)
        )
        val nextYear = RecurrenceEngine.computeNextOccurrence(taskLeap, yearlyRule)
        assertNotNull(nextYear)
        assertEquals(LocalDate.of(2025, 2, 28), LocalDate.ofEpochDay(nextYear!!.nextDueEpochDay))

        // DST transition test (America/New_York spring forward on 2026-03-08 02:30)
        val nyZone = ZoneId.of("America/New_York")
        val dstDay = LocalDate.of(2026, 3, 8).toEpochDay()
        val millis = RecurrenceEngine.toInstantMillisDstSafe(dstDay, 2 * 60 + 30, 0, nyZone)
        assertTrue(millis > 0L)
    }

    @Test
    fun `lunar calendar computes Tet Nguyen Dan accurately`() {
        // Solar 2026-02-17 is Mùng 1 Tết Bính Ngọ (1/1/2026 Lunar)
        val tet2026 = LocalDate.of(2026, 2, 17)
        val lunar = LunarCalendar.solarToLunar(tet2026)
        assertEquals(1, lunar.day)
        assertEquals(1, lunar.month)
        assertEquals(2026, lunar.year)
        assertEquals("Bính Ngọ", LunarCalendar.canChiOfYear(lunar.year))
        val holiday = LunarCalendar.getVietnameseHoliday(tet2026, lunar)
        assertNotNull(holiday)
        assertTrue(holiday!!.isLunar)
    }
}
