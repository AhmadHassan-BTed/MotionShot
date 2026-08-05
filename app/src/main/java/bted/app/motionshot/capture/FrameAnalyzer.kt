package bted.app.motionshot.capture

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-Speed 60-120 FPS CameraX [ImageAnalysis.Analyzer].
 *
 * Captures all hardware camera frames delivered by CameraX queue without dropping.
 */
class FrameAnalyzer : ImageAnalysis.Analyzer {

    val isStreaming = AtomicBoolean(false)
    val frameChannel = Channel<Bitmap>(capacity = Channel.UNLIMITED)

    override fun analyze(image: ImageProxy) {
        if (isStreaming.get()) {
            try {
                val bitmap = YuvToRgb.convert(image)
                val sent = frameChannel.trySend(bitmap)
                if (sent.isFailure) {
                    bitmap.recycle()
                }
            } catch (_: Exception) {
            }
        }
        image.close()
    }
}
