package bted.app.motionshot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 3x3 Rule-of-Thirds Grid Overlay.
 * Translucent thin lines with crosshair guidance to assist motion framing.
 */
@Composable
fun GridOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val gridColor = Color.White.copy(alpha = 0.18f)
        val strokeWidth = 1.dp.toPx()

        // Vertical Rule-of-Thirds Lines
        val oneThirdX = width / 3f
        val twoThirdsX = (width * 2f) / 3f

        drawLine(
            color = gridColor,
            start = Offset(oneThirdX, 0f),
            end = Offset(oneThirdX, height),
            strokeWidth = strokeWidth,
        )

        drawLine(
            color = gridColor,
            start = Offset(twoThirdsX, 0f),
            end = Offset(twoThirdsX, height),
            strokeWidth = strokeWidth,
        )

        // Horizontal Rule-of-Thirds Lines
        val oneThirdY = height / 3f
        val twoThirdsY = (height * 2f) / 3f

        drawLine(
            color = gridColor,
            start = Offset(0f, oneThirdY),
            end = Offset(width, oneThirdY),
            strokeWidth = strokeWidth,
        )

        drawLine(
            color = gridColor,
            start = Offset(0f, twoThirdsY),
            end = Offset(width, twoThirdsY),
            strokeWidth = strokeWidth,
        )
    }
}
