package com.pedro.eam.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedro.eam.data.EamRepository
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToEvents: () -> Unit,
    onNavigateToSubjects: () -> Unit
) {
    var expandedEventId by remember { mutableStateOf<String?>(null) }
    var selectedDateFilter by remember { mutableStateOf<String?>(null) }

    val subjects = EamRepository.subjects
    val events = EamRepository.events

    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
    
    val filteredEvents = if (selectedDateFilter != null) {
        events.filter { it.date == selectedDateFilter }
    } else {
        events.sortedBy { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda & Visão Geral", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { selectedDateFilter = null }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Limpar Filtro de Data")
                    }
                },
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Resumo Rápido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${subjects.size} Matérias cadastradas • ${events.size} Eventos na agenda", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onNavigateToSubjects,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ver Faltas")
                            }
                            OutlinedButton(
                                onClick = onNavigateToEvents,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Novo Evento")
                            }
                        }
                    }
                }
            }

            // Absence Warning Section
            item {
                Text("Alerta de Faltas por Matéria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(subjects) { subject ->
                val ratio = if (subject.maxAbsences > 0) subject.absences.toFloat() / subject.maxAbsences.toFloat() else 0f
                val isNearLimit = ratio >= 0.7f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNearLimit) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Faltas: ${subject.absences} / ${subject.maxAbsences} (Sala: ${subject.room})", style = MaterialTheme.typography.bodySmall)
                            if (isNearLimit) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Próximo do limite de faltas!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = { EamRepository.updateAbsences(subject.id, -1) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Menos Falta", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { EamRepository.updateAbsences(subject.id, 1) }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Mais Falta", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Events Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDateFilter != null) "Eventos em $selectedDateFilter" else "Próximos Eventos & Provas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDateFilter != null) {
                        TextButton(onClick = { selectedDateFilter = null }) {
                            Text("Mostrar Todos")
                        }
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum evento cadastrado para esta data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredEvents) { event ->
                    val subject = subjects.find { it.id == event.subjectId }
                    val isExpanded = expandedEventId == event.id
                    val isTodayOrSoon = event.date >= todayStr

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedEventId = if (isExpanded) null else event.id
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (event.isCompleted) Color.Gray else MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = event.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (event.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${subject?.name ?: "Geral"} • ${event.type.label} • 📅 ${event.date}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { EamRepository.toggleEventComplete(event.id) }) {
                                        Icon(
                                            imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Concluir Evento",
                                            tint = if (event.isCompleted) Color.Green else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expandir Descrição",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isTodayOrSoon && !event.isCompleted) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aviso: Evento próximo!", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Descrição do Evento:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (event.description.isBlank()) "Nenhuma descrição informada." else event.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
