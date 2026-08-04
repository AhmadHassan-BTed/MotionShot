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

    companion object {
        private const val PREFS_NAME = "motionshot_user_settings"
        private const val KEY_TIMER = "timer_seconds"
        private const val KEY_COUNT = "capture_count"
        private const val KEY_SHUTTER = "shutter_speed_ns"
        private const val KEY_ISO = "iso_value"
    }
}
