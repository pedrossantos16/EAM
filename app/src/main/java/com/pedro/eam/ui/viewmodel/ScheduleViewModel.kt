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
import java.time.LocalTime

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

    fun validateAndAddScheduleItem(
        subjectId: String,
        dayOfWeek: Int,
        startTimeStr: String,
        endTimeStr: String,
        room: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val start = LocalTime.parse(startTimeStr)
                val end = LocalTime.parse(endTimeStr)

                if (!end.isAfter(start)) {
                    onResult(false, "O horário de término deve ser posterior ao início.")
                    return@launch
                }

                if (EamRepository.checkScheduleConflict(subjectId, dayOfWeek, start, end)) {
                    onResult(false, "Conflito de horário! Já existe outra aula neste intervalo.")
                    return@launch
                }

                EamRepository.addScheduleItem(subjectId, dayOfWeek, startTimeStr, endTimeStr, room)
                loadData()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, "Formato de horário inválido. Use HH:mm.")
            }
        }
    }

    fun removeScheduleItem(itemId: String) {
        viewModelScope.launch {
            EamRepository.removeScheduleItem(itemId)
            loadData()
        }
    }
}
