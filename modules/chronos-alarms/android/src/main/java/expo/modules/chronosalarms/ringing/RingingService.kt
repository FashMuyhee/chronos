package expo.modules.chronosalarms.ringing

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
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
import java.util.Calendar

/**
 * Plays a ringing alarm: sound on the alarm stream with a volume ramp, vibration, a wake lock,
 * and the snooze / give-up timers. The ringing screen reads its state from [RingingState].
 */
class RingingService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private var player: MediaPlayer? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var volume = 0f
  private var targetVolume = 1f
  private var rampStep = 0.05f
  private var screenVisible = false
  private var screenOffReceiver: BroadcastReceiver? = null

  private val rampTick = object : Runnable {
    override fun run() {
      val p = player ?: return
      volume = if (volume < targetVolume) (volume + rampStep).coerceAtMost(targetVolume) else targetVolume
      runCatching { p.setVolume(volume, volume) }
      handler.postDelayed(this, RAMP_TICK_MS)
    }
  }

  private val unduck = Runnable {
    targetVolume = 1f
    RingingState.update { it.copy(ducked = false) }
  }

  /** Stops an alarm nobody touched, so it can't ring all day. */
  private val unattendedTimeout = Runnable {
    val session = RingingState.session.value ?: return@Runnable
    Notifications.showMissed(this, session.alarm, "It rang for $UNATTENDED_MINUTES minutes without being stopped.")
    finish()
  }

  /** In give-up mode: the quiet minute is over, ring again. */
  private val reRing = Runnable {
    RingingState.update { it.copy(phase = RingPhase.RINGING, nextRingAt = null, muted = false) }
    refreshNotification()
    startSound()
    val seconds = AlarmStore.get(this).settings().giveUpRingSeconds
    handler.postDelayed(endReRing, seconds * 1000L)
  }

  /** In give-up mode: this re-ring is over; wait for the next one or stop for good. */
  private val endReRing = Runnable {
    val session = RingingState.session.value ?: return@Runnable
    if (session.giveUpRound >= session.giveUpRings) {
      Notifications.showMissed(this, session.alarm, "It rang ${session.giveUpRings} more times, then stopped for today.")
      finish()
    } else {
      waitForNextRing(session.giveUpRound + 1)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        val alarm = id?.let { AlarmStore.get(this).alarm(it) }
        if (alarm == null) {
          stopSelfSafely()
        } else {
          start(alarm, intent.getIntExtra(EXTRA_SNOOZES_USED, 0))
        }
      }
      ACTION_SNOOZE -> {
        ensureForeground()
        snooze(intent.getIntExtra(EXTRA_MINUTES, -1))
      }
      ACTION_DISMISS -> {
        ensureForeground()
        finish()
      }
      ACTION_DUCK -> duck()
      ACTION_MUTE -> mute()
      ACTION_GIVE_UP -> giveUp()
      ACTION_SCREEN_SHOWN, ACTION_SCREEN_HIDDEN -> {
        screenVisible = intent.action == ACTION_SCREEN_SHOWN
        refreshNotification()
      }
      else -> if (RingingState.session.value == null) stopSelfSafely()
    }
    return START_NOT_STICKY
  }

  private fun start(alarm: Alarm, snoozesUsed: Int) {
    // A new alarm replaces whatever was ringing.
    handler.removeCallbacksAndMessages(null)
    stopSound()
    val settings = AlarmStore.get(this).settings()
    val session = RingSession(
      alarm = alarm,
      phrase = settings.phraseFor(alarm, Calendar.getInstance().get(Calendar.DAY_OF_YEAR)),
      challenge = alarm.challenge ?: settings.challenge,
      snoozeMinutes = alarm.snoozeMinutes ?: settings.snoozeMinutes,
      snoozesUsed = snoozesUsed,
      maxSnoozes = settings.maxSnoozes,
      giveUpRings = settings.giveUpRings,
      giveUpIntervalMinutes = settings.giveUpIntervalMinutes
    )
    RingingState.set(session)
    Notifications.cancelSnoozed(this, alarm.id)
    ensureForeground()
    acquireWakeLock()
    startSound()
    openRingingScreen()
    listenForPowerButton()
    handler.postDelayed(unattendedTimeout, UNATTENDED_MINUTES * 60_000L)
  }

  /**
   * Power or volume key while ringing: silence this ring, like the system Clock app. The alarm
   * stays active (screen, notification, snooze and stop all still work), and in give-up mode
   * the next re-ring sounds again.
   */
  private fun mute() {
    val session = RingingState.session.value ?: return
    if (session.phase != RingPhase.RINGING || session.muted) return
    stopSound()
    RingingState.update { it.copy(muted = true) }
  }

  /** The power button can't be intercepted by an activity; the screen turning off is the signal. */
  private fun listenForPowerButton() {
    if (screenOffReceiver != null) return
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) mute()
      }
    }
    registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    screenOffReceiver = receiver
  }

  private fun stopListeningForPowerButton() {
    screenOffReceiver?.let { runCatching { unregisterReceiver(it) } }
    screenOffReceiver = null
  }

  /**
   * The full-screen notification opens the ringing screen when the phone is locked. When the
   * phone is in use Android shows it only as a heads-up banner, so also try to open it directly;
   * Android allows that while Chronos is visible and silently ignores it otherwise.
   */
  private fun openRingingScreen() {
    runCatching {
      startActivity(
        Intent(this, RingingActivity::class.java).addFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
      )
    }.onFailure { Log.i(TAG, "ringing screen not opened directly; relying on the notification", it) }
  }

  private fun snooze(requestedMinutes: Int) {
    val session = RingingState.session.value ?: return finish()
    if (session.snoozesLeft <= 0) return
    val minutes = if (requestedMinutes > 0) requestedMinutes.coerceIn(1, 60) else session.snoozeMinutes
    val at = System.currentTimeMillis() + minutes * 60_000L
    val store = AlarmStore.get(this)
    store.putPendingRing(PendingRing(session.alarm.id, at, session.snoozesUsed + 1))
    AlarmScheduler.scheduleSnooze(this, session.alarm, at)
    Notifications.showSnoozed(this, session.alarm, at)
    ChronosEvents.emitAlarmsChanged()
    finish(clearSnooze = false)
  }

  private fun duck() {
    if (RingingState.session.value?.phase != RingPhase.RINGING) return
    targetVolume = DUCKED_VOLUME
    volume = minOf(volume, DUCKED_VOLUME)
    runCatching { player?.setVolume(volume, volume) }
    RingingState.update { it.copy(ducked = true) }
    handler.removeCallbacks(unduck)
    handler.postDelayed(unduck, UNDUCK_AFTER_MS)
    // Someone is answering: restart the unattended timer.
    handler.removeCallbacks(unattendedTimeout)
    handler.postDelayed(unattendedTimeout, UNATTENDED_MINUTES * 60_000L)
  }

  private fun giveUp() {
    val session = RingingState.session.value ?: return
    if (session.inGiveUp) return
    handler.removeCallbacks(unattendedTimeout)
    waitForNextRing(1)
  }

  private fun waitForNextRing(round: Int) {
    val session = RingingState.session.value ?: return
    stopSound()
    val delay = session.giveUpIntervalMinutes * 60_000L
    RingingState.update {
      it.copy(phase = RingPhase.WAITING, giveUpRound = round, nextRingAt = System.currentTimeMillis() + delay, ducked = false)
    }
    refreshNotification()
    handler.removeCallbacks(reRing)
    handler.postDelayed(reRing, delay)
  }

  private fun finish(clearSnooze: Boolean = true) {
    val session = RingingState.session.value
    if (clearSnooze && session != null) {
      AlarmStore.get(this).removePendingRing(session.alarm.id)
    }
    handler.removeCallbacksAndMessages(null)
    stopSound()
    stopListeningForPowerButton()
    RingingState.set(null)
    releaseWakeLock()
    ChronosEvents.emitAlarmsChanged()
    stopSelfSafely()
  }

  private fun ensureForeground() {
    val session = RingingState.session.value
    val notification = if (session != null) {
      Notifications.ringing(this, session, screenVisible)
    } else {
      // Asked to snooze or stop with nothing ringing: satisfy the foreground contract, then stop.
      Notifications.ensureChannels(this)
      androidx.core.app.NotificationCompat.Builder(this, Notifications.CHANNEL_STATUS)
        .setSmallIcon(R.drawable.chronos_ic_alarm)
        .setContentTitle("Chronos")
        .build()
    }
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
    } else {
      0
    }
    ServiceCompat.startForeground(this, Notifications.ringingId(screenVisible), notification, type)
  }

  /** Moves the foreground notification to the ID that matches [screenVisible], removing the other. */
  private fun refreshNotification() {
    if (RingingState.session.value == null) return
    ensureForeground()
    getSystemService(NotificationManager::class.java).cancel(Notifications.ringingId(!screenVisible))
  }

  private fun startSound() {
    stopSound()
    val session = RingingState.session.value ?: return
    val settings = AlarmStore.get(this).settings()
    val uri = AlarmSound.resolve(this, session.alarm.soundUri ?: settings.defaultSoundUri)

    val ramp = settings.rampSeconds
    volume = if (ramp > 0) MIN_VOLUME else 1f
    targetVolume = 1f
    rampStep = if (ramp > 0) (1f - MIN_VOLUME) / (ramp * 1000f / RAMP_TICK_MS) else 1f

    player = AlarmSound.player(this, uri)
    player?.setVolume(volume, volume)
    player?.start()
    handler.post(rampTick)
    if (session.alarm.vibrate) AlarmSound.vibrate(this)
  }

  private fun stopSound() {
    handler.removeCallbacks(rampTick)
    AlarmSound.release(player)
    player = null
    AlarmSound.stopVibrating(this)
  }

  private fun acquireWakeLock() {
    if (wakeLock?.isHeld == true) return
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "chronos:ringing").apply {
      setReferenceCounted(false)
      acquire(WAKE_LOCK_MS)
    }
  }

  private fun releaseWakeLock() {
    wakeLock?.let { if (it.isHeld) it.release() }
    wakeLock = null
  }

  private fun stopSelfSafely() {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    getSystemService(NotificationManager::class.java).apply {
      cancel(Notifications.ID_RINGING)
      cancel(Notifications.ID_RINGING_QUIET)
    }
    stopSelf()
  }

  override fun onDestroy() {
    stopListeningForPowerButton()
    handler.removeCallbacksAndMessages(null)
    stopSound()
    releaseWakeLock()
    super.onDestroy()
  }

  companion object {
    private const val TAG = "ChronosRinging"

    const val ACTION_START = "expo.modules.chronosalarms.START"
    const val ACTION_SNOOZE = "expo.modules.chronosalarms.SNOOZE"
    const val ACTION_DISMISS = "expo.modules.chronosalarms.DISMISS"
    const val ACTION_DUCK = "expo.modules.chronosalarms.DUCK"
    const val ACTION_MUTE = "expo.modules.chronosalarms.MUTE"
    const val ACTION_GIVE_UP = "expo.modules.chronosalarms.GIVE_UP"
    const val ACTION_SCREEN_SHOWN = "expo.modules.chronosalarms.SCREEN_SHOWN"
    const val ACTION_SCREEN_HIDDEN = "expo.modules.chronosalarms.SCREEN_HIDDEN"
    const val EXTRA_SNOOZES_USED = "snoozesUsed"
    const val EXTRA_MINUTES = "minutes"

    private const val MIN_VOLUME = 0.12f
    private const val DUCKED_VOLUME = 0.3f
    private const val RAMP_TICK_MS = 250L
    private const val UNDUCK_AFTER_MS = 15_000L
    private const val UNATTENDED_MINUTES = 10
    private const val WAKE_LOCK_MS = 30 * 60_000L

    fun send(context: Context, action: String, configure: Intent.() -> Unit = {}) {
      val intent = Intent(context, RingingService::class.java).setAction(action).apply(configure)
      context.startService(intent)
    }
  }
}
