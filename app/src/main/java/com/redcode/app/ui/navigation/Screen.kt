package com.redcode.app.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Home : Screen("home")
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String): String = "chat/$sessionId"
    }
    object Settings : Screen("settings")
    object WhatsNew : Screen("whats_new")
    object Experimental : Screen("experimental")
    object Language : Screen("language")
}
