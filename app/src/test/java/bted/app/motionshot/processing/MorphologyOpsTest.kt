package bted.app.motionshot.processing

import org.junit.Assert.assertEquals
import org.junit.Test

class MorphologyOpsTest {

    @Test
    fun erode_isolatedPixel_removesNoise() {
        val width = 5
        val height = 5
        val input = IntArray(width * height)
        val output = IntArray(width * height)

        // Set center pixel to 1 (isolated noise speck)
        input[2 * width + 2] = 1

        MorphologyOps.erode(input, output, width, height)

        // Isolated speck should be removed
        assertEquals(0, output[2 * width + 2])
    }

    @Test
    fun dilate_solidBlock_expandsBoundaries() {
        val width = 5
        val height = 5
        val input = IntArray(width * height)
        val output = IntArray(width * height)

        // Set center pixel to 1
        input[2 * width + 2] = 1

        MorphologyOps.dilate(input, output, width, height)

        // Center and all 8 neighbors should be 1
        assertEquals(1, output[2 * width + 2])
        assertEquals(1, output[1 * width + 1])
        assertEquals(1, output[1 * width + 2])
        assertEquals(1, output[1 * width + 3])
    }
}
