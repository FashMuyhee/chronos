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
internal fun WaitingContent(session: RingSession, onTryAgain: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      now = System.currentTimeMillis()
      delay(250)
    }
  }
  val next = session.nextRingAt ?: now
  val remaining = ((next - now) / 1000).coerceAtLeast(0)
  val interval = session.giveUpIntervalMinutes

  Column(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .padding(horizontal = 20.dp, vertical = 24.dp)
  ) {
    Spacer(Modifier.height(48.dp))
    Text("Not stopped yet. Rings again in", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
    Text(
      "%d:%02d".format(remaining / 60, remaining % 60),
      fontFamily = ChronosDisplayFamily,
      fontSize = 96.sp,
      lineHeight = 100.sp,
      letterSpacing = (-2).sp,
      color = cs.primary
    )
    Text(
      "Chronos rings every ${if (interval == 1) "minute" else "$interval minutes"} until you complete the check. " +
        "After ${session.giveUpRings} rings it stops for today.",
      style = MaterialTheme.typography.bodyLarge,
      color = cs.onSurfaceVariant,
      modifier = Modifier.padding(top = 16.dp)
    )

    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      for (k in 1..session.giveUpRings) {
        val shape = when (k) {
          1 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
          session.giveUpRings -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
          else -> RoundedCornerShape(4.dp)
        }
        val isNext = k == session.giveUpRound
        val status = when {
          k < session.giveUpRound -> "Rang"
          isNext -> "Next"
          else -> "in ${(k - session.giveUpRound) * interval + (remaining / 60).toInt()} min"
        }
        Surface(shape = shape, color = if (isNext) cs.secondaryContainer else cs.surfaceContainer) {
          Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              if (k == session.giveUpRings) "Ring $k, then stops" else "Ring $k",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = if (isNext) FontWeight.Medium else FontWeight.Normal,
              modifier = Modifier.weight(1f)
            )
            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (isNext) cs.onSecondaryContainer else cs.onSurfaceVariant)
          }
        }
      }
    }

    Spacer(Modifier.weight(1f))
    Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth().height(56.dp)) {
      Text("Try the phrase again")
    }
    Text(
      "Works from the lock screen. No need to unlock.",
      style = MaterialTheme.typography.bodyMedium,
      color = cs.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    )
  }
}

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

/** Lower-case, letters, digits and single spaces only. */
