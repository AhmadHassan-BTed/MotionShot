package bted.app.motionshot.processing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Multi-Core Parallelized 3x3 Morphological Opening (Erode + Dilate).
 */
object MorphologyOps {

    private val NUM_CORES = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)

    suspend fun openingParallel(
        inputMask: IntArray,
        tempMask: IntArray,
        outputMask: IntArray,
        width: Int,
        height: Int,
    ) {
        erodeParallel(inputMask, tempMask, width, height)
        dilateParallel(tempMask, outputMask, width, height)
    }

    suspend fun erodeParallel(
        input: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
    ) = coroutineScope {
        val rowsPerChunk = (height + NUM_CORES - 1) / NUM_CORES

        val jobs = List(NUM_CORES) { coreIdx ->
            val startY = coreIdx * rowsPerChunk
            val endY = ((coreIdx + 1) * rowsPerChunk).coerceAtMost(height)

            if (startY < endY) {
                async(Dispatchers.Default) {
                    erodeChunk(input, output, width, height, startY, endY)
                }
            } else null
        }.filterNotNull()

        jobs.awaitAll()
    }

    suspend fun dilateParallel(
        input: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
    ) = coroutineScope {
        val rowsPerChunk = (height + NUM_CORES - 1) / NUM_CORES

        val jobs = List(NUM_CORES) { coreIdx ->
            val startY = coreIdx * rowsPerChunk
            val endY = ((coreIdx + 1) * rowsPerChunk).coerceAtMost(height)

            if (startY < endY) {
                async(Dispatchers.Default) {
                    dilateChunk(input, output, width, height, startY, endY)
                }
            } else null
        }.filterNotNull()

        jobs.awaitAll()
    }

    fun erode(input: IntArray, output: IntArray, width: Int, height: Int) {
        erodeChunk(input, output, width, height, 0, height)
    }

    fun dilate(input: IntArray, output: IntArray, width: Int, height: Int) {
        dilateChunk(input, output, width, height, 0, height)
    }

    private fun erodeChunk(
        input: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
        startY: Int,
        endY: Int,
    ) {
        for (y in startY until endY) {
            if (y == 0 || y == height - 1) {
                for (x in 0 until width) output[y * width + x] = 0
                continue
            }
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val allSet = input[idx] == 1 &&
                        input[idx - 1] == 1 && input[idx + 1] == 1 &&
                        input[idx - width] == 1 && input[idx + width] == 1 &&
                        input[idx - width - 1] == 1 && input[idx - width + 1] == 1 &&
                        input[idx + width - 1] == 1 && input[idx + width + 1] == 1

                output[idx] = if (allSet) 1 else 0
            }
        }
    }

    private fun dilateChunk(
        input: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
        startY: Int,
        endY: Int,
    ) {
        for (y in startY until endY) {
            if (y == 0 || y == height - 1) {
                for (x in 0 until width) output[y * width + x] = 0
                continue
            }
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val anySet = input[idx] == 1 ||
                        input[idx - 1] == 1 || input[idx + 1] == 1 ||
                        input[idx - width] == 1 || input[idx + width] == 1 ||
                        input[idx - width - 1] == 1 || input[idx - width + 1] == 1 ||
                        input[idx + width - 1] == 1 || input[idx + width + 1] == 1

                output[idx] = if (anySet) 1 else 0
            }
        }
    }
}
