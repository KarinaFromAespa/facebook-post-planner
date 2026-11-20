package com.facebookpostplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1877F2), // Facebook blue
    secondary = Color(0xFF42B72A),
    surface = Color(0xFFF0F2F5)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF1877F2),
    secondary = Color(0xFF42B72A)
)

@Composable
fun PlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
