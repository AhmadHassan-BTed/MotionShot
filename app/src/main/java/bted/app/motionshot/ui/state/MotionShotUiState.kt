package bted.app.motionshot.ui.state

/**
 * Shutter speed utilities and helper mappings.
 */
object ShutterSpeedUtils {
    val SLIDER_STEPS = listOf(
        0L,          // 0: Auto
        8_000_000L,  // 1: 1/125s
        4_000_000L,  // 2: 1/250s
        2_000_000L,  // 3: 1/500s
        1_000_000L,  // 4: 1/1000s
        500_000L,    // 5: 1/2000s
        250_000L,    // 6: 1/4000s
    )

    fun getLabel(exposureNs: Long): String {
        if (exposureNs <= 0L) return "Auto"
        val denominator = (1_000_000_000L / exposureNs).toInt()
        return "1/${denominator}s"
    }

    fun exposureToStepIndex(exposureNs: Long): Int {
        val idx = SLIDER_STEPS.indexOf(exposureNs)
        return if (idx >= 0) idx else 3 // Default 1/500s (index 3)
    }

    fun stepIndexToExposure(index: Int): Long {
        val clamped = index.coerceIn(0, SLIDER_STEPS.lastIndex)
        return SLIDER_STEPS[clamped]
    }
}

/**
 * Full UI state for MotionShot camera screen.
 */
data class MotionShotUiState(
    val timerSeconds: Int = 5,
    val captureCount: Int = 10,
    val shutterSpeedNs: Long = 2_000_000L, // Default 1/500s (index 3)
    val framesCaptured: Int = 0,
    val phase: CapturePhase = CapturePhase.Idle,
)

enum class CapturePhase {
    Idle,
    Recording,
    Processing,
    Done,
}
