package com.pedro.eam.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.room.withTransaction
import com.pedro.eam.data.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.time.LocalDate
import java.time.LocalTime

data class Course(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = ""
)

data class CourseImpact(
    val subjects: Int,
    val schedules: Int,
    val events: Int,
    val absences: Int
)

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val courseId: String,
    val name: String,
    val professor: String = "",
    val maxAbsences: Int = 15,
    val room: String = ""
)

data class SubjectImpact(
    val schedules: Int,
    val events: Int,
    val absences: Int
)

data class ScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val dayOfWeek: Int, // 1 = Segunda, 2 = Terça, 3 = Quarta, 4 = Quinta, 5 = Sexta, 6 = Sábado, 7 = Domingo
    val startTime: String,
    val endTime: String,
    val room: String = ""
)

data class AbsenceRecord(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val date: String // YYYY-MM-DD
)

enum class EventType(val label: String) {
    PROVA("Prova"),
    TRABALHO("Trabalho"),
    APRESENTACAO("Apresentação"),
    LEITURA("Leitura"),
    OUTRO("Outro")
}

data class EventItem(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val title: String,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime? = null,
    val type: EventType = EventType.PROVA,
    val isCompleted: Boolean = false,
    val completedAt: LocalDate? = null,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 30
)

fun getFormattedDate(daysOffset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, daysOffset)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(cal.time)
}

object EamRepository {
    val courses = mutableStateListOf<Course>()
    val subjects = mutableStateListOf<Subject>()
    val scheduleItems = mutableStateListOf<ScheduleItem>()
    val absenceRecords = mutableStateListOf<AbsenceRecord>()
    val events = mutableStateListOf<EventItem>()

    private var db: EamDatabase? = null
    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        db = EamDatabase.getDatabase(appContext)
        prefs = appContext.getSharedPreferences("eam_prefs", Context.MODE_PRIVATE)

        CoroutineScope(Dispatchers.IO).launch {
            checkAndMigrateFromSharedPreferences()
            refreshData()
        }
        isInitialized = true
    }

    private suspend fun checkAndMigrateFromSharedPreferences() {
        val database = db ?: return
        val p = prefs ?: return
        val migrationDone = p.getBoolean("migration_to_room_completed", false)

        if (!migrationDone) {
            try {
                val coursesJson = p.getString("courses", null)
                val subjectsJson = p.getString("subjects", null)
                val scheduleJson = p.getString("schedule", null)
                val absencesJson = p.getString("absences", null)
                val eventsJson = p.getString("events", null)

                if (coursesJson != null || subjectsJson != null || scheduleJson != null || absencesJson != null || eventsJson != null) {
                    database.withTransaction {
                        if (coursesJson != null) {
                            val cArr = JSONArray(coursesJson)
                            val list = mutableListOf<CourseEntity>()
                            for (i in 0 until cArr.length()) {
                                val o = cArr.getJSONObject(i)
                                list.add(CourseEntity(o.getString("id"), o.getString("name"), o.optString("description", "")))
                            }
                            database.courseDao().insertAllCourses(list)
                        }

                        if (subjectsJson != null) {
                            val sArr = JSONArray(subjectsJson)
                            val list = mutableListOf<SubjectEntity>()
                            for (i in 0 until sArr.length()) {
                                val o = sArr.getJSONObject(i)
                                list.add(SubjectEntity(o.getString("id"), o.getString("courseId"), o.getString("name"), o.optString("professor", ""), o.optInt("maxAbsences", 15), o.optString("room", "")))
                            }
                            database.subjectDao().insertAllSubjects(list)
                        }

                        if (scheduleJson != null) {
                            val scArr = JSONArray(scheduleJson)
                            val list = mutableListOf<ScheduleItemEntity>()
                            for (i in 0 until scArr.length()) {
                                val o = scArr.getJSONObject(i)
                                list.add(ScheduleItemEntity(o.getString("id"), o.getString("subjectId"), o.getInt("dayOfWeek"), o.getString("startTime"), o.getString("endTime"), o.optString("room", "")))
                            }
                            database.scheduleDao().insertAllScheduleItems(list)
                        }

                        if (absencesJson != null) {
                            val aArr = JSONArray(absencesJson)
                            val list = mutableListOf<AbsenceRecordEntity>()
                            for (i in 0 until aArr.length()) {
                                val o = aArr.getJSONObject(i)
                                list.add(AbsenceRecordEntity(o.getString("id"), o.getString("subjectId"), o.getString("date")))
                            }
                            database.absenceDao().insertAllAbsences(list)
                        }

                        if (eventsJson != null) {
                            val eArr = JSONArray(eventsJson)
                            val list = mutableListOf<EventEntity>()
                            for (i in 0 until eArr.length()) {
                                val o = eArr.getJSONObject(i)
                                list.add(
                                    EventEntity(
                                        id = o.getString("id"),
                                        subjectId = o.getString("subjectId"),
                                        title = o.getString("title"),
                                        description = o.optString("description", ""),
                                        date = o.getString("date"),
                                        time = null,
                                        type = o.optString("type", "PROVA"),
                                        isCompleted = o.optBoolean("isCompleted", false),
                                        completedAt = null,
                                        reminderEnabled = false,
                                        reminderMinutesBefore = 30
                                    )
                                )
                            }
                            database.eventDao().insertAllEvents(list)
                        }
                    }
                }

                p.edit().putBoolean("migration_to_room_completed", true).apply()
                Log.i("EamRepository", "Migração de SharedPreferences para Room concluída com sucesso.")
            } catch (e: Exception) {
                Log.e("EamRepository", "Erro na migração para Room (dados antigos preservados): ${e.message}")
            }
        }
    }

    suspend fun refreshData() {
        val database = db ?: return
        val cList = database.courseDao().getAllCoursesList()
        val sList = database.subjectDao().getAllSubjectsList()
        val scList = database.scheduleDao().getAllScheduleItemsList()
        val aList = database.absenceDao().getAllAbsencesList()
        val eList = database.eventDao().getAllEventsList()

        withContext(Dispatchers.Main) {
            courses.clear()
            courses.addAll(cList.map { Course(it.id, it.name, it.description) })

            subjects.clear()
            subjects.addAll(sList.map { Subject(it.id, it.courseId, it.name, it.professor, it.maxAbsences, it.room) })

            scheduleItems.clear()
            scheduleItems.addAll(scList.map { ScheduleItem(it.id, it.subjectId, it.dayOfWeek, it.startTime, it.endTime, it.room) })

            absenceRecords.clear()
            absenceRecords.addAll(aList.map { AbsenceRecord(it.id, it.subjectId, it.date) })

            events.clear()
            events.addAll(eList.map {
                EventItem(
                    id = it.id,
                    subjectId = it.subjectId,
                    title = it.title,
                    description = it.description,
                    date = try { LocalDate.parse(it.date) } catch (_: Exception) { LocalDate.now() },
                    time = if (!it.time.isNullOrBlank()) try { LocalTime.parse(it.time) } catch (_: Exception) { null } else null,
                    type = try { EventType.valueOf(it.type) } catch (_: Exception) { EventType.PROVA },
                    isCompleted = it.isCompleted,
                    completedAt = if (!it.completedAt.isNullOrBlank()) try { LocalDate.parse(it.completedAt) } catch (_: Exception) { null } else null,
                    reminderEnabled = it.reminderEnabled,
                    reminderMinutesBefore = it.reminderMinutesBefore
                )
            })
        }
    }

    fun loadSampleData() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = db ?: return@launch
            database.withTransaction {
                val c1 = CourseEntity("c1", "Ciência da Computação", "Bacharelado integral")
                val c2 = CourseEntity("c2", "Engenharia de Software", "Ênfase em sistemas móveis")
                database.courseDao().insertAllCourses(listOf(c1, c2))

                val s1 = SubjectEntity("s1", "c1", "Cálculo I", "Dr. Carlos", 15, "Sala 101")
                val s2 = SubjectEntity("s2", "c1", "Estrutura de Dados", "Dra. Ana", 12, "Lab 03")
                val s3 = SubjectEntity("s3", "c2", "Desenvolvimento Mobile", "Prof. Marcos", 10, "Lab 05")
                val s4 = SubjectEntity("s4", "c2", "Banco de Dados", "Dra. Beatriz", 15, "Sala 204")
                database.subjectDao().insertAllSubjects(listOf(s1, s2, s3, s4))

                val sch1 = ScheduleItemEntity("sch1", "s1", 1, "07:30", "09:10", "Sala 101")
                val sch2 = ScheduleItemEntity("sch2", "s2", 2, "09:30", "11:10", "Lab 03")
                database.scheduleDao().insertAllScheduleItems(listOf(sch1, sch2))

                val ev1 = EventEntity("e1", "s1", "Prova P1 de Cálculo I", "Conteúdo: Limites, Derivadas.", getFormattedDate(2), "08:00", EventType.PROVA.name, false, null, false, 30)
                val ev2 = EventEntity("e2", "s2", "Entrega de Trabalho", "Árvores AVL.", getFormattedDate(5), "23:59", EventType.TRABALHO.name, false, null, false, 30)
                database.eventDao().insertAllEvents(listOf(ev1, ev2))
            }
            refreshData()
        }
    }

    fun addCourse(name: String, description: String) {
        val course = CourseEntity(id = UUID.randomUUID().toString(), name = name, description = description)
        CoroutineScope(Dispatchers.IO).launch {
            db?.courseDao()?.insertCourse(course)
            refreshData()
        }
    }

    fun removeCourse(courseId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            db?.let { database ->
                database.withTransaction {
                    database.courseDao().deleteCourse(courseId)
                }
            }
            refreshData()
        }
    }

    fun addSubject(courseId: String, name: String, professor: String, maxAbsences: Int, room: String) {
        val subject = SubjectEntity(id = UUID.randomUUID().toString(), courseId = courseId, name = name, professor = professor, maxAbsences = maxAbsences, room = room)
        CoroutineScope(Dispatchers.IO).launch {
            db?.subjectDao()?.insertSubject(subject)
            refreshData()
        }
    }

    fun removeSubject(subjectId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            db?.let { database ->
                database.withTransaction {
                    database.subjectDao().deleteSubject(subjectId)
                }
            }
            refreshData()
        }
    }

    fun addScheduleItem(subjectId: String, dayOfWeek: Int, startTime: String, endTime: String, room: String) {
        val item = ScheduleItemEntity(id = UUID.randomUUID().toString(), subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, room = room)
        CoroutineScope(Dispatchers.IO).launch {
            db?.scheduleDao()?.insertScheduleItem(item)
            refreshData()
        }
    }

    fun removeScheduleItem(itemId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            db?.scheduleDao()?.deleteScheduleItem(itemId)
            refreshData()
        }
    }

    fun isAbsentOnDate(subjectId: String, date: String): Boolean {
        return absenceRecords.any { it.subjectId == subjectId && it.date == date }
    }

    fun setAbsenceForDate(subjectId: String, date: String, isAbsent: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = db ?: return@launch
            if (isAbsent) {
                val record = AbsenceRecordEntity(id = UUID.randomUUID().toString(), subjectId = subjectId, date = date)
                database.absenceDao().insertAbsence(record)
            } else {
                database.absenceDao().deleteAbsence(subjectId, date)
            }
            refreshData()
        }
    }

    fun getAbsenceCount(subjectId: String): Int {
        return absenceRecords.count { it.subjectId == subjectId }
    }

    @SuppressLint("NewApi")
    fun addEvent(subjectId: String, title: String, description: String, date: LocalDate, time: LocalTime?, type: EventType, reminderEnabled: Boolean, reminderMinutesBefore: Int) {
        val event = EventEntity(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            title = title,
            description = description,
            date = date.toString(),
            time = time?.toString(),
            type = type.name,
            isCompleted = false,
            completedAt = null,
            reminderEnabled = reminderEnabled,
            reminderMinutesBefore = reminderMinutesBefore
        )
        CoroutineScope(Dispatchers.IO).launch {
            db?.eventDao()?.insertEvent(event)
            refreshData()
        }
    }

    @SuppressLint("NewApi")
    fun updateEvent(eventId: String, subjectId: String, title: String, description: String, date: LocalDate, time: LocalTime?, type: EventType, reminderEnabled: Boolean, reminderMinutesBefore: Int) {
        val existing = events.find { it.id == eventId } ?: return
        val event = EventEntity(
            id = eventId,
            subjectId = subjectId,
            title = title,
            description = description,
            date = date.toString(),
            time = time?.toString(),
            type = type.name,
            isCompleted = existing.isCompleted,
            completedAt = existing.completedAt?.toString(),
            reminderEnabled = reminderEnabled,
            reminderMinutesBefore = reminderMinutesBefore
        )
        CoroutineScope(Dispatchers.IO).launch {
            db?.eventDao()?.insertEvent(event)
            refreshData()
        }
    }

    fun removeEvent(eventId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            db?.eventDao()?.deleteEvent(eventId)
            refreshData()
        }
    }

    @SuppressLint("NewApi")
    fun toggleEventComplete(eventId: String) {
        val existing = events.find { it.id == eventId } ?: return
        val newCompleted = !existing.isCompleted
        val completedAtStr = if (newCompleted) LocalDate.now().toString() else null
        val updated = existing.copy(isCompleted = newCompleted, completedAt = if (newCompleted) LocalDate.now() else null)
        val entity = EventEntity(
            id = updated.id,
            subjectId = updated.subjectId,
            title = updated.title,
            description = updated.description,
            date = updated.date.toString(),
            time = updated.time?.toString(),
            type = updated.type.name,
            isCompleted = updated.isCompleted,
            completedAt = completedAtStr,
            reminderEnabled = updated.reminderEnabled,
            reminderMinutesBefore = updated.reminderMinutesBefore
        )
        CoroutineScope(Dispatchers.IO).launch {
            db?.eventDao()?.insertEvent(entity)
            refreshData()
        }
    }

    fun getBackupSnapshot(): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appVersion", 18)

        val cArr = JSONArray()
        courses.forEach { c -> cArr.put(JSONObject().put("id", c.id).put("name", c.name).put("description", c.description)) }
        root.put("courses", cArr)

        val sArr = JSONArray()
        subjects.forEach { s -> sArr.put(JSONObject().put("id", s.id).put("courseId", s.courseId).put("name", s.name).put("professor", s.professor).put("maxAbsences", s.maxAbsences).put("room", s.room)) }
        root.put("subjects", sArr)

        val scArr = JSONArray()
        scheduleItems.forEach { sc -> scArr.put(JSONObject().put("id", sc.id).put("subjectId", sc.subjectId).put("dayOfWeek", sc.dayOfWeek).put("startTime", sc.startTime).put("endTime", sc.endTime).put("room", sc.room)) }
        root.put("scheduleItems", scArr)

        val absArr = JSONArray()
        absenceRecords.forEach { ar -> absArr.put(JSONObject().put("id", ar.id).put("subjectId", ar.subjectId).put("date", ar.date)) }
        root.put("absenceRecords", absArr)

        val evArr = JSONArray()
        events.forEach { ev ->
            evArr.put(JSONObject().apply {
                put("id", ev.id)
                put("subjectId", ev.subjectId)
                put("title", ev.title)
                put("description", ev.description)
                put("date", ev.date.toString())
                put("time", ev.time?.toString() ?: JSONObject.NULL)
                put("type", ev.type.name)
                put("isCompleted", ev.isCompleted)
                put("completedAt", ev.completedAt?.toString() ?: JSONObject.NULL)
                put("reminderEnabled", ev.reminderEnabled)
                put("reminderMinutesBefore", ev.reminderMinutesBefore)
            })
        }
        root.put("events", evArr)

        return root
    }

    fun restoreFromSnapshot(json: JSONObject): Boolean {
        return runBlocking(Dispatchers.IO) {
            try {
                val database = db ?: return@runBlocking false
                val cArr = json.getJSONArray("courses")
                val sArr = json.getJSONArray("subjects")
                val scArr = json.getJSONArray("scheduleItems")
                val absArr = json.getJSONArray("absenceRecords")
                val evArr = json.getJSONArray("events")

                val newCourses = mutableListOf<CourseEntity>()
                for (i in 0 until cArr.length()) {
                    val o = cArr.getJSONObject(i)
                    newCourses.add(CourseEntity(o.getString("id"), o.getString("name"), o.optString("description", "")))
                }

                val newSubjects = mutableListOf<SubjectEntity>()
                for (i in 0 until sArr.length()) {
                    val o = sArr.getJSONObject(i)
                    newSubjects.add(SubjectEntity(o.getString("id"), o.getString("courseId"), o.getString("name"), o.optString("professor", ""), o.optInt("maxAbsences", 15), o.optString("room", "")))
                }

                val newSchedule = mutableListOf<ScheduleItemEntity>()
                for (i in 0 until scArr.length()) {
                    val o = scArr.getJSONObject(i)
                    newSchedule.add(ScheduleItemEntity(o.getString("id"), o.getString("subjectId"), o.getInt("dayOfWeek"), o.getString("startTime"), o.getString("endTime"), o.optString("room", "")))
                }

                val newAbs = mutableListOf<AbsenceRecordEntity>()
                for (i in 0 until absArr.length()) {
                    val o = absArr.getJSONObject(i)
                    newAbs.add(AbsenceRecordEntity(o.getString("id"), o.getString("subjectId"), o.getString("date")))
                }

                val newEvents = mutableListOf<EventEntity>()
                for (i in 0 until evArr.length()) {
                    val o = evArr.getJSONObject(i)
                    newEvents.add(
                        EventEntity(
                            id = o.getString("id"),
                            subjectId = o.getString("subjectId"),
                            title = o.getString("title"),
                            description = o.optString("description", ""),
                            date = o.getString("date"),
                            time = if (o.has("time") && !o.isNull("time")) o.getString("time") else null,
                            type = o.getString("type"),
                            isCompleted = o.optBoolean("isCompleted", false),
                            completedAt = if (o.has("completedAt") && !o.isNull("completedAt")) o.getString("completedAt") else null,
                            reminderEnabled = o.optBoolean("reminderEnabled", false),
                            reminderMinutesBefore = o.optInt("reminderMinutesBefore", 30)
                        )
                    )
                }

                database.withTransaction {
                    database.courseDao().insertAllCourses(newCourses)
                    database.subjectDao().insertAllSubjects(newSubjects)
                    database.scheduleDao().insertAllScheduleItems(newSchedule)
                    database.absenceDao().insertAllAbsences(newAbs)
                    database.eventDao().insertAllEvents(newEvents)
                }
                refreshData()
                true
            } catch (e: Exception) {
                Log.e("EamRepository", "Erro na restauração: ${e.message}")
                false
            }
        }
    }

    @SuppressLint("NewApi")
    fun checkScheduleConflict(subjectId: String, dayOfWeek: Int, startTime: java.time.LocalTime, endTime: java.time.LocalTime, excludeItemId: String? = null): Boolean {
        return scheduleItems.any { item ->
            if (item.id == excludeItemId) return@any false
            if (item.dayOfWeek != dayOfWeek) return@any false
            try {
                val existingStart = java.time.LocalTime.parse(item.startTime)
                val existingEnd = java.time.LocalTime.parse(item.endTime)
                startTime.isBefore(existingEnd) && endTime.isAfter(existingStart)
            } catch (_: Exception) {
                false
            }
        }
    }

    fun getCourseImpact(courseId: String): CourseImpact {
        val subIds = subjects.filter { it.courseId == courseId }.map { it.id }
        val subjectsCount = subIds.size
        val schedulesCount = scheduleItems.count { subIds.contains(it.subjectId) }
        val eventsCount = events.count { subIds.contains(it.subjectId) }
        val absencesCount = absenceRecords.count { subIds.contains(it.subjectId) }
        return CourseImpact(subjectsCount, schedulesCount, eventsCount, absencesCount)
    }

    fun getSubjectImpact(subjectId: String): SubjectImpact {
        val schedulesCount = scheduleItems.count { it.subjectId == subjectId }
        val eventsCount = events.count { it.subjectId == subjectId }
        val absencesCount = absenceRecords.count { it.subjectId == subjectId }
        return SubjectImpact(schedulesCount, eventsCount, absencesCount)
    }
}
