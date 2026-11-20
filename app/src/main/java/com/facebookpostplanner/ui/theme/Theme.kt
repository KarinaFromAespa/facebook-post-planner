package com.facebookpostplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Facebook blue is the brand color throughout. The old green secondary is
// gone — green now means only "published" (a success state, never the brand).
private val FacebookBlue = Color(0xFF1877F2)
private val BlueTint = Color(0xFFD6E4FF)
private val BlueTintDark = Color(0xFF0A2E5C)
private val Slate = Color(0xFF546E7A)
private val SlateTint = Color(0xFFD9E4EC)
private val SlateTintDark = Color(0xFF1B2A33)

private val LightColors = lightColorScheme(
    primary = FacebookBlue,
    onPrimary = Color.White,
    primaryContainer = BlueTint,
    onPrimaryContainer = BlueTintDark,
    secondary = Slate,
    onSecondary = Color.White,
    secondaryContainer = SlateTint,
    onSecondaryContainer = SlateTintDark,
    tertiary = Color(0xFF42A5F5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6ECFF),
    onTertiaryContainer = BlueTintDark,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF1C1E21),
    surface = Color.White,
    onSurface = Color(0xFF1C1E21),
    surfaceVariant = Color(0xFFE4E6EB),
    onSurfaceVariant = Color(0xFF606770),
    outline = Color(0xFFCCD0D5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EA8FE),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF00458F),
    onPrimaryContainer = BlueTint,
    secondary = Color(0xFFA8B8C4),
    onSecondary = SlateTintDark,
    secondaryContainer = Color(0xFF33454F),
    onSecondaryContainer = SlateTint,
    tertiary = Color(0xFF7BC6FA),
    onTertiary = Color(0xFF00315C),
    tertiaryContainer = Color(0xFF00458F),
    onTertiaryContainer = Color(0xFFD6ECFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF18191A),
    onBackground = Color(0xFFE4E6EB),
    surface = Color(0xFF242526),
    onSurface = Color(0xFFE4E6EB),
    surfaceVariant = Color(0xFF3A3B3C),
    onSurfaceVariant = Color(0xFFB0B3B8),
    outline = Color(0xFF4E4F50)
)

@Composable
fun PlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
