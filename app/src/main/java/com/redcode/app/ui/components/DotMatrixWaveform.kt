package com.redcode.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun DotMatrixWaveform(
    isRecording: Boolean,
    amplitude: Float = 0f,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val dotRadius = 1.6.dp.toPx()
        val spacing = 6.dp.toPx()
        val numCols = (width / spacing).toInt()
        val rows = 5

        // Scale factor dynamically driven by real voice amplitude (0f..1f)
        val ampFactor = if (isRecording) (0.35f + 1.45f * amplitude.coerceIn(0f, 1f)) else 0f

        for (col in 0 until numCols) {
            val x = col * spacing
            val normX = col.toFloat() / numCols

            // Wave envelope (higher in center, tapering at edges)
            val envelope = sin(normX * Math.PI.toFloat())

            val wave1 = sin(normX * 8f + phase) * 12.dp.toPx() * envelope * ampFactor
            val wave2 = sin(normX * 12f - phase * 1.5f) * 6.dp.toPx() * envelope * ampFactor
            val totalWave = wave1 + wave2

            for (r in -rows / 2..rows / 2) {
                val y = centerY + r * 5.dp.toPx() + totalWave * (r.toFloat() / 2f)

                val alpha = if (isRecording) {
                    (0.3f + 0.7f * envelope * (0.5f + 0.5f * amplitude)).coerceIn(0.1f, 1f)
                } else {
                    0.2f
                }

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
