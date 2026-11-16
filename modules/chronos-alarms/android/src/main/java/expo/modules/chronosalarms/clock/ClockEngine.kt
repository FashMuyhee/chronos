package expo.modules.chronosalarms.clock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single owner of timer and stopwatch state. Every change is persisted, mirrored to the
 * foreground service's notifications, and reported to JS; JS only renders.
 */
object ClockEngine {
  private const val TAG = "ChronosClock"
  private const val MINUTE = 60_000L
  /** A timer left ringing this long stops on its own. */
  const val RING_LIMIT_MS = 10 * MINUTE
  private const val TIMER_REQUEST_CODE = 11

  private val _timer = MutableStateFlow(TimerState())
  private val _stopwatch = MutableStateFlow(StopwatchState())
  val timer: StateFlow<TimerState> = _timer.asStateFlow()
  val stopwatch: StateFlow<StopwatchState> = _stopwatch.asStateFlow()

  @Volatile private var loaded = false

  /** Called with "timer" or "stopwatch" after a change; the JS module forwards it as an event. */
  @Volatile var listener: ((String) -> Unit)? = null

  /**
   * The user swiped a running notification away. It stays gone (Live Update rules: don't repost
   * a dismissed notification) until that clock starts over or the timer rings.
   */
  @Volatile var timerDismissed = false
  @Volatile var stopwatchDismissed = false

  fun notificationDismissed(kind: String) {
    if (kind == "timer") timerDismissed = true else stopwatchDismissed = true
  }

  /** Reads the saved state once per process. Safe to call from any entry point. */
  @Synchronized
  fun load(context: Context) {
    if (loaded) return
    val store = ClockStore.get(context)
    var t = store.timer()
    // A ring from before the process died that nobody stopped is long over.
    if (t.status == TimerStatus.RINGING && System.currentTimeMillis() - t.endAt > RING_LIMIT_MS) {
      t = t.copy(status = TimerStatus.IDLE)
      store.saveTimer(t)
    }
    _timer.value = t
    _stopwatch.value = store.stopwatch()
    loaded = true
  }

  /** After app start, boot or a time change: re-arm the timer alarm and the ongoing notification. */
  fun restore(context: Context) {
    load(context)
    scheduleTimerAlarm(context, _timer.value)
    if (anyActive()) ClockService.sync(context)
  }

  fun anyActive(): Boolean = _timer.value.active || _stopwatch.value.active

  // region Timer

  fun startTimer(context: Context, durationMs: Long) {
    load(context)
    val ms = durationMs.coerceIn(1_000L, 100 * 60 * MINUTE)
    val now = System.currentTimeMillis()
    setTimer(context, TimerState(TimerStatus.RUNNING, totalMs = ms, endAt = now + ms, lastMs = ms))
  }

  fun pauseTimer(context: Context) = updateTimer(context) { t ->
    if (t.status != TimerStatus.RUNNING) null
    else t.copy(status = TimerStatus.PAUSED, remainingMs = t.remaining().coerceAtLeast(0))
  }

  fun resumeTimer(context: Context) = updateTimer(context) { t ->
    if (t.status != TimerStatus.PAUSED) null
    else t.copy(status = TimerStatus.RUNNING, endAt = System.currentTimeMillis() + t.remainingMs)
  }

  /** "+1 min". While ringing it starts a fresh one-minute countdown, like the system clock. */
  fun addMinute(context: Context) = updateTimer(context) { t ->
    when (t.status) {
      TimerStatus.RUNNING -> t.copy(endAt = t.endAt + MINUTE, totalMs = t.totalMs + MINUTE)
      TimerStatus.PAUSED -> t.copy(remainingMs = t.remainingMs + MINUTE, totalMs = t.totalMs + MINUTE)
      TimerStatus.RINGING -> t.copy(status = TimerStatus.RUNNING, endAt = System.currentTimeMillis() + MINUTE, totalMs = MINUTE)
      TimerStatus.IDLE -> null
    }
  }

  /** Cancel while counting, or Stop while ringing. Keeps [TimerState.lastMs] for next time. */
  fun resetTimer(context: Context) = updateTimer(context) { t ->
    if (t.status == TimerStatus.IDLE) null else TimerState(lastMs = t.lastMs)
  }

  /** The exact alarm fired: start ringing if the countdown really is over. */
  fun fireTimer(context: Context) = updateTimer(context) { t ->
    when {
      t.status != TimerStatus.RUNNING -> null
      // Fired early, e.g. after the clock changed: wait for the real end.
      t.remaining() > 1_000 -> t
      else -> t.copy(status = TimerStatus.RINGING)
    }
  }

  private fun updateTimer(context: Context, transform: (TimerState) -> TimerState?) {
    load(context)
    val next = synchronized(this) { transform(_timer.value) } ?: return
    setTimer(context, next)
  }

  private fun setTimer(context: Context, state: TimerState) {
    val before = _timer.value.status
    // A new countdown, a ring or an end brings the notification back.
    if (state.status == TimerStatus.RINGING || state.status == TimerStatus.IDLE ||
      before == TimerStatus.IDLE || before == TimerStatus.RINGING
    ) {
      timerDismissed = false
    }
    synchronized(this) {
      _timer.value = state
      ClockStore.get(context).saveTimer(state)
    }
    scheduleTimerAlarm(context, state)
    ClockService.sync(context)
    listener?.let { runCatching { it("timer") } }
  }

  private fun scheduleTimerAlarm(context: Context, state: TimerState) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = timerPendingIntent(context)
    if (state.status != TimerStatus.RUNNING) {
      am.cancel(pi)
      return
    }
    try {
      // Exact and allowed in Doze, without taking over the lock screen's "next alarm".
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.endAt, pi)
    } catch (e: SecurityException) {
      Log.w(TAG, "exact alarm not permitted, falling back", e)
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.endAt, pi)
    }
  }

  private fun timerPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, ClockActionReceiver::class.java).setAction(ClockActionReceiver.TIMER_FIRE)
    return PendingIntent.getBroadcast(
      context,
      TIMER_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  // endregion

  // region Stopwatch

  fun startStopwatch(context: Context) = updateStopwatch(context) { s ->
    if (s.status == StopwatchStatus.RUNNING) null
    else s.copy(status = StopwatchStatus.RUNNING, startedAt = System.currentTimeMillis())
  }

  fun pauseStopwatch(context: Context) = updateStopwatch(context) { s ->
    if (s.status != StopwatchStatus.RUNNING) null
    else s.copy(status = StopwatchStatus.PAUSED, accumulatedMs = s.elapsed(), startedAt = 0)
  }

  fun lapStopwatch(context: Context) = updateStopwatch(context) { s ->
    if (s.status != StopwatchStatus.RUNNING || s.laps.size >= MAX_LAPS) null
    else s.copy(laps = s.laps + s.elapsed())
  }

  fun resetStopwatch(context: Context) = updateStopwatch(context) { s ->
    if (s.status == StopwatchStatus.IDLE) null else StopwatchState()
  }

  private fun updateStopwatch(context: Context, transform: (StopwatchState) -> StopwatchState?) {
    load(context)
    synchronized(this) {
      val next = transform(_stopwatch.value) ?: return
      if (_stopwatch.value.status == StopwatchStatus.IDLE || next.status == StopwatchStatus.IDLE) stopwatchDismissed = false
      _stopwatch.value = next
      ClockStore.get(context).saveStopwatch(next)
    }
    ClockService.sync(context)
    listener?.let { runCatching { it("stopwatch") } }
  }

  private const val MAX_LAPS = 999

  // endregion
}
