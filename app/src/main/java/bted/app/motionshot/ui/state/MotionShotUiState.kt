package bted.app.motionshot.ui.state

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Continuous Shutter Speed and ISO parameter mappings.
 */
object CameraParameterUtils {

    // ── Continuous Shutter Speed Mapping (1/30s to 1/8000s) ─────────────
    private const val MIN_EXPOSURE_NS = 125_000L      // 1/8000s
    private const val MAX_EXPOSURE_NS = 33_333_333L   // 1/30s

    fun shutterProgressToNs(progress: Float): Long {
        if (progress <= 0.02f) return 0L // 0 = Auto Shutter
        val p = ((progress - 0.02f) / 0.98f).coerceIn(0f, 1f)
        val logMin = ln(MIN_EXPOSURE_NS.toDouble())
        val logMax = ln(MAX_EXPOSURE_NS.toDouble())
        val value = exp(logMax - p * (logMax - logMin))
        return value.toLong()
    }

    fun shutterNsToProgress(exposureNs: Long): Float {
        if (exposureNs <= 0L) return 0f
        val logMin = ln(MIN_EXPOSURE_NS.toDouble())
        val logMax = ln(MAX_EXPOSURE_NS.toDouble())
        val logVal = ln(exposureNs.toDouble().coerceIn(MIN_EXPOSURE_NS.toDouble(), MAX_EXPOSURE_NS.toDouble()))
        val p = ((logMax - logVal) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)
        return 0.02f + p * 0.98f
    }

    fun getShutterLabel(exposureNs: Long): String {
        if (exposureNs <= 0L) return "Auto"
        val denominator = (1_000_000_000L / exposureNs).toInt()
        return "1/${denominator}s"
    }

    // ── Ultra-Extended ISO Mapping (ISO 50 to ISO 102400 Boost) ────────
    private const val MIN_ISO = 50
    private const val MAX_ISO = 102400

    fun isoProgressToValue(progress: Float): Float {
        return progress.coerceIn(0f, 1f)
    }

    fun isoProgressToActualIso(progress: Float): Int {
        if (progress <= 0.02f) return 0 // 0 = Auto ISO
        val p = ((progress - 0.02f) / 0.98f).coerceIn(0f, 1f)
        val logMin = ln(MIN_ISO.toDouble())
        val logMax = ln(MAX_ISO.toDouble())
        val value = exp(logMin + p * (logMax - logMin))
        return value.roundToInt()
    }

    fun isoValueToProgress(iso: Int): Float {
        if (iso <= 0) return 0f
        val logMin = ln(MIN_ISO.toDouble())
        val logMax = ln(MAX_ISO.toDouble())
        val logVal = ln(iso.toDouble().coerceIn(MIN_ISO.toDouble(), MAX_ISO.toDouble()))
        val p = ((logVal - logMin) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)
        return 0.02f + p * 0.98f
    }

    fun getIsoLabel(iso: Int): String {
        if (iso <= 0) return "Auto"
        return if (iso >= 1000) "${iso / 1000}k ISO" else "ISO $iso"
    }
}

/**
 * Full UI state for MotionShot camera screen.
 */
data class MotionShotUiState(
    val timerSeconds: Int = 2,
    val captureCount: Int = 15,
    val shutterSpeedNs: Long = 2_000_000L, // Default 1/500s
    val isoValue: Int = 0,                  // Default Auto ISO
    val framesCaptured: Int = 0,
    val phase: CapturePhase = CapturePhase.Idle,
)

enum class CapturePhase {
    Idle,
    Recording,
    Processing,
    Done,
}
