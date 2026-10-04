package com.phoneguard.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.phoneguard.R

/**
 * Big "Scan Now" button. While [isScanning], rings pulse outward from the button and the
 * label changes to "Scanning…".
 */
@Composable
fun ScanPulseButton(
    isScanning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    idleText: String = stringResource(R.string.scan_now),
) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "scanPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val scanningState = stringResource(R.string.scanning)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isScanning) {
            Canvas(Modifier.matchParentSize()) {
                val baseWidth = size.width * 0.82f
                val baseHeight = 64.dp.toPx()
                listOf(pulse, (pulse + 0.5f) % 1f).forEach { phase ->
                    val grow = 18.dp.toPx() * phase
                    val w = baseWidth + grow * 2
                    val h = baseHeight + grow * 2
                    drawRoundRect(
                        color = primary.copy(alpha = (1f - phase) * 0.45f),
                        topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
        }
        Button(
            onClick = { if (!isScanning) onClick() },
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(64.dp)
                .semantics { if (isScanning) stateDescription = scanningState },
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        ) {
            AnimatedContent(
                targetState = isScanning,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "scanLabel",
            ) { scanning ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    if (scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Radar, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (scanning) stringResource(R.string.scanning) else idleText,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}
