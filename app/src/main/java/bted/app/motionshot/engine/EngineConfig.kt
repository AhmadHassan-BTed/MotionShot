package bted.app.motionshot.engine

/**
 * Configuration parameters for the Motion Processing Engine.
 *
 * @property threshold    Sensitivity threshold for motion detection (RGB distance)
 * @property fadeEffect   Sequential opacity fade effect (UNIFORM, FADE_OUT, FADE_IN)
 * @property smoothEdges  Whether edge feathering is enabled for anti-aliasing
 */
data class EngineConfig(
    val threshold: Int = 45,
    val fadeEffect: EngineFadeEffect = EngineFadeEffect.FADE_OUT,
    val smoothEdges: Boolean = true,
)

enum class EngineFadeEffect {
    UNIFORM,
    FADE_OUT,
    FADE_IN,
}
