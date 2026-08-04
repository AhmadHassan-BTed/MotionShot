package bted.app.motionshot.processing

import kotlin.math.abs

/**
 * Advanced Chromaticity & Luminance Frame Differencer.
 *
 * The Core File responsible for subject extraction quality.
 * Uses Normalized Color (r, g chromaticity) + Luminance separation to ignore
 * camera auto-exposure shifts, sensor noise, and environmental shadows.
 */
object FrameDifferencer {

    /**
     * Computes high-quality motion mask (1 = subject, 0 = background).
     *
     * @param basePixels       Base frame pixel buffer (ARGB_8888)
     * @param currPixels       Current frame pixel buffer (ARGB_8888)
     * @param outputMask       Destination mask buffer (1 = motion, 0 = background)
     * @param width            Image width
     * @param height           Image height
     * @param chromaThreshold  Sensitivity to color shifts (default 32)
     * @param luminanceThreshold Sensitivity to brightness shifts (default 70)
     */
    fun computeMask(
        basePixels: IntArray,
        currPixels: IntArray,
        outputMask: IntArray,
        width: Int,
        height: Int,
        chromaThreshold: Int = 32,
        luminanceThreshold: Int = 70,
    ) {
        val totalPixels = width * height

        for (i in 0 until totalPixels) {
            val baseVal = basePixels[i]
            val currVal = currPixels[i]

            val r0 = (baseVal shr 16) and 0xFF
            val g0 = (baseVal shr 8) and 0xFF
            val b0 = baseVal and 0xFF

            val r1 = (currVal shr 16) and 0xFF
            val g1 = (currVal shr 8) and 0xFF
            val b1 = currVal and 0xFF

            // 1. Luminance calculation (BT.601)
            val y0 = (299 * r0 + 587 * g0 + 114 * b0) / 1000
            val y1 = (299 * r1 + 587 * g1 + 114 * b1) / 1000
            val deltaY = abs(y1 - y0)

            // 2. Normalized chromaticity (scaled by 256 for fast integer math)
            val sum0 = r0 + g0 + b0 + 1
            val sum1 = r1 + g1 + b1 + 1

            val normR0 = (r0 shl 8) / sum0
            val normG0 = (g0 shl 8) / sum0

            val normR1 = (r1 shl 8) / sum1
            val normG1 = (g1 shl 8) / sum1

            val deltaChroma = abs(normR1 - normR0) + abs(normG1 - normG0)

            // Motion criteria: significant color difference OR strong luminance difference
            val isMotion = (deltaChroma > chromaThreshold && deltaY > 25) || (deltaY > luminanceThreshold)

            outputMask[i] = if (isMotion) 1 else 0
        }
    }
}
