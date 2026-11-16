package expo.modules.chronosalarms.clock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import expo.modules.chronosalarms.R
import expo.modules.chronosalarms.ringing.Notifications
import expo.modules.chronosalarms.ui.TimerRingingActivity

/**
 * Timer and stopwatch notifications. The running ones use the system chronometer, so the shade
 * counts without the app waking up every second.
 *
 * On Android 16+ they also ask to be promoted to Live Updates (status-bar chip, top of the
 * shade, lock screen). The timer adds a ProgressStyle bar there. Older versions get the same
 * notification without promotion or the progress bar.
 */
object ClockNotifications {
  const val CHANNEL_CLOCK = "chronos_clock"
  const val ID_TIMER = 4301
  const val ID_STOPWATCH = 4302
  const val ID_TIMER_RING = 4303

  /** Android 16 (API 36) added promoted ongoing notifications and ProgressStyle. */
  val liveUpdates: Boolean get() = Build.VERSION.SDK_INT >= 36

  private const val ACCENT = 0xFF8C1D30.toInt()

  fun ensureChannel(context: Context) {
    Notifications.ensureChannels(context)
    // Default importance so Android 16 will promote it; silent because it updates often.
    val channel = NotificationChannel(CHANNEL_CLOCK, "Timer and stopwatch", NotificationManager.IMPORTANCE_DEFAULT).apply {
      description = "The running timer and stopwatch"
      setSound(null, null)
      enableVibration(false)
      setShowBadge(false)
      lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  fun timer(context: Context, t: TimerState): Notification {
    ensureChannel(context)
    if (t.status == TimerStatus.RINGING) return timesUp(context, t)
    val now = System.currentTimeMillis()
    val remaining = t.remaining(now).coerceAtLeast(0)
    val running = t.status == TimerStatus.RUNNING
    val builder = base(context, CHANNEL_CLOCK, "timer")
      .setSmallIcon(R.drawable.chronos_ic_hourglass_top)
      .setContentTitle(if (running) "Timer" else "Timer paused")
      .setContentText(if (running) "${formatLength(t.totalMs)} timer" else "${formatClock(remaining)} left")
    if (running) {
      builder.setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true).setWhen(t.endAt)
      builder.addAction(R.drawable.chronos_ic_pause, "Pause", action(context, ClockActionReceiver.TIMER_PAUSE))
    } else {
      builder.setShowWhen(false).setShortCriticalText(formatClock(remaining))
      builder.addAction(R.drawable.chronos_ic_play_arrow, "Resume", action(context, ClockActionReceiver.TIMER_RESUME))
    }
    builder.addAction(0, "+1 min", action(context, ClockActionReceiver.TIMER_ADD))
    builder.addAction(R.drawable.chronos_ic_stop, "Cancel", action(context, ClockActionReceiver.TIMER_RESET))
    if (liveUpdates && t.totalMs > 0) {
      val max = 1000
      val done = (((t.totalMs - remaining).toDouble() / t.totalMs) * max).toInt().coerceIn(0, max)
      builder.setStyle(
        NotificationCompat.ProgressStyle()
          .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(max).setColor(ACCENT)))
          .setStyledByProgress(true)
          .setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.chronos_ic_hourglass_top))
          .setProgress(done)
      )
    }
    return builder.build()
  }

  /** Heads-up while the phone is in use; the full-screen "Time's up" screen when it is locked. */
  private fun timesUp(context: Context, t: TimerState): Notification {
    val fullScreen = PendingIntent.getActivity(
      context,
      40,
      Intent(context, TimerRingingActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Builder(context, Notifications.CHANNEL_RINGING)
      .setSmallIcon(R.drawable.chronos_ic_hourglass_top)
      .setColor(ACCENT)
      .setContentTitle("Time's up")
      .setContentText("${formatLength(t.lastMs)} timer")
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      // Counts up from zero: how long it has been ringing.
      .setUsesChronometer(true)
      .setShowWhen(true)
      .setWhen(t.endAt)
      .setFullScreenIntent(fullScreen, true)
      .setContentIntent(fullScreen)
      .addAction(0, "+1 min", action(context, ClockActionReceiver.TIMER_ADD))
      .addAction(R.drawable.chronos_ic_stop, "Stop", action(context, ClockActionReceiver.TIMER_RESET))
      .build()
  }

  fun stopwatch(context: Context, s: StopwatchState): Notification {
    ensureChannel(context)
    val now = System.currentTimeMillis()
    val running = s.status == StopwatchStatus.RUNNING
    val builder = base(context, CHANNEL_CLOCK, "stopwatch")
      .setSmallIcon(R.drawable.chronos_ic_timer)
      .setContentTitle(if (running) "Stopwatch" else "Stopwatch paused")
    val lapText = if (s.laps.isEmpty()) null else "Lap ${s.laps.size + 1}"
    if (running) {
      builder.setUsesChronometer(true).setShowWhen(true).setWhen(now - s.elapsed(now))
      lapText?.let(builder::setContentText)
      builder.addAction(R.drawable.chronos_ic_pause, "Pause", action(context, ClockActionReceiver.STOPWATCH_PAUSE))
      builder.addAction(R.drawable.chronos_ic_flag, "Lap", action(context, ClockActionReceiver.STOPWATCH_LAP))
    } else {
      val elapsed = formatClock(s.elapsed(now))
      builder.setShowWhen(false).setContentText(listOfNotNull(elapsed, lapText).joinToString(" · ")).setShortCriticalText(elapsed)
      builder.addAction(R.drawable.chronos_ic_play_arrow, "Resume", action(context, ClockActionReceiver.STOPWATCH_START))
      builder.addAction(R.drawable.chronos_ic_restart_alt, "Reset", action(context, ClockActionReceiver.STOPWATCH_RESET))
    }
    return builder.build()
  }

  private fun base(context: Context, channel: String, tab: String): NotificationCompat.Builder =
    NotificationCompat.Builder(context, channel)
      .setColor(ACCENT)
      .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setAutoCancel(false)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .setContentIntent(openTab(context, tab))
      .setDeleteIntent(
        action(context, if (tab == "timer") ClockActionReceiver.TIMER_DISMISSED else ClockActionReceiver.STOPWATCH_DISMISSED)
      )
      .apply { if (liveUpdates) setRequestPromotedOngoing(true) }

  /** Opens the app on a tab through its Expo Router deep link (chronos://timer). */
  fun openTab(context: Context, tab: String): PendingIntent {
    val intent = deepLink(context, tab).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return PendingIntent.getActivity(context, 50 + tab.length, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }

  fun deepLink(context: Context, path: String): Intent {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    return Intent(Intent.ACTION_VIEW, Uri.parse("chronos://$path")).apply {
      launch?.component?.let { component = it } ?: setPackage(context.packageName)
    }
  }

  private fun action(context: Context, action: String): PendingIntent {
    val intent = Intent(context, ClockActionReceiver::class.java).setAction(action)
    return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }

  /** 1:05:09, 5:09 or 0:09. */
  fun formatClock(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
  }

  /** "5 min", "1 h 30 min", "45 s". */
  fun formatLength(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return listOfNotNull(
      if (h > 0) "$h h" else null,
      if (m > 0) "$m min" else null,
      if (s > 0 || total == 0L) "$s s" else null
    ).joinToString(" ")
  }
}
