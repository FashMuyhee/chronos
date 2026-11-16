@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package expo.modules.chronosalarms.ui

import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Morph
import expo.modules.chronosalarms.R
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

/** Icons the JS side may name in native view props. */
internal fun iconRes(name: String?): Int? = when (name) {
  "add" -> R.drawable.chronos_ic_add
  "alarm" -> R.drawable.chronos_ic_alarm
  "backspace" -> R.drawable.chronos_ic_backspace
  "close" -> R.drawable.chronos_ic_close
  "flag" -> R.drawable.chronos_ic_flag
  "hourglass" -> R.drawable.chronos_ic_hourglass_top
  "pause" -> R.drawable.chronos_ic_pause
  "play" -> R.drawable.chronos_ic_play_arrow
  "restart" -> R.drawable.chronos_ic_restart_alt
  "stop" -> R.drawable.chronos_ic_stop
  "stopwatch" -> R.drawable.chronos_ic_timer
  else -> null
}

/** Event payload with an index, e.g. the tab that was tapped. */
class IndexEvent(@Field val index: Int = 0) : Record

/** A Material shape morph as a path filling [size] (the polygons are in a unit square). */
internal fun Morph.pathIn(size: Size, progress: Float, path: Path = Path()): Path {
  val p = toPath(progress, path)
  p.transform(Matrix().apply { scale(size.width, size.height) })
  return p
}
