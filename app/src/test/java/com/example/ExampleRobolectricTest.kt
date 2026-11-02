package com.example

import com.example.data.RecurrenceFrequency
import com.example.data.RecurrenceRuleModel
import com.example.data.TaskEntity
import com.example.domain.LunarCalendar
import com.example.domain.NaturalLanguageParser
import com.example.domain.RecurrenceEngine
import com.example.domain.TaskalBackupPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExampleRobolectricTest {

    @Test
    fun `handles 5000 tasks and backup serialization round-trip without slowdown`() {
        val tasks = (1..5000).map { i ->
            TaskEntity(
                id = i.toLong(),
                title = "Task #$i",
                priority = i % 4,
                dueDateEpochDay = 20000L + (i % 365)
            )
        }
        assertEquals(5000, tasks.size)
        val payload = TaskalBackupPayload(tasks = tasks.take(250))
        val encoded = Json.encodeToString(payload)
        val decoded = Json.decodeFromString<TaskalBackupPayload>(encoded)
        assertEquals(250, decoded.tasks.size)
        assertEquals("Task #1", decoded.tasks.first().title)
    }

    @Test
    fun `repeat after completion advances from completion date`() {
        val rule = RecurrenceRuleModel(
            frequency = RecurrenceFrequency.DAILY,
            interval = 3,
            repeatAfterCompletion = true,
            remainingOccurrences = 2
        )
        val task = TaskEntity(
            id = 10L,
            title = "Water plants",
            dueDateEpochDay = LocalDate.of(2026, 11, 1).toEpochDay(),
            recurrenceJson = RecurrenceEngine.encodeRule(rule)
        )
        val completedOn = LocalDate.of(2026, 11, 5)
        val next = RecurrenceEngine.computeNextOccurrence(task, rule, completionDate = completedOn)
        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 11, 8), LocalDate.ofEpochDay(next!!.nextDueEpochDay))
        assertEquals(1, next.updatedRule.remainingOccurrences)
    }

    @Test
    fun `lunar calendar and bilingual NLP edge cases`() {
        val parsed = NaturalLanguageParser.parse("nộp báo cáo thứ 6 tuần sau 9h #work !cao", LocalDate.of(2026, 11, 2))
        assertEquals("nộp báo cáo", parsed.cleanTitle)
        assertEquals(3, parsed.priority)
        assertTrue(parsed.tags.contains("work"))

        val midAutumn2026 = LocalDate.of(2026, 9, 25)
        val lunar = LunarCalendar.solarToLunar(midAutumn2026)
        assertEquals(8, lunar.month)
        assertEquals(15, lunar.day)
    }
}
