package bted.app.motionshot.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Sequential Fade Effect options (matching Sony Motion Shot).
 */
enum class FadeEffect {
    UNIFORM,
    FADE_OUT,
    FADE_IN,
}

/**
 * Ultra-fast Stroboscopic Motion Compositor with Soft Multi-Sample Antialiased Edges.
 * Works flawlessly in broad daylight, outdoor sports, and low-light action scenes.
 */
object CanvasCompositor {

    suspend fun composite(
        frames: List<Bitmap>,
        threshold: Int = 45,
        fadeEffect: FadeEffect = FadeEffect.FADE_OUT,
        brightnessBoost: Float = 1.0f,
    ): Bitmap {
        require(frames.size >= 2) { "At least 2 frames are required for compositing." }

        val baseFrame = frames[0]
        val width = baseFrame.width
        val height = baseFrame.height
        val totalPixels = width * height

        val resultBitmap = baseFrame.copy(Bitmap.Config.ARGB_8888, true)

        // Apply Brightness Boost gain to full canvas base background if requested
        if (brightnessBoost > 1.05f) {
            applyFullCanvasBrightness(resultBitmap, brightnessBoost)
        }

        val canvas = Canvas(resultBitmap)

        // High-quality antialiased SRC_OVER layer compositor
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        val basePixels = IntArray(totalPixels)
        val currPixels = IntArray(totalPixels)
        val alignedPixels = IntArray(totalPixels)
        val rawMask = IntArray(totalPixels)
        val tempMask = IntArray(totalPixels)
        val cleanMask = IntArray(totalPixels)
        val softAlphaMask = FloatArray(totalPixels)
        val maskedPixels = IntArray(totalPixels)

        baseFrame.getPixels(basePixels, 0, width, 0, 0, width, height)

        for (i in 1 until frames.size) {
            val currentFrame = frames[i]
            currentFrame.getPixels(currPixels, 0, width, 0, 0, width, height)

            // 1. Handheld 2D Alignment
            val offset = ImageStabilizer.findAlignmentOffset(
                basePixels = basePixels,
                currPixels = currPixels,
                width = width,
                height = height,
            )

            ImageStabilizer.alignBuffer(
                inputPixels = currPixels,
                outputPixels = alignedPixels,
                width = width,
                height = height,
                dx = offset.dx,
                dy = offset.dy,
            )

            // 2. Parallel Multi-Core Normalized Chromaticity Differencing
            FrameDifferencer.computeMaskParallel(
                basePixels = basePixels,
                currPixels = alignedPixels,
                outputMask = rawMask,
                width = width,
                height = height,
                chromaThreshold = threshold,
            )

            // 3. Parallel Multi-Core 3x3 Morphological Opening
            MorphologyOps.openingParallel(
                inputMask = rawMask,
                tempMask = tempMask,
                outputMask = cleanMask,
                width = width,
                height = height,
            )

            // 4. Compute 3x3 Smooth Distance Feathering Mask
            computeSoftEdgeMask(
                binaryMask = cleanMask,
                softMask = softAlphaMask,
                width = width,
                height = height,
            )

            // 5. Calculate Sequential Opacity Alpha (FADE_OUT: pose N is 100% crisp)
            val sequentialOpacity = when (fadeEffect) {
                FadeEffect.UNIFORM -> 0.85f
                FadeEffect.FADE_OUT -> 0.40f + 0.60f * (i.toFloat() / (frames.size - 1))
                FadeEffect.FADE_IN -> 1.0f - 0.60f * (i.toFloat() / (frames.size - 1))
            }

            paint.alpha = (sequentialOpacity * 255).toInt().coerceIn(0, 255)

            // 6. Apply Antialiased Soft Alpha Mask & Digital Gain
            applySoftAlphaMask(
                srcPixels = alignedPixels,
                softMask = softAlphaMask,
                dstPixels = maskedPixels,
                width = width,
                height = height,
                brightnessBoost = brightnessBoost,
            )

            val layerBitmap = Bitmap.createBitmap(maskedPixels, width, height, Bitmap.Config.ARGB_8888)
            canvas.drawBitmap(layerBitmap, 0f, 0f, paint)
            layerBitmap.recycle()
        }

        return resultBitmap
    }

    private fun applyFullCanvasBrightness(bitmap: Bitmap, brightnessBoost: Float) {
        val canvas = Canvas(bitmap)
        val cm = ColorMatrix(
            floatArrayOf(
                brightnessBoost, 0f, 0f, 0f, 0f,
                0f, brightnessBoost, 0f, 0f, 0f,
                0f, 0f, brightnessBoost, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    private fun computeSoftEdgeMask(
        binaryMask: IntArray,
        softMask: FloatArray,
        width: Int,
        height: Int,
    ) {
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                if (binaryMask[idx] == 1) {
                    // Sum 3x3 neighborhood density for antialiased edge smoothing
                    var neighborSum = 0
                    for (dy in -1..1) {
                        val nRow = (y + dy) * width
                        for (dx in -1..1) {
                            if (binaryMask[nRow + x + dx] == 1) neighborSum++
                        }
                    }
                    softMask[idx] = neighborSum / 9.0f
                } else {
                    softMask[idx] = 0.0f
                }
            }
        }
    }

    private fun applySoftAlphaMask(
        srcPixels: IntArray,
        softMask: FloatArray,
        dstPixels: IntArray,
        width: Int,
        height: Int,
        brightnessBoost: Float = 1.0f,
    ) {
        val totalPixels = width * height
        val gain = brightnessBoost.coerceAtLeast(1.0f)

        for (i in 0 until totalPixels) {
            val alphaWeight = softMask[i]
            if (alphaWeight > 0.01f) {
                val p = srcPixels[i]
                val baseAlpha = (p ushr 24) and 0xFF
                val finalAlpha = (baseAlpha * alphaWeight).toInt().coerceIn(0, 255)

                var r = (p ushr 16) and 0xFF
                var g = (p ushr 8) and 0xFF
                var b = p and 0xFF

                if (gain > 1.01f) {
                    r = (r * gain).toInt().coerceAtMost(255)
                    g = (g * gain).toInt().coerceAtMost(255)
                    b = (b * gain).toInt().coerceAtMost(255)
                }

                dstPixels[i] = (finalAlpha shl 24) or (r shl 16) or (g shl 8) or b
            } else {
                dstPixels[i] = 0 // Transparent
            }
        }
    }
}
