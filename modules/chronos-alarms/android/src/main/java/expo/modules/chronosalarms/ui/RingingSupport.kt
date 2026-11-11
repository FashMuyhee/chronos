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


internal fun normalizePhrase(value: String): String =
  value.lowercase(Locale.getDefault())
    .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
    .trim()
    .replace(Regex("\\s+"), " ")

/** How many words from the start of the phrase are typed correctly. */
internal fun matchedWords(typed: String, phrase: String): Int {
  val typedWords = normalizePhrase(typed).split(" ").filter { it.isNotEmpty() }
  val target = normalizePhrase(phrase).split(" ")
  var count = 0
  for (i in typedWords.indices) {
    if (i < target.size && typedWords[i] == target[i]) count++ else break
  }
  return count
}

internal fun todayLabel(): String {
  val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
  return java.text.SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time)
}

internal fun animationsOff(context: Context): Boolean =
  AndroidSettings.Global.getFloat(context.contentResolver, AndroidSettings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** Returns a fixed value when the user turned animations off, otherwise the animation. */
@Composable
internal fun motion(reduceMotion: Boolean, still: Float, animated: @Composable () -> State<Float>): State<Float> =
  if (reduceMotion) remember { mutableFloatStateOf(still) } else animated()

/** Speech-to-text for "Say the phrase", preferring on-device recognition so it works offline. */
internal class SpeechController(
  private val context: Context,
  private val onText: (String) -> Unit,
  private val onListening: (Boolean) -> Unit
) {
  private var recognizer: SpeechRecognizer? = null

  fun start() {
    stop()
    val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
      SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
    } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
      SpeechRecognizer.createSpeechRecognizer(context)
    } else {
      return
    }
    r.setRecognitionListener(object : RecognitionListener {
      override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText)
        onListening(false)
      }

      override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let(onText)
      }

      override fun onError(error: Int) = onListening(false)
      override fun onReadyForSpeech(params: Bundle?) = Unit
      override fun onBeginningOfSpeech() = Unit
      override fun onRmsChanged(rmsdB: Float) = Unit
      override fun onBufferReceived(buffer: ByteArray?) = Unit
      override fun onEndOfSpeech() = Unit
      override fun onEvent(eventType: Int, params: Bundle?) = Unit
    })
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }
    r.startListening(intent)
    recognizer = r
    onListening(true)
  }

  fun stop() {
    recognizer?.let {
      runCatching { it.cancel() }
      it.destroy()
    }
    recognizer = null
    onListening(false)
  }
}
