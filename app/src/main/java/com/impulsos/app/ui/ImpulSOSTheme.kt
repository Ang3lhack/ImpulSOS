package com.impulsos.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppAppearance(val label: String) {
    SYSTEM("Usar tema del sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
}

private val LightPalette = lightColorScheme(
    primary = Color(0xFF5553D7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECEBFF),
    onPrimaryContainer = Color(0xFF2E2B83),
    secondary = Color(0xFFBEF54D),
    onSecondary = Color(0xFF191A32),
    tertiaryContainer = Color(0xFFE5F7EC),
    onTertiaryContainer = Color(0xFF14563B),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF191A32),
    surface = Color.White,
    onSurface = Color(0xFF191A32),
    surfaceVariant = Color(0xFFE9E8F3),
    onSurfaceVariant = Color(0xFF52546C),
    error = Color(0xFFC62D42),
)

private val DarkPalette = darkColorScheme(
    primary = Color(0xFFC2C1FF),
    onPrimary = Color(0xFF26246C),
    primaryContainer = Color(0xFF363477),
    onPrimaryContainer = Color(0xFFF0EFFF),
    secondary = Color(0xFFBEF54D),
    onSecondary = Color(0xFF191A32),
    tertiaryContainer = Color(0xFF1C3B2E),
    onTertiaryContainer = Color(0xFFBFF3D3),
    background = Color(0xFF141425),
    onBackground = Color(0xFFF3F2FF),
    surface = Color(0xFF202036),
    onSurface = Color(0xFFF3F2FF),
    surfaceVariant = Color(0xFF303048),
    onSurfaceVariant = Color(0xFFC8C9E0),
    outline = Color(0xFF9393AE),
    error = Color(0xFFFFB2BC),
    onError = Color(0xFF5E1022),
)

@Composable
fun ImpulSOSTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkPalette else LightPalette, content = content)
}
