package com.pedro.eam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.eam.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun updateEvent(eventId: String, title: String, description: String, date: String, type: EventType) {
        viewModelScope.launch {
            EamRepository.updateEvent(eventId, title, description, date, type)
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
