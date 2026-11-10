package com.impulsos.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Palette = lightColorScheme(
    primary = Color(0xFF5553D7),
    onPrimary = Color.White,
    secondary = Color(0xFFBEF54D),
    onSecondary = Color(0xFF191A32),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF191A32),
    surface = Color.White,
    onSurface = Color(0xFF191A32),
    error = Color(0xFFC62D42),
)

@Composable
fun ImpulSOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, content = content)
}
