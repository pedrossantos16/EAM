package com.pedro.eam.ui.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.eam.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SubjectAbsenceAnalytics(
    val subject: Subject,
    val absencesCount: Int,
    val maxAbsences: Int,
    val remainingAbsences: Int,
    val percentage: Float,
    val isRisk: Boolean
)

data class DayClassLoad(
    val dayName: String,
    val dayOfWeek: Int,
    val classesCount: Int
)

data class EventAnalyticsSummary(
    val upcoming7DaysCount: Int,
    val upcoming30DaysCount: Int,
    val overdueCount: Int,
    val completedCount: Int
)

data class AnalyticsUiState(
    val absenceAnalytics: List<SubjectAbsenceAnalytics> = emptyList(),
    val dayClassLoads: List<DayClassLoad> = emptyList(),
    val eventSummary: EventAnalyticsSummary = EventAnalyticsSummary(0, 0, 0, 0),
    val isLoading: Boolean = false
)

class AnalyticsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    @SuppressLint("NewApi")
    fun loadData() {
        viewModelScope.launch {
            val subjects = EamRepository.subjects
            val scheduleItems = EamRepository.scheduleItems
            val events = EamRepository.events
            val absences = EamRepository.absenceRecords
            val today = LocalDate.now()

            val absenceList = subjects.map { sub ->
                val absCount = absences.count { it.subjectId == sub.id }
                val max = sub.maxAbsences.coerceAtLeast(1)
                val remaining = (max - absCount).coerceAtLeast(0)
                val pct = (absCount.toFloat() / max.toFloat()) * 100f
                val isRisk = pct >= 70f
                SubjectAbsenceAnalytics(sub, absCount, max, remaining, pct, isRisk)
            }

            val daysMap = mapOf(
                1 to "Seg", 2 to "Ter", 3 to "Qua", 4 to "Qui", 5 to "Sex", 6 to "Sáb", 7 to "Dom"
            )
            val dayLoads = (1..7).map { dow ->
                val count = scheduleItems.count { it.dayOfWeek == dow }
                DayClassLoad(daysMap[dow] ?: "$dow", dow, count)
            }

            val next7End = today.plusDays(7)
            val next30End = today.plusDays(30)
            val upcoming7 = events.count { !it.isCompleted && (it.date.isAfter(today) || it.date.isEqual(today)) && it.date.isBefore(next7End) }
            val upcoming30 = events.count { !it.isCompleted && (it.date.isAfter(today) || it.date.isEqual(today)) && it.date.isBefore(next30End) }
            val overdue = events.count { !it.isCompleted && it.date.isBefore(today) }
            val completed = events.count { it.isCompleted }

            _uiState.update {
                it.copy(
                    absenceAnalytics = absenceList,
                    dayClassLoads = dayLoads,
                    eventSummary = EventAnalyticsSummary(upcoming7, upcoming30, overdue, completed),
                    isLoading = false
                )
            }
        }
    }
}
