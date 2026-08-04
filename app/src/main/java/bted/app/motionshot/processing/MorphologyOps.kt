package bted.app.motionshot.processing

import java.util.Arrays

/**
 * Ultra-fast 3x3 binary morphological operations using forward propagation & sparse iteration.
 *
 * Operates on pre-allocated buffers with zero heap allocations during processing.
 */
object MorphologyOps {

    /**
     * Erode operation with 3x3 box kernel into [output].
     * Only checks neighbors when candidate pixel is 1 (sparse check).
     */
    fun erode(input: IntArray, output: IntArray, width: Int, height: Int) {
        Arrays.fill(output, 0)

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                if (input[idx] == 1) {
                    if (input[idx - width - 1] == 1 && input[idx - width] == 1 && input[idx - width + 1] == 1 &&
                        input[idx - 1] == 1 && input[idx + 1] == 1 &&
                        input[idx + width - 1] == 1 && input[idx + width] == 1 && input[idx + width + 1] == 1
                    ) {
                        output[idx] = 1
                    }
                }
            }
        }
    }

    /**
     * Forward-propagation Dilate operation into [output].
     * Instead of checking 8 neighbors for every 0-pixel (millions of reads),
     * propagates 1s forward only when a foreground pixel is encountered (~20x faster).
     */
    fun dilate(input: IntArray, output: IntArray, width: Int, height: Int) {
        Arrays.fill(output, 0)

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                if (input[idx] == 1) {
                    output[idx - width - 1] = 1
                    output[idx - width] = 1
                    output[idx - width + 1] = 1
                    output[idx - 1] = 1
                    output[idx] = 1
                    output[idx + 1] = 1
                    output[idx + width - 1] = 1
                    output[idx + width] = 1
                    output[idx + width + 1] = 1
                }
            }
        }
    }

    /**
     * Morphological Opening (Erode into [tempBuffer], then Dilate into [outputBuffer]).
     */
    fun open(
        inputMask: IntArray,
        tempBuffer: IntArray,
        outputBuffer: IntArray,
        width: Int,
        height: Int,
    ) {
        erode(inputMask, tempBuffer, width, height)
        dilate(tempBuffer, outputBuffer, width, height)
    }
}
