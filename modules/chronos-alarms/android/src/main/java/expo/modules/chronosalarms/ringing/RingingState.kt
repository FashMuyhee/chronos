package expo.modules.chronosalarms.ringing

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the ringing screen should show. */
enum class RingPhase {
  /** Sound is playing. */
  RINGING,

  /** The user gave up on the phrase: quiet until the next re-ring at [RingSession.nextRingAt]. */
  WAITING
}

data class RingSession(
  val alarm: Alarm,
  val phrase: String,
  val challenge: Challenge,
  val snoozeMinutes: Int,
  val snoozesUsed: Int,
  val maxSnoozes: Int,
  val phase: RingPhase = RingPhase.RINGING,
  val ducked: Boolean = false,
  /** Power or a volume key was pressed: this ring is silent but the alarm is still on. */
  val muted: Boolean = false,
  /** 0 until the user gives up; then the number of the next (or current) re-ring. */
  val giveUpRound: Int = 0,
  val giveUpRings: Int,
  val giveUpIntervalMinutes: Int,
  val nextRingAt: Long? = null,
  val startedAt: Long = System.currentTimeMillis()
) {
  val snoozesLeft: Int get() = (maxSnoozes - snoozesUsed).coerceAtLeast(0)
  val inGiveUp: Boolean get() = giveUpRound > 0
}

/** In-process bridge between [RingingService] (writer) and [RingingActivity] (reader). */
object RingingState {
  private val _session = MutableStateFlow<RingSession?>(null)
  val session: StateFlow<RingSession?> = _session.asStateFlow()

  internal fun set(value: RingSession?) {
    _session.value = value
  }

  internal fun update(transform: (RingSession) -> RingSession) {
    _session.value?.let { _session.value = transform(it) }
  }
}
