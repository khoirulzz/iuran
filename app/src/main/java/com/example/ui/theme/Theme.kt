package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === GEMPALA IURAN — Light Mode ONLY (tidak mengikuti system dark mode) ===
private val GempalaLightColorScheme = lightColorScheme(
    primary           = AdminPrimary,
    onPrimary         = Color.White,
    primaryContainer  = AdminContainer,
    onPrimaryContainer = AdminDark,

    secondary         = OfficerPrimary,
    onSecondary       = Color.White,
    secondaryContainer = OfficerContainer,
    onSecondaryContainer = OfficerDark,

    tertiary          = AccentPurple,
    onTertiary        = Color.White,

    background        = AppBackground,
    onBackground      = TextPrimary,

    surface           = AppSurface,
    onSurface         = TextPrimary,
    onSurfaceVariant  = TextSecondary,

    error             = AccentDanger,
    onError           = Color.White,
    errorContainer    = Color(0xFFFFE4E6),
    onErrorContainer  = Color(0xFF7F1D1D),

    outline           = BorderColor,
    outlineVariant    = DisabledColor,
)

@Composable
fun MyApplicationTheme(
    // Parameter darkTheme dan dynamicColor sengaja diabaikan
    // agar tema selalu Terang (Light) sesuai spesifikasi Gempala Iuran
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Selalu gunakan GempalaLightColorScheme — TIDAK ada dark mode
    MaterialTheme(
        colorScheme = GempalaLightColorScheme,
        typography = Typography,
        content = content
    )
}
