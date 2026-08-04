package bted.app.motionshot.engine

import android.graphics.Bitmap
import bted.app.motionshot.processing.CanvasCompositor
import bted.app.motionshot.processing.FadeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standard fast pixel-differencing implementation of [MotionEngine].
 * Uses RGB Manhattan color distance, 3x3 morphological opening, and native Canvas PorterDuff compositing.
 */
class FastDifferenceMotionEngine : MotionEngine {

    override suspend fun process(
        frames: List<Bitmap>,
        config: EngineConfig,
    ): Bitmap = withContext(Dispatchers.Default) {
        val fade = when (config.fadeEffect) {
            EngineFadeEffect.UNIFORM -> FadeEffect.UNIFORM
            EngineFadeEffect.FADE_OUT -> FadeEffect.FADE_OUT
            EngineFadeEffect.FADE_IN -> FadeEffect.FADE_IN
        }

        CanvasCompositor.composite(
            frames = frames,
            threshold = config.threshold,
            fadeEffect = fade,
        )
    }
}
