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
 * Ultra-fast Sony Motion Shot Canvas compositor with Multi-Core Parallel Processing.
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

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        val basePixels = IntArray(totalPixels)
        val currPixels = IntArray(totalPixels)
        val alignedPixels = IntArray(totalPixels)
        val rawMask = IntArray(totalPixels)
        val tempMask = IntArray(totalPixels)
        val cleanMask = IntArray(totalPixels)
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

            // 4. Calculate Sequential Opacity Alpha
            val opacityAlpha = when (fadeEffect) {
                FadeEffect.UNIFORM -> 1.0f
                FadeEffect.FADE_OUT -> 0.35f + 0.65f * (i.toFloat() / (frames.size - 1))
                FadeEffect.FADE_IN -> 1.0f - 0.65f * (i.toFloat() / (frames.size - 1))
            }

            paint.alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)

            // 5. Apply Alpha Masking, Brightness Gain Boost & Fast Edge Feathering
            applyMaskAndFeather(
                srcPixels = alignedPixels,
                mask = cleanMask,
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

    private fun applyMaskAndFeather(
        srcPixels: IntArray,
        mask: IntArray,
        dstPixels: IntArray,
        width: Int,
        height: Int,
        brightnessBoost: Float = 1.0f,
    ) {
        val totalPixels = width * height
        for (i in 0 until totalPixels) {
            val m = mask[i]
            if (m == 1) {
                if (brightnessBoost > 1.05f) {
                    val p = srcPixels[i]
                    val a = (p ushr 24) and 0xFF
                    val r = (((p ushr 16) and 0xFF) * brightnessBoost).toInt().coerceAtMost(255)
                    val g = (((p ushr 8) and 0xFF) * brightnessBoost).toInt().coerceAtMost(255)
                    val b = ((p and 0xFF) * brightnessBoost).toInt().coerceAtMost(255)

                    dstPixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                } else {
                    dstPixels[i] = srcPixels[i]
                }
            } else {
                dstPixels[i] = 0 // Fully transparent
            }
        }

        // Fast 1-pixel boundary edge feathering
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                if (mask[idx] == 1) {
                    val boundary = mask[idx - 1] == 0 || mask[idx + 1] == 0 ||
                            mask[idx - width] == 0 || mask[idx + width] == 0
                    if (boundary) {
                        val argb = dstPixels[idx]
                        val originalAlpha = (argb ushr 24) and 0xFF
                        val softAlpha = (originalAlpha * 0.5f).toInt()
                        dstPixels[idx] = (softAlpha shl 24) or (argb and 0x00FFFFFF)
                    }
                }
            }
        }
    }
}
