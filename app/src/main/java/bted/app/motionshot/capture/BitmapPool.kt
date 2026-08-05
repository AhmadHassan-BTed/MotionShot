package bted.app.motionshot.capture

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Zero-Allocation Reusable Bitmap Pool for high-speed 120 FPS frame acquisition.
 * Prevents GC pauses and JVM heap allocations during hardware camera streaming.
 */
object BitmapPool {

    private val pool = ConcurrentLinkedQueue<Bitmap>()
    private var poolWidth = 0
    private var poolHeight = 0

    fun acquire(width: Int, height: Int): Bitmap {
        if (width != poolWidth || height != poolHeight) {
            clear()
            poolWidth = width
            poolHeight = height
        }

        val reusable = pool.poll()
        if (reusable != null && !reusable.isRecycled && reusable.width == width && reusable.height == height) {
            return reusable
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun release(bitmap: Bitmap) {
        if (!bitmap.isRecycled && bitmap.width == poolWidth && bitmap.height == poolHeight) {
            pool.offer(bitmap)
        } else {
            bitmap.recycle()
        }
    }

    fun clear() {
        while (true) {
            val b = pool.poll() ?: break
            if (!b.isRecycled) b.recycle()
        }
    }
}
