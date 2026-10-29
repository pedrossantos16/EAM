package com.pedro.eam

import com.pedro.eam.ui.viewmodel.CoursesViewModel
import com.pedro.eam.ui.viewmodel.EventsViewModel
import com.pedro.eam.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCoursesViewModelInitialization() {
        val viewModel = CoursesViewModel()
        assertNotNull(viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun testScheduleViewModelInitialization() {
        val viewModel = ScheduleViewModel()
        assertNotNull(viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun testEventsViewModelInitialization() {
        val viewModel = EventsViewModel()
        assertNotNull(viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
