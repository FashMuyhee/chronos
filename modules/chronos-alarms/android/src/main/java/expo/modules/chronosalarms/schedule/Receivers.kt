package expo.modules.chronosalarms.schedule

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R
import expo.modules.chronosalarms.clock.ClockEngine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Fires when an alarm, a snooze or a test ring is due. */
class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != AlarmScheduler.ACTION_FIRE) return
    val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
    val reason = intent.getStringExtra(AlarmScheduler.EXTRA_REASON) ?: AlarmScheduler.REASON_ALARM
    val store = AlarmStore.get(context)
    val alarm = store.alarm(alarmId) ?: return

    var snoozesUsed = 0
    when (reason) {
      AlarmScheduler.REASON_ALARM -> {
        // A regular ring: one-off alarms switch off, repeating ones move to their next day.
        if (alarm.kind == AlarmKind.REPEAT) {
          AlarmScheduler.schedule(context, alarm)
        } else {
          store.upsert(alarm.copy(enabled = false))
        }
        store.removePendingRing(alarm.id)
      }
      AlarmScheduler.REASON_SNOOZE -> {
        snoozesUsed = store.pendingRing(alarm.id)?.snoozesUsed ?: 0
        store.removePendingRing(alarm.id)
      }
    }

    ChronosEvents.emitAlarmsChanged()
    val service = Intent(context, RingingService::class.java).apply {
      action = RingingService.ACTION_START
      putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
      putExtra(RingingService.EXTRA_SNOOZES_USED, snoozesUsed)
    }
    // An alarm-clock broadcast may start a foreground service from the background.
    ContextCompat.startForegroundService(context, service)
  }
}

/** Puts every alarm back after a reboot, an app update, or a clock or time-zone change. */
class RescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_LOCKED_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_TIMEZONE_CHANGED,
      "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
        // Only re-schedule here: Android 15 forbids starting most foreground services from BOOT_COMPLETED.
        AlarmScheduler.rescheduleAll(context)
        ClockEngine.restore(context)
        ChronosEvents.emitAlarmsChanged()
      }
    }
  }
}

/** Handles the Cancel button on the "Snoozed until" notification. */
class SnoozeActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_CANCEL_SNOOZE) return
    val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
    val store = AlarmStore.get(context)
    store.alarm(alarmId)?.let { AlarmScheduler.cancelSnooze(context, it) }
    store.removePendingRing(alarmId)
    Notifications.cancelSnoozed(context, alarmId)
    ChronosEvents.emitAlarmsChanged()
  }

  companion object {
    const val ACTION_CANCEL_SNOOZE = "expo.modules.chronosalarms.CANCEL_SNOOZE"
  }
}

/** Lets native code tell the JS module that the alarm list changed. */
object ChronosEvents {
  @Volatile var listener: (() -> Unit)? = null

  fun emitAlarmsChanged() {
    runCatching { listener?.invoke() }
  }
}
