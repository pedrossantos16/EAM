package com.pedro.eam

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedro.eam.utils.BackupManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupTest {

    @Test
    fun testValidBackupValidation() {
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1700000000000,
              "appVersion": 18,
              "courses": [{"id": "c1", "name": "Computação"}],
              "subjects": [{"id": "s1", "courseId": "c1", "name": "Cálculo"}],
              "scheduleItems": [{"id": "sc1", "subjectId": "s1", "dayOfWeek": 1, "startTime": "08:00", "endTime": "09:40"}],
              "absenceRecords": [{"id": "a1", "subjectId": "s1", "date": "2026-10-29"}],
              "events": [{"id": "e1", "subjectId": "s1", "title": "Prova", "date": "2026-10-30", "type": "PROVA", "isCompleted": false}]
            }
        """.trimIndent()

        val result = BackupManager.validateAndGetSummary(backupJson)
        assertTrue(result.isSuccess)
        val (summary, _) = result.getOrThrow()
        assertEquals(1, summary.coursesCount)
        assertEquals(1, summary.subjectsCount)
        assertEquals(1, summary.scheduleCount)
        assertEquals(1, summary.absencesCount)
        assertEquals(1, summary.eventsCount)
    }

    @Test
    fun testInvalidSchemaVersion() {
        val backupJson = """
            {
              "schemaVersion": 99,
              "exportedAt": 1700000000000
            }
        """.trimIndent()

        val result = BackupManager.validateAndGetSummary(backupJson)
        assertTrue(result.isFailure)
    }

    @Test
    fun testCorruptedJson() {
        val invalidJson = "{ invalid_json_string "
        val result = BackupManager.validateAndGetSummary(invalidJson)
        assertTrue(result.isFailure)
    }

    @Test
    fun testNonExistentSubjectReference() {
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1700000000000,
              "courses": [{"id": "c1", "name": "Computação"}],
              "subjects": [{"id": "s1", "courseId": "c1", "name": "Cálculo"}],
              "events": [{"id": "e1", "subjectId": "s99", "title": "Prova", "date": "2026-10-30", "type": "PROVA"}]
            }
        """.trimIndent()

        val result = BackupManager.validateAndGetSummary(backupJson)
        assertTrue(result.isFailure)
    }
}
