package bted.app.motionshot.engine

/**
 * Pipeline Configuration for Step-by-Step Sequential Debugging.
 *
 * Allows enabling/disabling individual processing stages to isolate and verify
 * every step of the pipeline.
 *
 * @property mode                  Current view mode (RAW_FRAME_GALLERY or COMPOSITE)
 * @property enableStabilization    Step 2: Lock background via 2D translation alignment
 * @property enableDifferencing     Step 3: Chromaticity & luminance motion differencing
 * @property enableMorphology       Step 4: 3x3 Erode + Dilate noise removal
 * @property enableEdgeFeathering   Step 5: Soft boundary edge anti-aliasing
 * @property enableAlphaFade        Step 6: Sequence alpha decay (Fade Out)
 */
data class DebugPipelineConfig(
    val mode: PipelineMode = PipelineMode.RAW_FRAME_GALLERY,
    val enableStabilization: Boolean = true,
    val enableDifferencing: Boolean = true,
    val enableMorphology: Boolean = true,
    val enableEdgeFeathering: Boolean = true,
    val enableAlphaFade: Boolean = true,
)

enum class PipelineMode {
    /** Step 1: Focus purely on capturing and inspecting all individual raw frames. */
    RAW_FRAME_GALLERY,

    /** Full sequential compositing pipeline. */
    COMPOSITE,
}
