package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.RefluxViewModel
import com.example.ui.analysis.AnalysisScreen
import com.example.ui.backup.BackupScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.food.FoodsScreen
import com.example.ui.meal.AddMealScreen
import com.example.ui.symptom.AddSymptomScreen
import com.example.ui.theme.BotanicalCream
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextMuted
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.timeline.TimelineScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Start", Icons.Default.Home)
    object Timeline : Screen("timeline", "Timeline", Icons.Default.List)
    object Analysis : Screen("analysis", "Analyse", Icons.Default.Analytics)
    object Foods : Screen("foods", "Lebensmittel", Icons.Default.Fastfood)
    object AddMeal : Screen("add_meal", "Mahlzeit")
    object AddSymptom : Screen("add_symptom", "Symptom")
    object EditMeal : Screen("edit_meal/{mealId}", "Mahlzeit bearbeiten")
    object EditSymptom : Screen("edit_symptom/{symptomId}", "Symptom bearbeiten")
    object Backup : Screen("backup", "Datensicherung")
}

@Composable
fun RefluxNavHost(
    viewModel: RefluxViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Timeline,
        Screen.Analysis,
        Screen.Foods
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = BotanicalSurface,
                    contentColor = BotanicalTextPrimary
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BotanicalSurface,
                                selectedTextColor = BotanicalForest,
                                indicatorColor = BotanicalForest,
                                unselectedIconColor = BotanicalTextMuted,
                                unselectedTextColor = BotanicalTextMuted
                            ),
                            modifier = Modifier.testTag("nav_${screen.route}")
                        )
                    }
                }
            }
        },
        containerColor = BotanicalCream
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToAddSymptom = { navController.navigate(Screen.AddSymptom.route) },
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                    onNavigateToTimeline = { navController.navigate(Screen.Timeline.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
                )
            }

            composable(Screen.Timeline.route) {
                TimelineScreen(
                    viewModel = viewModel,
                    onNavigateToEditMeal = { mealId -> navController.navigate("edit_meal/$mealId") },
                    onNavigateToEditSymptom = { symptomId -> navController.navigate("edit_symptom/$symptomId") }
                )
            }

            composable(Screen.Analysis.route) {
                AnalysisScreen(viewModel = viewModel)
            }

            composable(Screen.Foods.route) {
                FoodsScreen(viewModel = viewModel)
            }

            composable(Screen.AddMeal.route) {
                AddMealScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditMeal.route,
                arguments = listOf(navArgument("mealId") { type = NavType.LongType })
            ) { backStackEntry ->
                val mealId = backStackEntry.arguments?.getLong("mealId") ?: 0L
                AddMealScreen(
                    viewModel = viewModel,
                    mealId = mealId,
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
                route = Screen.EditSymptom.route,
                arguments = listOf(navArgument("symptomId") { type = NavType.LongType })
            ) { backStackEntry ->
                val symptomId = backStackEntry.arguments?.getLong("symptomId") ?: 0L
                AddSymptomScreen(
                    viewModel = viewModel,
                    symptomId = symptomId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Backup.route) {
                BackupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
