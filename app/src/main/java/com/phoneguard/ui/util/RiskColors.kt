package com.phoneguard.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.ui.theme.PhoneGuardTheme
import com.phoneguard.ui.theme.StatusColors

/** Main colour for a risk level (icons, stripes, gauge). */
@Composable
@ReadOnlyComposable
fun RiskLevel.color(): Color = PhoneGuardTheme.statusColors.colorFor(this)

/** Soft background colour for chips and banners. */
@Composable
@ReadOnlyComposable
fun RiskLevel.containerColor(): Color {
    val c = PhoneGuardTheme.statusColors
    return when (this) {
        RiskLevel.SAFE -> c.safeContainer
        RiskLevel.LOW -> c.lowContainer
        RiskLevel.MEDIUM -> c.warningContainer
        RiskLevel.HIGH -> c.dangerContainer
    }
}

/** Text colour on top of [containerColor]. */
@Composable
@ReadOnlyComposable
fun RiskLevel.onContainerColor(): Color {
    val c = PhoneGuardTheme.statusColors
    return when (this) {
        RiskLevel.SAFE -> c.onSafeContainer
        RiskLevel.LOW -> c.onLowContainer
        RiskLevel.MEDIUM -> c.onWarningContainer
        RiskLevel.HIGH -> c.onDangerContainer
    }
}

/** Icon that matches a risk level. */
val RiskLevel.icon: ImageVector
    get() = when (this) {
        RiskLevel.SAFE -> Icons.Rounded.CheckCircle
        RiskLevel.LOW -> Icons.Rounded.Info
        RiskLevel.MEDIUM -> Icons.Rounded.Warning
        RiskLevel.HIGH -> Icons.Rounded.Error
    }

/** Maps a risk level to its colour in [StatusColors]. */
fun StatusColors.colorFor(level: RiskLevel): Color = when (level) {
    RiskLevel.SAFE -> safe
    RiskLevel.LOW -> low
    RiskLevel.MEDIUM -> warning
    RiskLevel.HIGH -> danger
}

/** Smooth red → amber → green colour for a 0–100 score. */
fun StatusColors.scoreColor(score: Float): Color = when {
    score < 50f -> lerp(danger, warning, (score / 50f).coerceIn(0f, 1f))
    else -> lerp(warning, safe, ((score - 50f) / 50f).coerceIn(0f, 1f))
}
