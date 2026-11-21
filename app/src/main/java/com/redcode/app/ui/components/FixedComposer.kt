package com.redcode.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.ui.theme.AccentBlue
import com.redcode.app.ui.theme.BrandRed
import com.redcode.app.ui.theme.ComposerBg
import com.redcode.app.ui.theme.SurfaceCardElevated
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.TextSecondary

@Composable
fun FixedComposer(
    text: String,
    onTextChange: (String) -> Unit,
    selectedMode: String,
    onModeChange: (String) -> Unit,
    selectedModel: String,
    onModelClick: () -> Unit,
    onAttachClick: () -> Unit,
    isAttachOpen: Boolean,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    recordingAmplitude: Float = 0f
) {
    var modeDropdownOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(ComposerBg, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        if (isRecording) {
            // Voice Recording Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCancelRecording,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceCardElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Recording",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    DotMatrixWaveform(isRecording = true, amplitude = recordingAmplitude)
                }

                IconButton(
                    onClick = onStopRecording,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceCardElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        tint = BrandRed,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF3A3A3C), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            // Standard Composer
            Column(modifier = Modifier.fillMaxWidth()) {
                val scrollState = rememberScrollState()

                // Auto-scroll text field when typing or cursor position changes
                LaunchedEffect(text.length) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }

                // Text input area: auto-grow from 1 line (24dp) to ~6 lines (136dp), then internal vertical scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp, max = 136.dp)
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Message",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(AccentBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSend() })
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom actions row (fixed height, never squished by text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + / X button with rotation animation
                    val rotation by animateFloatAsState(
                        targetValue = if (isAttachOpen) 45f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "attach_rot"
                    )
                    IconButton(
                        onClick = onAttachClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = if (isAttachOpen) AccentBlue else TextSecondary,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Mode Chip (Build / Plan) with Ellipsis
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { modeDropdownOpen = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMode.ifBlank { "Build" },
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = modeDropdownOpen,
                            onDismissRequest = { modeDropdownOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Build") },
                                onClick = {
                                    onModeChange("Build")
                                    modeDropdownOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Plan") },
                                onClick = {
                                    onModeChange("Plan")
                                    modeDropdownOpen = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Model Chip with Ellipsis and bounded width
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onModelClick)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayModel = if (selectedModel.isNotBlank()) selectedModel else "Model"
                        Text(
                            text = displayModel,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Mic Button
                    IconButton(
                        onClick = onStartRecording,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Arrow Button with animated enabled/disabled transition
                    val canSend = text.isNotBlank()
                    val sendBg by animateColorAsState(
                        targetValue = if (canSend) Color(0xFF3A3A3C) else Color(0xFF28282A),
                        animationSpec = tween(durationMillis = 200),
                        label = "send_bg"
                    )
                    val sendTint by animateColorAsState(
                        targetValue = if (canSend) TextPrimary else Color(0xFF666666),
                        animationSpec = tween(durationMillis = 200),
                        label = "send_tint"
                    )

                    IconButton(
                        onClick = { if (canSend) onSend() },
                        enabled = canSend,
                        modifier = Modifier
                            .size(36.dp)
                            .background(sendBg, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = sendTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
