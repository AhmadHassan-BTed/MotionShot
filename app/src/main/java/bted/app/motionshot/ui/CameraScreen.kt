@file:Suppress("MagicNumber")

package bted.app.motionshot.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bted.app.motionshot.engine.PipelineMode
import bted.app.motionshot.ui.components.CaptureControls
import bted.app.motionshot.ui.components.GridOverlay
import bted.app.motionshot.ui.components.LoadingOverlay
import bted.app.motionshot.ui.components.TopControlBar
import bted.app.motionshot.ui.state.CapturePhase
import bted.app.motionshot.ui.state.MotionShotUiState
import bted.app.motionshot.ui.theme.MotionBlue
import bted.app.motionshot.ui.theme.MotionPanel
import bted.app.motionshot.ui.theme.MotionPanelText
import bted.app.motionshot.ui.theme.MotionPanelTextMuted
import bted.app.motionshot.ui.theme.MotionSurface
import bted.app.motionshot.ui.theme.MotionTextMuted
import bted.app.motionshot.ui.theme.MotionTextPrimary
import bted.app.motionshot.viewmodel.MotionShotViewModel
import kotlinx.coroutines.delay

private val PanelShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/**
 * Root screen supporting Step 1: Raw Frame Inspection Gallery & Fast Slide Auto Preview.
 */
@Composable
fun CameraScreen(
    viewModel: MotionShotViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
    val rawFrames by viewModel.rawFrames.collectAsStateWithLifecycle()
    val selectedFrameIndex by viewModel.selectedFrameIndex.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        PermissionPlaceholder(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = modifier,
        )
        return
    }

    when (state.phase) {
        CapturePhase.Done -> Step1ResultView(
            compositeBitmap = previewBitmap,
            rawFrames = rawFrames,
            selectedIndex = selectedFrameIndex,
            onSelectFrame = viewModel::selectRawFrame,
            state = state,
            onRetake = viewModel::resetCapture,
            modifier = modifier,
        )
        else -> CameraContent(
            viewModel = viewModel,
            state = state,
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraContent(
    viewModel: MotionShotViewModel,
    state: MotionShotUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        CameraPreview(
            analyzer = viewModel.frameAnalyzer,
            shutterSpeedNs = state.shutterSpeedNs,
            isoValue = state.isoValue,
            brightnessBoost = state.brightnessBoost,
            isFlashEnabled = state.isFlashEnabled,
            isFocusLocked = state.isFocusLocked,
            isAwbLocked = state.isAwbLocked,
            onZoomChanged = viewModel::setZoomRatio,
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isGridEnabled) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }

        TopControlBar(
            isFlashEnabled = state.isFlashEnabled,
            isFocusLocked = state.isFocusLocked,
            isGridEnabled = state.isGridEnabled,
            isAwbLocked = state.isAwbLocked,
            onFlashToggle = viewModel::toggleFlash,
            onFocusLockToggle = viewModel::toggleFocusLock,
            onGridToggle = viewModel::toggleGrid,
            onAwbToggle = viewModel::toggleAwbLock,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shape = PanelShape,
            color = MotionPanel,
            shadowElevation = 8.dp,
        ) {
            CaptureControls(
                state = state,
                onTimerSelected = viewModel::setTimer,
                onFrameCountSelected = viewModel::setCaptureCount,
                onShutterSpeedSelected = viewModel::setShutterSpeed,
                onIsoSelected = viewModel::setIsoValue,
                onBrightnessSelected = viewModel::setBrightnessBoost,
                onCaptureToggle = viewModel::onCaptureToggle,
                modifier = Modifier.navigationBarsPadding(),
            )
        }

        AnimatedVisibility(
            visible = state.phase == CapturePhase.Processing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LoadingOverlay()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 Result View: Fast Slide & Auto-Play Flipbook Motion Preview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Step1ResultView(
    compositeBitmap: Bitmap?,
    rawFrames: List<Bitmap>,
    selectedIndex: Int,
    onSelectFrame: (Int) -> Unit,
    state: MotionShotUiState,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewMode by remember { mutableStateOf(PipelineMode.RAW_FRAME_GALLERY) }
    var isPlayingFlipbook by remember { mutableStateOf(false) }

    // Auto-Play Flipbook Animation Loop (15 FPS motion playback)
    LaunchedEffect(isPlayingFlipbook, rawFrames.size) {
        if (!isPlayingFlipbook || rawFrames.isEmpty()) return@LaunchedEffect
        while (isPlayingFlipbook) {
            delay(66) // ~15 FPS
            val nextIdx = (selectedIndex + 1) % rawFrames.size
            onSelectFrame(nextIdx)
        }
    }

    val displayedBitmap = when (viewMode) {
        PipelineMode.RAW_FRAME_GALLERY -> rawFrames.getOrNull(selectedIndex) ?: compositeBitmap
        PipelineMode.COMPOSITE -> compositeBitmap ?: rawFrames.getOrNull(selectedIndex)
    }

    val thumbnailListState = rememberLazyListState()

    // Keep active thumbnail scrolled into view
    LaunchedEffect(selectedIndex) {
        if (rawFrames.isNotEmpty()) {
            thumbnailListState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MotionSurface),
    ) {
        // 1. Large Main View with Horizontal Drag Gesture for Fast Slide
        displayedBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Displayed frame",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(rawFrames.size) {
                        if (viewMode == PipelineMode.RAW_FRAME_GALLERY && rawFrames.isNotEmpty()) {
                            var dragAccumulator = 0f
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                isPlayingFlipbook = false
                                dragAccumulator += dragAmount
                                if (dragAccumulator < -40f) { // Drag left -> next frame
                                    onSelectFrame((selectedIndex + 1).coerceAtMost(rawFrames.lastIndex))
                                    dragAccumulator = 0f
                                } else if (dragAccumulator > 40f) { // Drag right -> prev frame
                                    onSelectFrame((selectedIndex - 1).coerceAtLeast(0))
                                    dragAccumulator = 0f
                                }
                            }
                        }
                    },
                contentScale = ContentScale.Fit,
            )
        }

        // 2. Top Mode Selector & Auto-Play Controls
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xCC1A1A1E),
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ModeTabChip(
                    title = "Raw Frames (${rawFrames.size})",
                    selected = viewMode == PipelineMode.RAW_FRAME_GALLERY,
                    onClick = {
                        viewMode = PipelineMode.RAW_FRAME_GALLERY
                    },
                )
                ModeTabChip(
                    title = "Composite",
                    selected = viewMode == PipelineMode.COMPOSITE,
                    onClick = {
                        isPlayingFlipbook = false
                        viewMode = PipelineMode.COMPOSITE
                    },
                )

                if (viewMode == PipelineMode.RAW_FRAME_GALLERY && rawFrames.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        onClick = { isPlayingFlipbook = !isPlayingFlipbook },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPlayingFlipbook) MotionBlue else Color(0xFF2A2A2E),
                    ) {
                        Text(
                            text = if (isPlayingFlipbook) "⏸ Pause" else "▶ Play",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        // 3. Bottom Panel with Fast Thumbnail Strip & Retake Button
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shape = PanelShape,
            color = MotionPanel,
            shadowElevation = 8.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 16.dp, bottom = 16.dp),
            ) {
                if (viewMode == PipelineMode.RAW_FRAME_GALLERY && rawFrames.isNotEmpty()) {
                    Text(
                        text = "FRAME ${selectedIndex + 1} OF ${rawFrames.size} (SLIDE TO MOTION PREVIEW)",
                        color = MotionPanelTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        state = thumbnailListState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(rawFrames) { idx, frameBmp ->
                            RawThumbnailItem(
                                bitmap = frameBmp,
                                index = idx,
                                isSelected = idx == selectedIndex,
                                onClick = {
                                    isPlayingFlipbook = false
                                    onSelectFrame(idx)
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Surface(
                    onClick = onRetake,
                    shape = RoundedCornerShape(24.dp),
                    color = MotionPanelText,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Retake",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTabChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MotionBlue else Color.Transparent,
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else MotionTextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun RawThumbnailItem(
    bitmap: Bitmap,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MotionBlue else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 74.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                .background(Color.Black),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Frame ${index + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color(0xCC000000), CircleShape),
            ) {
                Text(
                    text = "${index + 1}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PermissionPlaceholder(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MotionSurface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Camera access is required",
                color = MotionTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Grant permission to start capturing",
                color = MotionTextMuted,
                fontSize = 13.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onRequestPermission) {
                Text(
                    text = "Grant Permission",
                    color = MotionBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
