@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package expo.modules.chronosalarms.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import expo.modules.chronosalarms.clock.ClockEngine
import expo.modules.chronosalarms.clock.ClockNotifications
import expo.modules.chronosalarms.clock.TimerStatus
import expo.modules.kotlin.views.ComposeProps
import kotlin.math.sin

data class TimerFaceProps(
  val size: Float? = null,
  val accentColor: String? = null,
  val containerColor: String? = null,
  val onContainerColor: String? = null,
  val trackColor: String? = null,
  val alertColor: String? = null,
  val alertContainerColor: String? = null,
  val onAlertContainerColor: String? = null
) : ComposeProps

/**
 * The running timer's face. A twelve-scallop cookie turns slowly while the timer runs, melts
 * into a circle when paused and bursts into a sun when time is up, every change on a spring. A
 * thick ring around it empties clockwise. Tapping the face pauses or resumes.
 *
 * It reads [ClockEngine] directly and redraws per frame, so JS never streams the time.
 */
@Composable
fun TimerFace(props: TimerFaceProps) {
  val accent = parseColor(props.accentColor, Color(0xFF8C1D30))
  val container = parseColor(props.containerColor, Color(0xFFFFDADB))
  val onContainer = parseColor(props.onContainerColor, Color(0xFF3F0010))
  val track = parseColor(props.trackColor, Color(0xFFF0DEDE))
  val alert = parseColor(props.alertColor, Color(0xFF755A2F))
  val alertContainer = parseColor(props.alertContainerColor, Color(0xFFFFDDB1))
  val onAlertContainer = parseColor(props.onAlertContainerColor, Color(0xFF2A1800))
  val context = LocalContext.current
  val reduceMotion = remember { animationsOff(context) }
  val faceSize = props.size ?: 300f

  val timer by ClockEngine.timer.collectAsState()
  val running = timer.status == TimerStatus.RUNNING
  val ringing = timer.status == TimerStatus.RINGING
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(timer) {
    now = System.currentTimeMillis()
    while (running || ringing) withFrameMillis { now = System.currentTimeMillis() }
  }

  val remaining = timer.remaining(now)
  val fraction = if (timer.totalMs > 0) (remaining.toFloat() / timer.totalMs).coerceIn(0f, 1f) else 0f
  // Springs between the three looks: 0 = cookie, round 1 = circle, burst 1 = sun.
  val round by animateFloatAsState(if (timer.status == TimerStatus.PAUSED) 1f else 0f, spring(0.55f, 220f), label = "round")
  val burst by animateFloatAsState(if (ringing) 1f else 0f, spring(0.4f, 260f), label = "burst")
  // The ring is the true remaining fraction, frame by frame; it only fills when time is up.
  val shownFraction = if (ringing) 1f else fraction

  val toCircle = remember { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Circle) }
  val toSun = remember { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.VerySunny) }
  val path = remember { Path() }

  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val press by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.5f, 600f), label = "press")

  val readout = if (ringing) "−" + ClockNotifications.formatClock((-remaining).coerceAtLeast(0) / 1000 * 1000)
  else ClockNotifications.formatClock(remaining.coerceAtLeast(0))
  val caption = when (timer.status) {
    TimerStatus.RINGING -> "Time's up"
    TimerStatus.PAUSED -> "Paused · tap to resume"
    else -> "of ${ClockNotifications.formatLength(timer.totalMs)}"
  }

  Box(
    modifier = Modifier
      .size(faceSize.dp)
      .graphicsLayer { scaleX = press; scaleY = press }
      .clickable(interactionSource = interaction, indication = null) {
        when (timer.status) {
          TimerStatus.RUNNING -> ClockEngine.pauseTimer(context)
          TimerStatus.PAUSED -> ClockEngine.resumeTimer(context)
          TimerStatus.RINGING -> ClockEngine.resetTimer(context)
          TimerStatus.IDLE -> Unit
        }
      }
      .semantics { contentDescription = "$readout. $caption" },
    contentAlignment = Alignment.Center
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = 14.dp.toPx()
      val inset = stroke / 2
      val arcSize = Size(size.width - stroke, size.height - stroke)
      val ringColor = if (ringing) alert else accent
      drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
      if (shownFraction > 0f) {
        drawArc(ringColor, -90f, 360f * shownFraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
      }

      // The shape, inset from the ring. It turns once every 40 s while running.
      val gap = stroke + 14.dp.toPx()
      val shapeSize = Size(size.width - gap * 2, size.height - gap * 2)
      val spin = if (reduceMotion) 0f else (now % 40_000L) / 40_000f * 360f
      val pulse = if (ringing && !reduceMotion) 1f + 0.035f * sin(now / 140.0).toFloat() else 1f
      val shape = if (burst > 0.01f) toSun.pathIn(shapeSize, burst, path) else toCircle.pathIn(shapeSize, round, path)
      translate(gap, gap) {
        scale(pulse, pivot = Offset(shapeSize.width / 2, shapeSize.height / 2)) {
          rotate(spin, pivot = Offset(shapeSize.width / 2, shapeSize.height / 2)) {
            drawPath(shape, if (ringing) alertContainer else container)
          }
        }
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = readout,
        style = TextStyle(
          fontFamily = ChronosDisplayFamily,
          fontSize = (faceSize * if (readout.length > 6) 0.15f else 0.2f).sp,
          color = if (ringing) onAlertContainer else onContainer,
          fontFeatureSettings = "tnum"
        )
      )
      Text(
        text = caption,
        style = TextStyle(fontFamily = ChronosTextFamily, fontSize = 15.sp, color = (if (ringing) onAlertContainer else onContainer).copy(alpha = 0.75f))
      )
    }
  }
}
