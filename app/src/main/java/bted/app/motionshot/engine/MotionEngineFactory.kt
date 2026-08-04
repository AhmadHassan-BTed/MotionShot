package bted.app.motionshot.engine

/**
 * Factory and registry for [MotionEngine] instances.
 *
 * Swap the default engine implementation here to plug in new AI models, OpenCV,
 * or experimental algorithms across the entire application.
 */
object MotionEngineFactory {

    enum class EngineType {
        FAST_DIFFERENCE,
        // AI_SEGMENTATION,  // Plug your AI model here!
        // OPENCV_MOG2,       // Plug OpenCV MOG2 background subtractor here!
    }

    /**
     * Creates and returns the active [MotionEngine].
     */
    fun create(type: EngineType = EngineType.FAST_DIFFERENCE): MotionEngine {
        return when (type) {
            EngineType.FAST_DIFFERENCE -> FastDifferenceMotionEngine()
        }
    }
}
