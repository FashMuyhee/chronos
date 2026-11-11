@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package expo.modules.chronosalarms.ui

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalHapticFeedback


/**
 * Slide the thumb to the end to stop. Built to work for a half-awake thumb:
 *  - the thumb follows the finger from the first pixel (no touch-slop wait), and a drag can
 *    start anywhere on the track;
 *  - it completes past 55% of the track, or on a quick fling to the right;
 *  - a haptic tick marks the point where letting go will stop the alarm;
 *  - position is plain state updated synchronously, so fast drags can't arrive out of order.
 * TalkBack users get a single "Stop alarm" action instead of the gesture.
 */
@Composable
internal fun SlideToStop(title: String, subtitle: String?, reduceMotion: Boolean, onStop: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  val density = LocalDensity.current
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  var trackWidth by remember { mutableIntStateOf(0) }
  var dragX by remember { mutableFloatStateOf(0f) }
  var dragging by remember { mutableStateOf(false) }
  var armed by remember { mutableStateOf(false) }
  var done by remember { mutableStateOf(false) }
  val settle = remember { Animatable(0f) }
  val thumbPx = with(density) { 56.dp.toPx() }
  val padPx = with(density) { 8.dp.toPx() }
  val maxOffset = (trackWidth - thumbPx - padPx * 2).coerceAtLeast(1f)
  val flingPx = with(density) { 900.dp.toPx() }
  val currentOnStop by rememberUpdatedState(onStop)

  val infinite = rememberInfiniteTransition(label = "nudge")
  val nudge = motion(reduceMotion, 0f) {
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 0f,
      animationSpec = infiniteRepeatable(
        keyframes {
          durationMillis = 3_000
          0f at 0
          0f at 2_100
          1f at 2_400
          0f at 2_700
        },
        repeatMode = RepeatMode.Restart
      ),
      label = "nudge"
    )
  }
  val offset = if (dragging) dragX else settle.value
  val nudgePx = if (dragging || offset > 0f) 0f else nudge.value * with(density) { 16.dp.toPx() }
  val progress = (offset / maxOffset).coerceIn(0f, 1f)

  fun complete() {
    if (done) return
    done = true
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    scope.launch {
      settle.snapTo(dragX)
      settle.animateTo(maxOffset, tween(90))
      currentOnStop()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(72.dp)
      .clip(CircleShape)
      .background(cs.surfaceContainerHigh)
      .onSizeChanged { trackWidth = it.width }
      .semantics(mergeDescendants = true) {
        contentDescription = title
        onClick(label = "Stop alarm") {
          currentOnStop()
          true
        }
      }
      .pointerInput(maxOffset) {
        val tracker = VelocityTracker()
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          if (done) return@awaitEachGesture
          tracker.resetTracking()
          tracker.addPosition(down.uptimeMillis, down.position)
          dragging = true
          dragX = settle.value
          armed = false
          horizontalDrag(down.id) { change ->
            tracker.addPosition(change.uptimeMillis, change.position)
            dragX = (dragX + change.positionChange().x).coerceIn(0f, maxOffset)
            change.consume()
            val nowArmed = dragX > maxOffset * 0.55f
            if (nowArmed && !armed) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            armed = nowArmed
          }
          dragging = false
          val velocity = tracker.calculateVelocity().x
          if (dragX > maxOffset * 0.55f || (velocity > flingPx && dragX > maxOffset * 0.2f)) {
            complete()
          } else {
            scope.launch {
              settle.snapTo(dragX)
              settle.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
            }
          }
        }
      },
    contentAlignment = Alignment.CenterStart
  ) {
    Row(Modifier.fillMaxSize().padding(start = 78.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.graphicsLayer { alpha = 1f - progress * 1.4f }) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        if (subtitle != null) {
          Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
      }
    }
    Box(
      modifier = Modifier
        .padding(8.dp)
        .offset { IntOffset((offset + nudgePx).roundToInt(), 0) }
        .size(56.dp)
        .clip(CircleShape)
        .background(if (armed || done) cs.primary else cs.onSurface),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painterResource(if (armed || done) R.drawable.chronos_ic_check else R.drawable.chronos_ic_arrow_forward),
        contentDescription = null,
        tint = if (armed || done) cs.onPrimary else cs.surface
      )
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Wake-up check
// ---------------------------------------------------------------------------------------------
