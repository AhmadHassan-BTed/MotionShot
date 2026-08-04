package bted.app.motionshot.engine

import android.graphics.Bitmap

/**
 * Pluggable contract for Motion Shot Image Processing Engines.
 *
 * Implement this interface to plug in alternative compositing engines,
 * AI segmentation models (e.g. MediaPipe / Segment Anything / ML Kit), OpenCV algorithms,
 * or custom background subtraction pipelines.
 */
interface MotionEngine {

    /**
     * Processes a sequence of captured frames and produces a single stroboscopic composite.
     *
     * @param frames List of captured Bitmaps (minimum 2). Frame 0 is the reference background.
     * @param config Engine configuration parameters
     * @return Final composited [Bitmap]
     */
    suspend fun process(
        frames: List<Bitmap>,
        config: EngineConfig = EngineConfig(),
    ): Bitmap
}
