package com.pedro.eam.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedro.eam.data.EamRepository
import com.pedro.eam.data.EventItem
import com.pedro.eam.data.EventType
import com.pedro.eam.ui.viewmodel.CalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: CalendarViewModel = viewModel(),
    onNavigateToSchedule: () -> Unit,
    onNavigateToEvents: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateEventDialog by remember { mutableStateOf(false) }
    var eventTitle by remember { mutableStateOf("") }
    var eventDesc by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf(EventType.PROVA) }
    var eventSubjectId by remember { mutableStateOf("") }

    val currentYearMonth = uiState.currentYearMonth
    val selectedDate = uiState.selectedDate
    val subjects = uiState.subjects
    val scheduleItems = uiState.scheduleItems
    val events = uiState.events

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"))
    val currentMonthName = monthFormatter.format(currentYearMonth).replaceFirstChar { it.uppercase() }

    val firstDayOfMonth = currentYearMonth.atDay(1)
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value % 7 // Sun = 0, Mon = 1...

    val today = LocalDate.now()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Agenda Acadêmica", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.goToToday() }) {
                        Icon(Icons.Default.Today, contentDescription = "Hoje")
                    }
                },
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
                // 1. Summary Blocks (Hoje, Próximos 7 dias, Frequência em Risco)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // HOJE
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📌 O que tenho hoje?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (uiState.todaySummary.nextClassName != null) {
                                    Text("Próxima aula: ${uiState.todaySummary.nextClassName} (${uiState.todaySummary.nextClassTime})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Nenhuma aula restante hoje.", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("Aulas restantes hoje: ${uiState.todaySummary.remainingClassesCount}", style = MaterialTheme.typography.bodySmall)
                                if (uiState.todaySummary.todayEvents.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Eventos de hoje: ${uiState.todaySummary.todayEvents.joinToString { it.title }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                                if (uiState.todaySummary.overdueEvents.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("⚠️ Tarefas vencidas: ${uiState.todaySummary.overdueEvents.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // PRÓXIMOS 7 DIAS
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("⏳ O que está chegando (Próximos 7 dias)?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (uiState.upcomingEvents.isEmpty()) {
                                    Text("Nenhum prazo nos próximos 7 dias.", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    uiState.upcomingEvents.take(3).forEach { ev ->
                                        Text("• ${ev.date}: ${ev.title} (${ev.type.label})", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // FREQUÊNCIA EM RISCO
                        if (uiState.riskSubjects.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("⚠️ Como está minha frequência?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    uiState.riskSubjects.forEach { risk ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(risk.subject.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("${risk.absences}/${risk.maxAbsences} (${risk.percentage.toInt()}% - ${risk.status.label})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Unified Monthly Calendar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.previousMonth() }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior")
                                }
                                Text(currentMonthName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.nextMonth() }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                                    Text(day, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val totalCells = daysInMonth + firstDayOfWeekIndex
                            val rows = (totalCells + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (row in 0 until rows) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                        for (col in 0 until 7) {
                                            val dayIndex = row * 7 + col
                                            val dayNumber = dayIndex - firstDayOfWeekIndex + 1

                                            if (dayNumber in 1..daysInMonth) {
                                                val dayDate = currentYearMonth.atDay(dayNumber)
                                                val isSelected = dayDate.isEqual(selectedDate)
                                                val isToday = dayDate.isEqual(today)

                                                val dayEvents = events.filter { it.date.isEqual(dayDate) }
                                                val hasEvent = dayEvents.isNotEmpty()
                                                val hasAbsence = subjects.any { EamRepository.isAbsentOnDate(it.id, dayDate.toString()) }

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                hasAbsence -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable {
                                                            viewModel.selectDate(dayDate)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = dayNumber.toString(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = when {
                                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                                hasAbsence -> MaterialTheme.colorScheme.error
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                        if (hasEvent) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
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

                // 3. Selected Day Details (All items for selected date)
                item {
                    val dow = when (selectedDate.dayOfWeek) {
                        DayOfWeek.MONDAY -> 1
                        DayOfWeek.TUESDAY -> 2
                        DayOfWeek.WEDNESDAY -> 3
                        DayOfWeek.THURSDAY -> 4
                        DayOfWeek.FRIDAY -> 5
                        DayOfWeek.SATURDAY -> 6
                        DayOfWeek.SUNDAY -> 7
                    }
                    val dayClasses = scheduleItems.filter { it.dayOfWeek == dow }
                    val dayEvents = events.filter { it.date.isEqual(selectedDate) }

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
                                Text("Detalhes do dia: $selectedDate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Button(onClick = {
                                    eventSubjectId = subjects.firstOrNull()?.id ?: ""
                                    eventTitle = ""
                                    eventDesc = ""
                                    showCreateEventDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Novo Evento")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Aulas e Frequência:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (dayClasses.isEmpty()) {
                                Text("Nenhuma aula cadastrada para este dia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                dayClasses.forEach { sch ->
                                    val sub = subjects.find { it.id == sch.subjectId }
                                    var isPresent by remember(selectedDate, sch.id) {
                                        mutableStateOf(!EamRepository.isAbsentOnDate(sch.subjectId, selectedDate.toString()))
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${sch.startTime}–${sch.endTime} — ${sub?.name ?: "Matéria"}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Sala: ${sch.room.ifBlank { sub?.room ?: "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Checkbox(
                                            checked = isPresent,
                                            onCheckedChange = { checked ->
                                                isPresent = checked
                                                viewModel.setAbsenceForDate(sch.subjectId, selectedDate.toString(), !checked)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Eventos e Prazos:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (dayEvents.isEmpty()) {
                                Text("Nenhum evento para esta data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                dayEvents.forEach { ev ->
                                    val sub = subjects.find { it.id == ev.subjectId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(if (ev.isCompleted) "✅ ${ev.title}" else ev.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("${sub?.name ?: "Geral"} • ${ev.type.label}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { viewModel.toggleEventComplete(ev.id) }) {
                                            Icon(
                                                imageVector = if (ev.isCompleted) Icons.Default.Refresh else Icons.Default.CheckCircle,
                                                contentDescription = "Concluir/Reabrir",
                                                tint = if (ev.isCompleted) Color.Gray else Color.Green
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
    }

    if (showCreateEventDialog) {
        AlertDialog(
            onDismissRequest = { showCreateEventDialog = false },
            title = { Text("Novo Evento em $selectedDate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subjects.isNotEmpty()) {
                        var expandedSub by remember { mutableStateOf(false) }
                        val curSub = subjects.find { it.id == eventSubjectId } ?: subjects.first()
                        OutlinedButton(onClick = { expandedSub = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Matéria: ${curSub.name}")
                        }
                        DropdownMenu(expanded = expandedSub, onDismissRequest = { expandedSub = false }) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        eventSubjectId = s.id
                                        expandedSub = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Título *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = eventDesc,
                        onValueChange = { eventDesc = it },
                        label = { Text("Descrição") }
                    )

                    var expandedType by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tipo: ${eventType.label}")
                    }
                    DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        EventType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    eventType = t
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (eventTitle.isNotBlank() && subjects.isNotEmpty()) {
                        val subId = if (eventSubjectId.isNotBlank()) eventSubjectId else subjects.first().id
                        viewModel.addEvent(subId, eventTitle, eventDesc, selectedDate, null, eventType)
                        showCreateEventDialog = false
                    } else {
                        Toast.makeText(context, "Preencha o título.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateEventDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
