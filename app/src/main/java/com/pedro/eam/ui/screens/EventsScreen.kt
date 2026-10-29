package com.pedro.eam.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedro.eam.data.EventItem
import com.pedro.eam.data.EventType
import com.pedro.eam.ui.viewmodel.EventsViewModel
import java.time.LocalDate

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: EventsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventItem?>(null) }
    var expandedEventId by remember { mutableStateOf<String?>(null) }

    // Form fields
    var formTitle by remember { mutableStateOf("") }
    var formDesc by remember { mutableStateOf("") }
    var formDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var formTime by remember { mutableStateOf("") }
    var formSubjectId by remember { mutableStateOf("") }
    var formType by remember { mutableStateOf(EventType.PROVA) }

    // Filters
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, OVERDUE, TODAY, UPCOMING, COMPLETED

    val subjects = uiState.subjects
    val today = LocalDate.now()

    // Filter and sort events
    val filteredEvents = uiState.events.filter { ev ->
        when (statusFilter) {
            "OVERDUE" -> !ev.isCompleted && ev.date.isBefore(today)
            "TODAY" -> !ev.isCompleted && ev.date.isEqual(today)
            "UPCOMING" -> !ev.isCompleted && ev.date.isAfter(today)
            "COMPLETED" -> ev.isCompleted
            else -> true
        }
    }.sortedWith(compareBy<EventItem> { it.date }.thenBy { it.time ?: java.time.LocalTime.MAX })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Eventos & Provas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            // Filter Chips
            ScrollableTabRow(
                selectedTabIndex = when (statusFilter) {
                    "OVERDUE" -> 1
                    "TODAY" -> 2
                    "UPCOMING" -> 3
                    "COMPLETED" -> 4
                    else -> 0
                },
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = statusFilter == "ALL", onClick = { statusFilter = "ALL" }) { Text("Todos", modifier = Modifier.padding(12.dp)) }
                Tab(selected = statusFilter == "TODAY", onClick = { statusFilter = "TODAY" }) { Text("Hoje", modifier = Modifier.padding(12.dp)) }
                Tab(selected = statusFilter == "UPCOMING", onClick = { statusFilter = "UPCOMING" }) { Text("Próximos", modifier = Modifier.padding(12.dp)) }
                Tab(selected = statusFilter == "OVERDUE", onClick = { statusFilter = "OVERDUE" }) { Text("Vencidos", modifier = Modifier.padding(12.dp)) }
                Tab(selected = statusFilter == "COMPLETED", onClick = { statusFilter = "COMPLETED" }) { Text("Concluídos", modifier = Modifier.padding(12.dp)) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredEvents.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum evento encontrado para este filtro.")
                        }
                    }
                }

                items(filteredEvents) { event ->
                    val subject = subjects.find { it.id == event.subjectId }
                    val isExpanded = expandedEventId == event.id
                    val isOverdue = !event.isCompleted && event.date.isBefore(today)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedEventId = if (isExpanded) null else event.id },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                event.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    IconButton(onClick = { viewModel.toggleEventComplete(event.id) }) {
                                        Icon(
                                            imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Concluir/Reabrir",
                                            tint = if (event.isCompleted) Color.Green else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = event.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (event.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${subject?.name ?: "Geral"} • ${event.type.label} • 📅 ${event.date} ${if (event.time != null) "⏰ ${event.time}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOverDoubt(isOverdue, event.isCompleted)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        editingEvent = event
                                        formTitle = event.title
                                        formDesc = event.description
                                        formDate = event.date.toString()
                                        formTime = event.time?.toString() ?: ""
                                        formSubjectId = event.subjectId
                                        formType = event.type
                                        showEventDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.removeEvent(event.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (event.description.isBlank()) "Sem descrição." else event.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingEvent = null
                formTitle = ""
                formDesc = ""
                formDate = LocalDate.now().toString()
                formTime = ""
                formSubjectId = subjects.firstOrNull()?.id ?: ""
                formType = EventType.PROVA
                showEventDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Evento")
        }
    }

    if (showEventDialog) {
        AlertDialog(
            onDismissRequest = { showEventDialog = false },
            title = { Text(if (editingEvent == null) "Novo Evento" else "Editar Evento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subjects.isNotEmpty()) {
                        var expandedSub by remember { mutableStateOf(false) }
                        val curSub = subjects.find { it.id == formSubjectId } ?: subjects.first()
                        OutlinedButton(onClick = { expandedSub = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Matéria: ${curSub.name}")
                        }
                        DropdownMenu(expanded = expandedSub, onDismissRequest = { expandedSub = false }) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        formSubjectId = s.id
                                        expandedSub = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Título *") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formDesc,
                        onValueChange = { formDesc = it },
                        label = { Text("Descrição") }
                    )

                    OutlinedTextField(
                        value = formDate,
                        onValueChange = { formDate = it },
                        label = { Text("Data (AAAA-MM-DD)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formTime,
                        onValueChange = { formTime = it },
                        label = { Text("Horário opcional (HH:mm)") },
                        singleLine = true
                    )

                    var expandedType by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tipo: ${formType.label}")
                    }
                    DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        EventType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    formType = t
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val subId = if (formSubjectId.isNotBlank()) formSubjectId else subjects.firstOrNull()?.id ?: ""
                    viewModel.validateAndSaveEvent(
                        eventId = editingEvent?.id,
                        subjectId = subId,
                        title = formTitle,
                        description = formDesc,
                        dateStr = formDate,
                        timeStr = formTime,
                        type = formType
                    ) { success, errorMsg ->
                        if (success) {
                            showEventDialog = false
                            editingEvent = null
                        } else {
                            Toast.makeText(context, errorMsg ?: "Erro", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEventDialog = false; editingEvent = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun isOverDoubt(isOverdue: Boolean, isCompleted: Boolean): Boolean {
    return isOverdue && !isCompleted
}
