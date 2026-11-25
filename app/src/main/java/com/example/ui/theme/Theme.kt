package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
data class GhestYarExtendedColors(
    val canvas: Color,
    val positive: Color,
    val positiveBg: Color,
    val positiveText: Color,
    val warning: Color,
    val warningBg: Color,
    val warningText: Color,
    val errorBg: Color
)

private val LightExtendedColors = GhestYarExtendedColors(
    canvas = VitalsLightCanvas,
    positive = VitalsLightTertiary,
    positiveBg = VitalsLightPositiveBg,
    positiveText = VitalsLightSecondary,
    warning = VitalsLightWarning,
    warningBg = VitalsLightWarningBg,
    warningText = VitalsLightWarningText,
    errorBg = VitalsLightErrorBg
)

private val DarkExtendedColors = GhestYarExtendedColors(
    canvas = VitalsDarkCanvas,
    positive = VitalsDarkTertiary,
    positiveBg = VitalsDarkPositiveBg,
    positiveText = VitalsDarkSecondary,
    warning = VitalsDarkWarning,
    warningBg = VitalsDarkWarningBg,
    warningText = VitalsDarkWarningText,
    errorBg = VitalsDarkErrorBg
)

val LocalGhestYarColors = staticCompositionLocalOf { LightExtendedColors }

private val VitalsLightColorScheme = lightColorScheme(
    primary = VitalsLightPrimary,
    onPrimary = VitalsLightOnPrimary,
    primaryContainer = VitalsLightPrimaryContainer,
    onPrimaryContainer = VitalsLightOnPrimaryContainer,
    secondary = VitalsLightSecondary,
    onSecondary = Color.White,
    tertiary = VitalsLightTertiary,
    onTertiary = Color.White,
    background = VitalsLightCanvas,
    onBackground = VitalsLightOnSurface,
    surface = VitalsLightBackground,
    onSurface = VitalsLightOnSurface,
    surfaceVariant = VitalsLightSurfaceContainer,
    onSurfaceVariant = VitalsLightOnSurfaceVariant,
    surfaceContainerLow = VitalsLightCanvas,
    surfaceContainer = VitalsLightSurfaceContainer,
    surfaceContainerHigh = VitalsLightSurfaceContainerHigh,
    outline = VitalsLightOutline,
    outlineVariant = VitalsLightOutlineVariant,
    error = VitalsLightError,
    onError = VitalsLightOnError,
    errorContainer = VitalsLightErrorBg,
    onErrorContainer = VitalsLightError
)

private val VitalsDarkColorScheme = darkColorScheme(
    primary = VitalsDarkPrimary,
    onPrimary = VitalsDarkOnPrimary,
    primaryContainer = VitalsDarkPrimaryContainer,
    onPrimaryContainer = VitalsDarkOnPrimaryContainer,
    secondary = VitalsDarkSecondary,
    onSecondary = VitalsDarkOnPrimary,
    tertiary = VitalsDarkTertiary,
    onTertiary = VitalsDarkOnPrimary,
    background = VitalsDarkCanvas,
    onBackground = VitalsDarkOnSurface,
    surface = VitalsDarkBackground,
    onSurface = VitalsDarkOnSurface,
    surfaceVariant = VitalsDarkSurfaceContainer,
    onSurfaceVariant = VitalsDarkOnSurfaceVariant,
    surfaceContainerLow = VitalsDarkCanvas,
    surfaceContainer = VitalsDarkSurfaceContainer,
    surfaceContainerHigh = VitalsDarkSurfaceContainerHigh,
    outline = VitalsDarkOutline,
    outlineVariant = VitalsDarkOutlineVariant,
    error = VitalsDarkError,
    onError = VitalsDarkOnError,
    errorContainer = VitalsDarkErrorBg,
    onErrorContainer = VitalsDarkError
)

private val GhestYarShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun GhestYarTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VitalsDarkColorScheme else VitalsLightColorScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalGhestYarColors provides extended,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = GhestYarShapes,
            content = content
        )
    }
}
