package com.pedro.eam.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CourseEntity::class,
        SubjectEntity::class,
        ScheduleItemEntity::class,
        AbsenceRecordEntity::class,
        EventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EamDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun subjectDao(): SubjectDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun absenceDao(): AbsenceDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EamDatabase? = null

        fun getDatabase(context: Context): EamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EamDatabase::class.java,
                    "eam_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
