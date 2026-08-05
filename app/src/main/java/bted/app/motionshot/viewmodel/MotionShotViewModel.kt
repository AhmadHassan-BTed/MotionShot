package bted.app.motionshot.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bted.app.motionshot.capture.FrameAnalyzer
import bted.app.motionshot.data.PreferencesRepository
import bted.app.motionshot.engine.EngineConfig
import bted.app.motionshot.engine.MotionEngine
import bted.app.motionshot.engine.MotionEngineFactory
import bted.app.motionshot.ui.state.CapturePhase
import bted.app.motionshot.ui.state.MotionShotUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * State holder enforcing Exact Time Duration and Target Frame Count Subsampling.
 *
 * Captures all hardware frames delivered during totalDurationMs and downsamples
 * evenly to deliver exactly captureCount frames spanning the selected timer window.
 */
class MotionShotViewModel @JvmOverloads constructor(
    application: Application,
    private val engine: MotionEngine = MotionEngineFactory.create(),
) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(
        MotionShotUiState(
            timerSeconds = prefsRepo.timerSeconds,
            captureCount = prefsRepo.captureCount,
            shutterSpeedNs = prefsRepo.shutterSpeedNs,
            isoValue = prefsRepo.isoValue,
        )
    )
    val uiState: StateFlow<MotionShotUiState> = _uiState.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _rawFrames = MutableStateFlow<List<Bitmap>>(emptyList())
    val rawFrames: StateFlow<List<Bitmap>> = _rawFrames.asStateFlow()

    private val _selectedFrameIndex = MutableStateFlow(0)
    val selectedFrameIndex: StateFlow<Int> = _selectedFrameIndex.asStateFlow()

    val frameAnalyzer = FrameAnalyzer()

    private val capturedFrames = mutableListOf<Bitmap>()
    private var captureJob: Job? = null

    fun setTimer(seconds: Int) {
        prefsRepo.timerSeconds = seconds
        _uiState.update { it.copy(timerSeconds = seconds) }
    }

    fun setCaptureCount(count: Int) {
        prefsRepo.captureCount = count
        _uiState.update { it.copy(captureCount = count) }
    }

    fun setShutterSpeed(shutterSpeedNs: Long) {
        prefsRepo.shutterSpeedNs = shutterSpeedNs
        _uiState.update { it.copy(shutterSpeedNs = shutterSpeedNs) }
    }

    fun setIsoValue(iso: Int) {
        prefsRepo.isoValue = iso
        _uiState.update { it.copy(isoValue = iso) }
    }

    fun setBrightnessBoost(boost: Float) {
        prefsRepo.brightnessBoost = boost
        _uiState.update { it.copy(brightnessBoost = boost) }
    }

    fun toggleFlash() {
        val newFlash = !_uiState.value.isFlashEnabled
        prefsRepo.isFlashEnabled = newFlash
        _uiState.update { it.copy(isFlashEnabled = newFlash) }
    }

    fun toggleFocusLock() {
        val newLock = !_uiState.value.isFocusLocked
        prefsRepo.isFocusLocked = newLock
        _uiState.update { it.copy(isFocusLocked = newLock) }
    }

    fun toggleGrid() {
        val newGrid = !_uiState.value.isGridEnabled
        prefsRepo.isGridEnabled = newGrid
        _uiState.update { it.copy(isGridEnabled = newGrid) }
    }

    fun toggleAwbLock() {
        val newAwb = !_uiState.value.isAwbLocked
        prefsRepo.isAwbLocked = newAwb
        _uiState.update { it.copy(isAwbLocked = newAwb) }
    }

    fun toggleSensorMode() {
        val newMode = !_uiState.value.isHighFpsVideoMode
        prefsRepo.isHighFpsVideoMode = newMode
        _uiState.update { it.copy(isHighFpsVideoMode = newMode) }
    }

    fun toggleFrontCamera() {
        val newCam = !_uiState.value.isFrontCamera
        prefsRepo.isFrontCamera = newCam
        _uiState.update { it.copy(isFrontCamera = newCam) }
    }

    fun setZoomRatio(zoom: Float) {
        _uiState.update { it.copy(zoomRatio = zoom.coerceIn(1.0f, 20.0f)) }
    }

    fun selectRawFrame(index: Int) {
        if (index in 0 until _rawFrames.value.size) {
            _selectedFrameIndex.value = index
        }
    }

    fun onCaptureToggle() {
        when (_uiState.value.phase) {
            CapturePhase.Idle -> startCapture()
            CapturePhase.Recording -> stopCapture()
            else -> { /* Ignore during Processing / Done */ }
        }
    }

    // ── Continuous High-Speed Capture & Uniform Subsampling ─────────────

    private fun startCapture() {
        clearRawFrames()

        val state = _uiState.value
        val targetCount = state.captureCount
        val totalDurationMs = state.timerSeconds * 1000L

        _uiState.update { it.copy(phase = CapturePhase.Recording, framesCaptured = 0) }

        captureJob = viewModelScope.launch(Dispatchers.Default) {
            val rawCollected = mutableListOf<Bitmap>()
            val startTimeMs = SystemClock.elapsedRealtime()
            val endTimeMs = startTimeMs + totalDurationMs

            // Start continuous high-FPS frame streaming
            frameAnalyzer.isStreaming.set(true)

            while (isActive && SystemClock.elapsedRealtime() < endTimeMs) {
                val remainingMs = endTimeMs - SystemClock.elapsedRealtime()
                if (remainingMs <= 2L) break

                val frame = withTimeoutOrNull(remainingMs) {
                    frameAnalyzer.frameChannel.receive()
                } ?: break

                rawCollected.add(frame)
                _uiState.update { it.copy(framesCaptured = rawCollected.size.coerceAtMost(targetCount)) }
            }

            // Drain any remaining buffered frames collected during the timer window
            while (isActive && rawCollected.size < targetCount) {
                val bufferedFrame = frameAnalyzer.frameChannel.tryReceive().getOrNull() ?: break
                rawCollected.add(bufferedFrame)
                _uiState.update { it.copy(framesCaptured = rawCollected.size.coerceAtMost(targetCount)) }
            }

            // Stop streaming immediately
            frameAnalyzer.isStreaming.set(false)
            drainChannel()

            if (!isActive || rawCollected.isEmpty()) {
                rawCollected.forEach { it.recycle() }
                _uiState.update { it.copy(phase = CapturePhase.Idle, framesCaptured = 0) }
                return@launch
            }

            // Subsample evenly to deliver EXACTLY targetCount frames across the timer window
            if (rawCollected.size > targetCount && targetCount > 1) {
                val step = (rawCollected.size - 1).toDouble() / (targetCount - 1)
                val selectedIndices = mutableSetOf<Int>()

                for (i in 0 until targetCount) {
                    val idx = (i * step).roundToInt().coerceIn(0, rawCollected.lastIndex)
                    selectedIndices.add(idx)
                    capturedFrames.add(rawCollected[idx])
                }

                // Recycle intermediate unselected frames
                rawCollected.forEachIndexed { idx, bitmap ->
                    if (idx !in selectedIndices) {
                        bitmap.recycle()
                    }
                }
            } else {
                capturedFrames.addAll(rawCollected)
            }

            finishCapture()
        }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        frameAnalyzer.isStreaming.set(false)
        drainChannel()

        if (capturedFrames.size >= 2) {
            finishCapture()
        } else {
            clearRawFrames()
            _uiState.update { it.copy(phase = CapturePhase.Idle, framesCaptured = 0) }
        }
    }

    private fun finishCapture() {
        _uiState.update { it.copy(phase = CapturePhase.Processing) }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val frameCopies = capturedFrames.map { it.copy(Bitmap.Config.ARGB_8888, false) }
                _rawFrames.value = frameCopies
                _selectedFrameIndex.value = 0

                val compositeResult = engine.process(
                    frames = capturedFrames,
                    config = EngineConfig(
                        threshold = 45,
                        brightnessBoost = _uiState.value.brightnessBoost,
                    ),
                )

                _previewBitmap.value = compositeResult
                _uiState.update { it.copy(phase = CapturePhase.Done) }
            } catch (e: Exception) {
                _uiState.update { it.copy(phase = CapturePhase.Idle, framesCaptured = 0) }
            } finally {
                capturedFrames.forEach { it.recycle() }
                capturedFrames.clear()
            }
        }
    }

    private fun drainChannel() {
        while (true) {
            val stale: Bitmap = frameAnalyzer.frameChannel.tryReceive().getOrNull() ?: break
            stale.recycle()
        }
    }

    private fun clearRawFrames() {
        _rawFrames.value.forEach { it.recycle() }
        _rawFrames.value = emptyList()
        _selectedFrameIndex.value = 0
        capturedFrames.forEach { it.recycle() }
        capturedFrames.clear()
    }

    /** Discards all captured data and returns to idle. */
    fun resetCapture() {
        captureJob?.cancel()
        captureJob = null
        frameAnalyzer.isStreaming.set(false)
        drainChannel()
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
        clearRawFrames()
        _uiState.update { MotionShotUiState(
            timerSeconds = prefsRepo.timerSeconds,
            captureCount = prefsRepo.captureCount,
            shutterSpeedNs = prefsRepo.shutterSpeedNs,
            isoValue = prefsRepo.isoValue,
        ) }
    }

    override fun onCleared() {
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
        clearRawFrames()
        super.onCleared()
    }
}
