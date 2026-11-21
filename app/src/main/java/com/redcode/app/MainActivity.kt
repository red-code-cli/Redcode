package com.redcode.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.redcode.app.service.RedcodeForegroundService
import com.redcode.app.ui.MainViewModel
import com.redcode.app.ui.components.NavigationDrawerContent
import com.redcode.app.ui.navigation.RedcodeNavHost
import com.redcode.app.ui.navigation.Screen
import com.redcode.app.ui.theme.DarkBackground
import com.redcode.app.ui.theme.RedcodeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            RedcodeTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val isSetupCompleted by mainViewModel.isSetupCompleted.collectAsState()
                val sessions by mainViewModel.sessions.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Auto-start background service if setup already completed
                LaunchedEffect(isSetupCompleted) {
                    if (isSetupCompleted) {
                        startForegroundService()
                    }
                }

                val gesturesAllowed = currentRoute == Screen.Home.route || currentRoute?.startsWith("chat/") == true

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = gesturesAllowed,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = DarkBackground
                        ) {
                            val activeSessionId = if (currentRoute?.startsWith("chat/") == true) {
                                navBackStackEntry?.arguments?.getString("sessionId")
                            } else null

                            NavigationDrawerContent(
                                sessions = sessions,
                                selectedSessionId = activeSessionId,
                                onSelectSession = { id ->
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Chat.createRoute(id)) {
                                        popUpTo(Screen.Home.route)
                                    }
                                },
                                onNewSession = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                },
                                onOpenSettings = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }
                    }
                ) {
                    RedcodeNavHost(
                        navController = navController,
                        drawerState = drawerState,
                        isSetupCompleted = isSetupCompleted,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // Manage External Storage check (for binding /sdcard to proot /mnt/sdcard)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, RedcodeForegroundService::class.java).apply {
            action = RedcodeForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
