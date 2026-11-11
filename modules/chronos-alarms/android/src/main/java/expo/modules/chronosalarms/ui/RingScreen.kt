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


@Composable
internal fun RingContent(session: RingSession, reduceMotion: Boolean, onSnooze: (Int) -> Unit, onStop: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  val context = LocalContext.current
  var snoozeMinutes by remember(session.alarm.id) { mutableIntStateOf(session.snoozeMinutes) }
  val time = remember(session.alarm) { Notifications.formatTime(context, session.alarm.hour, session.alarm.minute) }
  val date = remember { todayLabel() }

  val infinite = rememberInfiniteTransition(label = "ring")
  val spin = motion(reduceMotion, 0f) {
    infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "spin")
  }
  val ripple = motion(reduceMotion, 1f) {
    infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "ripple")
  }
  val bell = motion(reduceMotion, 0f) {
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 0f,
      animationSpec = infiniteRepeatable(
        keyframes {
          durationMillis = 2_400
          0f at 0
          -16f at 144
          14f at 288
          -10f at 432
          6f at 576
          0f at 720
        }
      ),
      label = "bell"
    )
  }
  val pop = remember { Animatable(if (reduceMotion) 1f else 0.7f) }
  LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
  val cookie = MaterialShapes.Cookie9Sided.toShape()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .padding(horizontal = 16.dp, vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(shape = CircleShape, color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer) {
      Row(
        modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          painterResource(R.drawable.chronos_ic_alarm),
          contentDescription = null,
          modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = if (session.muted) 0f else bell.value }
        )
        Text(
          if (session.muted) "${session.alarm.label.ifBlank { "Alarm" }} · Muted" else session.alarm.label.ifBlank { "Alarm" },
          style = MaterialTheme.typography.labelLarge
        )
      }
    }

    Spacer(Modifier.height(16.dp))

    Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
      if (!reduceMotion) {
        listOf(ripple.value, (ripple.value + 0.5f) % 1f).forEach { p ->
          Box(
            Modifier
              .size(280.dp)
              .graphicsLayer {
                scaleX = 0.9f + 0.32f * p
                scaleY = 0.9f + 0.32f * p
                alpha = 0.6f * (1f - p)
              }
              .border(2.dp, cs.primary, CircleShape)
          )
        }
      }
      Box(
        Modifier
          .size(292.dp)
          .graphicsLayer {
            rotationZ = spin.value
            scaleX = pop.value
            scaleY = pop.value
          }
          .clip(cookie)
          .background(cs.primaryContainer)
      )
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          time,
          color = cs.onPrimaryContainer,
          fontFamily = ChronosDisplayFamily,
          fontSize = 84.sp,
          lineHeight = 88.sp,
          letterSpacing = (-2).sp,
          fontWeight = FontWeight.Normal
        )
        Text(date, color = cs.onPrimaryContainer, style = MaterialTheme.typography.bodyLarge)
      }
    }

    Spacer(Modifier.weight(1f))

    if (session.snoozesLeft > 0) {
      Row(modifier = Modifier.fillMaxWidth().height(152.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalIconButton(
          onClick = { snoozeMinutes = (snoozeMinutes - 1).coerceAtLeast(1) },
          modifier = Modifier.width(56.dp).fillMaxHeight(),
          shape = RoundedCornerShape(28.dp)
        ) {
          Icon(painterResource(R.drawable.chronos_ic_remove), contentDescription = "Snooze 1 minute less")
        }
        Button(
          onClick = { onSnooze(snoozeMinutes) },
          modifier = Modifier.weight(1f).fillMaxHeight(),
          shape = RoundedCornerShape(44.dp)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painterResource(R.drawable.chronos_ic_snooze), contentDescription = null, modifier = Modifier.size(32.dp))
            Text("Snooze $snoozeMinutes min", style = MaterialTheme.typography.headlineSmall)
            Text(
              if (session.snoozesLeft == 1) "1 snooze left" else "${session.snoozesLeft} snoozes left",
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
        FilledTonalIconButton(
          onClick = { snoozeMinutes = (snoozeMinutes + 1).coerceAtMost(60) },
          modifier = Modifier.width(56.dp).fillMaxHeight(),
          shape = RoundedCornerShape(28.dp)
        ) {
          Icon(painterResource(R.drawable.chronos_ic_add), contentDescription = "Snooze 1 minute more")
        }
      }
      Spacer(Modifier.height(12.dp))
    }

    SlideToStop(
      title = "Slide to stop",
      subtitle = when (session.challenge) {
        Challenge.OFF -> null
        Challenge.SAY -> "Then say your wake-up phrase"
        Challenge.SECRET -> "Then type your secret phrase"
        Challenge.TYPE -> "Then type your wake-up phrase"
      },
      reduceMotion = reduceMotion,
      onStop = onStop
    )
  }
}
