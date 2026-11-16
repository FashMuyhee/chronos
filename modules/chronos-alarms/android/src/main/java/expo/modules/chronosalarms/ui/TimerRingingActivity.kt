@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package expo.modules.chronosalarms.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import expo.modules.chronosalarms.R
import expo.modules.chronosalarms.clock.ClockEngine
import expo.modules.chronosalarms.clock.ClockNotifications
import expo.modules.chronosalarms.clock.TimerState
import expo.modules.chronosalarms.clock.TimerStatus
import expo.modules.chronosalarms.data.AlarmStore
import kotlinx.coroutines.delay

/** "Time's up", shown over the lock screen by the finished timer's full-screen notification. */
class TimerRingingActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    ClockEngine.load(this)

    val settings = AlarmStore.get(this).settings()
    setContent {
      val timer by ClockEngine.timer.collectAsState()
      LaunchedEffect(timer.status) {
        if (timer.status != TimerStatus.RINGING) finish()
      }
      ChronosTheme(palette = settings.palette, theme = settings.theme) {
        TimesUp(
          timer = timer,
          onAddMinute = { ClockEngine.addMinute(this) },
          onStop = { ClockEngine.resetTimer(this) }
        )
      }
    }
  }
}

@Composable
private fun TimesUp(timer: TimerState, onAddMinute: () -> Unit, onStop: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  val reduceMotion = animationsOff(LocalContext.current)
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      now = System.currentTimeMillis()
      delay(250)
    }
  }
  // A springy heartbeat on the badge, once a second.
  val pulse = remember { Animatable(1f) }
  LaunchedEffect(reduceMotion) {
    if (reduceMotion) return@LaunchedEffect
    while (true) {
      pulse.animateTo(1.12f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
      pulse.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
      delay(350)
    }
  }
  val over = (now - timer.endAt).coerceAtLeast(0)

  Column(
    modifier = Modifier.fillMaxSize().background(cs.surface).systemBarsPadding().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(168.dp)
        .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value; rotationZ = (pulse.value - 1f) * 90f }
        .clip(MaterialShapes.Cookie9Sided.toShape())
        .background(cs.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painter = painterResource(R.drawable.chronos_ic_hourglass_top),
        contentDescription = null,
        tint = cs.onPrimaryContainer,
        modifier = Modifier.size(72.dp)
      )
    }
    Spacer(Modifier.height(40.dp))
    Text("Time's up", style = MaterialTheme.typography.displayMedium, color = cs.onSurface)
    Spacer(Modifier.height(8.dp))
    Text(
      "−" + ClockNotifications.formatClock(over).let { if (over < 1000) "0:00" else it },
      style = TextStyle(fontFamily = ChronosDisplayFamily, fontSize = 40.sp, color = cs.primary, fontFeatureSettings = "tnum")
    )
    Spacer(Modifier.height(4.dp))
    Text("${ClockNotifications.formatLength(timer.lastMs)} timer", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
    Spacer(Modifier.height(56.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      FilledTonalButton(onClick = onAddMinute, modifier = Modifier.height(64.dp)) {
        Text("+1 min", style = MaterialTheme.typography.titleMedium)
      }
      Spacer(Modifier.width(16.dp))
      Button(onClick = onStop, modifier = Modifier.height(64.dp).width(160.dp)) {
        Icon(painterResource(R.drawable.chronos_ic_stop), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text("Stop", style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}
