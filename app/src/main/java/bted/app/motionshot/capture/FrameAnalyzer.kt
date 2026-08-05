package bted.app.motionshot.capture

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-throughput CameraX [ImageAnalysis.Analyzer] supporting software Digital Signal Amplification.
 */
class FrameAnalyzer : ImageAnalysis.Analyzer {

    val isStreaming = AtomicBoolean(false)
    var brightnessBoost: Float = 1.0f

    val frameChannel = Channel<Bitmap>(capacity = Channel.UNLIMITED)

    private val conversionPool = Executors.newFixedThreadPool(
        (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)
    )

    override fun analyze(image: ImageProxy) {
        if (isStreaming.get()) {
            try {
                val snapshot = RawFrameSnapshot.capture(image)
                image.close()

                val currentBoost = brightnessBoost
                conversionPool.execute {
                    try {
                        val bitmap = snapshot.toBitmap(currentBoost)
                        val sent = frameChannel.trySend(bitmap)
                        if (sent.isFailure) {
                            bitmap.recycle()
                        }
                    } catch (e: Exception) {
                        Log.e("FrameAnalyzer", "Async conversion failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("FrameAnalyzer", "Snapshot capture failed: ${e.message}")
                image.close()
            }
        } else {
            image.close()
        }
    }
}

/**
 * Immutable snapshot of raw YUV_420_888 plane data with hardware + digital gain scaling.
 */
class RawFrameSnapshot private constructor(
    private val yData: ByteArray,
    private val uData: ByteArray,
    private val vData: ByteArray,
    private val width: Int,
    private val height: Int,
    private val yRowStride: Int,
    private val uvRowStride: Int,
    private val uvPixelStride: Int,
    private val rotationDegrees: Int,
) {
    companion object {
        fun capture(image: ImageProxy): RawFrameSnapshot {
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val yBuffer = yPlane.buffer.duplicate()
            val uBuffer = uPlane.buffer.duplicate()
            val vBuffer = vPlane.buffer.duplicate()

            val yBytes = ByteArray(yBuffer.remaining())
            val uBytes = ByteArray(uBuffer.remaining())
            val vBytes = ByteArray(vBuffer.remaining())

            yBuffer.get(yBytes)
            uBuffer.get(uBytes)
            vBuffer.get(vBytes)

            return RawFrameSnapshot(
                yData = yBytes,
                uData = uBytes,
                vData = vBytes,
                width = image.width,
                height = image.height,
                yRowStride = yPlane.rowStride,
                uvRowStride = uPlane.rowStride,
                uvPixelStride = uPlane.pixelStride,
                rotationDegrees = image.imageInfo.rotationDegrees,
            )
        }
    }

    fun toBitmap(brightnessBoost: Float = 1.0f): Bitmap {
        val argb = IntArray(width * height)
        var idx = 0

        val gain = brightnessBoost.coerceAtLeast(1.0f)

        for (y in 0 until height) {
            val yRow = y * yRowStride
            val uvRow = (y shr 1) * uvRowStride

            for (x in 0 until width) {
                val yVal = yData[yRow + x].toInt() and 0xFF
                val uvIdx = uvRow + (x shr 1) * uvPixelStride

                val uVal = (uData[uvIdx].toInt() and 0xFF) - 128
                val vVal = (vData[uvIdx].toInt() and 0xFF) - 128

                var r = yVal + ((1436 * vVal) shr 10)
                var g = yVal - ((352 * uVal + 731 * vVal) shr 10)
                var b = yVal + ((1815 * uVal) shr 10)

                // Apply Digital Signal Amplification Gain
                if (gain > 1.01f) {
                    r = (r * gain).toInt()
                    g = (g * gain).toInt()
                    b = (b * gain).toInt()
                }

                if (r < 0) r = 0 else if (r > 255) r = 255
                if (g < 0) g = 0 else if (g > 255) g = 255
                if (b < 0) b = 0 else if (b > 255) b = 255

                argb[idx++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val raw = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)

        if (rotationDegrees == 0) return raw

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (rotated !== raw) raw.recycle()
        return rotated
    }
}
