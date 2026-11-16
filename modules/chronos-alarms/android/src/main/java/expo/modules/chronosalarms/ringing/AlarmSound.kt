package expo.modules.chronosalarms.ringing

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/** Sound and vibration shared by ringing alarms ([RingingService]) and finished timers. */
object AlarmSound {
  private const val TAG = "ChronosSound"

  /** The chosen sound, else the phone's alarm sound, else any default sound the phone has. */
  fun resolve(context: Context, preferred: String?): Uri? =
    preferred?.let(Uri::parse)
      ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

  /** A prepared, looping player on the alarm stream, falling back to the ringtone. */
  fun player(context: Context, uri: Uri?): MediaPlayer? =
    create(context, uri) ?: create(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))

  private fun create(context: Context, uri: Uri?): MediaPlayer? {
    if (uri == null) return null
    return try {
      MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        setDataSource(context, uri)
        isLooping = true
        prepare()
      }
    } catch (e: Exception) {
      Log.w(TAG, "could not play $uri", e)
      null
    }
  }

  fun release(player: MediaPlayer?) {
    player?.let { runCatching { it.stop() }; it.release() }
  }

  fun vibrate(context: Context) {
    val effect = VibrationEffect.createWaveform(longArrayOf(0, 900, 700), 0)
    val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
    @Suppress("DEPRECATION")
    vibrator(context)?.vibrate(effect, attrs)
  }

  fun stopVibrating(context: Context) {
    vibrator(context)?.cancel()
  }

  private fun vibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
