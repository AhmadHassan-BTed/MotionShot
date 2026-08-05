package bted.app.motionshot.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists user camera settings across app launches.
 */
class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var timerSeconds: Int
        get() = prefs.getInt(KEY_TIMER, 2)
        set(value) = prefs.edit().putInt(KEY_TIMER, value).apply()

    var captureCount: Int
        get() = prefs.getInt(KEY_COUNT, 15)
        set(value) = prefs.edit().putInt(KEY_COUNT, value).apply()

    var shutterSpeedNs: Long
        get() = prefs.getLong(KEY_SHUTTER, 2_000_000L) // Default 1/500s
        set(value) = prefs.edit().putLong(KEY_SHUTTER, value).apply()

    var isoValue: Int
        get() = prefs.getInt(KEY_ISO, 0) // Default Auto
        set(value) = prefs.edit().putInt(KEY_ISO, value).apply()

    var isFlashEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLASH, false)
        set(value) = prefs.edit().putBoolean(KEY_FLASH, value).apply()

    var isFocusLocked: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_FOCUS_LOCK, value).apply()

    var isGridEnabled: Boolean
        get() = prefs.getBoolean(KEY_GRID, true)
        set(value) = prefs.edit().putBoolean(KEY_GRID, value).apply()

    var isAwbLocked: Boolean
        get() = prefs.getBoolean(KEY_AWB_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_AWB_LOCK, value).apply()

    var isHighFpsVideoMode: Boolean
        get() = prefs.getBoolean(KEY_SENSOR_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_SENSOR_MODE, value).apply()

    var isFrontCamera: Boolean
        get() = prefs.getBoolean(KEY_FRONT_CAM, false)
        set(value) = prefs.edit().putBoolean(KEY_FRONT_CAM, value).apply()

    var brightnessBoost: Float
        get() = prefs.getFloat(KEY_BRIGHTNESS, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_BRIGHTNESS, value).apply()

    companion object {
        private const val PREFS_NAME = "motionshot_user_settings"
        private const val KEY_TIMER = "timer_seconds"
        private const val KEY_COUNT = "capture_count"
        private const val KEY_SHUTTER = "shutter_speed_ns"
        private const val KEY_ISO = "iso_value"
        private const val KEY_FLASH = "flash_enabled"
        private const val KEY_FOCUS_LOCK = "focus_locked"
        private const val KEY_GRID = "grid_enabled"
        private const val KEY_AWB_LOCK = "awb_locked"
        private const val KEY_SENSOR_MODE = "high_fps_video_mode"
        private const val KEY_FRONT_CAM = "front_camera"
        private const val KEY_BRIGHTNESS = "brightness_boost"
    }
}
