package com.pedro.eam.data

import androidx.compose.runtime.mutableStateListOf
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

// Schedule / Grade Curricular item
data class ScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val dayOfWeek: Int, // 1 = Segunda, 2 = Terça, 3 = Quarta, 4 = Quinta, 5 = Sexta, 6 = Sábado, 7 = Domingo
    val startTime: String, // e.g. "08:00"
    val endTime: String,   // e.g. "09:40"
    val room: String = ""
)

// Absence record per date and subject
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
    val date: String = getFormattedDate(0), // YYYY-MM-DD
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
    val courses = mutableStateListOf(
        Course(id = "c1", name = "Ciência da Computação", description = "Bacharelado integral"),
        Course(id = "c2", name = "Engenharia de Software", description = "Ênfase em sistemas móveis")
    )

    val subjects = mutableStateListOf(
        Subject(id = "s1", courseId = "c1", name = "Cálculo I", professor = "Dr. Carlos", maxAbsences = 15, room = "Sala 101"),
        Subject(id = "s2", courseId = "c1", name = "Estrutura de Dados", professor = "Dra. Ana", maxAbsences = 12, room = "Lab 03"),
        Subject(id = "s3", courseId = "c2", name = "Desenvolvimento Mobile", professor = "Prof. Marcos", maxAbsences = 10, room = "Lab 05"),
        Subject(id = "s4", courseId = "c2", name = "Banco de Dados", professor = "Dra. Beatriz", maxAbsences = 15, room = "Sala 204")
    )

    val scheduleItems = mutableStateListOf(
        ScheduleItem(id = "sch1", subjectId = "s1", dayOfWeek = 1, startTime = "07:30", endTime = "09:10", room = "Sala 101"),
        ScheduleItem(id = "sch2", subjectId = "s2", dayOfWeek = 2, startTime = "09:30", endTime = "11:10", room = "Lab 03"),
        ScheduleItem(id = "sch3", subjectId = "s3", dayOfWeek = 3, startTime = "13:30", endTime = "15:10", room = "Lab 05"),
        ScheduleItem(id = "sch4", subjectId = "s4", dayOfWeek = 4, startTime = "15:30", endTime = "17:10", room = "Sala 204")
    )

    val absenceRecords = mutableStateListOf<AbsenceRecord>()

    val events = mutableStateListOf(
        EventItem(
            id = "e1",
            subjectId = "s1",
            title = "Prova P1 de Cálculo I",
            description = "Conteúdo: Limites, Derivadas e Aplicações. Trazer calculadora científica.",
            date = getFormattedDate(2),
            type = EventType.PROVA
        ),
        EventItem(
            id = "e2",
            subjectId = "s2",
            title = "Entrega do Trabalho de Árvores AVL",
            description = "Implementar em C++ ou Java com testes unitários e relatório em PDF.",
            date = getFormattedDate(5),
            type = EventType.TRABALHO
        ),
        EventItem(
            id = "e3",
            subjectId = "s3",
            title = "Apresentação do Projeto Jetpack Compose",
            description = "Demonstração ao vivo do aplicativo EAM com todas as telas e gráficos.",
            date = getFormattedDate(8),
            type = EventType.APRESENTACAO
        ),
        EventItem(
            id = "e4",
            subjectId = "s4",
            title = "Quiz de Normalização SQL",
            description = "Quiz rápido sobre 1FN, 2FN e 3FN no início da aula.",
            date = getFormattedDate(-1),
            type = EventType.LEITURA
        )
    )

    fun addCourse(name: String, description: String) {
        courses.add(Course(name = name, description = description))
    }

    fun removeCourse(courseId: String) {
        val subIds = subjects.filter { it.courseId == courseId }.map { it.id }
        scheduleItems.removeAll { subIds.contains(it.subjectId) }
        absenceRecords.removeAll { subIds.contains(it.subjectId) }
        events.removeAll { subIds.contains(it.subjectId) }
        subjects.removeAll { it.courseId == courseId }
        courses.removeAll { it.id == courseId }
    }

    fun addSubject(courseId: String, name: String, professor: String, maxAbsences: Int, room: String) {
        subjects.add(Subject(courseId = courseId, name = name, professor = professor, maxAbsences = maxAbsences, room = room))
    }

    fun removeSubject(subjectId: String) {
        scheduleItems.removeAll { it.subjectId == subjectId }
        absenceRecords.removeAll { it.subjectId == subjectId }
        events.removeAll { it.subjectId == subjectId }
        subjects.removeAll { it.id == subjectId }
    }

    fun addScheduleItem(subjectId: String, dayOfWeek: Int, startTime: String, endTime: String, room: String) {
        scheduleItems.add(ScheduleItem(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, room = room))
    }

    fun removeScheduleItem(itemId: String) {
        scheduleItems.removeAll { it.id == itemId }
    }

    // Check if absent for subject on date
    fun isAbsentOnDate(subjectId: String, date: String): Boolean {
        return absenceRecords.any { it.subjectId == subjectId && it.date == date }
    }

    // Toggle absence for subject on date
    fun setAbsenceForDate(subjectId: String, date: String, isAbsent: Boolean) {
        if (isAbsent) {
            if (!isAbsentOnDate(subjectId, date)) {
                absenceRecords.add(AbsenceRecord(subjectId = subjectId, date = date))
            }
        } else {
            absenceRecords.removeAll { it.subjectId == subjectId && it.date == date }
        }
    }

    // Total absences count for a subject = count of absenceRecords for that subject
    fun getAbsenceCount(subjectId: String): Int {
        return absenceRecords.count { it.subjectId == subjectId }
    }

    fun addEvent(subjectId: String, title: String, description: String, date: String, type: EventType) {
        events.add(EventItem(subjectId = subjectId, title = title, description = description, date = date, type = type))
    }

    fun updateEvent(eventId: String, title: String, description: String, date: String, type: EventType) {
        val index = events.indexOfFirst { it.id == eventId }
        if (index != -1) {
            val ev = events[index]
            events[index] = ev.copy(title = title, description = description, date = date, type = type)
        }
    }

    fun removeEvent(eventId: String) {
        events.removeAll { it.id == eventId }
    }

    fun toggleEventComplete(eventId: String) {
        val index = events.indexOfFirst { it.id == eventId }
        if (index != -1) {
            val ev = events[index]
            events[index] = ev.copy(isCompleted = !ev.isCompleted)
        }
    }
}
