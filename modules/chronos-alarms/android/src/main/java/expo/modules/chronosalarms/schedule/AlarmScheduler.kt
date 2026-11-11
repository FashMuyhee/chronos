package expo.modules.chronosalarms.schedule

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Turns alarms into AlarmManager.setAlarmClock() entries.
 *
 * setAlarmClock is the only scheduling call Android treats as a user-visible alarm clock:
 * it fires in Doze, shows the status-bar alarm icon and the next alarm on the lock screen.
 */
object AlarmScheduler {
  private const val TAG = "ChronosScheduler"

  const val ACTION_FIRE = "expo.modules.chronosalarms.FIRE"
  const val EXTRA_ALARM_ID = "alarmId"
  const val EXTRA_REASON = "reason"
  const val REASON_ALARM = "alarm"
  const val REASON_SNOOZE = "snooze"
  const val REASON_TEST = "test"

  /** The next time [alarm] should ring after [now], or null if it never will. */
  fun nextTrigger(alarm: Alarm, now: Long = System.currentTimeMillis()): Long? {
    val cal = Calendar.getInstance().apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, alarm.hour)
      set(Calendar.MINUTE, alarm.minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return when (alarm.kind) {
      AlarmKind.ONCE -> {
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.timeInMillis
      }
      AlarmKind.REPEAT -> {
        if (alarm.days == 0) return null
        for (offset in 0..7) {
          val candidate = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
          if (candidate.timeInMillis > now && alarm.days and dayBit(candidate) != 0) {
            return candidate.timeInMillis
          }
        }
        null
      }
      AlarmKind.DATE -> {
        val parts = alarm.date?.split("-")?.mapNotNull { it.toIntOrNull() }
        if (parts == null || parts.size != 3) return null
        cal.set(parts[0], parts[1] - 1, parts[2])
        cal.timeInMillis.takeIf { it > now }
      }
    }
  }

  /** Monday = bit 0 … Sunday = bit 6. */
  fun dayBit(cal: Calendar): Int {
    val mondayBased = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return 1 shl mondayBased
  }

  /** Schedules (or cancels) the regular ring for one alarm. Returns the trigger time. */
  fun schedule(context: Context, alarm: Alarm): Long? {
    cancelMain(context, alarm)
    if (!alarm.enabled) return null
    val at = nextTrigger(alarm) ?: return null
    setAlarmClock(context, at, firePendingIntent(context, alarm.requestCode, alarm.id, REASON_ALARM))
    return at
  }

  fun scheduleSnooze(context: Context, alarm: Alarm, at: Long) {
    setAlarmClock(context, at, firePendingIntent(context, alarm.requestCode + 1, alarm.id, REASON_SNOOZE))
  }

  fun cancelSnooze(context: Context, alarm: Alarm) {
    alarmManager(context).cancel(firePendingIntent(context, alarm.requestCode + 1, alarm.id, REASON_SNOOZE))
  }

  fun cancel(context: Context, alarm: Alarm) {
    cancelMain(context, alarm)
    cancelSnooze(context, alarm)
  }

  /** A one-off ring a few seconds from now, so the user can try the ringing screen. */
  fun scheduleTest(context: Context, alarm: Alarm, delayMs: Long) {
    val at = System.currentTimeMillis() + delayMs
    setAlarmClock(context, at, firePendingIntent(context, TEST_REQUEST_CODE, alarm.id, REASON_TEST))
  }

  /** Re-registers every alarm and snooze. Called on boot, time changes and app start. */
  fun rescheduleAll(context: Context) {
    val store = AlarmStore.get(context)
    val now = System.currentTimeMillis()
    store.alarms().forEach { alarm ->
      runCatching { schedule(context, alarm) }.onFailure { Log.w(TAG, "schedule failed for ${alarm.id}", it) }
    }
    store.pendingRings().forEach { ring ->
      val alarm = store.alarm(ring.alarmId)
      if (alarm == null || ring.triggerAt <= now) {
        store.removePendingRing(ring.alarmId)
      } else {
        runCatching { scheduleSnooze(context, alarm, ring.triggerAt) }
      }
    }
  }

  /** The soonest upcoming ring across all alarms and snoozes. */
  fun nextUpcoming(context: Context): Pair<Alarm, Long>? {
    val store = AlarmStore.get(context)
    val regular = store.alarms().filter { it.enabled }.mapNotNull { a -> nextTrigger(a)?.let { a to it } }
    val snoozed = store.pendingRings().mapNotNull { r -> store.alarm(r.alarmId)?.let { it to r.triggerAt } }
    return (regular + snoozed).minByOrNull { it.second }
  }

  fun canScheduleExact(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

  private fun cancelMain(context: Context, alarm: Alarm) {
    alarmManager(context).cancel(firePendingIntent(context, alarm.requestCode, alarm.id, REASON_ALARM))
  }

  private fun setAlarmClock(context: Context, at: Long, operation: PendingIntent) {
    val am = alarmManager(context)
    val info = AlarmManager.AlarmClockInfo(at, showPendingIntent(context))
    try {
      am.setAlarmClock(info, operation)
    } catch (e: SecurityException) {
      // Exact alarms revoked by the user: fall back to the closest allowed call.
      Log.w(TAG, "exact alarm not permitted, falling back", e)
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
    }
  }

  private fun firePendingIntent(context: Context, requestCode: Int, alarmId: String, reason: String): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = ACTION_FIRE
      putExtra(EXTRA_ALARM_ID, alarmId)
      putExtra(EXTRA_REASON, reason)
    }
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  /** What the system opens when the user taps the next-alarm entry in quick settings. */
  private fun showPendingIntent(context: Context): PendingIntent {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
      ?: Intent().setPackage(context.packageName)
    return PendingIntent.getActivity(context, SHOW_REQUEST_CODE, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun alarmManager(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private const val TEST_REQUEST_CODE = 7
  private const val SHOW_REQUEST_CODE = 9
}
