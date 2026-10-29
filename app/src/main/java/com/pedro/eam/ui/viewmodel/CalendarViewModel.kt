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
import java.time.LocalTime

data class CalendarUiState(
    val subjects: List<Subject> = emptyList(),
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val events: List<EventItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CalendarViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    subjects = EamRepository.subjects,
                    scheduleItems = EamRepository.scheduleItems,
                    events = EamRepository.events,
                    isLoading = false
                )
            }
        }
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
    fun updateEvent(eventId: String, subjectId: String, title: String, description: String, dateStr: String, timeStr: String?, type: EventType) {
        viewModelScope.launch {
            val date = LocalDate.parse(dateStr)
            val time = if (!timeStr.isNullOrBlank()) LocalTime.parse(timeStr) else null
            EamRepository.updateEvent(eventId, subjectId, title, description, date, time, type, false, 30)
            loadData()
        }
    }

    fun removeEvent(eventId: String) {
        viewModelScope.launch {
            EamRepository.removeEvent(eventId)
            loadData()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            EamRepository.loadSampleData()
            loadData()
        }
    }
}
