package com.pedro.eam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.eam.data.EamRepository
import com.pedro.eam.data.ScheduleItem
import com.pedro.eam.data.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ScheduleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState(isLoading = true))
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scheduleItems = EamRepository.scheduleItems,
                    subjects = EamRepository.subjects,
                    isLoading = false
                )
            }
        }
    }

    fun addScheduleItem(subjectId: String, dayOfWeek: Int, startTime: String, endTime: String, room: String) {
        viewModelScope.launch {
            EamRepository.addScheduleItem(subjectId, dayOfWeek, startTime, endTime, room)
            loadData()
        }
    }

    fun removeScheduleItem(itemId: String) {
        viewModelScope.launch {
            EamRepository.removeScheduleItem(itemId)
            loadData()
        }
    }
}
