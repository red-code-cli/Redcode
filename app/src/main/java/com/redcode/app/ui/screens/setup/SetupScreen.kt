package com.redcode.app.ui.screens.setup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.data.model.SetupStepItem
import com.redcode.app.data.model.SetupStepStatus
import com.redcode.app.ui.theme.AccentBlue
import com.redcode.app.ui.theme.BrandRed
import com.redcode.app.ui.theme.DarkBackground
import com.redcode.app.ui.theme.HealthGreen
import com.redcode.app.ui.theme.SoftRed
import com.redcode.app.ui.theme.SurfaceCard
import com.redcode.app.ui.theme.SurfaceCode
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.TextSecondary

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit
) {
    val steps by viewModel.steps.collectAsState()
    val speed by viewModel.currentSpeed.collectAsState()
    val eta by viewModel.currentEta.collectAsState()
    val progress by viewModel.overallProgress.collectAsState()
    val logs by viewModel.terminalLogs.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    var isLogsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            onSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Redcode Glowing Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF222222), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(BrandRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    color = BrandRed,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Redcode",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Initializing runtime & OpenCode engine",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Overall Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = AccentBlue,
            trackColor = Color(0xFF2A2A2A),
            strokeCap = StrokeCap.Round
        )

        if (speed.isNotBlank() || eta.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = speed, color = TextSecondary, fontSize = 12.sp)
                Text(text = "ETA: $eta", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Steps List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            steps.forEach { step ->
                StepRow(step = step)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terminal Log Expandable Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isLogsExpanded = !isLogsExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terminal Output (${logs.size} lines)",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isLogsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isLogsExpanded) {
                val listState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        listState.animateScrollToItem(logs.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 10.dp)
                        .background(SurfaceCode, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(state = listState) {
                        items(logs) { line ->
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
        }

        // Error & Actions
        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error ?: "Unknown error occurred",
                color = SoftRed,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.startSetup() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Logs", logs.joinToString("\n")))
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Log")
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: SetupStepItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                SetupStepStatus.COMPLETED -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(HealthGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = HealthGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                SetupStepStatus.IN_PROGRESS -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AccentBlue,
                        strokeWidth = 2.dp
                    )
                }
                SetupStepStatus.ERROR -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(SoftRed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = SoftRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                SetupStepStatus.PENDING -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF555555), CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = step.title,
            color = if (step.status == SetupStepStatus.PENDING) TextSecondary else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (step.status == SetupStepStatus.IN_PROGRESS && step.progressPercent > 0) {
            Text(
                text = "${step.progressPercent}%",
                color = AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
