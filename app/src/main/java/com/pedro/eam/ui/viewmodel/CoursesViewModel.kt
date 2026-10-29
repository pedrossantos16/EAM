package com.pedro.eam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.eam.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoursesUiState(
    val courses: List<Course> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CoursesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CoursesUiState(isLoading = true))
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                _uiState.update {
                    it.copy(
                        courses = EamRepository.courses,
                        subjects = EamRepository.subjects,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun addCourse(name: String, description: String) {
        viewModelScope.launch {
            EamRepository.addCourse(name, description)
            loadData()
        }
    }

    fun removeCourse(courseId: String) {
        viewModelScope.launch {
            EamRepository.removeCourse(courseId)
            loadData()
        }
    }

    fun addSubject(courseId: String, name: String, professor: String, maxAbsences: Int, room: String) {
        viewModelScope.launch {
            EamRepository.addSubject(courseId, name, professor, maxAbsences, room)
            loadData()
        }
    }

    fun removeSubject(subjectId: String) {
        viewModelScope.launch {
            EamRepository.removeSubject(subjectId)
            loadData()
        }
    }

    fun getCourseImpact(courseId: String): CourseImpact {
        return EamRepository.getCourseImpact(courseId)
    }

    fun getSubjectImpact(subjectId: String): SubjectImpact {
        return EamRepository.getSubjectImpact(subjectId)
    }
}
