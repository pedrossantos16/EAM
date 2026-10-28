package com.pedro.eam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedro.eam.data.EamRepository
import com.pedro.eam.data.EventItem
import com.pedro.eam.data.EventType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToSchedule: () -> Unit,
    onNavigateToEvents: () -> Unit
) {
    var selectedDateForAbsence by remember { mutableStateOf<String?>(null) }
    var selectedEventForDialog by remember { mutableStateOf<EventItem?>(null) }
    var isEditingEvent by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(EventType.PROVA) }

    val subjects = EamRepository.subjects
    val scheduleItems = EamRepository.scheduleItems
    val events = EamRepository.events

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pt-BR"))
    val currentMonthName = monthFormat.format(calendar.time).replaceFirstChar { it.uppercase() }

    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Visão Geral & Calendário", fontWeight = FontWeight.Bold) },
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Absence Management Calendar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Registro de Faltas por Dia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Clique em um dia para marcar/desmarcar faltas nas aulas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = onNavigateToSchedule) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Ver Grade", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(currentMonthName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                                    Text(day, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val totalCells = daysInMonth + firstDayOfWeek
                            val rows = (totalCells + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (row in 0 until rows) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                        for (col in 0 until 7) {
                                            val dayIndex = row * 7 + col
                                            val dayNumber = dayIndex - firstDayOfWeek + 1

                                            if (dayNumber in 1..daysInMonth) {
                                                val calDay = Calendar.getInstance().apply {
                                                    set(currentYear, currentMonth, dayNumber)
                                                }
                                                val dateStr = dateFormat.format(calDay.time)
                                                val dow = calDay.get(Calendar.DAY_OF_WEEK)
                                                val mappedDow = if (dow == Calendar.SUNDAY) 7 else dow - 1
                                                val hasClasses = scheduleItems.any { it.dayOfWeek == mappedDow }
                                                val hasAbsence = subjects.any { EamRepository.isAbsentOnDate(it.id, dateStr) }

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                hasAbsence -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                                                hasClasses -> MaterialTheme.colorScheme.primaryContainer
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable {
                                                            selectedDateForAbsence = dateStr
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayNumber.toString(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (hasClasses) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (hasAbsence) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(38.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Events Calendar & Interaction
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Calendário de Eventos & Provas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Dias com eventos destacados. Clique no dia para ver detalhes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = onNavigateToEvents) {
                                    Icon(Icons.Default.Add, contentDescription = "Adicionar Evento", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val totalCells = daysInMonth + firstDayOfWeek
                            val rows = (totalCells + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (row in 0 until rows) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                        for (col in 0 until 7) {
                                            val dayIndex = row * 7 + col
                                            val dayNumber = dayIndex - firstDayOfWeek + 1

                                            if (dayNumber in 1..daysInMonth) {
                                                val calDay = Calendar.getInstance().apply {
                                                    set(currentYear, currentMonth, dayNumber)
                                                }
                                                val dateStr = dateFormat.format(calDay.time)
                                                val dayEvents = events.filter { it.date == dateStr }
                                                val hasEvent = dayEvents.isNotEmpty()
                                                val allCompleted = hasEvent && dayEvents.all { it.isCompleted }

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                allCompleted -> Color(0xFF81C784).copy(alpha = 0.5f)
                                                                hasEvent -> MaterialTheme.colorScheme.tertiaryContainer
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable(enabled = hasEvent) {
                                                            if (hasEvent) {
                                                                selectedEventForDialog = dayEvents.first()
                                                                isEditingEvent = false
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = dayNumber.toString(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (hasEvent) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (hasEvent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (hasEvent) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (allCompleted) Color.Green else MaterialTheme.colorScheme.tertiary)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(38.dp))
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
    }

    // Dialog 1: Absence Registration for Selected Date
    if (selectedDateForAbsence != null) {
        val dateStr = selectedDateForAbsence!!
        val cal = Calendar.getInstance()
        try {
            dateFormat.parse(dateStr)?.let { cal.time = it }
        } catch (_: Exception) {}
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val mappedDow = if (dow == Calendar.SUNDAY) 7 else dow - 1
        val dayClasses = scheduleItems.filter { it.dayOfWeek == mappedDow }

        AlertDialog(
            onDismissRequest = { selectedDateForAbsence = null },
            title = { Text("Aulas em $dateStr") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dayClasses.isEmpty()) {
                        Text("Nenhuma aula cadastrada na grade para este dia da semana.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Desmarque o item caso tenha faltado à aula:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        dayClasses.forEach { sch ->
                            val sub = subjects.find { it.id == sch.subjectId }
                            val subName = sub?.name ?: "Matéria"
                            var isPresent by remember(dateStr, sch.id) {
                                mutableStateOf(!EamRepository.isAbsentOnDate(sch.subjectId, dateStr))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("⏰ ${sch.startTime} - ${sch.endTime} • Sala: ${sch.room.ifBlank { sub?.room ?: "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(
                                    checked = isPresent,
                                    onCheckedChange = { checked ->
                                        isPresent = checked
                                        EamRepository.setAbsenceForDate(sch.subjectId, dateStr, !checked)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedDateForAbsence = null }) {
                    Text("Concluir")
                }
            }
        )
    }

    // Dialog 2: Event Details / Edit / Delete / Finish
    if (selectedEventForDialog != null) {
        val ev = selectedEventForDialog!!
        val sub = subjects.find { it.id == ev.subjectId }

        AlertDialog(
            onDismissRequest = { selectedEventForDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ev.isCompleted) "✅ [Concluído] ${ev.title}" else ev.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Matéria: ${sub?.name ?: "Geral"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Tipo: ${ev.type.label} • Data: ${ev.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    if (isEditingEvent) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Título") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editDesc,
                            onValueChange = { editDesc = it },
                            label = { Text("Descrição") }
                        )
                    } else {
                        Text("Descrição do Evento:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (ev.description.isBlank()) "Nenhuma descrição informada." else ev.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!ev.isCompleted && !isEditingEvent) {
                        Button(
                            onClick = {
                                EamRepository.toggleEventComplete(ev.id)
                                selectedEventForDialog = events.find { it.id == ev.id }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Finalizar")
                        }
                    }

                    if (!ev.isCompleted) {
                        if (isEditingEvent) {
                            Button(onClick = {
                                EamRepository.updateEvent(ev.id, editTitle, editDesc, ev.date, editType)
                                selectedEventForDialog = events.find { it.id == ev.id }
                                isEditingEvent = false
                            }) {
                                Text("Salvar")
                            }
                        } else {
                            OutlinedButton(onClick = {
                                isEditingEvent = true
                                editTitle = ev.title
                                editDesc = ev.description
                                editType = ev.type
                            }) {
                                Text("Editar")
                            }
                            OutlinedButton(
                                onClick = {
                                    EamRepository.removeEvent(ev.id)
                                    selectedEventForDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text("Excluir", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Button(onClick = { selectedEventForDialog = null }) {
                        Text("Fechar")
                    }
                }
            }
        )
    }
}
