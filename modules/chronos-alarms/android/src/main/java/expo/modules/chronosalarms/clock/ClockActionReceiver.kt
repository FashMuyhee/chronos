package expo.modules.chronosalarms.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The timer's exact alarm, and the buttons on the timer and stopwatch notifications. */
class ClockActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      TIMER_FIRE -> ClockEngine.fireTimer(context)
      TIMER_PAUSE -> ClockEngine.pauseTimer(context)
      TIMER_RESUME -> ClockEngine.resumeTimer(context)
      TIMER_ADD -> ClockEngine.addMinute(context)
      TIMER_RESET -> ClockEngine.resetTimer(context)
      STOPWATCH_START -> ClockEngine.startStopwatch(context)
      STOPWATCH_PAUSE -> ClockEngine.pauseStopwatch(context)
      STOPWATCH_LAP -> ClockEngine.lapStopwatch(context)
      STOPWATCH_RESET -> ClockEngine.resetStopwatch(context)
    }
  }

  companion object {
    private const val P = "expo.modules.chronosalarms.clock."
    const val TIMER_FIRE = P + "TIMER_FIRE"
    const val TIMER_PAUSE = P + "TIMER_PAUSE"
    const val TIMER_RESUME = P + "TIMER_RESUME"
    const val TIMER_ADD = P + "TIMER_ADD"
    const val TIMER_RESET = P + "TIMER_RESET"
    const val STOPWATCH_START = P + "STOPWATCH_START"
    const val STOPWATCH_PAUSE = P + "STOPWATCH_PAUSE"
    const val STOPWATCH_LAP = P + "STOPWATCH_LAP"
    const val STOPWATCH_RESET = P + "STOPWATCH_RESET"
  }
}
