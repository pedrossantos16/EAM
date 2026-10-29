package com.pedro.eam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.eam.data.EamRepository
import com.pedro.eam.data.EventItem
import com.pedro.eam.data.EventType
import com.pedro.eam.data.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventsUiState(
    val events: List<EventItem> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class EventsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EventsUiState(isLoading = true))
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    events = EamRepository.events,
                    subjects = EamRepository.subjects,
                    isLoading = false
                )
            }
        }
    }

    fun addEvent(subjectId: String, title: String, description: String, date: String, type: EventType) {
        viewModelScope.launch {
            EamRepository.addEvent(subjectId, title, description, date, type)
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

    fun toggleEventComplete(eventId: String) {
        viewModelScope.launch {
            EamRepository.toggleEventComplete(eventId)
            loadData()
        }
    }
}
