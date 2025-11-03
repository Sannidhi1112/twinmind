package com.twinmind.voicerecorder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.twinmind.voicerecorder.ui.screen.DashboardScreen
import com.twinmind.voicerecorder.ui.screen.RecordingScreen
import com.twinmind.voicerecorder.ui.screen.SummaryScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Recording : Screen("recording")
    object Summary : Screen("summary/{meetingId}") {
        fun createRoute(meetingId: Long) = "summary/$meetingId"
    }
}

@Composable
fun VoiceRecorderNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRecording = {
                    navController.navigate(Screen.Recording.route)
                },
                onNavigateToSummary = { meetingId ->
                    navController.navigate(Screen.Summary.createRoute(meetingId))
                }
            )
        }

        composable(Screen.Recording.route) {
            RecordingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Summary.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.StringType }
            )
        ) {
            SummaryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
