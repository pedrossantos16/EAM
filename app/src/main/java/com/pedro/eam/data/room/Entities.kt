package com.pedro.eam.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class SubjectEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val name: String,
    val professor: String,
    val maxAbsences: Int,
    val room: String
)

@Entity(
    tableName = "schedule_items",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class ScheduleItemEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String
)

@Entity(
    tableName = "absence_records",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index(value = ["subjectId", "date"], unique = true)]
)
data class AbsenceRecordEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val date: String
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScheduleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("scheduleItemId"), Index(value = ["subjectId", "scheduleItemId", "date"], unique = true)]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val scheduleItemId: String?,
    val date: String,
    val status: String, // PRESENT, ABSENT, PARTIAL
    val periodsAbsent: Int,
    val observation: String,
    val origin: String // NORMAL, MIGRATED_LEGACY
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class EventEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String?,
    val type: String,
    val isCompleted: Boolean,
    val completedAt: String?,
    val reminderEnabled: Boolean,
    val reminderMinutesBefore: Int
)
