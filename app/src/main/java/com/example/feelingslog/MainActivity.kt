package com.example.feelingslog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feelingslog.ui.theme.FeelingslogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Manual "Dependency Injection" for the Repository
        val repository = LogRepository(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LogViewModel(repository) as T
            }
        }

        setContent {
            FeelingslogTheme {
                val navController = rememberNavController()
                val logViewModel: LogViewModel = viewModel(factory = viewModelFactory)
                
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = logViewModel,
                            onLogClick = { logId ->
                                navController.navigate("editor/$logId")
                            },
                            onAddLogClick = {
                                navController.navigate("editor/new")
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
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    FeelingslogTheme {
        HomeScreen(onLogClick = {}, onAddLogClick = {})
    }
}
