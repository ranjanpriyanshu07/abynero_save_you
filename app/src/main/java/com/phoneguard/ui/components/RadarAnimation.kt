package com.phoneguard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Blips shown on the radar, as (angle in degrees, distance from centre 0–1). */
private val Blips = listOf(40f to 0.62f, 130f to 0.38f, 215f to 0.78f, 300f to 0.5f, 350f to 0.85f)

/**
 * Animated radar with a rotating sweep, an expanding ripple and a shield in the middle.
 * Shown while a scan is running. When [animate] is false it is drawn still.
 */
@Composable
fun RadarAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    animate: Boolean = true,
) {
    val color = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ripple",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "breathe",
    )
    val angle = if (animate) sweep else 315f

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            // Rings and cross-hair.
            for (i in 1..4) {
                drawCircle(gridColor, radius = radius * i / 4f, style = Stroke(width = 1.5.dp.toPx()))
            }
            drawLine(gridColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.dp.toPx())
            drawLine(gridColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1.dp.toPx())

            if (animate) {
                drawCircle(
                    color = color.copy(alpha = (1f - ripple) * 0.4f),
                    radius = radius * ripple,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }

            // Rotating sweep: transparent tail that brightens towards the leading edge.
            rotate(angle) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.70f to Color.Transparent,
                        1f to color.copy(alpha = 0.55f),
                        center = center,
                    ),
                    radius = radius,
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 2.5.dp.toPx(),
                )
            }

            // Blips glow right after the sweep passes them.
            Blips.forEach { (blipAngle, distance) ->
                val behind = ((angle - blipAngle) % 360f + 360f) % 360f
                val alpha = if (animate) (1f - behind / 180f).coerceIn(0f, 1f) else 0.35f
                if (alpha > 0f) {
                    val rad = Math.toRadians(blipAngle.toDouble())
                    val pos = Offset(
                        center.x + (radius * distance * cos(rad)).toFloat(),
                        center.y + (radius * distance * sin(rad)).toFloat(),
                    )
                    drawCircle(color.copy(alpha = alpha * 0.35f), radius = 9.dp.toPx(), center = pos)
                    drawCircle(color.copy(alpha = alpha), radius = 4.dp.toPx() * (1f + abs(alpha - 0.5f) * 0.3f), center = pos)
                }
            }
        }
        Box(
            modifier = Modifier
                .size(size * 0.3f)
                .scale(if (animate) breathe else 1f)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.16f),
            )
        }
    }
}
