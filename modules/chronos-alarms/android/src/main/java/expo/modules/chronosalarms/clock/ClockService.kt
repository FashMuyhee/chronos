package expo.modules.chronosalarms.clock

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import expo.modules.chronosalarms.data.AlarmStore
import expo.modules.chronosalarms.ringing.AlarmSound

/**
 * Keeps the running timer and stopwatch alive in the background and owns their notifications.
 * When the timer reaches zero it also plays the alarm sound and vibrates until stopped.
 *
 * A foreground service holds one notification. It anchors on the timer when there is one and on
 * the stopwatch otherwise; the other is posted as a normal notification beside it.
 */
class ClockService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private var player: MediaPlayer? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var anchorId = 0

  /** Live Update progress bars don't animate on their own; nudge the timer's once a second. */
  private val progressTick = object : Runnable {
    override fun run() {
      if (ClockEngine.timer.value.status != TimerStatus.RUNNING) return
      notifyTimer()
      handler.postDelayed(this, 1_000)
    }
  }

  private val ringLimit = Runnable { ClockEngine.resetTimer(this) }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    instance = this
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ClockEngine.load(this)
    refresh()
    return START_STICKY
  }

  /** Brings sound and notifications in line with [ClockEngine]. Main thread only. */
  fun refresh() {
    val timer = ClockEngine.timer.value
    val stopwatch = ClockEngine.stopwatch.value
    if (timer.status == TimerStatus.RINGING) startRinging() else stopRinging()

    val nm = getSystemService(NotificationManager::class.java)
    val timerId = if (timer.status == TimerStatus.RINGING) ClockNotifications.ID_TIMER_RING else ClockNotifications.ID_TIMER
    val timerNotification = if (timer.active) ClockNotifications.timer(this, timer) else null
    val stopwatchNotification = if (stopwatch.active) ClockNotifications.stopwatch(this, stopwatch) else null

    when {
      timerNotification != null -> {
        promote(timerId, timerNotification)
        if (stopwatchNotification != null) nm.notify(ClockNotifications.ID_STOPWATCH, stopwatchNotification)
        else nm.cancel(ClockNotifications.ID_STOPWATCH)
      }
      stopwatchNotification != null -> promote(ClockNotifications.ID_STOPWATCH, stopwatchNotification)
      else -> {
        // Started for a change that was undone before we ran: Android still requires one
        // startForeground call before the service may stop.
        if (anchorId == 0) {
          promote(ClockNotifications.ID_STOPWATCH, ClockNotifications.stopwatch(this, StopwatchState(StopwatchStatus.PAUSED)))
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    }
    // Clear whichever timer notification no longer applies (running vs. time's up, or none).
    if (timerNotification == null || timerId != ClockNotifications.ID_TIMER) nm.cancel(ClockNotifications.ID_TIMER)
    if (timerNotification == null || timerId != ClockNotifications.ID_TIMER_RING) nm.cancel(ClockNotifications.ID_TIMER_RING)

    handler.removeCallbacks(progressTick)
    if (ClockNotifications.liveUpdates && timer.status == TimerStatus.RUNNING) handler.postDelayed(progressTick, 1_000)
  }

  private fun notifyTimer() {
    val timer = ClockEngine.timer.value
    if (!timer.active) return
    getSystemService(NotificationManager::class.java).notify(ClockNotifications.ID_TIMER, ClockNotifications.timer(this, timer))
  }

  private fun promote(id: Int, notification: Notification) {
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
    } else {
      0
    }
    ServiceCompat.startForeground(this, id, notification, type)
    anchorId = id
  }

  private fun startRinging() {
    if (player != null) return
    acquireWakeLock()
    val settings = AlarmStore.get(this).settings()
    player = AlarmSound.player(this, AlarmSound.resolve(this, settings.defaultSoundUri))?.apply { start() }
    AlarmSound.vibrate(this)
    handler.removeCallbacks(ringLimit)
    val left = ClockEngine.RING_LIMIT_MS - (System.currentTimeMillis() - ClockEngine.timer.value.endAt)
    handler.postDelayed(ringLimit, left.coerceAtLeast(1_000))
  }

  private fun stopRinging() {
    handler.removeCallbacks(ringLimit)
    if (player == null && wakeLock == null) return
    AlarmSound.release(player)
    player = null
    AlarmSound.stopVibrating(this)
    wakeLock?.let { if (it.isHeld) it.release() }
    wakeLock = null
  }

  private fun acquireWakeLock() {
    if (wakeLock?.isHeld == true) return
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "chronos:timer").apply {
      setReferenceCounted(false)
      acquire(ClockEngine.RING_LIMIT_MS + 60_000)
    }
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    stopRinging()
    if (instance === this) instance = null
    super.onDestroy()
  }

  companion object {
    private const val TAG = "ChronosClockService"

    @Volatile private var instance: ClockService? = null
    private val main = Handler(Looper.getMainLooper())

    /** Applies the current state: refreshes a running service, or starts one if needed. */
    fun sync(context: Context) {
      val running = instance
      if (running != null) {
        main.post { running.refresh() }
        return
      }
      if (!ClockEngine.anyActive()) return
      val app = context.applicationContext
      try {
        ContextCompat.startForegroundService(app, Intent(app, ClockService::class.java))
      } catch (e: Exception) {
        // Android 12+ refuses some background starts; the exact alarm still rings the timer.
        Log.w(TAG, "could not start the clock service", e)
      }
    }
  }
}
