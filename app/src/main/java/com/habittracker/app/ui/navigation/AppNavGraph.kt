package com.habittracker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.habittracker.app.ui.screens.calendar.CalendarScreen
import com.habittracker.app.ui.screens.calendar.CalendarViewModel
import com.habittracker.app.ui.screens.habits.AddEditHabitScreen
import com.habittracker.app.ui.screens.habits.AddEditHabitViewModel
import com.habittracker.app.ui.screens.home.HomeScreen
import com.habittracker.app.ui.screens.home.HomeViewModel
import com.habittracker.app.ui.screens.stats.StatsScreen
import com.habittracker.app.ui.screens.stats.StatsViewModel

sealed class Screen(val route: String, val title: String = "") {
    object Home : Screen("home", "Today")
    object Calendar : Screen("calendar", "Calendar")
    object Stats : Screen("stats", "Statistics")
    object AddEditHabit : Screen("add_edit_habit?habitId={habitId}") {
        fun createRoute(habitId: Long? = null): String =
            if (habitId != null) "add_edit_habit?habitId=$habitId" else "add_edit_habit"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onAddHabitClick = onNavigateToAddHabit
            )
        }

        composable(Screen.Calendar.route) {
            val viewModel: CalendarViewModel = hiltViewModel()
            CalendarScreen(viewModel = viewModel)
        }

        composable(Screen.Stats.route) {
            val viewModel: StatsViewModel = hiltViewModel()
            StatsScreen(
                viewModel = viewModel,
                onEditHabit = onEditHabit
            )
        }

        composable(
            route = Screen.AddEditHabit.route,
            arguments = listOf(
                navArgument("habitId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: -1L
            val viewModel: AddEditHabitViewModel = hiltViewModel()
            AddEditHabitScreen(
                viewModel = viewModel,
                habitId = if (habitId > 0) habitId else null,
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
