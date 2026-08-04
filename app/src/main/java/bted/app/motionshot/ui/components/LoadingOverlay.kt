package bted.app.motionshot.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bted.app.motionshot.ui.theme.MotionBlue

/**
 * Full-screen semi-transparent overlay shown during frame compositing.
 * Features concentric pulsing rings for a premium processing indicator.
 */
@Composable
fun LoadingOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")

    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val stroke = 2.5.dp.toPx()
                // Outer ring
                drawCircle(
                    color = MotionBlue,
                    radius = (size.minDimension / 2) * pulse,
                    alpha = alpha * 0.5f,
                    style = Stroke(width = stroke),
                )
                // Inner ring
                drawCircle(
                    color = MotionBlue,
                    radius = (size.minDimension / 3.2f) * pulse,
                    alpha = alpha,
                    style = Stroke(width = stroke),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Processing\u2026",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
            )
        }
    }
}
