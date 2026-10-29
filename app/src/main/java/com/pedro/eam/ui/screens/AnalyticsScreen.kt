package com.pedro.eam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedro.eam.data.EamRepository
import com.pedro.eam.data.EventType
import com.pedro.eam.ui.components.BarChart
import com.pedro.eam.ui.components.BarChartData
import com.pedro.eam.ui.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AnalyticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subjects = uiState.subjects
    val events = uiState.events

    val absencesChartData = subjects.map { sub ->
        val absCount = EamRepository.getAbsenceCount(sub.id)
        BarChartData(
            label = sub.name,
            value = absCount.toFloat(),
            maxValue = sub.maxAbsences.toFloat().coerceAtLeast(5f),
            color = if (absCount >= sub.maxAbsences * 0.7f) Color(0xFFE57373) else Color(0xFF64B5F6),
            secondaryValue = sub.maxAbsences.toFloat(),
            secondaryColor = Color.Gray
        )
    }

    val eventsPerSubjectData = subjects.map { sub ->
        val count = events.count { it.subjectId == sub.id }.toFloat()
        BarChartData(
            label = sub.name,
            value = count,
            maxValue = 10f,
            color = Color(0xFF81C784)
        )
    }

    val eventsByTypeData = EventType.entries.map { type ->
        val count = events.count { it.type == type }.toFloat()
        BarChartData(
            label = type.label,
            value = count,
            maxValue = 10f,
            color = Color(0xFFFFB74D)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Análises & Gráficos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            BarChart(
                                title = "Faltas por Matéria",
                                subtitle = "",
                                items = absencesChartData
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            BarChart(
                                title = "Eventos por Matéria",
                                subtitle = "",
                                items = eventsPerSubjectData
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            BarChart(
                                title = "Eventos por Categoria",
                                subtitle = "",
                                items = eventsByTypeData
                            )
                        }
                    }
                }
            }
        }
    }
}
