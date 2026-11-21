package com.redcode.app.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.BuildConfig
import com.redcode.app.ui.components.EcgHeartbeatView
import com.redcode.app.ui.theme.RedcodeAccentBlue
import com.redcode.app.ui.theme.RedcodeAccentGreen
import com.redcode.app.ui.theme.RedcodeAccentRed
import com.redcode.app.ui.theme.RedcodeCardBackground
import com.redcode.app.ui.theme.RedcodeDarkBackground
import com.redcode.app.ui.theme.RedcodeDivider
import com.redcode.app.ui.theme.RedcodeTextPrimary
import com.redcode.app.ui.theme.RedcodeTextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWhatsNew: () -> Unit,
    onNavigateToExperimental: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onResetCompleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val serverLogs by viewModel.serverLogs.collectAsState()
    val sseEvents by viewModel.sseEvents.collectAsState()
    val context = LocalContext.current

    var showResetDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showServerLogsDialog by remember { mutableStateOf(false) }
    var showEventsDialog by remember { mutableStateOf(false) }

    val themeOptions = listOf("Dark (Pure OLED)", "Dark (Charcoal)", "Light", "System Default")

    Scaffold(
        containerColor = RedcodeDarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = RedcodeTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = RedcodeTextPrimary
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. ECG Heartbeat Live Monitor Card
            HeartbeatMonitorCard(
                isConnected = state.health.isConnected,
                latencyMs = state.health.latencyMs,
                address = "${state.serverHost}:${state.serverPort}",
                statusText = state.health.statusText
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Preferences Group
            SectionHeader(title = "PREFERENCES")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "Appearance",
                        subtitle = state.selectedTheme,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = state.selectedLanguage,
                        onClick = onNavigateToLanguage
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Sound & Haptics",
                        subtitle = if (state.experimentalSettings.soundEffects) "Enabled" else "Muted",
                        onClick = onNavigateToExperimental
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Server & Runtime Group
            SectionHeader(title = "SERVER & RUNTIME")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "Server Status",
                        subtitle = state.health.statusText,
                        badgeColor = if (state.health.isConnected) RedcodeAccentGreen else RedcodeAccentRed,
                        onClick = null
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Code,
                        title = "API Endpoint",
                        subtitle = "http://${state.serverHost}:${state.serverPort}",
                        onClick = null
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Storage,
                        title = "Storage Path",
                        subtitle = state.storagePath,
                        onClick = null
                    )
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !state.isRestarting) { viewModel.restartServer() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = RedcodeAccentBlue
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restart Server",
                                color = RedcodeAccentBlue,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Restart OpenCode daemon process",
                                color = RedcodeTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (state.isRestarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = RedcodeAccentBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Developer & Debug Group
            SectionHeader(title = "DEVELOPER & DEBUG")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Code,
                        title = "Server Log",
                        subtitle = "Last ${serverLogs.takeLast(100).size} lines from daemon",
                        onClick = { showServerLogsDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        title = "Last 50 Events",
                        subtitle = "Raw SSE event stream (${sseEvents.size} events)",
                        onClick = { showEventsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(title = "UPDATES & LABS")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "What's New in Redcode",
                        subtitle = "Changelog and release highlights",
                        onClick = onNavigateToWhatsNew
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Science,
                        title = "Experimental Features",
                        subtitle = if (state.experimentalSettings.allExperiments) "All Features Active" else "Custom Labs",
                        badgeText = if (state.experimentalSettings.allExperiments) "All On" else null,
                        badgeColor = RedcodeAccentBlue,
                        onClick = onNavigateToExperimental
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Danger Zone / Re-run Setup Card
            SectionHeader(title = "RESET")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Reset",
                        tint = RedcodeAccentRed
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Re-run Initial Setup",
                            color = RedcodeAccentRed,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Reset runtime configuration and re-install",
                            color = RedcodeTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = RedcodeTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 6. Dynamic Version Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Redcode for Android",
                    color = RedcodeTextSecondary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = RedcodeTextSecondary.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Appearance", color = RedcodeTextPrimary) },
            text = {
                Column {
                    themeOptions.forEach { themeName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(themeName)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (themeName == state.selectedTheme),
                                onClick = {
                                    viewModel.setTheme(themeName)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = RedcodeAccentBlue,
                                    unselectedColor = RedcodeTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = themeName, color = RedcodeTextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = RedcodeAccentBlue)
                }
            },
            containerColor = RedcodeCardBackground
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Re-run Initial Setup?", color = RedcodeTextPrimary) },
            text = {
                Text(
                    "This will stop the OpenCode server and return to the initial Ubuntu setup wizard.",
                    color = RedcodeTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetSetup(onResetCompleted)
                    }
                ) {
                    Text("Reset", color = RedcodeAccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = RedcodeTextPrimary)
                }
            },
            containerColor = RedcodeCardBackground
        )
    }

    if (showServerLogsDialog) {
        val last100 = serverLogs.takeLast(100)
        AlertDialog(
            onDismissRequest = { showServerLogsDialog = false },
            title = { Text("Server Log (Last 100 lines)", color = RedcodeTextPrimary) },
            text = {
                val listState = rememberLazyListState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (last100.isEmpty()) {
                        Text("No logs available yet.", color = RedcodeTextSecondary, fontSize = 12.sp)
                    } else {
                        LazyColumn(state = listState) {
                            items(last100) { line ->
                                Text(
                                    text = line,
                                    color = Color(0xFFC0C0C0),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Server Logs", last100.joinToString("\n")))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy", color = RedcodeAccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerLogsDialog = false }) {
                    Text("Close", color = RedcodeTextSecondary)
                }
            },
            containerColor = RedcodeCardBackground
        )
    }

    if (showEventsDialog) {
        AlertDialog(
            onDismissRequest = { showEventsDialog = false },
            title = { Text("Last 50 Events (Raw)", color = RedcodeTextPrimary) },
            text = {
                val listState = rememberLazyListState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (sseEvents.isEmpty()) {
                        Text("No SSE events received yet.", color = RedcodeTextSecondary, fontSize = 12.sp)
                    } else {
                        LazyColumn(state = listState) {
                            items(sseEvents) { ev ->
                                Text(
                                    text = ev,
                                    color = Color(0xFF81D4FA),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("SSE Events", sseEvents.joinToString("\n\n")))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy", color = RedcodeAccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEventsDialog = false }) {
                    Text("Close", color = RedcodeTextSecondary)
                }
            },
            containerColor = RedcodeCardBackground
        )
    }
}

@Composable
private fun HeartbeatMonitorCard(
    isConnected: Boolean,
    latencyMs: Long,
    address: String,
    statusText: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = RedcodeCardBackground
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) RedcodeAccentGreen else RedcodeAccentRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE HEARTBEAT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = RedcodeTextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = if (isConnected) "$latencyMs ms" else "Offline",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) RedcodeAccentGreen else RedcodeAccentRed,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Waveform
            EcgHeartbeatView(
                isConnected = isConnected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$address · Loopback",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RedcodeTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isConnected) RedcodeAccentGreen else RedcodeAccentRed
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = RedcodeTextSecondary,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = RedcodeAccentBlue,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RedcodeTextPrimary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = RedcodeTextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                color = RedcodeTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = RedcodeTextSecondary
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(RedcodeDivider)
    )
}
