package com.pedro.eam.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedro.eam.data.EamRepository
import com.pedro.eam.data.EventType
import com.pedro.eam.data.getFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    var showAddEventDialog by remember { mutableStateOf(false) }
    var expandedEventId by remember { mutableStateOf<String?>(null) }

    var selectedSubjectId by remember { mutableStateOf(EamRepository.subjects.firstOrNull()?.id ?: "") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getFormattedDate(0)) }
    var eventType by remember { mutableStateOf(EventType.PROVA) }

    val subjects = EamRepository.subjects
    val events = EamRepository.events

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciamento de Eventos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Evento")
            }
        }
    ) { padding ->
        val combinedBottom = padding.calculateBottomPadding() + contentPadding.calculateBottomPadding()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = combinedBottom + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Provas, Trabalhos e Compromissos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Clique em qualquer evento para visualizar sua descrição detalhada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (events.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum evento cadastrado.")
                    }
                }
            }

            items(events) { event ->
                val subject = subjects.find { it.id == event.subjectId }
                val isExpanded = expandedEventId == event.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedEventId = if (isExpanded) null else event.id },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (event.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { EamRepository.toggleEventComplete(event.id) }) {
                                    Icon(
                                        imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Concluir",
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
                                        text = "${subject?.name ?: "Geral"} • Tipo: ${event.type.label} • Data: ${event.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { EamRepository.removeEvent(event.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expandir",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Descrição Definida pelo Usuário:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (event.description.isBlank()) "Nenhuma descrição informada para este evento." else event.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Cadastrar Novo Evento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subjects.isNotEmpty()) {
                        var expandedSubMenu by remember { mutableStateOf(false) }
                        val currentSub = subjects.find { it.id == selectedSubjectId } ?: subjects.first()

                        OutlinedButton(onClick = { expandedSubMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Matéria: ${currentSub.name}")
                        }
                        DropdownMenu(expanded = expandedSubMenu, onDismissRequest = { expandedSubMenu = false }) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        selectedSubjectId = s.id
                                        expandedSubMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do Evento (ex: Prova P2)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição detalhada (visível ao clicar)") }
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Data (AAAA-MM-DD)") },
                        singleLine = true
                    )

                    var expandedTypeMenu by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { expandedTypeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tipo: ${eventType.label}")
                    }
                    DropdownMenu(expanded = expandedTypeMenu, onDismissRequest = { expandedTypeMenu = false }) {
                        EventType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    eventType = t
                                    expandedTypeMenu = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && subjects.isNotEmpty()) {
                        val subId = if (selectedSubjectId.isNotBlank()) selectedSubjectId else subjects.first().id
                        EamRepository.addEvent(subId, title, description, date, eventType)
                        title = ""
                        description = ""
                        showAddEventDialog = false
                    }
                }) {
                    Text("Salvar Evento")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
