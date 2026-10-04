package com.phoneguard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Fixed safety colours. These never follow dynamic colour, so green always means safe
 * and red always means danger.
 */
@Immutable
data class StatusColors(
    val safe: Color,
    val safeContainer: Color,
    val onSafeContainer: Color,
    val low: Color,
    val lowContainer: Color,
    val onLowContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

private val DarkStatusColors = StatusColors(
    safe = Green400, safeContainer = Green900, onSafeContainer = Green100,
    low = Sky400, lowContainer = Sky900, onLowContainer = Sky100,
    warning = Amber400, warningContainer = Amber900, onWarningContainer = Amber100,
    danger = Red400, dangerContainer = Red900, onDangerContainer = Red100,
)

private val LightStatusColors = StatusColors(
    safe = Green700, safeContainer = Green100, onSafeContainer = Green900,
    low = Sky700, lowContainer = Sky100, onLowContainer = Sky900,
    warning = Amber700, warningContainer = Amber100, onWarningContainer = Amber900,
    danger = Red700, dangerContainer = Red100, onDangerContainer = Red900,
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Teal400,
    onPrimary = Teal900,
    primaryContainer = Teal700,
    onPrimaryContainer = Teal100,
    secondary = Green400,
    onSecondary = Green900,
    secondaryContainer = Navy700,
    onSecondaryContainer = Slate100,
    tertiary = Amber400,
    onTertiary = Amber900,
    tertiaryContainer = Amber900,
    onTertiaryContainer = Amber100,
    error = Red400,
    onError = Red900,
    errorContainer = Red900,
    onErrorContainer = Red100,
    background = Navy900,
    onBackground = Slate100,
    surface = Navy900,
    onSurface = Slate100,
    surfaceVariant = Navy800,
    onSurfaceVariant = Slate300,
    surfaceContainerLowest = Navy950,
    surfaceContainerLow = Navy850,
    surfaceContainer = Navy850,
    surfaceContainerHigh = Navy800,
    surfaceContainerHighest = Navy700,
    outline = Navy600,
    outlineVariant = Navy700,
    inverseSurface = Slate100,
    inverseOnSurface = Navy900,
    inversePrimary = Teal700,
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Teal700,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondary = Green700,
    onSecondary = Color.White,
    secondaryContainer = Mist200,
    onSecondaryContainer = Ink900,
    tertiary = Amber700,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber900,
    error = Red700,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red900,
    background = Mist50,
    onBackground = Ink900,
    surface = Mist50,
    onSurface = Ink900,
    surfaceVariant = Mist100,
    onSurfaceVariant = Ink600,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Mist100,
    surfaceContainerHighest = Mist200,
    outline = Slate400,
    outlineVariant = Mist200,
    inverseSurface = Navy900,
    inverseOnSurface = Slate100,
    inversePrimary = Teal400,
)

/**
 * PhoneGuard theme. Uses Material You dynamic colour on Android 12+ and the navy/teal brand
 * palette elsewhere. Light and dark modes follow the system.
 */
@Composable
fun PhoneGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PhoneGuardTypography,
            shapes = PhoneGuardShapes,
            content = content,
        )
    }
}

/** Access to PhoneGuard-specific theme values. */
object PhoneGuardTheme {
    /** Safety colours for the current light/dark mode. */
    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
