package expo.modules.chronosalarms

import org.json.JSONArray
import org.json.JSONObject

/** How an alarm decides its next ring time. */
enum class AlarmKind(val id: String) {
  ONCE("once"),
  REPEAT("repeat"),
  DATE("date");

  companion object {
    fun from(id: String?): AlarmKind = entries.firstOrNull { it.id == id } ?: ONCE
  }
}

/** How hard it is to stop the alarm. */
enum class Challenge(val id: String) {
  OFF("off"),
  TYPE("type"),
  SAY("say"),
  SECRET("secret");

  companion object {
    fun from(id: String?): Challenge = entries.firstOrNull { it.id == id } ?: TYPE
  }
}

/**
 * One alarm. [days] is a bitmask, Monday = bit 0 … Sunday = bit 6, used when [kind] is REPEAT.
 * [date] is an ISO local date (yyyy-MM-dd), used when [kind] is DATE.
 * Null [challenge], [snoozeMinutes] or [soundUri] mean "use the global setting".
 */
data class Alarm(
  val id: String,
  val hour: Int,
  val minute: Int,
  val label: String = "",
  val kind: AlarmKind = AlarmKind.ONCE,
  val days: Int = 0,
  val date: String? = null,
  val enabled: Boolean = true,
  val soundUri: String? = null,
  val soundTitle: String? = null,
  val vibrate: Boolean = true,
  val snoozeMinutes: Int? = null,
  val challenge: Challenge? = null,
  val phrase: String? = null,
  val requestCode: Int = 0
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("hour", hour)
    put("minute", minute)
    put("label", label)
    put("kind", kind.id)
    put("days", days)
    put("date", date ?: JSONObject.NULL)
    put("enabled", enabled)
    put("soundUri", soundUri ?: JSONObject.NULL)
    put("soundTitle", soundTitle ?: JSONObject.NULL)
    put("vibrate", vibrate)
    put("snoozeMinutes", snoozeMinutes ?: JSONObject.NULL)
    put("challenge", challenge?.id ?: JSONObject.NULL)
    put("phrase", phrase ?: JSONObject.NULL)
    put("requestCode", requestCode)
  }

  fun toMap(nextTriggerAt: Long?): Map<String, Any?> = mapOf(
    "id" to id,
    "hour" to hour,
    "minute" to minute,
    "label" to label,
    "kind" to kind.id,
    "days" to days,
    "date" to date,
    "enabled" to enabled,
    "soundUri" to soundUri,
    "soundTitle" to soundTitle,
    "vibrate" to vibrate,
    "snoozeMinutes" to snoozeMinutes,
    "challenge" to challenge?.id,
    "phrase" to phrase,
    "nextTriggerAt" to nextTriggerAt?.toDouble()
  )

  companion object {
    fun fromJson(o: JSONObject): Alarm = Alarm(
      id = o.getString("id"),
      hour = o.optInt("hour", 7),
      minute = o.optInt("minute", 0),
      label = o.optString("label", ""),
      kind = AlarmKind.from(o.optStringOrNull("kind")),
      days = o.optInt("days", 0),
      date = o.optStringOrNull("date"),
      enabled = o.optBoolean("enabled", true),
      soundUri = o.optStringOrNull("soundUri"),
      soundTitle = o.optStringOrNull("soundTitle"),
      vibrate = o.optBoolean("vibrate", true),
      snoozeMinutes = if (o.isNull("snoozeMinutes") || !o.has("snoozeMinutes")) null else o.optInt("snoozeMinutes"),
      challenge = o.optStringOrNull("challenge")?.let { Challenge.from(it) },
      phrase = o.optStringOrNull("phrase"),
      requestCode = o.optInt("requestCode", 0)
    )

    /** Builds an alarm from the JS payload, keeping fields the payload leaves out. */
    fun fromMap(m: Map<String, Any?>, base: Alarm?): Alarm {
      fun int(key: String, fallback: Int) = (m[key] as? Number)?.toInt() ?: fallback
      fun str(key: String, fallback: String?) = if (m.containsKey(key)) m[key] as? String else fallback
      return Alarm(
        id = (m["id"] as? String) ?: base?.id ?: java.util.UUID.randomUUID().toString(),
        hour = int("hour", base?.hour ?: 7).coerceIn(0, 23),
        minute = int("minute", base?.minute ?: 0).coerceIn(0, 59),
        label = str("label", base?.label) ?: "",
        kind = AlarmKind.from(str("kind", base?.kind?.id)),
        days = int("days", base?.days ?: 0) and 0x7F,
        date = str("date", base?.date),
        enabled = (m["enabled"] as? Boolean) ?: base?.enabled ?: true,
        soundUri = str("soundUri", base?.soundUri),
        soundTitle = str("soundTitle", base?.soundTitle),
        vibrate = (m["vibrate"] as? Boolean) ?: base?.vibrate ?: true,
        snoozeMinutes = if (m.containsKey("snoozeMinutes")) (m["snoozeMinutes"] as? Number)?.toInt() else base?.snoozeMinutes,
        challenge = if (m.containsKey("challenge")) (m["challenge"] as? String)?.let { Challenge.from(it) } else base?.challenge,
        phrase = str("phrase", base?.phrase),
        requestCode = base?.requestCode ?: 0
      )
    }
  }
}

/** App-wide settings. */
data class Settings(
  val snoozeMinutes: Int = 9,
  val maxSnoozes: Int = 3,
  val challenge: Challenge = Challenge.TYPE,
  val phrases: List<String> = DEFAULT_PHRASES,
  val giveUpIntervalMinutes: Int = 1,
  val giveUpRings: Int = 5,
  val giveUpRingSeconds: Int = 20,
  val rampSeconds: Int = 30,
  val palette: String = "chronos",
  val theme: String = "system",
  val defaultSoundUri: String? = null,
  val defaultSoundTitle: String? = null,
  val setupDone: Boolean = false
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("snoozeMinutes", snoozeMinutes)
    put("maxSnoozes", maxSnoozes)
    put("challenge", challenge.id)
    put("phrases", JSONArray(phrases))
    put("giveUpIntervalMinutes", giveUpIntervalMinutes)
    put("giveUpRings", giveUpRings)
    put("giveUpRingSeconds", giveUpRingSeconds)
    put("rampSeconds", rampSeconds)
    put("palette", palette)
    put("theme", theme)
    put("defaultSoundUri", defaultSoundUri ?: JSONObject.NULL)
    put("defaultSoundTitle", defaultSoundTitle ?: JSONObject.NULL)
    put("setupDone", setupDone)
  }

  fun toMap(): Map<String, Any?> = mapOf(
    "snoozeMinutes" to snoozeMinutes,
    "maxSnoozes" to maxSnoozes,
    "challenge" to challenge.id,
    "phrases" to phrases,
    "giveUpIntervalMinutes" to giveUpIntervalMinutes,
    "giveUpRings" to giveUpRings,
    "giveUpRingSeconds" to giveUpRingSeconds,
    "rampSeconds" to rampSeconds,
    "palette" to palette,
    "theme" to theme,
    "defaultSoundUri" to defaultSoundUri,
    "defaultSoundTitle" to defaultSoundTitle,
    "setupDone" to setupDone
  )

  /** The phrase of the day, so it changes daily but stays the same through snoozes. */
  fun phraseFor(alarm: Alarm, dayOfYear: Int): String {
    alarm.phrase?.takeIf { it.isNotBlank() }?.let { return it }
    val list = phrases.filter { it.isNotBlank() }.ifEmpty { DEFAULT_PHRASES }
    return list[dayOfYear % list.size]
  }

  companion object {
    val DEFAULT_PHRASES = listOf(
      "I am awake and ready for today",
      "My feet are on the floor",
      "Today starts right now",
      "I will not go back to sleep"
    )

    fun fromJson(o: JSONObject): Settings {
      val arr = o.optJSONArray("phrases")
      val phrases = if (arr == null) DEFAULT_PHRASES else List(arr.length()) { arr.optString(it) }
      return Settings(
        snoozeMinutes = o.optInt("snoozeMinutes", 9),
        maxSnoozes = o.optInt("maxSnoozes", 3),
        challenge = Challenge.from(o.optStringOrNull("challenge")),
        phrases = phrases,
        giveUpIntervalMinutes = o.optInt("giveUpIntervalMinutes", 1),
        giveUpRings = o.optInt("giveUpRings", 5),
        giveUpRingSeconds = o.optInt("giveUpRingSeconds", 20),
        rampSeconds = o.optInt("rampSeconds", 30),
        palette = o.optString("palette", "chronos"),
        theme = o.optString("theme", "system"),
        defaultSoundUri = o.optStringOrNull("defaultSoundUri"),
        defaultSoundTitle = o.optStringOrNull("defaultSoundTitle"),
        setupDone = o.optBoolean("setupDone", false)
      )
    }

    fun fromMap(m: Map<String, Any?>, base: Settings): Settings {
      fun int(key: String, fallback: Int) = (m[key] as? Number)?.toInt() ?: fallback
      fun str(key: String, fallback: String?) = if (m.containsKey(key)) m[key] as? String else fallback
      @Suppress("UNCHECKED_CAST")
      val phrases = (m["phrases"] as? List<Any?>)?.mapNotNull { it as? String }?.map { it.trim() }?.filter { it.isNotEmpty() }
      return base.copy(
        snoozeMinutes = int("snoozeMinutes", base.snoozeMinutes).coerceIn(1, 60),
        maxSnoozes = int("maxSnoozes", base.maxSnoozes).coerceIn(0, 10),
        challenge = if (m.containsKey("challenge")) Challenge.from(m["challenge"] as? String) else base.challenge,
        phrases = phrases?.ifEmpty { null } ?: base.phrases,
        giveUpIntervalMinutes = int("giveUpIntervalMinutes", base.giveUpIntervalMinutes).coerceIn(1, 10),
        giveUpRings = int("giveUpRings", base.giveUpRings).coerceIn(1, 20),
        giveUpRingSeconds = int("giveUpRingSeconds", base.giveUpRingSeconds).coerceIn(5, 120),
        rampSeconds = int("rampSeconds", base.rampSeconds).coerceIn(0, 120),
        palette = str("palette", base.palette) ?: "chronos",
        theme = str("theme", base.theme) ?: "system",
        defaultSoundUri = str("defaultSoundUri", base.defaultSoundUri),
        defaultSoundTitle = str("defaultSoundTitle", base.defaultSoundTitle),
        setupDone = (m["setupDone"] as? Boolean) ?: base.setupDone
      )
    }
  }
}

/** A snooze waiting to ring again. Stored so it survives the process being killed. */
data class PendingRing(val alarmId: String, val triggerAt: Long, val snoozesUsed: Int) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("alarmId", alarmId)
    put("triggerAt", triggerAt)
    put("snoozesUsed", snoozesUsed)
  }

  companion object {
    fun fromJson(o: JSONObject) = PendingRing(o.getString("alarmId"), o.getLong("triggerAt"), o.optInt("snoozesUsed", 0))
  }
}

internal fun JSONObject.optStringOrNull(key: String): String? =
  if (!has(key) || isNull(key)) null else optString(key)
