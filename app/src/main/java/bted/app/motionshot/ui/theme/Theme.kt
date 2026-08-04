package bted.app.motionshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MotionBlue,
    onPrimary = Color.White,
    secondary = MotionBlueDim,
    onSecondary = Color.White,
    surface = MotionSurface,
    onSurface = MotionTextPrimary,
    surfaceVariant = MotionSurfaceVariant,
    onSurfaceVariant = MotionTextMuted,
    background = MotionSurface,
    onBackground = MotionTextPrimary,
)

/**
 * Always-dark theme for the camera UI.
 * No dynamic color — consistent branding across all devices.
 */
@Composable
fun MotionShotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}