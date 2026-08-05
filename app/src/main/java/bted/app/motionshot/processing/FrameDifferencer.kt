package bted.app.motionshot.processing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

/**
 * Ultra-Fast Multi-Core Parallelized Motion Frame Differencer.
 */
object FrameDifferencer {

    private val NUM_CORES = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)

    suspend fun computeMaskParallel(
        basePixels: IntArray,
        currPixels: IntArray,
        outputMask: IntArray,
        width: Int,
        height: Int,
        chromaThreshold: Int = 25,
    ) = coroutineScope {
        val rowsPerChunk = (height + NUM_CORES - 1) / NUM_CORES

        val jobs = List(NUM_CORES) { coreIdx ->
            val startY = coreIdx * rowsPerChunk
            val endY = ((coreIdx + 1) * rowsPerChunk).coerceAtMost(height)

            if (startY < endY) {
                async(Dispatchers.Default) {
                    processChunk(
                        basePixels = basePixels,
                        currPixels = currPixels,
                        outputMask = outputMask,
                        width = width,
                        startY = startY,
                        endY = endY,
                        chromaThreshold = chromaThreshold,
                    )
                }
            } else null
        }.filterNotNull()

        jobs.awaitAll()
    }

    fun computeMask(
        basePixels: IntArray,
        currPixels: IntArray,
        outputMask: IntArray,
        width: Int,
        height: Int,
        chromaThreshold: Int = 25,
    ) {
        processChunk(basePixels, currPixels, outputMask, width, 0, height, chromaThreshold)
    }

    private fun processChunk(
        basePixels: IntArray,
        currPixels: IntArray,
        outputMask: IntArray,
        width: Int,
        startY: Int,
        endY: Int,
        chromaThreshold: Int,
    ) {
        val startIdx = startY * width
        val endIdx = endY * width

        for (i in startIdx until endIdx) {
            val p0 = basePixels[i]
            val r0 = (p0 shr 16) and 0xFF
            val g0 = (p0 shr 8) and 0xFF
            val b0 = p0 and 0xFF

            val p1 = currPixels[i]
            val r1 = (p1 shr 16) and 0xFF
            val g1 = (p1 shr 8) and 0xFF
            val b1 = p1 and 0xFF

            val sum0 = r0 + g0 + b0 + 1
            val sum1 = r1 + g1 + b1 + 1

            val nr0 = (r0 shl 8) / sum0
            val ng0 = (g0 shl 8) / sum0
            val nr1 = (r1 shl 8) / sum1
            val ng1 = (g1 shl 8) / sum1

            val chromaDiff = abs(nr1 - nr0) + abs(ng1 - ng0)

            val y0 = (299 * r0 + 587 * g0 + 114 * b0) / 1000
            val y1 = (299 * r1 + 587 * g1 + 114 * b1) / 1000
            val lumDiff = abs(y1 - y0)

            outputMask[i] = if (chromaDiff > chromaThreshold || lumDiff > 35) 1 else 0
        }
    }
}
