package bted.app.motionshot.ui

import android.hardware.camera2.CaptureRequest
import android.util.Size
import android.view.ViewGroup
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import bted.app.motionshot.capture.FrameAnalyzer
import bted.app.motionshot.ui.theme.MotionBlue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Full-bleed CameraX preview with persistent executor lifecycle and robust mode toggling.
 */
@Composable
fun CameraPreview(
    analyzer: FrameAnalyzer,
    shutterSpeedNs: Long,
    isoValue: Int,
    brightnessBoost: Float,
    isFlashEnabled: Boolean,
    isFocusLocked: Boolean,
    isAwbLocked: Boolean,
    isHighFpsVideoMode: Boolean,
    isFrontCamera: Boolean,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    val analysisExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    // Cleanly shutdown analysisExecutor ONLY when CameraPreview composable is destroyed
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    var activeCamera2Control by remember { mutableStateOf<Camera2CameraControl?>(null) }
    var boundCameraInstance by remember { mutableStateOf<Camera?>(null) }
    var zoomRatioState by remember { mutableStateOf(1.0f) }

    // Tap-to-Focus Ring animation states
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    val focusRingScale = remember { Animatable(1.5f) }
    val focusRingAlpha = remember { Animatable(1.0f) }

    // Trigger focus ring animation when tapped
    LaunchedEffect(tapPoint) {
        val point = tapPoint ?: return@LaunchedEffect
        focusRingScale.snapTo(1.5f)
        focusRingAlpha.snapTo(1.0f)

        focusRingScale.animateTo(1.0f, tween(250))
        focusRingAlpha.animateTo(0.0f, tween(600))
        tapPoint = null
    }

    // Torch / Flash Control via CameraControl
    LaunchedEffect(isFlashEnabled, isFrontCamera, boundCameraInstance) {
        val camera = boundCameraInstance ?: return@LaunchedEffect
        if (!isFrontCamera) {
            try {
                camera.cameraControl.enableTorch(isFlashEnabled)
            } catch (_: Exception) {
            }
        }
    }

    // Dynamic Hardware Parameter Updates
    LaunchedEffect(shutterSpeedNs, isoValue, brightnessBoost, isFlashEnabled, isFocusLocked, isAwbLocked, isFrontCamera, activeCamera2Control) {
        val camera2Control = activeCamera2Control ?: return@LaunchedEffect
        val builder = CaptureRequestOptions.Builder()

        val isShutterManual = shutterSpeedNs > 0L
        val isIsoManual = isoValue > 0

        // Flash Torch via Camera2 CaptureRequest
        if (isFlashEnabled && !isFrontCamera) {
            builder.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
        } else {
            builder.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
        }

        // Enable Optical Image Stabilization (OIS) in hardware
        builder.setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON,
        )

        // White Balance (AWB) Lock vs Auto
        if (isAwbLocked) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
        } else {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, false)
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }

        // Focus Mode: Locked AF vs Continuous Picture AF
        if (isFocusLocked) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
        } else if (!isShutterManual && !isIsoManual) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        }

        // Apply Exposure Compensation EV steps
        val evSteps = ((brightnessBoost - 1.0f) * 3f).roundToInt()
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evSteps)

        // Manual Shutter / ISO vs Auto Exposure
        if (isShutterManual || isIsoManual) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

            if (isShutterManual) {
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedNs)
                val minFrameDurationNs = shutterSpeedNs.coerceAtLeast(8_333_333L)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, minFrameDurationNs)
            }

            // Calculate Auto ISO Sensitivity boost when shutter speed is manual
            val effectiveIso = if (isIsoManual) {
                (isoValue * brightnessBoost).roundToInt().coerceIn(100, 102_400)
            } else if (isShutterManual) {
                val baseIso = 400f * (2_000_000L.toFloat() / shutterSpeedNs.coerceAtLeast(100_000L))
                (baseIso * brightnessBoost).roundToInt().coerceIn(200, 51_200)
            } else {
                (200f * brightnessBoost).roundToInt().coerceIn(100, 25_600)
            }

            builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, effectiveIso)
        } else {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                android.util.Range(30, 60),
            )
        }

        try {
            camera2Control.setCaptureRequestOptions(builder.build())
        } catch (_: Exception) {
        }
    }

    DisposableEffect(lifecycleOwner, isHighFpsVideoMode, isFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val targetResolution = if (isHighFpsVideoMode) Size(1280, 720) else Size(1920, 1080)
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            targetResolution,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()

                val targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetRotation(targetRotation)
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setImageQueueDepth(4)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                )

                boundCameraInstance = boundCamera
                activeCamera2Control = Camera2CameraControl.from(boundCamera.cameraControl)
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
            boundCameraInstance = null
            activeCamera2Control = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(boundCameraInstance) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val camera = boundCameraInstance ?: return@detectTransformGestures
                    if (zoomChange != 1.0f) {
                        val maxSupported = camera.cameraInfo.zoomState.value?.maxZoomRatio?.coerceAtMost(20.0f) ?: 20.0f
                        zoomRatioState = (zoomRatioState * zoomChange).coerceIn(1.0f, maxSupported)
                        camera.cameraControl.setZoomRatio(zoomRatioState)
                        onZoomChanged(zoomRatioState)
                    }
                }
            }
            .pointerInput(boundCameraInstance) {
                detectTapGestures { offset ->
                    val camera = boundCameraInstance ?: return@detectTapGestures
                    tapPoint = offset

                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(offset.x, offset.y)
                    val action = FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                    )
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()

                    try {
                        camera.cameraControl.startFocusAndMetering(action)
                    } catch (_: Exception) {
                    }
                }
            },
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // Animated Tap-to-Focus Ring Overlay
        tapPoint?.let { point ->
            val ringSize = 64.dp
            val scale = focusRingScale.value
            val alpha = focusRingAlpha.value

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (point.x - (ringSize.toPx() / 2)).roundToInt(),
                            (point.y - (ringSize.toPx() / 2)).roundToInt(),
                        )
                    }
                    .size(ringSize),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val radius = (size.minDimension / 2f) * scale
                    drawCircle(
                        color = MotionBlue.copy(alpha = alpha.coerceIn(0f, 1f)),
                        radius = radius,
                        style = Stroke(width = strokeWidth),
                    )
                }
            }
        }

        // Zoom Level Indicator Pill Overlay (1.0x to 20.0x)
        if (zoomRatioState > 1.05f) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp),
            ) {
                Text(
                    text = String.format("%.1fx", zoomRatioState),
                    color = MotionBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
