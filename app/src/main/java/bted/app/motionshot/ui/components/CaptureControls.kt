@file:Suppress("MagicNumber")

package bted.app.motionshot.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bted.app.motionshot.ui.state.CameraParameterUtils
import bted.app.motionshot.ui.state.CapturePhase
import bted.app.motionshot.ui.state.MotionShotUiState
import bted.app.motionshot.ui.theme.MotionBlue
import bted.app.motionshot.ui.theme.MotionPanelRing
import bted.app.motionshot.ui.theme.MotionPanelText
import bted.app.motionshot.ui.theme.MotionPanelTextMuted

// ─────────────────────────────────────────────────────────────────────────────
// Public: ultra-compact capture controls with Continuous Shutter & ISO Sliders
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CaptureControls(
    state: MotionShotUiState,
    onTimerSelected: (Int) -> Unit,
    onFrameCountSelected: (Int) -> Unit,
    onShutterSpeedSelected: (Long) -> Unit,
    onIsoSelected: (Int) -> Unit,
    onCaptureToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIdle = state.phase == CapturePhase.Idle

    var showCustomTimerDialog by remember { mutableStateOf(false) }
    var showCustomFrameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Two-column selector strip (TIMER / FRAMES) ───────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectorColumn(
                label = "TIMER",
                presetOptions = listOf(3, 5, 10),
                selected = state.timerSeconds,
                onSelected = onTimerSelected,
                onCustomClick = { if (isIdle) showCustomTimerDialog = true },
                suffix = "s",
                enabled = isIdle,
                modifier = Modifier.weight(1f),
            )
            SelectorColumn(
                label = "FRAMES",
                presetOptions = listOf(5, 10, 20),
                selected = state.captureCount,
                onSelected = onFrameCountSelected,
                onCustomClick = { if (isIdle) showCustomFrameDialog = true },
                suffix = "",
                enabled = isIdle,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── Continuous Shutter Speed Slider ─────────────────────────────
        ContinuousSliderRow(
            label = "SHUTTER",
            valueText = CameraParameterUtils.getShutterLabel(state.shutterSpeedNs),
            progress = CameraParameterUtils.shutterNsToProgress(state.shutterSpeedNs),
            onProgressChanged = { p ->
                val newNs = CameraParameterUtils.shutterProgressToNs(p)
                onShutterSpeedSelected(newNs)
            },
            enabled = isIdle,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Continuous ISO Slider ────────────────────────────────────────
        ContinuousSliderRow(
            label = "ISO",
            valueText = CameraParameterUtils.getIsoLabel(state.isoValue),
            progress = CameraParameterUtils.isoValueToProgress(state.isoValue),
            onProgressChanged = { p ->
                val newIso = CameraParameterUtils.isoProgressToValue(p)
                onIsoSelected(newIso)
            },
            enabled = isIdle,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── Capture button ───────────────────────────────────────────────
        CaptureButton(
            isRecording = state.phase == CapturePhase.Recording,
            progress = if (state.captureCount > 0) {
                state.framesCaptured.toFloat() / state.captureCount
            } else 0f,
            enabled = isIdle || state.phase == CapturePhase.Recording,
            onClick = onCaptureToggle,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Status ───────────────────────────────────────────────────────
        StatusLabel(state = state)
    }

    // ── Custom Timer Dialog ──────────────────────────────────────────────
    if (showCustomTimerDialog) {
        CustomInputDialog(
            title = "Custom Timer (seconds)",
            initialValue = state.timerSeconds,
            range = 1..60,
            onDismiss = { showCustomTimerDialog = false },
            onConfirm = { customValue ->
                onTimerSelected(customValue)
                showCustomTimerDialog = false
            },
        )
    }

    // ── Custom Frame Count Dialog ────────────────────────────────────────
    if (showCustomFrameDialog) {
        CustomInputDialog(
            title = "Custom Frame Count",
            initialValue = state.captureCount,
            range = 2..50,
            onDismiss = { showCustomFrameDialog = false },
            onConfirm = { customValue ->
                onFrameCountSelected(customValue)
                showCustomFrameDialog = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Continuous Slider Row (Shutter / ISO)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContinuousSliderRow(
    label: String,
    valueText: String,
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    enabled: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "$label: ",
                color = MotionPanelTextMuted.copy(alpha = 0.55f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = valueText,
                color = MotionBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Slider(
            value = progress,
            onValueChange = { p -> if (enabled) onProgressChanged(p) },
            valueRange = 0f..1f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MotionBlue,
                activeTrackColor = MotionBlue,
                inactiveTrackColor = MotionPanelRing,
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(24.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Selector column (label + horizontal text items including Custom button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectorColumn(
    label: String,
    presetOptions: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    onCustomClick: () -> Unit,
    suffix: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val isCustomSelected = selected !in presetOptions

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MotionPanelTextMuted.copy(alpha = 0.55f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            presetOptions.forEach { value ->
                val isSelected = value == selected
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MotionPanelText else MotionPanelTextMuted,
                    animationSpec = tween(150),
                    label = "sel",
                )

                Text(
                    text = "$value$suffix",
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { if (enabled) onSelected(value) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }

            val customTextColor by animateColorAsState(
                targetValue = if (isCustomSelected) MotionBlue else MotionPanelTextMuted,
                animationSpec = tween(150),
                label = "customSel",
            )

            val customLabel = if (isCustomSelected) "$selected$suffix" else "Custom"

            Text(
                text = customLabel,
                color = customTextColor,
                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (enabled) onCustomClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Custom numeric input dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomInputDialog(
    title: String,
    initialValue: Int,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var textValue by remember { mutableStateOf(initialValue.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MotionPanelText,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input.filter { it.isDigit() }
                        errorMessage = null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionBlue,
                        unfocusedBorderColor = MotionPanelRing,
                        focusedTextColor = MotionPanelText,
                        unfocusedTextColor = MotionPanelText,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = err,
                        color = Color.Red,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = textValue.toIntOrNull()
                    if (parsed != null && parsed in range) {
                        onConfirm(parsed)
                    } else {
                        errorMessage = "Enter a value between ${range.first} and ${range.last}"
                    }
                },
            ) {
                Text(
                    text = "Set",
                    color = MotionBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = MotionPanelTextMuted,
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Capture button — tap to start/stop, with progress ring
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CaptureButton(
    isRecording: Boolean,
    progress: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "scale",
    )

    val innerColor by animateColorAsState(
        targetValue = if (isRecording) MotionBlue else Color(0xFF1A1A1E),
        animationSpec = tween(200),
        label = "innerColor",
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(200),
        label = "progress",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onClick()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.5.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f

            drawCircle(
                color = if (isRecording) MotionBlue.copy(alpha = 0.25f) else MotionPanelRing,
                radius = radius,
                style = Stroke(width = strokeWidth),
            )

            if (animatedProgress > 0f) {
                val arcSize = Size(
                    size.width - strokeWidth,
                    size.height - strokeWidth,
                )
                drawArc(
                    color = MotionBlue,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            drawCircle(
                color = innerColor,
                radius = radius * 0.72f,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusLabel(state: MotionShotUiState) {
    val text = when (state.phase) {
        CapturePhase.Idle -> "Tap to start"
        CapturePhase.Recording -> "${state.framesCaptured} / ${state.captureCount}"
        CapturePhase.Processing -> "Compositing\u2026"
        CapturePhase.Done -> "Done"
    }

    Text(
        text = text,
        color = MotionPanelTextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}
