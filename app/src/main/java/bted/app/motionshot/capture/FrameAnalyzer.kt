package bted.app.motionshot.capture

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX [ImageAnalysis.Analyzer] gated by an [AtomicBoolean] flag.
 *
 * Flow:
 * 1. ViewModel sets [shouldCapture] = true.
 * 2. Next frame → analyzer converts YUV→Bitmap, sends to [frameChannel].
 * 3. ViewModel receives from [frameChannel], stores the bitmap.
 * 4. Repeat.
 *
 * When [shouldCapture] is false, the proxy is closed immediately (zero cost).
 */
class FrameAnalyzer : ImageAnalysis.Analyzer {

    /** Set to true by the ViewModel when a frame is needed. */
    val shouldCapture = AtomicBoolean(false)

    /**
     * Captured bitmaps are delivered here.
     * Capacity = 1 so the analyzer never blocks; failed sends recycle the bitmap.
     */
    val frameChannel = Channel<Bitmap>(capacity = 1)

    override fun analyze(image: ImageProxy) {
        if (shouldCapture.compareAndSet(true, false)) {
            val bitmap = YuvToRgb.convert(image)
            val sent = frameChannel.trySend(bitmap)
            if (sent.isFailure) {
                // Channel full (shouldn't happen in normal flow) — prevent leak.
                bitmap.recycle()
            }
        }
        image.close()
    }
}
