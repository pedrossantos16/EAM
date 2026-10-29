package com.pedro.eam.ui.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.eam.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

data class TodaySummary(
    val nextClassName: String?,
    val nextClassTime: String?,
    val remainingClassesCount: Int,
    val todayEvents: List<EventItem>,
    val overdueEvents: List<EventItem>
)

data class SubjectRiskInfo(
    val subject: Subject,
    val absences: Int,
    val maxAbsences: Int,
    val percentage: Float,
    val status: RiskStatus
)

enum class RiskStatus(val label: String, val iconName: String) {
    NORMAL("Normal", "Check"),
    WARNING("Atenção", "Warning"),
    CRITICAL("Crítico", "Error"),
    EXCEEDED("Excedido", "Dangerous")
}

data class CalendarUiState(
    val currentYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val subjects: List<Subject> = emptyList(),
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val events: List<EventItem> = emptyList(),
    val absenceRecords: List<AbsenceRecord> = emptyList(),
    val todaySummary: TodaySummary = TodaySummary(null, null, 0, emptyList(), emptyList()),
    val upcomingEvents: List<EventItem> = emptyList(),
    val riskSubjects: List<SubjectRiskInfo> = emptyList(),
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

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

            val todayDow = when (today.dayOfWeek) {
                DayOfWeek.MONDAY -> 1
                DayOfWeek.TUESDAY -> 2
                DayOfWeek.WEDNESDAY -> 3
                DayOfWeek.THURSDAY -> 4
                DayOfWeek.FRIDAY -> 5
                DayOfWeek.SATURDAY -> 6
                DayOfWeek.SUNDAY -> 7
            }
            val todaySchedules = scheduleItems.filter { it.dayOfWeek == todayDow }.sortedBy { it.startTime }
            val nowTime = LocalTime.now()
            val nextClass = todaySchedules.firstOrNull { 
                try { LocalTime.parse(it.startTime).isAfter(nowTime) } catch (_: Exception) { false }
            } ?: todaySchedules.firstOrNull()
            
            val nextSubject = subjects.find { it.id == nextClass?.subjectId }
            val todayEvents = events.filter { it.date.isEqual(today) }
            val overdueEvents = events.filter { !it.isCompleted && it.date.isBefore(today) }

            val next7DaysEnd = today.plusDays(7)
            val upcomingEvents = events.filter { !it.isCompleted && (it.date.isAfter(today) || it.date.isEqual(today)) && it.date.isBefore(next7DaysEnd) }
                .sortedBy { it.date }

            val riskList = subjects.map { sub ->
                val absCount = absences.count { it.subjectId == sub.id }
                val max = sub.maxAbsences.coerceAtLeast(1)
                val pct = (absCount.toFloat() / max.toFloat()) * 100f
                val status = when {
                    pct >= 100f -> RiskStatus.EXCEEDED
                    pct >= 85f -> RiskStatus.CRITICAL
                    pct >= 70f -> RiskStatus.WARNING
                    else -> RiskStatus.NORMAL
                }
                SubjectRiskInfo(sub, absCount, max, pct, status)
            }.filter { it.status != RiskStatus.NORMAL }

            _uiState.update {
                it.copy(
                    subjects = subjects,
                    scheduleItems = scheduleItems,
                    events = events,
                    absenceRecords = absences,
                    todaySummary = TodaySummary(
                        nextClassName = nextSubject?.name,
                        nextClassTime = nextClass?.let { "${it.startTime} - ${it.endTime}" },
                        remainingClassesCount = todaySchedules.size,
                        todayEvents = todayEvents,
                        overdueEvents = overdueEvents
                    ),
                    upcomingEvents = upcomingEvents,
                    riskSubjects = riskList,
                    isLoading = false
                )
            }
        }
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.plusMonths(1)) }
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentYearMonth = it.currentYearMonth.minusMonths(1)) }
    }

    fun goToToday() {
        _uiState.update { it.copy(currentYearMonth = YearMonth.now(), selectedDate = LocalDate.now()) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun setAbsenceForDate(subjectId: String, date: String, isAbsent: Boolean) {
        viewModelScope.launch {
            EamRepository.setAbsenceForDate(subjectId, date, isAbsent)
            loadData()
        }
    }

    fun toggleEventComplete(eventId: String) {
        viewModelScope.launch {
            EamRepository.toggleEventComplete(eventId)
            loadData()
        }
    }

    @SuppressLint("NewApi")
    fun addEvent(subjectId: String, title: String, description: String, date: LocalDate, time: LocalTime?, type: EventType) {
        viewModelScope.launch {
            EamRepository.addEvent(subjectId, title, description, date, time, type, false, 30)
            loadData()
        }
    }
}
