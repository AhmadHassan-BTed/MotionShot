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
 * - MODE (High-FPS Video Sensor vs High-Res Photo Sensor Toggle)
 * - CAM (Back Camera vs Front Selfie Camera Toggle)
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
    isHighFpsVideoMode: Boolean,
    isFrontCamera: Boolean,
    onFlashToggle: () -> Unit,
    onFocusLockToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onAwbToggle: () -> Unit,
    onSensorModeToggle: () -> Unit,
    onFrontCameraToggle: () -> Unit,
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

    val modeColor by animateColorAsState(
        targetValue = if (isHighFpsVideoMode) MotionBlue else Color(0xFFFF9800),
        animationSpec = tween(200),
        label = "modeColor",
    )
    val modeBg by animateColorAsState(
        targetValue = if (isHighFpsVideoMode) MotionBlue.copy(alpha = 0.25f) else Color(0x33FF9800),
        animationSpec = tween(200),
        label = "modeBg",
    )

    val camColor by animateColorAsState(
        targetValue = if (isFrontCamera) Color(0xFFE91E63) else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "camColor",
    )
    val camBg by animateColorAsState(
        targetValue = if (isFrontCamera) Color(0x33E91E63) else Color(0x22FFFFFF),
        animationSpec = tween(200),
        label = "camBg",
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Branding
        Text(
            text = "MOTIONSHOT",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )

        // Right Pro Controls Strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // SENSOR MODE Pill (Video High-FPS vs Photo Still Quality)
            ControlPill(
                text = if (isHighFpsVideoMode) "MODE VIDEO" else "MODE PHOTO",
                color = modeColor,
                backgroundColor = modeBg,
                onClick = onSensorModeToggle,
            )

            // FRONT / BACK CAMERA Pill
            ControlPill(
                text = if (isFrontCamera) "CAM FRONT" else "CAM BACK",
                color = camColor,
                backgroundColor = camBg,
                onClick = onFrontCameraToggle,
            )

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
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
        )
    }
}
