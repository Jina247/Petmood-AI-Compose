package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryButtonYellow,
    onPrimary = TextPrimaryDarkBrown,
    secondary = MoodHappyBlue,
    onSecondary = TextPrimaryDarkBrown,
    background = WarmCreamBackground,
    onBackground = TextPrimaryDarkBrown,
    surface = CardSurfaceCream,
    onSurface = TextPrimaryDarkBrown,
    surfaceVariant = Color(0xFFFBE4C5), // Slightly darker helper shade
    onSurfaceVariant = TextPrimaryDarkBrown
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Light mode only required for v1
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
