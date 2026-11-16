package expo.modules.chronosalarms.clock

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Timer and stopwatch persistence, in device-protected storage like AlarmStore. */
class ClockStore private constructor(context: Context) {
  private val prefs: SharedPreferences =
    context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  @Synchronized
  fun timer(): TimerState =
    prefs.getString(KEY_TIMER, null)?.let { runCatching { TimerState.fromJson(JSONObject(it)) }.getOrNull() } ?: TimerState()

  @Synchronized
  fun saveTimer(state: TimerState) {
    prefs.edit().putString(KEY_TIMER, state.toJson().toString()).apply()
  }

  @Synchronized
  fun stopwatch(): StopwatchState =
    prefs.getString(KEY_STOPWATCH, null)?.let { runCatching { StopwatchState.fromJson(JSONObject(it)) }.getOrNull() }
      ?: StopwatchState()

  @Synchronized
  fun saveStopwatch(state: StopwatchState) {
    prefs.edit().putString(KEY_STOPWATCH, state.toJson().toString()).apply()
  }

  companion object {
    private const val PREFS = "chronos_clock"
    private const val KEY_TIMER = "timer"
    private const val KEY_STOPWATCH = "stopwatch"

    @Volatile private var instance: ClockStore? = null

    fun get(context: Context): ClockStore =
      instance ?: synchronized(this) {
        instance ?: ClockStore(context.applicationContext).also { instance = it }
      }
  }
}
