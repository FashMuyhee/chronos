package expo.modules.chronosalarms.clock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import expo.modules.chronosalarms.R

/**
 * Long-press launcher shortcuts: New alarm, Timer and Stopwatch. Each opens an Expo Router deep
 * link, so JS decides what to show.
 */
object AppShortcuts {
  private const val TAG = "ChronosShortcuts"

  private data class Entry(val id: String, val label: String, val longLabel: String, val path: String, val icon: Int)

  private val entries = listOf(
    Entry("new_alarm", "New alarm", "Add an alarm", "alarm/new", R.drawable.chronos_ic_add),
    Entry("timer", "Timer", "Start a timer", "timer", R.drawable.chronos_ic_hourglass_top),
    Entry("stopwatch", "Stopwatch", "Open the stopwatch", "stopwatch", R.drawable.chronos_ic_timer)
  )

  fun publish(context: Context) {
    runCatching {
      val shortcuts = entries.mapIndexed { rank, e ->
        ShortcutInfoCompat.Builder(context, e.id)
          .setShortLabel(e.label)
          .setLongLabel(e.longLabel)
          .setIcon(icon(context, e.icon))
          .setIntent(ClockNotifications.deepLink(context, e.path))
          .setRank(rank)
          .build()
      }
      ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }.onFailure { Log.w(TAG, "could not publish shortcuts", it) }
  }

  /** An adaptive icon: the glyph in onPrimaryContainer on a primaryContainer field. */
  private fun icon(context: Context, res: Int): IconCompat {
    // Adaptive icons are 108dp with the visible 72dp in the middle; draw at 4x.
    val size = 432
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(0xFFFFDADB.toInt())
    val glyph = ContextCompat.getDrawable(context, res)!!.mutate()
    glyph.setTint(0xFF3F0010.toInt())
    val g = (size * 0.4f).toInt()
    val inset = (size - g) / 2
    glyph.setBounds(inset, inset, inset + g, inset + g)
    glyph.draw(canvas)
    return IconCompat.createWithAdaptiveBitmap(bitmap)
  }
}
