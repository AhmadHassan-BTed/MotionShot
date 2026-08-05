package bted.app.motionshot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bted.app.motionshot.ui.theme.MotionBlue

/**
 * Translucent top overlay bar with faded pro camera controls:
 * - GRID (Rule-of-Thirds Grid Toggle)
 * - AWB (Auto White Balance Lock Toggle)
 * - AF (Auto Focus Lock Toggle)
 * - FLASH (Hardware Flash Torch Toggle)
 * Zero emojis.
 */
@Composable
fun TopControlBar(
    isFlashEnabled: Boolean,
    isFocusLocked: Boolean,
    isGridEnabled: Boolean,
    isAwbLocked: Boolean,
    onFlashToggle: () -> Unit,
    onFocusLockToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onAwbToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flashColor by animateColorAsState(
        targetValue = if (isFlashEnabled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "flashColor",
    )
    val flashBg by animateColorAsState(
        targetValue = if (isFlashEnabled) Color(0x33FFD700) else Color(0x22FFFFFF),
        animationSpec = tween(200),
        label = "flashBg",
    )

    val focusColor by animateColorAsState(
        targetValue = if (isFocusLocked) MotionBlue else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "focusColor",
    )
    val focusBg by animateColorAsState(
        targetValue = if (isFocusLocked) MotionBlue.copy(alpha = 0.25f) else Color(0x22FFFFFF),
        animationSpec = tween(200),
        label = "focusBg",
    )

    val gridColor by animateColorAsState(
        targetValue = if (isGridEnabled) Color.White else Color.White.copy(alpha = 0.40f),
        animationSpec = tween(200),
        label = "gridColor",
    )
    val gridBg by animateColorAsState(
        targetValue = if (isGridEnabled) Color(0x33FFFFFF) else Color(0x15FFFFFF),
        animationSpec = tween(200),
        label = "gridBg",
    )

    val awbColor by animateColorAsState(
        targetValue = if (isAwbLocked) MotionBlue else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "awbColor",
    )
    val awbBg by animateColorAsState(
        targetValue = if (isAwbLocked) MotionBlue.copy(alpha = 0.25f) else Color(0x22FFFFFF),
        animationSpec = tween(200),
        label = "awbBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Branding
        Text(
            text = "MOTIONSHOT",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )

        // Right Pro Controls Strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // GRID Pill
            ControlPill(
                text = if (isGridEnabled) "GRID 3x3" else "GRID OFF",
                color = gridColor,
                backgroundColor = gridBg,
                onClick = onGridToggle,
            )

            // AWB Lock Pill
            ControlPill(
                text = if (isAwbLocked) "WB LOCK" else "WB AUTO",
                color = awbColor,
                backgroundColor = awbBg,
                onClick = onAwbToggle,
            )

            // AF Lock Pill
            ControlPill(
                text = if (isFocusLocked) "AF LOCK" else "AF AUTO",
                color = focusColor,
                backgroundColor = focusBg,
                onClick = onFocusLockToggle,
            )

            // Flash Torch Pill
            ControlPill(
                text = if (isFlashEnabled) "FLASH ON" else "FLASH OFF",
                color = flashColor,
                backgroundColor = flashBg,
                onClick = onFlashToggle,
            )
        }
    }
}

@Composable
private fun ControlPill(
    text: String,
    color: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}
