package com.pedro.eam.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        CourseEntity::class,
        SubjectEntity::class,
        ScheduleItemEntity::class,
        AbsenceRecordEntity::class,
        AttendanceRecordEntity::class,
        EventEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class EamDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun subjectDao(): SubjectDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun absenceDao(): AbsenceDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EamDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `attendance_records` (
                        `id` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `scheduleItemId` TEXT,
                        `date` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `periodsAbsent` INTEGER NOT NULL,
                        `observation` TEXT NOT NULL,
                        `origin` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`scheduleItemId`) REFERENCES `schedule_items`(`id`) ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_subjectId` ON `attendance_records` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_scheduleItemId` ON `attendance_records` (`scheduleItemId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_records_subjectId_scheduleItemId_date` ON `attendance_records` (`subjectId`, `scheduleItemId`, `date`)")

                val cursor = db.query("SELECT id, subjectId, date FROM absence_records")
                while (cursor.moveToNext()) {
                    val subId = cursor.getString(1)
                    val dateStr = cursor.getString(2)
                    val newId = "mig_${UUID.randomUUID()}"
                    db.execSQL("""
                        INSERT OR IGNORE INTO attendance_records (id, subjectId, scheduleItemId, date, status, periodsAbsent, observation, origin)
                        VALUES ('$newId', '$subId', NULL, '$dateStr', 'ABSENT', 1, 'Migrado do sistema legado', 'MIGRATED_LEGACY')
                    """)
                }
                cursor.close()
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN time TEXT")
                db.execSQL("ALTER TABLE events ADD COLUMN completedAt TEXT")
                db.execSQL("ALTER TABLE events ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE events ADD COLUMN reminderMinutesBefore INTEGER NOT NULL DEFAULT 30")
            }
        }

        fun getDatabase(context: Context): EamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EamDatabase::class.java,
                    "eam_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
