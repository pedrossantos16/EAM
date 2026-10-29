package com.pedro.eam.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.pedro.eam.utils.BackupManager
import com.pedro.eam.utils.BackupSummary
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

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

    var selectedDateForAbsence by remember { mutableStateOf<String?>(null) }
    var selectedEventForDialog by remember { mutableStateOf<EventItem?>(null) }
    var isEditingEvent by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(EventType.PROVA) }

    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var restoreSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    val json = EamRepository.getBackupSnapshot().toString(2)
                    output.write(json.toByteArray())
                }
                Toast.makeText(context, "Backup exportado com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao exportar backup: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: ""
                val validationResult = BackupManager.validateAndGetSummary(jsonStr)
                if (validationResult.isSuccess) {
                    val (summary, jsonObj) = validationResult.getOrThrow()
                    restoreSummary = summary
                    pendingRestoreJson = jsonObj.toString()
                } else {
                    restoreError = validationResult.exceptionOrNull()?.localizedMessage ?: "Arquivo inválido."
                }
            } catch (e: Exception) {
                restoreError = "Erro ao ler arquivo: ${e.localizedMessage}"
            }
        }
    }

    val subjects = uiState.subjects
    val scheduleItems = uiState.scheduleItems
    val events = uiState.events

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
                title = { Text("Agenda & Visão Geral", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val fileName = "eam_backup_${SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())}.json"
                        exportLauncher.launch(fileName)
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = "Exportar Backup")
                    }
                    IconButton(onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Restaurar Backup")
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
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
                                Text("Controle de Faltas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = onNavigateToSchedule) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Ver Grade", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
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
                                Text("Calendário de Eventos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                                val dayEvents = events.filter { it.date.toString() == dateStr }
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

    if (restoreSummary != null) {
        val summary = restoreSummary!!
        AlertDialog(
            onDismissRequest = { restoreSummary = null; pendingRestoreJson = null },
            title = { Text("Resumo do Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Data: ${summary.exportedAt}")
                    Text("Versão do App: ${summary.appVersion}")
                    Text("Cursos: ${summary.coursesCount}")
                    Text("Matérias: ${summary.subjectsCount}")
                    Text("Horários: ${summary.scheduleCount}")
                    Text("Faltas: ${summary.absencesCount}")
                    Text("Eventos: ${summary.eventsCount}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Deseja substituir todos os dados atuais por este backup? Esta ação não pode ser desfeita.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val jsonStr = pendingRestoreJson
                        if (jsonStr != null) {
                            try {
                                val jsonObj = JSONObject(jsonStr)
                                val success = EamRepository.restoreFromSnapshot(jsonObj)
                                if (success) {
                                    viewModel.loadData()
                                    Toast.makeText(context, "Restaurado com sucesso!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Falha na restauração transacional.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        restoreSummary = null
                        pendingRestoreJson = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restaurar Dados")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreSummary = null; pendingRestoreJson = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (restoreError != null) {
        AlertDialog(
            onDismissRequest = { restoreError = null },
            title = { Text("Erro no Backup") },
            text = { Text(restoreError!!) },
            confirmButton = {
                Button(onClick = { restoreError = null }) {
                    Text("OK")
                }
            }
        )
    }

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
                        Text("Nenhuma aula cadastrada.", style = MaterialTheme.typography.bodyMedium)
                    } else {
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
                                    Text("⏰ ${sch.startTime} - ${sch.endTime}", style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(
                                    checked = isPresent,
                                    onCheckedChange = { checked ->
                                        isPresent = checked
                                        viewModel.setAbsenceForDate(sch.subjectId, dateStr, !checked)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedDateForAbsence = null }) {
                    Text("OK")
                }
            }
        )
    }

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
                    Text("Data: ${ev.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
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
                        Text(
                            text = if (ev.description.isBlank()) "Sem descrição." else ev.description,
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
                                viewModel.toggleEventComplete(ev.id)
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
                                viewModel.updateEvent(ev.id, ev.subjectId, editTitle, editDesc, ev.date.toString(), ev.time?.toString(), ev.type)
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
                                    viewModel.removeEvent(ev.id)
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
