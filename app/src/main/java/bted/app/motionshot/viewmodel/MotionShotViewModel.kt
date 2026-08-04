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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * State holder enforcing Strict Time Window Execution.
 *
 * Guarantees capture loop hard-stops at exactly totalDurationMs (e.g. 1.0s),
 * preventing camera frame delivery from overrunning the selected timer.
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

    // ── Strict Time-Bounded Capture Engine ───────────────────────────────

    private fun startCapture() {
        clearRawFrames()

        val state = _uiState.value
        val count = state.captureCount
        val totalDurationMs = state.timerSeconds * 1000L

        val intervalStepMs = if (count > 1) totalDurationMs.toDouble() / (count - 1) else 0.0

        _uiState.update { it.copy(phase = CapturePhase.Recording, framesCaptured = 0) }

        captureJob = viewModelScope.launch(Dispatchers.Default) {
            val startTimeMs = SystemClock.elapsedRealtime()
            val endTimeMs = startTimeMs + totalDurationMs

            while (isActive && SystemClock.elapsedRealtime() < endTimeMs && capturedFrames.size < count) {
                val currentIndex = capturedFrames.size
                val targetTimeMs = startTimeMs + (currentIndex * intervalStepMs).toLong()
                val nowMs = SystemClock.elapsedRealtime()
                val waitTimeMs = targetTimeMs - nowMs

                if (waitTimeMs > 0) {
                    delay(waitTimeMs)
                }

                val remainingWindowMs = endTimeMs - SystemClock.elapsedRealtime()
                if (!isActive || remainingWindowMs <= 5L) break

                frameAnalyzer.shouldCapture.set(true)

                // Time-bounded receive: hard timeout if remaining timer window expires
                val frameResult = withTimeoutOrNull(remainingWindowMs) {
                    frameAnalyzer.frameChannel.receive()
                }

                if (frameResult != null) {
                    capturedFrames.add(frameResult)
                    _uiState.update { it.copy(framesCaptured = capturedFrames.size) }
                } else {
                    // Time window expired — stop immediately
                    break
                }
            }

            // Guaranteed exit at or before totalDurationMs
            finishCapture()
        }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        frameAnalyzer.shouldCapture.set(false)
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
                    config = EngineConfig(threshold = 45),
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
            val stale = frameAnalyzer.frameChannel.tryReceive().getOrNull() ?: break
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
        frameAnalyzer.shouldCapture.set(false)
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
