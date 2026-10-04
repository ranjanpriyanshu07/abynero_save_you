package com.phoneguard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.phoneguard.ui.theme.PhoneGuardTheme
import com.phoneguard.ui.util.scoreColor

/**
 * Line chart of scores over time (oldest on the left). Draws itself in from left to right.
 *
 * @param scores 0–100 values in chronological order.
 * @param description spoken by screen readers.
 */
@Composable
fun TrendChart(
    scores: List<Int>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(scores) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(900))
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val status = PhoneGuardTheme.statusColors
    val surface = MaterialTheme.colorScheme.surfaceContainerLow

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val labelWidth = 28.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val left = labelWidth
        val right = size.width - 8.dp.toPx()
        fun yFor(score: Int) = bottom - (bottom - top) * score.coerceIn(0, 100) / 100f

        listOf(0, 50, 100).forEach { mark ->
            val y = yFor(mark)
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
            val layout = textMeasurer.measure(mark.toString(), labelStyle)
            drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
        }

        if (scores.isEmpty()) return@Canvas
        val step = if (scores.size > 1) (right - left) / (scores.size - 1) else 0f
        val points = scores.mapIndexed { i, s ->
            Offset(if (scores.size > 1) left + step * i else (left + right) / 2f, yFor(s))
        }

        clipRect(right = left + (right - left) * reveal.value + 12.dp.toPx()) {
            if (points.size > 1) {
                val line = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val cur = points[i]
                        val midX = (prev.x + cur.x) / 2f
                        cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                    }
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(points.last().x, bottom)
                    lineTo(points.first().x, bottom)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0f)), top, bottom),
                )
                drawPath(
                    path = line,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            points.forEachIndexed { i, p ->
                val dotColor = status.scoreColor(scores[i].toFloat())
                drawCircle(surface, radius = 6.dp.toPx(), center = p)
                drawCircle(dotColor, radius = 4.dp.toPx(), center = p)
            }
        }
    }
}
