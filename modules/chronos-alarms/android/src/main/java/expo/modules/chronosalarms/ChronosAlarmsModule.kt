package expo.modules.chronosalarms

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.clock.*
import expo.modules.chronosalarms.R

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.core.app.NotificationManagerCompat
import com.facebook.react.common.assets.ReactFontManager
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.views.ComposableScope

class ChronosAlarmsModule : Module() {
  private val context: Context
    get() = appContext.reactContext ?: throw CodedException("ERR_NO_CONTEXT", "React context is not available", null)

  private var pendingSoundPromise: Promise? = null

  override fun definition() = ModuleDefinition {
    Name("ChronosAlarms")

    Events("onAlarmsChanged", "onClockChanged")

    OnCreate {
      ChronosEvents.listener = { sendEvent("onAlarmsChanged", emptyMap<String, Any>()) }
      ClockEngine.listener = { kind -> sendEvent("onClockChanged", clockMap(kind)) }
      appContext.reactContext?.let { ctx ->
        // The ringing screen's font files double as the JS fonts, so the APK carries one copy.
        // Family names must match `fonts` in src/theme/tokens.ts.
        val fontManager = ReactFontManager.getInstance()
        fontManager.addCustomFont(ctx, "ChronosText", R.font.chronos_text)
        fontManager.addCustomFont(ctx, "ChronosTextMedium", R.font.chronos_text_medium)
        fontManager.addCustomFont(ctx, "ChronosDisplay", R.font.chronos_display)
        Notifications.ensureChannels(ctx)
        AlarmScheduler.rescheduleAll(ctx)
        // The app is in the foreground here, so the clock service may start if something runs.
        ClockEngine.restore(ctx)
        AppShortcuts.publish(ctx)
      }
    }

    OnDestroy {
      ChronosEvents.listener = null
      ClockEngine.listener = null
    }

    // region Timer and stopwatch. State lives in ClockEngine; each call returns the new state.

    Function("getClock") {
      ClockEngine.load(context)
      clockMap(null)
    }

    Function("startTimer") { durationMs: Double ->
      ClockEngine.startTimer(context, durationMs.toLong())
      ClockEngine.timer.value.toMap()
    }

    Function("pauseTimer") { ClockEngine.pauseTimer(context); ClockEngine.timer.value.toMap() }
    Function("resumeTimer") { ClockEngine.resumeTimer(context); ClockEngine.timer.value.toMap() }
    Function("addTimerMinute") { ClockEngine.addMinute(context); ClockEngine.timer.value.toMap() }
    Function("resetTimer") { ClockEngine.resetTimer(context); ClockEngine.timer.value.toMap() }

    Function("startStopwatch") { ClockEngine.startStopwatch(context); ClockEngine.stopwatch.value.toMap() }
    Function("pauseStopwatch") { ClockEngine.pauseStopwatch(context); ClockEngine.stopwatch.value.toMap() }
    Function("lapStopwatch") { ClockEngine.lapStopwatch(context); ClockEngine.stopwatch.value.toMap() }
    Function("resetStopwatch") { ClockEngine.resetStopwatch(context); ClockEngine.stopwatch.value.toMap() }

    /** The stopwatch face, drawn natively so it can sweep every frame. Use inside an expo-ui Host. */
    View<StopwatchDialProps>("StopwatchDialView") {
      Content { props -> StopwatchDial(props) }
    }

    View<LapListProps>("LapListView") {
      Content { props -> LapList(props) }
    }

    View<TimerFaceProps>("TimerFaceView") {
      Content { props -> TimerFace(props) }
    }

    View<MorphButtonProps>("MorphButtonView") {
      val onPress by Event<Unit>()
      Content { props -> MorphButton(props) { onPress(Unit) } }
    }

    /** Home: its children are pages, swiped under a frosted-glass tab bar. */
    View<GlassTabScaffoldProps>("GlassTabScaffoldView") {
      val onTabSelect by Event<IndexEvent>()
      val onPageChange by Event<IndexEvent>()
      val onAction by Event<Unit>()
      Content { props ->
        GlassTabScaffold(
          props,
          pageCount = view.childCount,
          onTabSelect = { onTabSelect(IndexEvent(it)) },
          onPageChange = { onPageChange(IndexEvent(it)) },
          onAction = { onAction(Unit) }
        ) { index -> Child(ComposableScope(), index) }
      }
    }

    // endregion

    Function("getAlarms") {
      AlarmStore.get(context).alarms().map { it.toMap(if (it.enabled) AlarmScheduler.nextTrigger(it) else null) }
    }

    Function("saveAlarm") { payload: Map<String, Any?> ->
      val store = AlarmStore.get(context)
      val base = (payload["id"] as? String)?.let { store.alarm(it) }
      val saved = store.upsert(Alarm.fromMap(payload, base))
      // Editing an alarm cancels any snooze that was waiting on it.
      AlarmScheduler.cancelSnooze(context, saved)
      store.removePendingRing(saved.id)
      Notifications.cancelSnoozed(context, saved.id)
      val at = AlarmScheduler.schedule(context, saved)
      saved.toMap(at)
    }

    Function("setAlarmEnabled") { id: String, enabled: Boolean ->
      val store = AlarmStore.get(context)
      val alarm = store.alarm(id) ?: return@Function null
      val saved = store.upsert(alarm.copy(enabled = enabled))
      if (!enabled) {
        AlarmScheduler.cancel(context, saved)
        store.removePendingRing(id)
        Notifications.cancelSnoozed(context, id)
      }
      saved.toMap(AlarmScheduler.schedule(context, saved))
    }

    Function("deleteAlarm") { id: String ->
      val store = AlarmStore.get(context)
      store.alarm(id)?.let { AlarmScheduler.cancel(context, it) }
      Notifications.cancelSnoozed(context, id)
      store.delete(id) != null
    }

    Function("getNextAlarm") {
      val (alarm, at) = AlarmScheduler.nextUpcoming(context) ?: return@Function null
      mapOf(
        "alarmId" to alarm.id,
        "label" to alarm.label,
        "triggerAt" to at.toDouble(),
        "snoozed" to (AlarmStore.get(context).pendingRing(alarm.id)?.triggerAt == at)
      )
    }

    Function("getSnoozed") {
      AlarmStore.get(context).pendingRings().map { mapOf("alarmId" to it.alarmId, "triggerAt" to it.triggerAt.toDouble()) }
    }

    Function("cancelSnooze") { id: String ->
      val store = AlarmStore.get(context)
      store.alarm(id)?.let { AlarmScheduler.cancelSnooze(context, it) }
      store.removePendingRing(id)
      Notifications.cancelSnoozed(context, id)
    }

    Function("getSettings") {
      AlarmStore.get(context).settings().toMap()
    }

    Function("saveSettings") { payload: Map<String, Any?> ->
      val store = AlarmStore.get(context)
      val next = Settings.fromMap(payload, store.settings())
      store.saveSettings(next)
      next.toMap()
    }

    Function("testAlarm") { id: String, delaySeconds: Int ->
      val alarm = AlarmStore.get(context).alarm(id) ?: return@Function false
      AlarmScheduler.scheduleTest(context, alarm, delaySeconds.coerceIn(1, 120) * 1000L)
      true
    }

    Function("getPermissionStatus") {
      val ctx = context
      val nm = ctx.getSystemService(NotificationManager::class.java)
      val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
      mapOf(
        "notifications" to NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
        "fullScreen" to (Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()),
        "exactAlarms" to AlarmScheduler.canScheduleExact(ctx),
        "batteryUnrestricted" to pm.isIgnoringBatteryOptimizations(ctx.packageName),
        "microphone" to (appContext.permissions?.hasGrantedPermissions(Manifest.permission.RECORD_AUDIO) == true)
      )
    }

    AsyncFunction("requestNotifications") { promise: Promise ->
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        promise.resolve(NotificationManagerCompat.from(context).areNotificationsEnabled())
        return@AsyncFunction
      }
      requestPermission(Manifest.permission.POST_NOTIFICATIONS, promise)
    }

    AsyncFunction("requestMicrophone") { promise: Promise ->
      requestPermission(Manifest.permission.RECORD_AUDIO, promise)
    }

    Function("openFullScreenSettings") {
      if (Build.VERSION.SDK_INT >= 34) {
        openSettings(Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri()))
      } else {
        openAppNotificationSettings()
      }
    }

    Function("openExactAlarmSettings") {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        openSettings(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()))
      }
    }

    Function("openNotificationSettings") {
      openAppNotificationSettings()
    }

    Function("requestBatteryUnrestricted") {
      // Alarm clocks are an allowed use of this request under Play policy.
      val direct = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri())
      if (!openSettings(direct)) {
        openSettings(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
      }
    }

    Function("getDefaultSoundTitle") {
      val ctx = context
      val uri = RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_ALARM)
      uri?.let { runCatching { RingtoneManager.getRingtone(ctx, it)?.getTitle(ctx) }.getOrNull() } ?: "Phone's alarm sound"
    }

    AsyncFunction("pickSound") { currentUri: String?, promise: Promise ->
      val activity = appContext.currentActivity
      if (activity == null) {
        promise.reject(CodedException("ERR_NO_ACTIVITY", "No activity to open the sound picker from", null))
        return@AsyncFunction
      }
      pendingSoundPromise?.resolve(mapOf("cancelled" to true))
      pendingSoundPromise = promise
      val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, AndroidSettings.System.DEFAULT_ALARM_ALERT_URI)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(
          RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
          currentUri?.let(Uri::parse) ?: AndroidSettings.System.DEFAULT_ALARM_ALERT_URI
        )
      }
      @Suppress("DEPRECATION")
      activity.startActivityForResult(intent, REQUEST_SOUND)
    }

    OnActivityResult { _: Activity, payload ->
      if (payload.requestCode != REQUEST_SOUND) return@OnActivityResult
      val promise = pendingSoundPromise ?: return@OnActivityResult
      pendingSoundPromise = null
      if (payload.resultCode != Activity.RESULT_OK) {
        promise.resolve(mapOf("cancelled" to true))
        return@OnActivityResult
      }
      val uri: Uri? = payload.data?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
          @Suppress("DEPRECATION")
          it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
      }
      // Choosing "Default" stores nothing, so the alarm follows the phone's alarm sound.
      if (uri == null || uri == AndroidSettings.System.DEFAULT_ALARM_ALERT_URI) {
        promise.resolve(mapOf("cancelled" to false, "uri" to null, "title" to null))
        return@OnActivityResult
      }
      val title = runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
      promise.resolve(mapOf("cancelled" to false, "uri" to uri.toString(), "title" to title))
    }
  }

  private fun requestPermission(permission: String, promise: Promise) {
    val permissions = appContext.permissions
    if (permissions == null) {
      promise.resolve(false)
      return
    }
    permissions.askForPermissions({ result ->
      promise.resolve(result[permission]?.status?.name == "GRANTED")
    }, permission)
  }

  private fun clockMap(changed: String?): Map<String, Any?> = mapOf(
    "changed" to changed,
    "timer" to ClockEngine.timer.value.toMap(),
    "stopwatch" to ClockEngine.stopwatch.value.toMap()
  )

  private fun packageUri(): Uri = Uri.parse("package:${context.packageName}")

  private fun openAppNotificationSettings() {
    openSettings(
      Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
    )
  }

  /** Opens a settings screen. Returns false when the device has no screen for that intent. */
  private fun openSettings(intent: Intent): Boolean {
    val activity = appContext.currentActivity
    return try {
      if (activity != null) {
        activity.startActivity(intent)
      } else {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  companion object {
    private const val REQUEST_SOUND = 4317
  }
}
