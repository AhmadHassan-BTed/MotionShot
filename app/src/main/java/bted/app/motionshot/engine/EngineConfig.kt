package bted.app.motionshot.engine

/**
 * Configuration parameters for the Motion Processing Engine.
 *
 * @property threshold       Sensitivity threshold for motion detection (RGB distance)
 * @property fadeEffect      Sequential opacity fade effect (UNIFORM, FADE_OUT, FADE_IN)
 * @property smoothEdges     Whether edge feathering is enabled for anti-aliasing
 * @property brightnessBoost Brightness gain multiplier for dark scenes (1.0x to 4.0x)
 */
data class EngineConfig(
    val threshold: Int = 45,
    val fadeEffect: EngineFadeEffect = EngineFadeEffect.FADE_OUT,
    val smoothEdges: Boolean = true,
    val brightnessBoost: Float = 1.0f,
)

enum class EngineFadeEffect {
    UNIFORM,
    FADE_OUT,
    FADE_IN,
}
