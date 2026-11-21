package com.redcode.app.ui.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.redcode.app.ui.screens.chat.ChatScreen
import com.redcode.app.ui.screens.chat.ChatViewModel
import com.redcode.app.ui.screens.home.HomeScreen
import com.redcode.app.ui.screens.home.HomeViewModel
import com.redcode.app.ui.screens.settings.ExperimentalScreen
import com.redcode.app.ui.screens.settings.LanguageScreen
import com.redcode.app.ui.screens.settings.SettingsScreen
import com.redcode.app.ui.screens.settings.SettingsViewModel
import com.redcode.app.ui.screens.settings.WhatsNewScreen
import com.redcode.app.ui.screens.setup.SetupScreen
import com.redcode.app.ui.screens.setup.SetupViewModel

@Composable
fun RedcodeNavHost(
    navController: NavHostController,
    drawerState: DrawerState,
    isSetupCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val startDestination = if (isSetupCompleted) Screen.Home.route else Screen.Setup.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Setup.route) {
            val setupViewModel: SetupViewModel = hiltViewModel()
            SetupScreen(
                viewModel = setupViewModel,
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                drawerState = drawerState,
                onNavigateToChat = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            val chatViewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                viewModel = chatViewModel,
                drawerState = drawerState,
                onBackToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSession = { newSessionId ->
                    navController.navigate(Screen.Chat.createRoute(newSessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWhatsNew = { navController.navigate(Screen.WhatsNew.route) },
                onNavigateToExperimental = { navController.navigate(Screen.Experimental.route) },
                onNavigateToLanguage = { navController.navigate(Screen.Language.route) },
                onResetCompleted = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.WhatsNew.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            WhatsNewScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Experimental.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            ExperimentalScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Language.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            LanguageScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
