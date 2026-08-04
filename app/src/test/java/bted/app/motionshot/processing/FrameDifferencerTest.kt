package bted.app.motionshot.processing

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameDifferencerTest {

    @Test
    fun computeMask_identicalPixels_returnsAllZeroMask() {
        val width = 4
        val height = 4
        val total = width * height
        val basePixels = IntArray(total) { 0xFF102030.toInt() }
        val currPixels = IntArray(total) { 0xFF102030.toInt() }
        val mask = IntArray(total)

        FrameDifferencer.computeMask(
            basePixels = basePixels,
            currPixels = currPixels,
            outputMask = mask,
            width = width,
            height = height,
        )

        for (i in 0 until total) {
            assertEquals(0, mask[i])
        }
    }

    @Test
    fun computeMask_distinctMotionPixels_returnsMotionMask() {
        val width = 2
        val height = 2
        val total = width * height
        val basePixels = IntArray(total) { 0xFF000000.toInt() } // Black
        val currPixels = IntArray(total) { 0xFFFFFFFF.toInt() } // White
        val mask = IntArray(total)

        FrameDifferencer.computeMask(
            basePixels = basePixels,
            currPixels = currPixels,
            outputMask = mask,
            width = width,
            height = height,
            chromaThreshold = 20,
        )

        for (i in 0 until total) {
            assertEquals(1, mask[i])
        }
    }
}
