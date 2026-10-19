package com.pedro.eam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pedro.eam.ui.screens.AnalyticsScreen
import com.pedro.eam.ui.screens.CalendarScreen
import com.pedro.eam.ui.screens.CoursesSubjectsScreen
import com.pedro.eam.ui.screens.EventsScreen
import com.pedro.eam.ui.theme.EAMTheme
import com.pedro.eam.utils.UpdateInfo
import com.pedro.eam.utils.UpdateManager
import kotlinx.coroutines.launch

sealed class BottomNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Calendar : BottomNavTab("Agenda", Icons.Default.Home)
    object Courses : BottomNavTab("Matérias", Icons.Default.School)
    object Events : BottomNavTab("Eventos", Icons.Default.Event)
    object Analytics : BottomNavTab("Análises", Icons.Default.Analytics)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EAMTheme {
                val context = LocalContext.current
                var currentTab by remember { mutableStateOf<BottomNavTab>(BottomNavTab.Calendar) }
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    coroutineScope.launch {
                        val currentVersionCode = try {
                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                pInfo.longVersionCode.toInt()
                            } else {
                                @Suppress("DEPRECATION")
                                pInfo.versionCode
                            }
                        } catch (_: Exception) {
                            1
                        }

                        val info = UpdateManager.checkForUpdate(currentVersionCode)
                        if (info != null) {
                            updateInfo = info
                        }
                    }
                }

                if (updateInfo != null) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Nova Atualização Disponível! 🚀") },
                        text = {
                            Column {
                                Text("Versão ${updateInfo?.versionName} está disponível no GitHub.")
                                Spacer(modifier = Modifier.padding(top = 8.dp))
                                Text("Novidades: ${updateInfo?.releaseNotes}", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                updateInfo?.let {
                                    UpdateManager.openDownloadUrl(context, it.apkUrl)
                                }
                            }) {
                                Text("Atualizar Agora")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateInfo = null }) {
                                Text("Depois")
                            }
                        }
                    )
                }

                val tabs = listOf(
                    BottomNavTab.Calendar,
                    BottomNavTab.Courses,
                    BottomNavTab.Events,
                    BottomNavTab.Analytics
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentTab) {
                            is BottomNavTab.Calendar -> CalendarScreen(
                                onNavigateToEvents = { currentTab = BottomNavTab.Events },
                                onNavigateToSubjects = { currentTab = BottomNavTab.Courses }
                            )
                            is BottomNavTab.Courses -> CoursesSubjectsScreen()
                            is BottomNavTab.Events -> EventsScreen()
                            is BottomNavTab.Analytics -> AnalyticsScreen()
                        }
                    }
                }
            }
        }
    }
}
