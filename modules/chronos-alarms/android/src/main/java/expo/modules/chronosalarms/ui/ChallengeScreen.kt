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
internal fun ChallengeContent(
  session: RingSession,
  onDuck: () -> Unit,
  onSolved: () -> Unit,
  onSnooze: (() -> Unit)?,
  onGiveUp: (() -> Unit)?
) {
  val cs = MaterialTheme.colorScheme
  val context = LocalContext.current
  var text by rememberSaveable { mutableStateOf("") }
  var listening by remember { mutableStateOf(false) }
  val secret = session.challenge == Challenge.SECRET
  val sayMode = session.challenge == Challenge.SAY
  val targetWords = remember(session.phrase) { session.phrase.trim().split(Regex("\\s+")) }
  val matched = matchedWords(text, session.phrase)
  val focus = remember { FocusRequester() }

  LaunchedEffect(text) {
    if (text.isNotBlank() && normalizePhrase(text) == normalizePhrase(session.phrase)) onSolved()
  }
  LaunchedEffect(Unit) {
    if (!sayMode) runCatching { focus.requestFocus() }
  }

  val speech = remember { SpeechController(context, onText = { text = it }, onListening = { listening = it }) }
  DisposableEffect(Unit) { onDispose { speech.stop() } }
  LaunchedEffect(listening) {
    while (listening) {
      onDuck()
      delay(5_000)
    }
  }
  val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) speech.start()
  }
  val toggleMic = {
    if (listening) {
      speech.stop()
    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
      speech.start()
    } else {
      micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
  }
  val micTrailing: (@Composable () -> Unit)? = if (sayMode) {
    null
  } else {
    @Composable {
      IconButton(onClick = toggleMic) {
        Icon(painterResource(R.drawable.chronos_ic_mic), contentDescription = if (listening) "Stop listening" else "Say it instead")
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .imePadding()
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (session.inGiveUp) {
        StatusChip("Ring ${session.giveUpRound} of ${session.giveUpRings}", cs.primaryContainer, cs.onPrimaryContainer)
      }
      if (session.ducked || listening) {
        StatusChip("Volume 30% while you answer", cs.secondaryContainer, cs.onSecondaryContainer)
      }
    }

    Text(
      when {
        secret -> "Type your secret phrase"
        sayMode -> "Say this to stop the alarm"
        else -> "Type this to stop the alarm"
      },
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(top = 32.dp, start = 4.dp)
    )
    Text(
      if (sayMode) "Speak clearly. You can type it instead." else "Spelling counts. Capitals and punctuation don't.",
      style = MaterialTheme.typography.bodyLarge,
      color = cs.onSurfaceVariant,
      modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )

    if (!secret) {
      Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceContainer
      ) {
        Text(
          text = buildAnnotatedString {
            targetWords.forEachIndexed { i, word ->
              if (i > 0) append(" ")
              if (i < matched) {
                withStyle(SpanStyle(color = cs.primary, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) { append(word) }
              } else {
                append(word)
              }
            }
          },
          fontSize = 28.sp,
          lineHeight = 38.sp,
          modifier = Modifier.padding(20.dp)
        )
      }
    }

    if (sayMode) {
      Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
        FilledIconButton(onClick = toggleMic, modifier = Modifier.size(96.dp)) {
          Icon(
            painterResource(R.drawable.chronos_ic_mic),
            contentDescription = if (listening) "Stop listening" else "Say the phrase",
            modifier = Modifier.size(40.dp)
          )
        }
      }
      Text(
        if (listening) "Listening…" else "Tap the microphone and say the phrase",
        style = MaterialTheme.typography.bodyMedium,
        color = cs.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
      )
    }

    OutlinedTextField(
      value = text,
      onValueChange = {
        text = it
        onDuck()
      },
      label = { Text("Your answer") },
      trailingIcon = micTrailing,
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        imeAction = ImeAction.Done
      ),
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp).focusRequester(focus)
    )

    if (!secret) {
      LinearProgressIndicator(
        progress = { matched.toFloat() / targetWords.size.coerceAtLeast(1) },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
      )
      Text(
        "$matched of ${targetWords.size} words right",
        style = MaterialTheme.typography.bodySmall,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
      )
    }

    Spacer(Modifier.height(32.dp))

    if (onSnooze != null) {
      FilledTonalButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Icon(painterResource(R.drawable.chronos_ic_snooze), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Snooze instead")
      }
    }
    if (onGiveUp != null) {
      TextButton(onClick = onGiveUp, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text("I can't do this right now")
      }
    }
  }
}

@Composable
internal fun StatusChip(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
  Surface(shape = RoundedCornerShape(8.dp), color = container, contentColor = content) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
  }
}

// ---------------------------------------------------------------------------------------------
// Give-up countdown
// ---------------------------------------------------------------------------------------------
