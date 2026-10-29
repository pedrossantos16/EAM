package com.pedro.eam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedro.eam.ui.viewmodel.ScheduleViewModel

val daysOfWeekMap = mapOf(
    1 to "Segunda-feira",
    2 to "Terça-feira",
    3 to "Quarta-feira",
    4 to "Quinta-feira",
    5 to "Sexta-feira",
    6 to "Sábado",
    7 to "Domingo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSubjectId by remember { mutableStateOf(uiState.subjects.firstOrNull()?.id ?: "") }
    var selectedDayOfWeek by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:40") }
    var room by remember { mutableStateOf("") }

    val subjects = uiState.subjects
    val scheduleItems = uiState.scheduleItems

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Grade Curricular & Horários", fontWeight = FontWeight.Bold) },
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (scheduleItems.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum horário cadastrado.")
                        }
                    }
                }

                val grouped = scheduleItems.groupBy { it.dayOfWeek }

                daysOfWeekMap.forEach { (dayNum, dayName) ->
                    val dayItems = grouped[dayNum] ?: emptyList()
                    if (dayItems.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(dayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    dayItems.sortedBy { it.startTime }.forEach { item ->
                                        val subject = subjects.find { it.id == item.subjectId }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(subject?.name ?: "Matéria desconhecida", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                Text("⏰ ${item.startTime} - ${item.endTime} • Sala: ${item.room.ifBlank { subject?.room ?: "N/A" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = { viewModel.removeScheduleItem(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Horário")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar Horário") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subjects.isNotEmpty()) {
                        var expandedSub by remember { mutableStateOf(false) }
                        val curSub = subjects.find { it.id == selectedSubjectId } ?: subjects.first()
                        OutlinedButton(onClick = { expandedSub = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Matéria: ${curSub.name}")
                        }
                        DropdownMenu(expanded = expandedSub, onDismissRequest = { expandedSub = false }) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        selectedSubjectId = s.id
                                        expandedSub = false
                                    }
                                )
                            }
                        }
                    }

                    var expandedDay by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { expandedDay = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Dia: ${daysOfWeekMap[selectedDayOfWeek]}")
                    }
                    DropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                        daysOfWeekMap.forEach { (num, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedDayOfWeek = num
                                    expandedDay = false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Início (ex: 08:00)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Fim (ex: 09:40)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Sala (opcional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (subjects.isNotEmpty()) {
                        val subId = if (selectedSubjectId.isNotBlank()) selectedSubjectId else subjects.first().id
                        viewModel.addScheduleItem(subId, selectedDayOfWeek, startTime, endTime, room)
                        showAddDialog = false
                    }
                }) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
