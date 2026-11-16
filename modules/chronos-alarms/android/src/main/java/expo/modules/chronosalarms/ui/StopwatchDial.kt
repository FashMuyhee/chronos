package expo.modules.chronosalarms.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import expo.modules.chronosalarms.clock.ClockEngine
import expo.modules.chronosalarms.clock.StopwatchStatus
import expo.modules.kotlin.views.ComposeProps

/** Props from JS. Colors are the app theme's roles as #RRGGBB or #RRGGBBAA. */
data class StopwatchDialProps(
  val size: Float? = null,
  val accentColor: String? = null,
  val contentColor: String? = null,
  val mutedColor: String? = null,
  val trackColor: String? = null,
  val lapColor: String? = null,
  /** Fill behind the ticks. */
  val faceColor: String? = null
) : ComposeProps

/**
 * The stopwatch face: 60 tick marks, a hand that sweeps once a minute, and the readout with
 * centiseconds. It reads [ClockEngine] directly and redraws every frame while running, so JS
 * never has to stream the time across the bridge.
 *
 * Ticks the hand has passed this minute light up in the accent color. The hand springs back to
 * twelve on reset.
 */
@Composable
fun StopwatchDial(props: StopwatchDialProps) {
  val accent = parseColor(props.accentColor, Color(0xFF8C1D30))
  val content = parseColor(props.contentColor, Color(0xFF22191A))
  val muted = parseColor(props.mutedColor, Color(0xFF534343))
  val track = parseColor(props.trackColor, Color(0xFFD8C2C2))
  val lapMark = parseColor(props.lapColor, Color(0xFF755A2F))
  val face = props.faceColor?.let { parseColor(it, Color.Transparent) }

  val state by ClockEngine.stopwatch.collectAsState()
  val running = state.status == StopwatchStatus.RUNNING
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(running, state) {
    now = System.currentTimeMillis()
    while (running) withFrameMillis { now = System.currentTimeMillis() }
  }

  val elapsed = state.elapsed(now)
  val lap = state.currentLap(now)
  val target = (elapsed % 60_000L) / 60_000f * 360f
  val angle by animateFloatAsState(
    targetValue = target,
    animationSpec = if (running) snap() else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
    label = "hand"
  )
  val lastLapAngle = state.laps.lastOrNull()?.let { (it % 60_000L) / 60_000f * 360f }

  val dial = props.size ?: 296f
  val sizeDp = dial.dp
  Box(
    modifier = Modifier
      .size(sizeDp)
      .semantics { contentDescription = "Stopwatch ${spoken(elapsed)}" },
    contentAlignment = Alignment.Center
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val radius = size.minDimension / 2f
      val center = Offset(size.width / 2f, size.height / 2f)
      val tickWidth = 3.dp.toPx()
      face?.let { drawCircle(it, radius, center) }
      // How many ticks the hand has passed in this minute.
      val lit = (angle / 6f).toInt()
      for (i in 0 until 60) {
        val major = i % 5 == 0
        val length = if (major) 14.dp.toPx() else 8.dp.toPx()
        val color = if (elapsed > 0 && i <= lit) accent else if (major) muted else track
        rotate(i * 6f, center) {
          drawLine(
            color = color,
            start = Offset(center.x, center.y - radius + 14.dp.toPx()),
            end = Offset(center.x, center.y - radius + 14.dp.toPx() + length),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
          )
        }
      }
      lastLapAngle?.let { a ->
        rotate(a, center) {
          drawCircle(lapMark, radius = 4.dp.toPx(), center = Offset(center.x, center.y - radius + 38.dp.toPx()))
        }
      }
      rotate(angle, center) {
        val tip = Offset(center.x, center.y - radius + 32.dp.toPx())
        drawLine(accent, start = Offset(center.x, center.y - radius * 0.62f), end = tip, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(accent, radius = 7.dp.toPx(), center = tip)
      }
    }

    val big = (dial * 0.19f).sp
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      Text(
        text = buildAnnotatedString {
          append(mainPart(elapsed))
          withStyle(SpanStyle(fontSize = big * 0.62f, color = accent)) { append(".%02d".format((elapsed / 10) % 100)) }
        },
        style = TextStyle(fontFamily = ChronosDisplayFamily, fontSize = big, color = content, fontFeatureSettings = "tnum")
      )
      if (state.laps.isNotEmpty()) {
        Text(
          text = "Lap ${state.laps.size + 1}  ${mainPart(lap)}.%02d".format((lap / 10) % 100),
          style = TextStyle(fontFamily = ChronosTextFamily, fontSize = (dial * 0.055f).sp, color = muted, fontFeatureSettings = "tnum")
        )
      }
    }
  }
}

/** 0:07, 12:07 or 1:12:07. */
private fun mainPart(ms: Long): String {
  val total = ms / 1000
  val h = total / 3600
  val m = (total % 3600) / 60
  val s = total % 60
  return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun spoken(ms: Long): String {
  val total = ms / 1000
  return "${total / 60} minutes ${total % 60} seconds"
}

/** #RGB, #RRGGBB or #RRGGBBAA (the format the app's Material colors come in). */
internal fun parseColor(value: String?, fallback: Color): Color {
  val hex = value?.removePrefix("#") ?: return fallback
  val v = hex.toLongOrNull(16) ?: return fallback
  return when (hex.length) {
    6 -> Color(0xFF000000 or v)
    8 -> Color(((v and 0xFF) shl 24) or (v ushr 8))
    3 -> {
      val r = (v shr 8) and 0xF
      val g = (v shr 4) and 0xF
      val b = v and 0xF
      Color(0xFF000000 or (r * 17 shl 16) or (g * 17 shl 8) or (b * 17))
    }
    else -> fallback
  }
}
