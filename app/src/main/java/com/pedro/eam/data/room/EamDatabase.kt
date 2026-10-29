package com.pedro.eam.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CourseEntity::class,
        SubjectEntity::class,
        ScheduleItemEntity::class,
        AbsenceRecordEntity::class,
        EventEntity::class
    ],
    version = 2,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migração segura de v1 para v2 sem perda de dados
            }
        }

        fun getDatabase(context: Context): EamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EamDatabase::class.java,
                    "eam_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
