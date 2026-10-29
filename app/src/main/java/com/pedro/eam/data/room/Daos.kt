package com.pedro.eam.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesList(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCourses(courses: List<CourseEntity>)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: String)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsList(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubjects(subjects: List<SubjectEntity>)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubject(subjectId: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items")
    fun getAllScheduleItems(): Flow<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items")
    suspend fun getAllScheduleItemsList(): List<ScheduleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleItem(item: ScheduleItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllScheduleItems(items: List<ScheduleItemEntity>)

    @Query("DELETE FROM schedule_items WHERE id = :itemId")
    suspend fun deleteScheduleItem(itemId: String)
}

@Dao
interface AbsenceDao {
    @Query("SELECT * FROM absence_records")
    fun getAllAbsences(): Flow<List<AbsenceRecordEntity>>

    @Query("SELECT * FROM absence_records")
    suspend fun getAllAbsencesList(): List<AbsenceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: AbsenceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAbsences(absences: List<AbsenceRecordEntity>)

    @Query("DELETE FROM absence_records WHERE subjectId = :subjectId AND date = :date")
    suspend fun deleteAbsence(subjectId: String, date: String)

    @Query("DELETE FROM absence_records WHERE subjectId = :subjectId")
    suspend fun deleteAbsencesForSubject(subjectId: String)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendancesList(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendances(records: List<AttendanceRecordEntity>)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND scheduleItemId = :scheduleItemId AND date = :date")
    suspend fun deleteAttendance(subjectId: String, scheduleItemId: String?, date: String)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteAttendancesForSubject(subjectId: String)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAllEventsList(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEvents(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: String)
}
