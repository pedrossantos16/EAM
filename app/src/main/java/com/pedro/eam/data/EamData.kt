package com.pedro.eam.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class Course(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = ""
)

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val courseId: String,
    val name: String,
    val professor: String = "",
    val maxAbsences: Int = 15,
    val room: String = ""
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
    val date: String = getFormattedDate(0),
    val type: EventType = EventType.PROVA,
    val isCompleted: Boolean = false
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

    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.getSharedPreferences("eam_prefs", Context.MODE_PRIVATE)
        loadData()
        isInitialized = true
    }

    private fun saveData() {
        val p = prefs ?: return
        val editor = p.edit()

        val coursesArr = JSONArray()
        courses.forEach { c ->
            coursesArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("description", c.description)
            })
        }
        editor.putString("courses", coursesArr.toString())

        val subsArr = JSONArray()
        subjects.forEach { s ->
            subsArr.put(JSONObject().apply {
                put("id", s.id)
                put("courseId", s.courseId)
                put("name", s.name)
                put("professor", s.professor)
                put("maxAbsences", s.maxAbsences)
                put("room", s.room)
            })
        }
        editor.putString("subjects", subsArr.toString())

        val schArr = JSONArray()
        scheduleItems.forEach { sc ->
            schArr.put(JSONObject().apply {
                put("id", sc.id)
                put("subjectId", sc.subjectId)
                put("dayOfWeek", sc.dayOfWeek)
                put("startTime", sc.startTime)
                put("endTime", sc.endTime)
                put("room", sc.room)
            })
        }
        editor.putString("schedule", schArr.toString())

        val absArr = JSONArray()
        absenceRecords.forEach { ar ->
            absArr.put(JSONObject().apply {
                put("id", ar.id)
                put("subjectId", ar.subjectId)
                put("date", ar.date)
            })
        }
        editor.putString("absences", absArr.toString())

        val evArr = JSONArray()
        events.forEach { ev ->
            evArr.put(JSONObject().apply {
                put("id", ev.id)
                put("subjectId", ev.subjectId)
                put("title", ev.title)
                put("description", ev.description)
                put("date", ev.date)
                put("type", ev.type.name)
                put("isCompleted", ev.isCompleted)
            })
        }
        editor.putString("events", evArr.toString())

        editor.apply()
    }

    private fun loadData() {
        val p = prefs ?: return
        val hasData = p.contains("courses")

        if (!hasData) {
            courses.addAll(listOf(
                Course(id = "c1", name = "Ciência da Computação", description = "Bacharelado integral"),
                Course(id = "c2", name = "Engenharia de Software", description = "Ênfase em sistemas móveis")
            ))

            subjects.addAll(listOf(
                Subject(id = "s1", courseId = "c1", name = "Cálculo I", professor = "Dr. Carlos", maxAbsences = 15, room = "Sala 101"),
                Subject(id = "s2", courseId = "c1", name = "Estrutura de Dados", professor = "Dra. Ana", maxAbsences = 12, room = "Lab 03"),
                Subject(id = "s3", courseId = "c2", name = "Desenvolvimento Mobile", professor = "Prof. Marcos", maxAbsences = 10, room = "Lab 05"),
                Subject(id = "s4", courseId = "c2", name = "Banco de Dados", professor = "Dra. Beatriz", maxAbsences = 15, room = "Sala 204")
            ))

            scheduleItems.addAll(listOf(
                ScheduleItem(id = "sch1", subjectId = "s1", dayOfWeek = 1, startTime = "07:30", endTime = "09:10", room = "Sala 101"),
                ScheduleItem(id = "sch2", subjectId = "s2", dayOfWeek = 2, startTime = "09:30", endTime = "11:10", room = "Lab 03"),
                ScheduleItem(id = "sch3", subjectId = "s3", dayOfWeek = 3, startTime = "13:30", endTime = "15:10", room = "Lab 05"),
                ScheduleItem(id = "sch4", subjectId = "s4", dayOfWeek = 4, startTime = "15:30", endTime = "17:10", room = "Sala 204")
            ))

            events.addAll(listOf(
                EventItem(id = "e1", subjectId = "s1", title = "Prova P1 de Cálculo I", description = "Conteúdo: Limites, Derivadas.", date = getFormattedDate(2), type = EventType.PROVA),
                EventItem(id = "e2", subjectId = "s2", title = "Entrega de Trabalho", description = "Árvores AVL.", date = getFormattedDate(5), type = EventType.TRABALHO)
            ))

            saveData()
            return
        }

        try {
            courses.clear()
            val coursesJson = p.getString("courses", "[]") ?: "[]"
            val cArr = JSONArray(coursesJson)
            for (i in 0 until cArr.length()) {
                val obj = cArr.getJSONObject(i)
                courses.add(Course(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    description = obj.optString("description", "")
                ))
            }

            subjects.clear()
            val subsJson = p.getString("subjects", "[]") ?: "[]"
            val sArr = JSONArray(subsJson)
            for (i in 0 until sArr.length()) {
                val obj = sArr.getJSONObject(i)
                subjects.add(Subject(
                    id = obj.getString("id"),
                    courseId = obj.getString("courseId"),
                    name = obj.getString("name"),
                    professor = obj.optString("professor", ""),
                    maxAbsences = obj.optInt("maxAbsences", 15),
                    room = obj.optString("room", "")
                ))
            }

            scheduleItems.clear()
            val schJson = p.getString("schedule", "[]") ?: "[]"
            val scArr = JSONArray(schJson)
            for (i in 0 until scArr.length()) {
                val obj = scArr.getJSONObject(i)
                scheduleItems.add(ScheduleItem(
                    id = obj.getString("id"),
                    subjectId = obj.getString("subjectId"),
                    dayOfWeek = obj.getInt("dayOfWeek"),
                    startTime = obj.getString("startTime"),
                    endTime = obj.getString("endTime"),
                    room = obj.optString("room", "")
                ))
            }

            absenceRecords.clear()
            val absJson = p.getString("absences", "[]") ?: "[]"
            val aArr = JSONArray(absJson)
            for (i in 0 until aArr.length()) {
                val obj = aArr.getJSONObject(i)
                absenceRecords.add(AbsenceRecord(
                    id = obj.getString("id"),
                    subjectId = obj.getString("subjectId"),
                    date = obj.getString("date")
                ))
            }

            events.clear()
            val evJson = p.getString("events", "[]") ?: "[]"
            val eArr = JSONArray(evJson)
            for (i in 0 until eArr.length()) {
                val obj = eArr.getJSONObject(i)
                events.add(EventItem(
                    id = obj.getString("id"),
                    subjectId = obj.getString("subjectId"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    date = obj.getString("date"),
                    type = try { EventType.valueOf(obj.getString("type")) } catch (_: Exception) { EventType.PROVA },
                    isCompleted = obj.optBoolean("isCompleted", false)
                ))
            }
        } catch (e: Exception) {
            Log.e("EamRepository", "Erro ao carregar dados salvos: ${e.message}")
        }
    }

    fun addCourse(name: String, description: String) {
        courses.add(Course(name = name, description = description))
        saveData()
    }

    fun removeCourse(courseId: String) {
        val subIds = subjects.filter { it.courseId == courseId }.map { it.id }
        scheduleItems.removeAll { subIds.contains(it.subjectId) }
        absenceRecords.removeAll { subIds.contains(it.subjectId) }
        events.removeAll { subIds.contains(it.subjectId) }
        subjects.removeAll { it.courseId == courseId }
        courses.removeAll { it.id == courseId }
        saveData()
    }

    fun addSubject(courseId: String, name: String, professor: String, maxAbsences: Int, room: String) {
        subjects.add(Subject(courseId = courseId, name = name, professor = professor, maxAbsences = maxAbsences, room = room))
        saveData()
    }

    fun removeSubject(subjectId: String) {
        scheduleItems.removeAll { it.subjectId == subjectId }
        absenceRecords.removeAll { it.subjectId == subjectId }
        events.removeAll { it.subjectId == subjectId }
        subjects.removeAll { it.id == subjectId }
        saveData()
    }

    fun addScheduleItem(subjectId: String, dayOfWeek: Int, startTime: String, endTime: String, room: String) {
        scheduleItems.add(ScheduleItem(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, room = room))
        saveData()
    }

    fun removeScheduleItem(itemId: String) {
        scheduleItems.removeAll { it.id == itemId }
        saveData()
    }

    fun isAbsentOnDate(subjectId: String, date: String): Boolean {
        return absenceRecords.any { it.subjectId == subjectId && it.date == date }
    }

    fun setAbsenceForDate(subjectId: String, date: String, isAbsent: Boolean) {
        if (isAbsent) {
            if (!isAbsentOnDate(subjectId, date)) {
                absenceRecords.add(AbsenceRecord(subjectId = subjectId, date = date))
            }
        } else {
            absenceRecords.removeAll { it.subjectId == subjectId && it.date == date }
        }
        saveData()
    }

    fun getAbsenceCount(subjectId: String): Int {
        return absenceRecords.count { it.subjectId == subjectId }
    }

    fun addEvent(subjectId: String, title: String, description: String, date: String, type: EventType) {
        events.add(EventItem(subjectId = subjectId, title = title, description = description, date = date, type = type))
        saveData()
    }

    fun updateEvent(eventId: String, title: String, description: String, date: String, type: EventType) {
        val index = events.indexOfFirst { it.id == eventId }
        if (index != -1) {
            val ev = events[index]
            events[index] = ev.copy(title = title, description = description, date = date, type = type)
            saveData()
        }
    }

    fun removeEvent(eventId: String) {
        events.removeAll { it.id == eventId }
        saveData()
    }

    fun toggleEventComplete(eventId: String) {
        val index = events.indexOfFirst { it.id == eventId }
        if (index != -1) {
            val ev = events[index]
            events[index] = ev.copy(isCompleted = !ev.isCompleted)
            saveData()
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
        events.forEach { ev -> evArr.put(JSONObject().put("id", ev.id).put("subjectId", ev.subjectId).put("title", ev.title).put("description", ev.description).put("date", ev.date).put("type", ev.type.name).put("isCompleted", ev.isCompleted)) }
        root.put("events", evArr)

        return root
    }

    fun restoreFromSnapshot(json: JSONObject): Boolean {
        return try {
            val cArr = json.getJSONArray("courses")
            val sArr = json.getJSONArray("subjects")
            val scArr = json.getJSONArray("scheduleItems")
            val absArr = json.getJSONArray("absenceRecords")
            val evArr = json.getJSONArray("events")

            val newCourses = mutableListOf<Course>()
            for (i in 0 until cArr.length()) {
                val o = cArr.getJSONObject(i)
                newCourses.add(Course(o.getString("id"), o.getString("name"), o.optString("description", "")))
            }

            val newSubjects = mutableListOf<Subject>()
            for (i in 0 until sArr.length()) {
                val o = sArr.getJSONObject(i)
                newSubjects.add(Subject(o.getString("id"), o.getString("courseId"), o.getString("name"), o.optString("professor", ""), o.optInt("maxAbsences", 15), o.optString("room", "")))
            }

            val newSchedule = mutableListOf<ScheduleItem>()
            for (i in 0 until scArr.length()) {
                val o = scArr.getJSONObject(i)
                newSchedule.add(ScheduleItem(o.getString("id"), o.getString("subjectId"), o.getInt("dayOfWeek"), o.getString("startTime"), o.getString("endTime"), o.optString("room", "")))
            }

            val newAbs = mutableListOf<AbsenceRecord>()
            for (i in 0 until absArr.length()) {
                val o = absArr.getJSONObject(i)
                newAbs.add(AbsenceRecord(o.getString("id"), o.getString("subjectId"), o.getString("date")))
            }

            val newEvents = mutableListOf<EventItem>()
            for (i in 0 until evArr.length()) {
                val o = evArr.getJSONObject(i)
                newEvents.add(EventItem(o.getString("id"), o.getString("subjectId"), o.getString("title"), o.optString("description", ""), o.getString("date"), try { EventType.valueOf(o.getString("type")) } catch (_: Exception) { EventType.PROVA }, o.optBoolean("isCompleted", false)))
            }

            courses.clear()
            courses.addAll(newCourses)
            subjects.clear()
            subjects.addAll(newSubjects)
            scheduleItems.clear()
            scheduleItems.addAll(newSchedule)
            absenceRecords.clear()
            absenceRecords.addAll(newAbs)
            events.clear()
            events.addAll(newEvents)
            saveData()
            true
        } catch (e: Exception) {
            false
        }
    }
}
