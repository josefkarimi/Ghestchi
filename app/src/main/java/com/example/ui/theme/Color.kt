package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Vitals Light Palette
val VitalsLightBackground = Color(0xFFFFFFFF)
val VitalsLightCanvas = Color(0xFFF1F7F8)
val VitalsLightSurfaceContainer = Color(0xFFE3F1F3)
val VitalsLightSurfaceContainerHigh = Color(0xFFD5E9EC)
val VitalsLightOnSurface = Color(0xFF1B2B33)
val VitalsLightOnSurfaceVariant = Color(0xFF46585F)
val VitalsLightOutline = Color(0xFF7B9198)
val VitalsLightOutlineVariant = Color(0xFFD5E3E5)
val VitalsLightPrimary = Color(0xFF0F7C8A)
val VitalsLightOnPrimary = Color(0xFFFFFFFF)
val VitalsLightPrimaryContainer = Color(0xFFE3F1F3)
val VitalsLightOnPrimaryContainer = Color(0xFF0A5560)
val VitalsLightSecondary = Color(0xFF1D5E3A)
val VitalsLightTertiary = Color(0xFF2E7D4F)
val VitalsLightError = Color(0xFFB3261E)
val VitalsLightOnError = Color(0xFFFFFFFF)
val VitalsLightPositiveBg = Color(0xFFE4F3EA)
val VitalsLightWarning = Color(0xFFB86E00)
val VitalsLightWarningBg = Color(0xFFFEF3C7)
val VitalsLightWarningText = Color(0xFF854D0E)
val VitalsLightErrorBg = Color(0xFFFCE8E6)

// Vitals Dark Palette
val VitalsDarkBackground = Color(0xFF0F1A1F)
val VitalsDarkCanvas = Color(0xFF15242A)
val VitalsDarkSurfaceContainer = Color(0xFF1C2E35)
val VitalsDarkSurfaceContainerHigh = Color(0xFF243A42)
val VitalsDarkOnSurface = Color(0xFFE6EFF1)
val VitalsDarkOnSurfaceVariant = Color(0xFFA9BCC2)
val VitalsDarkOutline = Color(0xFF5E7880)
val VitalsDarkOutlineVariant = Color(0xFF2C4048)
val VitalsDarkPrimary = Color(0xFF4FB8C6)
val VitalsDarkOnPrimary = Color(0xFF0F1A1F)
val VitalsDarkPrimaryContainer = Color(0xFF18363E)
val VitalsDarkOnPrimaryContainer = Color(0xFFBFE6EB)
val VitalsDarkSecondary = Color(0xFF9FDCB7)
val VitalsDarkTertiary = Color(0xFF5CBF85)
val VitalsDarkError = Color(0xFFF2867C)
val VitalsDarkOnError = Color(0xFF0F1A1F)
val VitalsDarkPositiveBg = Color(0x295CBF85)
val VitalsDarkWarning = Color(0xFFE2A43B)
val VitalsDarkWarningBg = Color(0x29E2A43B)
val VitalsDarkWarningText = Color(0xFFF3C875)
val VitalsDarkErrorBg = Color(0x29F2867C)

// Swatch presets matching HTML app
val AccountColorPresets = listOf("#0f7c8a", "#0284c7", "#7c3aed", "#2e7d4f", "#b86e00", "#db2777")
val CategoryColorPresets = listOf("#0f7c8a", "#2e7d4f", "#f59e0b", "#3b82f6", "#ec4899", "#6366f1", "#e11d48", "#f97316")

fun parseHexColor(hex: String?, fallback: Color = VitalsLightPrimary): Color {
    if (hex.isNullOrBlank()) return fallback
    val cleaned = hex.trim().removePrefix("#")
    return try {
        when (cleaned.length) {
            6 -> Color(("FF$cleaned").toLong(16))
            8 -> Color(cleaned.toLong(16))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
