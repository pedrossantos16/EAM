package com.pedro.eam.ui.viewmodel

import android.annotation.SuppressLint
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
import java.time.LocalDate
import java.time.LocalTime

data class EventsUiState(
    val events: List<EventItem> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectIdFilter: String? = null,
    val selectedTypeFilter: EventType? = null,
    val selectedStatusFilter: String? = null, // "ALL", "OVERDUE", "TODAY", "UPCOMING", "COMPLETED"
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

    fun setSubjectFilter(subjectId: String?) {
        _uiState.update { it.copy(selectedSubjectIdFilter = subjectId) }
    }

    fun setTypeFilter(type: EventType?) {
        _uiState.update { it.copy(selectedTypeFilter = type) }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    @SuppressLint("NewApi")
    fun validateAndSaveEvent(
        eventId: String?,
        subjectId: String,
        title: String,
        description: String,
        dateStr: String,
        timeStr: String?,
        type: EventType,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                onResult(false, "O título do evento não pode estar vazio.")
                return@launch
            }

            val date = try {
                LocalDate.parse(dateStr)
            } catch (_: Exception) {
                onResult(false, "Data inválida. Use o formato AAAA-MM-DD.")
                return@launch
            }

            val time = if (!timeStr.isNullOrBlank()) {
                try {
                    LocalTime.parse(timeStr)
                } catch (_: Exception) {
                    onResult(false, "Horário inválido. Use o formato HH:mm.")
                    return@launch
                }
            } else {
                null
            }

            if (subjectId.isBlank()) {
                onResult(false, "Selecione uma matéria válida.")
                return@launch
            }

            if (eventId == null) {
                EamRepository.addEvent(subjectId, title, description, date, time, type, false, 30)
            } else {
                EamRepository.updateEvent(eventId, subjectId, title, description, date, time, type, false, 30)
            }
            loadData()
            onResult(true, null)
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
