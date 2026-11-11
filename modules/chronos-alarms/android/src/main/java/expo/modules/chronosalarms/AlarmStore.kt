package expo.modules.chronosalarms

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Alarm, settings and snooze storage.
 *
 * Uses device-protected storage so the boot receiver can read alarms and re-schedule them
 * before the user unlocks the phone for the first time (direct boot).
 */
class AlarmStore private constructor(context: Context) {
  private val prefs: SharedPreferences =
    context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  @Synchronized
  fun alarms(): List<Alarm> {
    val raw = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      List(arr.length()) { Alarm.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())
  }

  @Synchronized
  fun alarm(id: String): Alarm? = alarms().firstOrNull { it.id == id }

  /** Inserts or replaces an alarm. New alarms get a stable PendingIntent request code. */
  @Synchronized
  fun upsert(alarm: Alarm): Alarm {
    val list = alarms().toMutableList()
    val index = list.indexOfFirst { it.id == alarm.id }
    val saved = if (alarm.requestCode != 0) alarm else alarm.copy(requestCode = nextRequestCode())
    if (index >= 0) list[index] = saved else list.add(saved)
    write(list)
    return saved
  }

  @Synchronized
  fun delete(id: String): Alarm? {
    val list = alarms().toMutableList()
    val removed = list.firstOrNull { it.id == id } ?: return null
    list.remove(removed)
    write(list)
    pendingRings().filter { it.alarmId == id }.forEach { removePendingRing(it.alarmId) }
    return removed
  }

  @Synchronized
  fun settings(): Settings {
    val raw = prefs.getString(KEY_SETTINGS, null) ?: return Settings()
    return runCatching { Settings.fromJson(JSONObject(raw)) }.getOrDefault(Settings())
  }

  @Synchronized
  fun saveSettings(settings: Settings) {
    prefs.edit().putString(KEY_SETTINGS, settings.toJson().toString()).apply()
  }

  @Synchronized
  fun pendingRings(): List<PendingRing> {
    val raw = prefs.getString(KEY_PENDING, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      List(arr.length()) { PendingRing.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())
  }

  @Synchronized
  fun pendingRing(alarmId: String): PendingRing? = pendingRings().firstOrNull { it.alarmId == alarmId }

  @Synchronized
  fun putPendingRing(ring: PendingRing) {
    val list = pendingRings().filter { it.alarmId != ring.alarmId } + ring
    prefs.edit().putString(KEY_PENDING, JSONArray(list.map { it.toJson() }).toString()).apply()
  }

  @Synchronized
  fun removePendingRing(alarmId: String) {
    val list = pendingRings().filter { it.alarmId != alarmId }
    prefs.edit().putString(KEY_PENDING, JSONArray(list.map { it.toJson() }).toString()).apply()
  }

  private fun write(list: List<Alarm>) {
    prefs.edit().putString(KEY_ALARMS, JSONArray(list.map { it.toJson() }).toString()).apply()
  }

  private fun nextRequestCode(): Int {
    // Even numbers ring the alarm itself; code + 1 is its snooze.
    val next = prefs.getInt(KEY_RC, 1000) + 2
    prefs.edit().putInt(KEY_RC, next).apply()
    return next
  }

  companion object {
    private const val PREFS = "chronos_alarms"
    private const val KEY_ALARMS = "alarms"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_PENDING = "pending_rings"
    private const val KEY_RC = "request_code"

    @Volatile private var instance: AlarmStore? = null

    fun get(context: Context): AlarmStore =
      instance ?: synchronized(this) {
        instance ?: AlarmStore(context.applicationContext).also { instance = it }
      }
  }
}
