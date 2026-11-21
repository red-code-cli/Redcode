package com.redcode.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.data.model.ExperimentalSettings
import com.redcode.app.ui.theme.RedcodeAccentBlue
import com.redcode.app.ui.theme.RedcodeCardBackground
import com.redcode.app.ui.theme.RedcodeCardSecondary
import com.redcode.app.ui.theme.RedcodeDarkBackground
import com.redcode.app.ui.theme.RedcodeDivider
import com.redcode.app.ui.theme.RedcodeTextPrimary
import com.redcode.app.ui.theme.RedcodeTextSecondary

@Composable
fun ExperimentalScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val exp = state.experimentalSettings

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
                    text = "Experimental Features",
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
            // Master Switch
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "All Experiments",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RedcodeTextPrimary
                            )
                        )
                        Text(
                            text = "Toggle all experimental lab features simultaneously",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = RedcodeTextSecondary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = exp.allExperiments,
                        onCheckedChange = { viewModel.toggleAllExperiments(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RedcodeTextPrimary,
                            checkedTrackColor = RedcodeAccentBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sub-toggles Card
            Text(
                text = "INDIVIDUAL EXPERIMENTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = RedcodeTextSecondary,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Column {
                    ToggleRow(
                        title = "Hide Unused Models",
                        description = "Omit models from picker that have no recent activity",
                        checked = exp.hideUnusedModels,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(hideUnusedModels = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Background Call Shortcut",
                        description = "Quick widget access to call background agent sessions",
                        checked = exp.backgroundCallShortcut,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(backgroundCallShortcut = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Session Delegator",
                        description = "Allow OpenCode agent to branch and delegate subtasks",
                        checked = exp.sessionDelegator,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(sessionDelegator = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Share Screen With Agents",
                        description = "Stream live display context to agent tool invocation",
                        checked = exp.shareScreenWithAgents,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(shareScreenWithAgents = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Inbox Sidebar",
                        description = "Show compact sidebar drawer on wide displays",
                        checked = exp.inboxSidebar,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(inboxSidebar = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Session Live Updates",
                        description = "Subscribe to SSE event stream for real-time tokens",
                        checked = exp.sessionLiveUpdates,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(sessionLiveUpdates = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Sound Effects & Haptics",
                        description = "Play feedback audio on completion and clicks",
                        checked = exp.soundEffects,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(soundEffects = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Send Sound",
                        description = "Chime tone when prompt message is submitted",
                        checked = exp.sendSound,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(sendSound = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "In-App Browser",
                        description = "Open source documentation links within app tabs",
                        checked = exp.inAppBrowser,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(inAppBrowser = it)) }
                    )
                    ExpDivider()
                    ToggleRow(
                        title = "Open Links by Server Address",
                        description = "Route external URLs via local proxy server",
                        checked = exp.openLinksByServerAddress,
                        onCheckedChange = { viewModel.updateExperimentalSettings(exp.copy(openLinksByServerAddress = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio Volume Slider Card
            Text(
                text = "AUDIO LEVEL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = RedcodeTextSecondary,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RedcodeCardBackground
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sound Effects Volume",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = RedcodeTextPrimary
                            )
                        )
                        Text(
                            text = "${exp.volumePercent}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RedcodeAccentBlue
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = exp.volumePercent.toFloat(),
                        onValueChange = { viewModel.updateExperimentalSettings(exp.copy(volumePercent = it.toInt())) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = RedcodeAccentBlue,
                            activeTrackColor = RedcodeAccentBlue,
                            inactiveTrackColor = RedcodeCardSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = RedcodeTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = RedcodeTextSecondary
                )
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RedcodeTextPrimary,
                checkedTrackColor = RedcodeAccentBlue
            )
        )
    }
}

@Composable
private fun ExpDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(RedcodeDivider)
    )
}
