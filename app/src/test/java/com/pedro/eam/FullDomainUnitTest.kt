package com.pedro.eam

import com.pedro.eam.data.EventItem
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FullDomainUnitTest {

    @Test
    fun testScheduleConflictDetection() {
        val start1 = LocalTime.of(8, 0)
        val end1 = LocalTime.of(9, 40)

        val startOverlap = LocalTime.of(9, 0)
        val endOverlap = LocalTime.of(10, 40)

        val startNoOverlap = LocalTime.of(10, 0)
        val endNoOverlap = LocalTime.of(11, 40)

        val isOverlapping = startOverlap.isBefore(end1) && endOverlap.isAfter(start1)
        assertTrue(isOverlapping)

        val isNotOverlapping = startNoOverlap.isBefore(end1) && endNoOverlap.isAfter(start1)
        assertFalse(isNotOverlapping)
    }

    @Test
    fun testEventCompletionAndReopening() {
        val event = EventItem(
            id = "e1",
            subjectId = "s1",
            title = "Prova P1",
            date = LocalDate.now(),
            isCompleted = false
        )

        val completedEvent = event.copy(isCompleted = true, completedAt = LocalDate.now())
        assertTrue(completedEvent.isCompleted)
        assertNotNull(completedEvent.completedAt)

        val reopenedEvent = completedEvent.copy(isCompleted = false, completedAt = null)
        assertFalse(reopenedEvent.isCompleted)
        assertNull(reopenedEvent.completedAt)
    }
}
