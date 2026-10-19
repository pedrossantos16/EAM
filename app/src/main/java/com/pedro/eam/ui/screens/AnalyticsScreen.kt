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
import com.pedro.eam.data.EamRepository
import com.pedro.eam.data.EventType
import com.pedro.eam.ui.components.BarChart
import com.pedro.eam.ui.components.BarChartData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val subjects = EamRepository.subjects
    val events = EamRepository.events

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análises & Relatórios", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(padding.calculateTopPadding()))
                Text("Visão Analítica Completa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Relação detalhada de faltas, limites e distribuição de eventos acadêmicos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        BarChart(
                            title = "1. Faltas vs. Limite Máximo por Matéria",
                            subtitle = "Barras azuis/vermelhas = Faltas atuais (via calendário). Barras cinzas = Limite permitido.",
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
                            title = "2. Volume de Eventos por Matéria",
                            subtitle = "Quantidade de provas, trabalhos e compromissos cadastrados.",
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
                            title = "3. Distribuição de Eventos por Categoria",
                            subtitle = "Provas, Trabalhos, Apresentações e Leituras.",
                            items = eventsByTypeData
                        )
                    }
                }
                Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
            }
        }
    }
}
