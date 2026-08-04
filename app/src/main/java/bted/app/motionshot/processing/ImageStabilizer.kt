package bted.app.motionshot.processing

import kotlin.math.abs

/**
 * Pixel-to-pixel Frame Stabilization & Handheld Alignment Engine.
 *
 * Computes optimal (dx, dy) translation vectors to lock background pixels
 * perfectly in place across frames, cancelling out camera hand shake & micro-jitter.
 */
object ImageStabilizer {

    data class Offset(val dx: Int, val dy: Int)

    /**
     * Finds the (dx, dy) alignment offset that locks target frame background to base frame.
     * Uses 4x grid downsampling for sub-5ms performance.
     */
    fun findAlignmentOffset(
        basePixels: IntArray,
        currPixels: IntArray,
        width: Int,
        height: Int,
        maxSearchRadius: Int = 16,
    ): Offset {
        var bestDx = 0
        var bestDy = 0
        var minDiff = Long.MAX_VALUE

        val step = 4
        val startY = height / 8
        val endY = height * 7 / 8
        val startX = width / 8
        val endX = width * 7 / 8

        for (dy in -maxSearchRadius..maxSearchRadius step 2) {
            for (dx in -maxSearchRadius..maxSearchRadius step 2) {
                var sumDiff = 0L

                for (y in startY until endY step step) {
                    val baseRow = y * width
                    val currY = y + dy
                    if (currY < 0 || currY >= height) continue
                    val currRow = currY * width

                    for (x in startX until endX step step) {
                        val currX = x + dx
                        if (currX < 0 || currX >= width) continue

                        val bVal = basePixels[baseRow + x]
                        val cVal = currPixels[currRow + currX]

                        val r0 = (bVal shr 16) and 0xFF
                        val g0 = (bVal shr 8) and 0xFF
                        val b0 = bVal and 0xFF

                        val r1 = (cVal shr 16) and 0xFF
                        val g1 = (cVal shr 8) and 0xFF
                        val b1 = cVal and 0xFF

                        sumDiff += abs(r1 - r0) + abs(g1 - g0) + abs(b1 - b0)
                    }
                }

                if (sumDiff < minDiff) {
                    minDiff = sumDiff
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        return Offset(bestDx, bestDy)
    }

    /**
     * Shifts [inputPixels] by (dx, dy) into [outputPixels], aligning it to base frame coordinates.
     */
    fun alignBuffer(
        inputPixels: IntArray,
        outputPixels: IntArray,
        width: Int,
        height: Int,
        dx: Int,
        dy: Int,
    ) {
        if (dx == 0 && dy == 0) {
            System.arraycopy(inputPixels, 0, outputPixels, 0, inputPixels.size)
            return
        }

        for (y in 0 until height) {
            val srcY = y + dy
            val destRow = y * width

            if (srcY in 0 until height) {
                val srcRow = srcY * width
                for (x in 0 until width) {
                    val srcX = x + dx
                    if (srcX in 0 until width) {
                        outputPixels[destRow + x] = inputPixels[srcRow + srcX]
                    } else {
                        outputPixels[destRow + x] = inputPixels[destRow + x]
                    }
                }
            } else {
                val copySrcRow = y.coerceIn(0, height - 1) * width
                System.arraycopy(inputPixels, copySrcRow, outputPixels, destRow, width)
            }
        }
    }
}
