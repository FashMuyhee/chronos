package expo.modules.chronosalarms.clock

import org.json.JSONArray
import org.json.JSONObject

enum class TimerStatus(val id: String) {
  IDLE("idle"),
  RUNNING("running"),
  PAUSED("paused"),
  /** Reached zero and is sounding until stopped. */
  RINGING("ringing");

  companion object {
    fun from(id: String?) = entries.firstOrNull { it.id == id } ?: IDLE
  }
}

/**
 * The countdown timer. All times are wall-clock epoch milliseconds so they survive the process
 * being killed and match the notification chronometer, which counts in the same clock.
 *
 * - RUNNING / RINGING: [endAt] is when it reaches (or reached) zero.
 * - PAUSED: [remainingMs] is frozen.
 * [totalMs] is the length including any "+1 min", which the progress ring measures against.
 * [lastMs] is the last length the user started, offered again after the timer ends.
 */
data class TimerState(
  val status: TimerStatus = TimerStatus.IDLE,
  val totalMs: Long = 0,
  val endAt: Long = 0,
  val remainingMs: Long = 0,
  val lastMs: Long = 0
) {
  val active: Boolean get() = status != TimerStatus.IDLE

  fun remaining(now: Long = System.currentTimeMillis()): Long = when (status) {
    TimerStatus.RUNNING, TimerStatus.RINGING -> endAt - now
    TimerStatus.PAUSED -> remainingMs
    TimerStatus.IDLE -> 0
  }

  fun toJson(): JSONObject = JSONObject()
    .put("status", status.id)
    .put("totalMs", totalMs)
    .put("endAt", endAt)
    .put("remainingMs", remainingMs)
    .put("lastMs", lastMs)

  fun toMap(): Map<String, Any> = mapOf(
    "status" to status.id,
    "totalMs" to totalMs.toDouble(),
    "endAt" to endAt.toDouble(),
    "remainingMs" to remainingMs.toDouble(),
    "lastMs" to lastMs.toDouble()
  )

  companion object {
    fun fromJson(o: JSONObject) = TimerState(
      status = TimerStatus.from(o.optString("status")),
      totalMs = o.optLong("totalMs"),
      endAt = o.optLong("endAt"),
      remainingMs = o.optLong("remainingMs"),
      lastMs = o.optLong("lastMs")
    )
  }
}

enum class StopwatchStatus(val id: String) {
  IDLE("idle"),
  RUNNING("running"),
  PAUSED("paused");

  companion object {
    fun from(id: String?) = entries.firstOrNull { it.id == id } ?: IDLE
  }
}

/**
 * The stopwatch: elapsed = [accumulatedMs] + (now - [startedAt]) while RUNNING.
 * [laps] holds the total elapsed time at each lap press, oldest first.
 */
data class StopwatchState(
  val status: StopwatchStatus = StopwatchStatus.IDLE,
  val startedAt: Long = 0,
  val accumulatedMs: Long = 0,
  val laps: List<Long> = emptyList()
) {
  val active: Boolean get() = status != StopwatchStatus.IDLE

  fun elapsed(now: Long = System.currentTimeMillis()): Long =
    accumulatedMs + if (status == StopwatchStatus.RUNNING) (now - startedAt).coerceAtLeast(0) else 0

  /** Time on the lap in progress. */
  fun currentLap(now: Long = System.currentTimeMillis()): Long = elapsed(now) - (laps.lastOrNull() ?: 0)

  fun toJson(): JSONObject = JSONObject()
    .put("status", status.id)
    .put("startedAt", startedAt)
    .put("accumulatedMs", accumulatedMs)
    .put("laps", JSONArray(laps))

  fun toMap(): Map<String, Any> = mapOf(
    "status" to status.id,
    "startedAt" to startedAt.toDouble(),
    "accumulatedMs" to accumulatedMs.toDouble(),
    "laps" to laps.map { it.toDouble() }
  )

  companion object {
    fun fromJson(o: JSONObject): StopwatchState {
      val arr = o.optJSONArray("laps") ?: JSONArray()
      return StopwatchState(
        status = StopwatchStatus.from(o.optString("status")),
        startedAt = o.optLong("startedAt"),
        accumulatedMs = o.optLong("accumulatedMs"),
        laps = List(arr.length()) { arr.getLong(it) }
      )
    }
  }
}
