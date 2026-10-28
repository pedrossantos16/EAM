package com.pedro.eam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedro.eam.data.EamRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesSubjectsScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    var newCourseName by remember { mutableStateOf("") }
    var newCourseDesc by remember { mutableStateOf("") }

    var selectedCourseIdForSubject by remember { mutableStateOf(EamRepository.courses.firstOrNull()?.id ?: "") }
    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectProf by remember { mutableStateOf("") }
    var newSubjectRoom by remember { mutableStateOf("") }
    var newSubjectMaxAbs by remember { mutableStateOf("15") }

    val courses = EamRepository.courses
    val subjects = EamRepository.subjects

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Cursos & Matérias", fontWeight = FontWeight.Bold) },
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
                item {
                    Text("Seus Cursos e Matérias Vinculadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Gerencie as matérias. As faltas são registradas no calendário da Visão Geral.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (courses.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum curso cadastrado. Clique em '+' para adicionar.")
                        }
                    }
                }

                items(courses) { course ->
                    val courseSubjects = subjects.filter { it.courseId == course.id }

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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(course.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        if (course.description.isNotBlank()) {
                                            Text(course.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                IconButton(onClick = { EamRepository.removeCourse(course.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir Curso", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Matérias (${courseSubjects.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            if (courseSubjects.isEmpty()) {
                                Text("Nenhuma matéria neste curso.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                courseSubjects.forEach { sub ->
                                    val absCount = EamRepository.getAbsenceCount(sub.id)
                                    val ratio = if (sub.maxAbsences > 0) absCount.toFloat() / sub.maxAbsences.toFloat() else 0f
                                    val isOverOrNear = ratio >= 0.7f

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isOverOrNear) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sub.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Prof: ${sub.professor.ifBlank { "Não informado" }} • Sala: ${sub.room.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                                Text("Faltas registradas: $absCount / Máx: ${sub.maxAbsences}", style = MaterialTheme.typography.bodySmall, color = if (isOverOrNear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            IconButton(onClick = { EamRepository.removeSubject(sub.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Excluir Matéria", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
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

        // Floating Action Buttons pinned above bottom bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { showAddSubjectDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Class, contentDescription = "Nova Matéria")
            }
            FloatingActionButton(
                onClick = { showAddCourseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Curso")
            }
        }
    }

    if (showAddCourseDialog) {
        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Adicionar Novo Curso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCourseName,
                        onValueChange = { newCourseName = it },
                        label = { Text("Nome do Curso") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCourseDesc,
                        onValueChange = { newCourseDesc = it },
                        label = { Text("Descrição / Período") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCourseName.isNotBlank()) {
                        EamRepository.addCourse(newCourseName, newCourseDesc)
                        newCourseName = ""
                        newCourseDesc = ""
                        showAddCourseDialog = false
                    }
                }) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourseDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAddSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Adicionar Nova Matéria") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (courses.isNotEmpty()) {
                        var expandedDropdown by remember { mutableStateOf(false) }
                        val currentSelectedCourse = courses.find { it.id == selectedCourseIdForSubject } ?: courses.first()

                        OutlinedButton(onClick = { expandedDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Curso: ${currentSelectedCourse.name}")
                        }
                        DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                            courses.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedCourseIdForSubject = c.id
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("Nome da Matéria") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSubjectProf,
                        onValueChange = { newSubjectProf = it },
                        label = { Text("Professor(a)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSubjectRoom,
                        onValueChange = { newSubjectRoom = it },
                        label = { Text("Sala / Laboratório") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSubjectMaxAbs,
                        onValueChange = { newSubjectMaxAbs = it },
                        label = { Text("Limite Máximo de Faltas") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newSubjectName.isNotBlank() && courses.isNotEmpty()) {
                        val maxAbs = newSubjectMaxAbs.toIntOrNull() ?: 15
                        val targetCourseId = if (selectedCourseIdForSubject.isNotBlank()) selectedCourseIdForSubject else courses.first().id
                        EamRepository.addSubject(targetCourseId, newSubjectName, newSubjectProf, maxAbs, newSubjectRoom)
                        newSubjectName = ""
                        newSubjectProf = ""
                        newSubjectRoom = ""
                        showAddSubjectDialog = false
                    }
                }) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
