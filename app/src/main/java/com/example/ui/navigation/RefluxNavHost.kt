package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.RefluxViewModel
import com.example.ui.analysis.AnalysisScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.food.FoodsScreen
import com.example.ui.habit.HabitsScreen
import com.example.ui.meal.AddMealScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.symptom.AddSymptomScreen
import com.example.ui.timeline.TimelineScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Timeline : Screen("timeline", "Timeline", Icons.Default.Today)
    object Foods : Screen("foods", "Lebensmittel", Icons.Default.Restaurant)
    object Analysis : Screen("analysis", "Trigger", Icons.Default.Analytics)
    object Habits : Screen("habits", "Gewohnheiten")

    object AddMeal : Screen("add_meal", "Mahlzeit erfassen")
    object EditMeal : Screen("edit_meal/{mealId}", "Mahlzeit bearbeiten") {
        fun createRoute(mealId: Long): String = "edit_meal/$mealId"
    }
    object AddSymptom : Screen("add_symptom", "Symptom erfassen")
    object EditSymptom : Screen("edit_symptom/{symptomId}", "Symptom bearbeiten") {
        fun createRoute(symptomId: Long): String = "edit_symptom/$symptomId"
    }
    object Settings : Screen("settings", "Einstellungen")
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Timeline,
    Screen.Foods,
    Screen.Analysis
)

@Composable
fun RefluxNavHost(
    viewModel: RefluxViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    BottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToAddSymptom = { navController.navigate(Screen.AddSymptom.route) },
                    onNavigateToTimeline = { navController.navigate(Screen.Timeline.route) },
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Timeline.route) {
                TimelineScreen(
                    viewModel = viewModel,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToAddSymptom = { navController.navigate(Screen.AddSymptom.route) },
                    onNavigateToEditMeal = { mealId ->
                        navController.navigate(Screen.EditMeal.createRoute(mealId))
                    },
                    onNavigateToEditSymptom = { symptomId ->
                        navController.navigate(Screen.EditSymptom.createRoute(symptomId))
                    }
                )
            }

            composable(Screen.Foods.route) {
                FoodsScreen(
                    viewModel = viewModel,
                    onNavigateToHabits = { navController.navigate(Screen.Habits.route) }
                )
            }

            composable(Screen.Habits.route) {
                HabitsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Analysis.route) {
                AnalysisScreen(viewModel = viewModel)
            }

            composable(Screen.AddMeal.route) {
                AddMealScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddSymptom.route) {
                AddSymptomScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditMeal.route,
                arguments = listOf(navArgument("mealId") { type = NavType.LongType })
            ) { backStackEntry ->
                val mealId = backStackEntry.arguments?.getLong("mealId")
                if (mealId != null) {
                    AddMealScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        mealId = mealId
                    )
                }
            }

            composable(
                route = Screen.EditSymptom.route,
                arguments = listOf(navArgument("symptomId") { type = NavType.LongType })
            ) { backStackEntry ->
                val symptomId = backStackEntry.arguments?.getLong("symptomId")
                if (symptomId != null) {
                    AddSymptomScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        symptomId = symptomId
                    )
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRequestCsvExport = {
                        // Hand off to a helper that wraps the file in a FileProvider URI
                        // and starts the share-sheet. The helper itself does no IO.
                        triggerCsvShare(context, viewModel)
                    }
                )
            }
        }
    }
}

private fun triggerCsvShare(
    context: android.content.Context,
    viewModel: RefluxViewModel
) {
    viewModel.exportCsv(
        context = context,
        onReady = { file ->
            val authority = context.packageName + ".fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, authority, file
            )
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "RefluxTracker Export")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // The grant flag must also be present on the chooser intent on
            // some Android versions; otherwise the target app cannot read the
            // FileProvider URI.
            val chooser = android.content.Intent.createChooser(send, "CSV teilen").apply {
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        },
        onError = { t ->
            android.widget.Toast.makeText(
                context,
                "Export fehlgeschlagen: ${t.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    )
}
