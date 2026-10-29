package com.pedro.eam.utils

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSummary(
    val exportedAt: String,
    val schemaVersion: Int,
    val appVersion: Int,
    val coursesCount: Int,
    val subjectsCount: Int,
    val scheduleCount: Int,
    val absencesCount: Int,
    val eventsCount: Int
)

object BackupManager {
    fun validateAndGetSummary(jsonString: String): Result<Pair<BackupSummary, JSONObject>> {
        return try {
            val json = JSONObject(jsonString)
            
            val schemaVersion = json.optInt("schemaVersion", -1)
            if (schemaVersion != 1) {
                return Result.failure(IllegalArgumentException("Versão de schema inválida ou não suportada ($schemaVersion)."))
            }

            val exportedAtMillis = json.optLong("exportedAt", System.currentTimeMillis())
            val exportedAtStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(exportedAtMillis))
            val appVersion = json.optInt("appVersion", 1)

            val courses = json.optJSONArray("courses") ?: JSONArray()
            val subjects = json.optJSONArray("subjects") ?: JSONArray()
            val schedule = json.optJSONArray("scheduleItems") ?: JSONArray()
            val absences = json.optJSONArray("absenceRecords") ?: JSONArray()
            val events = json.optJSONArray("events") ?: JSONArray()

            val courseIds = mutableSetOf<String>()
            for (i in 0 until courses.length()) {
                val c = courses.getJSONObject(i)
                if (!c.has("id") || !c.has("name")) {
                    return Result.failure(IllegalArgumentException("Curso malformado."))
                }
                courseIds.add(c.getString("id"))
            }

            val subjectIds = mutableSetOf<String>()
            for (i in 0 until subjects.length()) {
                val s = subjects.getJSONObject(i)
                if (!s.has("id") || !s.has("courseId") || !s.has("name")) {
                    return Result.failure(IllegalArgumentException("Matéria malformada."))
                }
                val courseId = s.getString("courseId")
                if (!courseIds.contains(courseId)) {
                    return Result.failure(IllegalArgumentException("Matéria referencia curso inexistente ($courseId)."))
                }
                subjectIds.add(s.getString("id"))
            }

            for (i in 0 until schedule.length()) {
                val sc = schedule.getJSONObject(i)
                if (!sc.has("subjectId")) {
                    return Result.failure(IllegalArgumentException("Horário malformado."))
                }
                val subId = sc.getString("subjectId")
                if (!subjectIds.contains(subId)) {
                    return Result.failure(IllegalArgumentException("Horário referencia matéria inexistente ($subId)."))
                }
            }

            for (i in 0 until absences.length()) {
                val ar = absences.getJSONObject(i)
                if (!ar.has("subjectId")) {
                    return Result.failure(IllegalArgumentException("Registro de falta malformado."))
                }
                val subId = ar.getString("subjectId")
                if (!subjectIds.contains(subId)) {
                    return Result.failure(IllegalArgumentException("Falta referencia matéria inexistente ($subId)."))
                }
            }

            for (i in 0 until events.length()) {
                val ev = events.getJSONObject(i)
                if (!ev.has("subjectId")) {
                    return Result.failure(IllegalArgumentException("Evento malformado."))
                }
                val subId = ev.getString("subjectId")
                if (!subjectIds.contains(subId)) {
                    return Result.failure(IllegalArgumentException("Evento referencia matéria inexistente ($subId)."))
                }
            }

            val summary = BackupSummary(
                exportedAt = exportedAtStr,
                schemaVersion = schemaVersion,
                appVersion = appVersion,
                coursesCount = courses.length(),
                subjectsCount = subjects.length(),
                scheduleCount = schedule.length(),
                absencesCount = absences.length(),
                eventsCount = events.length()
            )

            Result.success(Pair(summary, json))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Arquivo JSON corrompido ou inválido: ${e.localizedMessage}"))
        }
    }
}
