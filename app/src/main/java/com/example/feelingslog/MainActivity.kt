package com.example.feelingslog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.feelingslog.ui.theme.FeelingslogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = LogRepository(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LogViewModel(repository) as T
            }
        }

        setContent {
            FeelingslogTheme {
                val logViewModel: LogViewModel = viewModel(factory = viewModelFactory)
                MainAppScreen(logViewModel = logViewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(logViewModel: LogViewModel) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainTabs = listOf("home", "calendar", "settings")
    val showBottomBar = currentRoute in mainTabs

    // Trigger cleanup when app returns to foreground (ON_START / ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                logViewModel.cleanupExpiredLogs()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MainBottomDock(
                    currentRoute = currentRoute,
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddLogClick = {
                        navController.navigate("editor/new")
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = logViewModel,
                    onLogClick = { logId ->
                        navController.navigate("editor/$logId")
                    }
                )
            }
            composable("calendar") {
                CalendarScreen(
                    viewModel = logViewModel,
                    onLogClick = { logId ->
                        navController.navigate("editor/$logId")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToRecentlyDeleted = {
                        navController.navigate("recently_deleted")
                    }
                )
            }
            composable("recently_deleted") {
                RecentlyDeletedScreen(
                    viewModel = logViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("editor/{logId}") { backStackEntry ->
                val logId = backStackEntry.arguments?.getString("logId")
                val actualLogId = if (logId == "new") null else logId
                EditorScreen(
                    viewModel = logViewModel,
                    logId = actualLogId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun MainBottomDock(
    currentRoute: String?,
    onNavigateToTab: (String) -> Unit,
    onAddLogClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { onNavigateToTab("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "calendar",
            onClick = { onNavigateToTab("calendar") },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
            label = { Text("Calendar") }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { onNavigateToTab("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onAddLogClick,
            icon = { Icon(Icons.Default.Add, contentDescription = "Add Log") },
            label = { Text("+") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    FeelingslogTheme {
        HomeScreen(onLogClick = {})
    }
}
