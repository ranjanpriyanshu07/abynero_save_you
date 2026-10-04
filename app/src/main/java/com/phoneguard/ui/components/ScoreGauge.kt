package com.phoneguard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phoneguard.R
import com.phoneguard.domain.model.ScoreGrade
import com.phoneguard.ui.theme.PhoneGuardTheme
import com.phoneguard.ui.util.labelRes
import com.phoneguard.ui.util.scoreColor
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

/**
 * Large circular score gauge. Animates from 0 up to [score] whenever the score or
 * [animationKey] changes (for example after every scan).
 *
 * @param score 0–100, or null when the phone has not been scanned yet.
 */
@Composable
fun ScoreGauge(
    score: Int?,
    animationKey: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 236.dp,
    strokeWidth: Dp = 18.dp,
) {
    val target = (score ?: 0).coerceIn(0, 100).toFloat()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target, animationKey) {
        progress.snapTo(0f)
        progress.animateTo(target, tween(durationMillis = 1_400, easing = FastOutSlowInEasing))
    }

    val status = PhoneGuardTheme.statusColors
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val value = progress.value
    val color = if (score == null) MaterialTheme.colorScheme.outline else status.scoreColor(value)

    val description = if (score == null) {
        stringResource(R.string.gauge_no_score_description)
    } else {
        stringResource(
            R.string.gauge_description,
            score,
            stringResource(ScoreGrade.forScore(score).labelRes),
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val glow = stroke * 1.9f
            val inset = glow / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val radius = arcSize.width / 2f

            // Background track.
            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Tick marks every 10 points, just inside the track.
            for (i in 0..10) {
                val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * i / 10f).toDouble())
                val outer = radius - stroke
                val inner = outer - if (i % 5 == 0) 10.dp.toPx() else 5.dp.toPx()
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + (outer * cos(angle)).toFloat(), center.y + (outer * sin(angle)).toFloat()),
                    end = Offset(center.x + (inner * cos(angle)).toFloat(), center.y + (inner * sin(angle)).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            if (value > 0f) {
                val sweep = SWEEP_ANGLE * value / 100f
                // Soft glow behind the value arc.
                drawArc(
                    color = color.copy(alpha = 0.18f),
                    startAngle = START_ANGLE,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = glow, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = START_ANGLE,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // Knob at the end of the arc.
                val endAngle = Math.toRadians((START_ANGLE + sweep).toDouble())
                val knob = Offset(
                    center.x + (radius * cos(endAngle)).toFloat(),
                    center.y + (radius * sin(endAngle)).toFloat(),
                )
                drawCircle(color = color, radius = stroke * 0.75f, center = knob)
                drawCircle(color = trackColor, radius = stroke * 0.32f, center = knob)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (score == null) "--" else value.toInt().toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.gauge_out_of_100),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (score == null) {
                    stringResource(R.string.gauge_not_scanned)
                } else {
                    stringResource(ScoreGrade.forScore(score).labelRes)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
    }
}
