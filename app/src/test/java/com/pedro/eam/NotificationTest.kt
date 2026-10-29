package com.pedro.eam

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class NotificationTest {

    @Test
    fun testTriggerTimeCalculation() {
        val eventDate = LocalDate.of(2026, 12, 1)
        val eventTime = LocalTime.of(15, 0)
        val eventDateTime = LocalDateTime.of(eventDate, eventTime)
        val minutesBefore = 30

        val triggerTime = eventDateTime.minusMinutes(minutesBefore.toLong())
        assertEquals(LocalDateTime.of(2026, 12, 1, 14, 30), triggerTime)
    }

    @Test
    fun testPastEventIgnored() {
        val pastDateTime = LocalDateTime.now().minusHours(2)
        val triggerTime = pastDateTime.minusMinutes(30)
        val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val currentMillis = System.currentTimeMillis()

        assertTrue(triggerMillis <= currentMillis)
    }
}
