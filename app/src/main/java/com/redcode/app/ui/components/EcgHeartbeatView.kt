package com.redcode.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.redcode.app.ui.theme.HealthGreen

@Composable
fun EcgHeartbeatView(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ecg_transition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ecg_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1E1E1E))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val dotSpacing = 8.dp.toPx()
        val dotRadius = 1.8.dp.toPx()

        val totalDots = (width / dotSpacing).toInt()
        val activeHead = (totalDots * progress).toInt()

        for (i in 0 until totalDots) {
            val x = i * dotSpacing
            val normPos = (i.toFloat() / totalDots) % 0.5f // two ECG waves per width

            var yOffset = 0f
            if (isConnected) {
                // Heartbeat P-Q-R-S-T wave calculation
                when {
                    normPos in 0.12f..0.15f -> yOffset = -centerY * 0.25f // P wave
                    normPos in 0.16f..0.18f -> yOffset = centerY * 0.15f // Q dip
                    normPos in 0.18f..0.21f -> yOffset = -centerY * 0.75f // R peak
                    normPos in 0.21f..0.24f -> yOffset = centerY * 0.35f // S dip
                    normPos in 0.26f..0.30f -> yOffset = -centerY * 0.30f // T wave
                    else -> yOffset = 0f
                }
            }

            val y = centerY + yOffset

            // Fade trail behind the sweep head
            val distFromHead = (activeHead - i + totalDots) % totalDots
            val alpha = if (isConnected) {
                ((1f - (distFromHead.toFloat() / totalDots * 1.5f)).coerceIn(0.15f, 1f))
            } else {
                0.2f
            }

            val dotColor = if (isConnected) {
                HealthGreen.copy(alpha = alpha)
            } else {
                Color(0xFF555555).copy(alpha = alpha)
            }

            drawCircle(
                color = dotColor,
                radius = if (distFromHead in 0..2) dotRadius * 1.3f else dotRadius,
                center = Offset(x, y)
            )
        }
    }
}
