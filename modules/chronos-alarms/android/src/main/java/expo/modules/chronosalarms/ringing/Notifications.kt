package expo.modules.chronosalarms.ringing

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.DateFormat
import java.util.Date

object Notifications {
  const val CHANNEL_RINGING = "chronos_ringing"
  const val CHANNEL_STATUS = "chronos_status"
  const val ID_RINGING = 4201
  /** Same alarm, posted on the quiet channel while the ringing screen is on top. */
  const val ID_RINGING_QUIET = 4202

  fun ringingId(screenVisible: Boolean) = if (screenVisible) ID_RINGING_QUIET else ID_RINGING

  fun ensureChannels(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)
    val ringing = NotificationChannel(CHANNEL_RINGING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
      description = "Shows the alarm screen when an alarm goes off"
      // Chronos plays the alarm sound itself on the alarm audio stream.
      setSound(null, null)
      enableVibration(false)
      lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
    val status = NotificationChannel(CHANNEL_STATUS, "Snoozed and missed alarms", NotificationManager.IMPORTANCE_LOW).apply {
      description = "Snooze countdowns and alarms that stopped on their own"
    }
    nm.createNotificationChannels(listOf(ringing, status))
  }

  /**
   * The ongoing ringing notification. While the ringing screen is on top ([screenVisible]) it is
   * posted under a different ID on the low-importance channel without a full-screen intent.
   * Updating the original would keep its heads-up banner pinned; a new ID on a quiet channel
   * never shows one, and cancelling the original removes the banner at once.
   */
  fun ringing(context: Context, session: RingSession, screenVisible: Boolean = false): Notification {
    ensureChannels(context)
    val fullScreen = activityIntent(context, 1)
    val title = session.alarm.label.ifBlank { "Alarm" }
    val time = formatTime(context, session.alarm.hour, session.alarm.minute)
    val channel = if (screenVisible) CHANNEL_STATUS else CHANNEL_RINGING
    val builder = NotificationCompat.Builder(context, channel)
      .setSmallIcon(R.drawable.chronos_ic_alarm)
      .setContentTitle(title)
      .setContentText(if (session.phase == RingPhase.WAITING) "Rings again soon. Open to stop it." else time)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setPriority(if (screenVisible) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setAutoCancel(false)
      .setContentIntent(fullScreen)
    if (!screenVisible) builder.setFullScreenIntent(fullScreen, true)
    if (session.snoozesLeft > 0 && !session.inGiveUp) {
      builder.addAction(0, "Snooze ${session.snoozeMinutes} min", serviceIntent(context, RingingService.ACTION_SNOOZE, 2))
    }
    // Stopping goes through the wake-up check when one is set.
    val stop = if (session.challenge == Challenge.OFF) {
      serviceIntent(context, RingingService.ACTION_DISMISS, 3)
    } else {
      activityIntent(context, 4)
    }
    builder.addAction(0, "Stop", stop)
    return builder.build()
  }

  fun showSnoozed(context: Context, alarm: Alarm, until: Long) {
    ensureChannels(context)
    val cancel = Intent(context, SnoozeActionReceiver::class.java).apply {
      action = SnoozeActionReceiver.ACTION_CANCEL_SNOOZE
      putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
    }
    val cancelPi = PendingIntent.getBroadcast(context, alarm.requestCode + 1, cancel, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val n = NotificationCompat.Builder(context, CHANNEL_STATUS)
      .setSmallIcon(R.drawable.chronos_ic_alarm)
      .setContentTitle("Snoozed until ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(until))}")
      .setContentText(alarm.label.ifBlank { "Alarm" })
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setShowWhen(true)
      .setWhen(until)
      .setUsesChronometer(true)
      .setChronometerCountDown(true)
      .setContentIntent(launchIntent(context))
      .addAction(0, "Cancel snooze", cancelPi)
      .build()
    context.getSystemService(NotificationManager::class.java).notify(snoozeId(alarm.id), n)
  }

  fun cancelSnoozed(context: Context, alarmId: String) {
    context.getSystemService(NotificationManager::class.java).cancel(snoozeId(alarmId))
  }

  fun showMissed(context: Context, alarm: Alarm, reason: String) {
    ensureChannels(context)
    val n = NotificationCompat.Builder(context, CHANNEL_STATUS)
      .setSmallIcon(R.drawable.chronos_ic_alarm)
      .setContentTitle("${alarm.label.ifBlank { "Alarm" }} stopped")
      .setContentText(reason)
      .setAutoCancel(true)
      .setContentIntent(launchIntent(context))
      .build()
    context.getSystemService(NotificationManager::class.java).notify(snoozeId(alarm.id) + 1, n)
  }

  fun formatTime(context: Context, hour: Int, minute: Int): String {
    val cal = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.HOUR_OF_DAY, hour)
      set(java.util.Calendar.MINUTE, minute)
    }
    return android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
  }

  private fun snoozeId(alarmId: String) = 5000 + (alarmId.hashCode() and 0xFFFF) * 2

  private fun activityIntent(context: Context, requestCode: Int): PendingIntent {
    val intent = Intent(context, RingingActivity::class.java).addFlags(
      Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_SINGLE_TOP
    )
    return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent {
    val intent = Intent(context, RingingService::class.java).setAction(action)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      PendingIntent.getForegroundService(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    } else {
      PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
  }

  private fun launchIntent(context: Context): PendingIntent {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
    return PendingIntent.getActivity(context, 6, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }
}
