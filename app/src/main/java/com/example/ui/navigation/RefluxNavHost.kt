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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.RefluxViewModel
import com.example.ui.analysis.AnalysisScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.food.FoodsScreen
import com.example.ui.meal.AddMealScreen
import com.example.ui.symptom.AddSymptomScreen
import com.example.ui.timeline.TimelineScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Timeline : Screen("timeline", "Timeline", Icons.Default.Today)
    object Foods : Screen("foods", "Lebensmittel", Icons.Default.Restaurant)
    object Analysis : Screen("analysis", "Trigger", Icons.Default.Analytics)

    object AddMeal : Screen("add_meal", "Mahlzeit erfassen")
    object AddSymptom : Screen("add_symptom", "Symptom erfassen")
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
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) }
                )
            }

            composable(Screen.Timeline.route) {
                TimelineScreen(
                    viewModel = viewModel,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToAddSymptom = { navController.navigate(Screen.AddSymptom.route) }
                )
            }

            composable(Screen.Foods.route) {
                FoodsScreen(viewModel = viewModel)
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
        }
    }
}
