package com.pedro.eam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pedro.eam.data.EamRepository
import com.pedro.eam.ui.screens.AnalyticsScreen
import com.pedro.eam.ui.screens.CalendarScreen
import com.pedro.eam.ui.screens.CoursesSubjectsScreen
import com.pedro.eam.ui.screens.EventsScreen
import com.pedro.eam.ui.screens.ScheduleScreen
import com.pedro.eam.ui.theme.EAMTheme
import com.pedro.eam.utils.UpdateManager
import com.pedro.eam.utils.UpdateState
import kotlinx.coroutines.launch

sealed class BottomNavTab(val title: String, val icon: ImageVector) {
    object Calendar : BottomNavTab("Agenda", Icons.Default.Home)
    object Courses : BottomNavTab("Matérias", Icons.Default.School)
    object Schedule : BottomNavTab("Grade", Icons.Default.GridView)
    object Events : BottomNavTab("Eventos", Icons.Default.Event)
    object Analytics : BottomNavTab("Análises", Icons.Default.Analytics)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EamRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            EAMTheme {
                val context = LocalContext.current
                var currentTab by remember { mutableStateOf<BottomNavTab>(BottomNavTab.Calendar) }
                val coroutineScope = rememberCoroutineScope()
                val updateState by UpdateManager.state.collectAsState()

                LaunchedEffect(Unit) {
                    coroutineScope.launch {
                        UpdateManager.checkForUpdate(context)
                    }
                }

                when (val state = updateState) {
                    is UpdateState.UpdateAvailable -> {
                        AlertDialog(
                            onDismissRequest = { UpdateManager.resetState() },
                            title = { Text("Nova Atualização Disponível! 🚀") },
                            text = {
                                Column {
                                    Text("Versão ${state.info.versionName} está disponível.")
                                    Spacer(modifier = Modifier.padding(top = 8.dp))
                                    Text("Novidades: ${state.info.releaseNotes}", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    coroutineScope.launch {
                                        UpdateManager.startUpdate(context, state.info)
                                    }
                                }) {
                                    Text("Atualizar Agora")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { UpdateManager.resetState() }) {
                                    Text("Depois")
                                }
                            }
                        )
                    }
                    is UpdateState.PermissionRequired -> {
                        AlertDialog(
                            onDismissRequest = { UpdateManager.resetState() },
                            title = { Text("Permissão Necessária") },
                            text = { Text("Para atualizar o EAM, você precisa permitir a instalação de fontes desconhecidas nas configurações do Android.") },
                            confirmButton = {
                                Button(onClick = {
                                    UpdateManager.openInstallPermissionSettings(context)
                                    UpdateManager.resetState()
                                }) {
                                    Text("Abrir Configurações")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { UpdateManager.resetState() }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                    is UpdateState.Downloading -> {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Baixando Atualização...") },
                            text = {
                                Column {
                                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.padding(top = 8.dp))
                                    Text("Progresso: ${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            confirmButton = { }
                        )
                    }
                    is UpdateState.Verifying -> {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Verificando APK...") },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.padding(top = 8.dp))
                                    Text("Validando integridade e assinatura...", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            confirmButton = { }
                        )
                    }
                    is UpdateState.Failed -> {
                        AlertDialog(
                            onDismissRequest = { UpdateManager.resetState() },
                            title = { Text("Falha na Atualização") },
                            text = {
                                Column {
                                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                                }
                            },
                            confirmButton = {
                                Button(onClick = { UpdateManager.resetState() }) {
                                    Text("Tentar Novamente")
                                }
                            }
                        )
                    }
                    else -> {}
                }

                val tabs = listOf(
                    BottomNavTab.Calendar,
                    BottomNavTab.Courses,
                    BottomNavTab.Schedule,
                    BottomNavTab.Events,
                    BottomNavTab.Analytics
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentTab) {
                            is BottomNavTab.Calendar -> CalendarScreen(
                                contentPadding = innerPadding,
                                onNavigateToSchedule = { currentTab = BottomNavTab.Schedule },
                                onNavigateToEvents = { currentTab = BottomNavTab.Events }
                            )
                            is BottomNavTab.Courses -> CoursesSubjectsScreen(contentPadding = innerPadding)
                            is BottomNavTab.Schedule -> ScheduleScreen(contentPadding = innerPadding)
                            is BottomNavTab.Events -> EventsScreen(contentPadding = innerPadding)
                            is BottomNavTab.Analytics -> AnalyticsScreen(contentPadding = innerPadding)
                        }
                    }
                }
            }
        }
    }
}
