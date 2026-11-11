package expo.modules.chronosalarms.ui

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * The full-screen alarm. Native Compose, not React Native, so it appears instantly over the
 * lock screen without starting the JS runtime.
 */
class RingingActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    showOverLockScreen()

    // Back must not silence an alarm.
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() = Unit
    })

    val settings = AlarmStore.get(this).settings()
    setContent {
      val session by RingingState.session.collectAsState()
      LaunchedEffect(session == null) {
        if (session == null) finish()
      }
      ChronosTheme(palette = settings.palette, theme = settings.theme) {
        session?.let { current ->
          RingingScreen(
            session = current,
            onSnooze = { minutes ->
              RingingService.send(this, RingingService.ACTION_SNOOZE) { putExtra(RingingService.EXTRA_MINUTES, minutes) }
            },
            onDismiss = { RingingService.send(this, RingingService.ACTION_DISMISS) },
            onDuck = { RingingService.send(this, RingingService.ACTION_DUCK) },
            onGiveUp = { RingingService.send(this, RingingService.ACTION_GIVE_UP) }
          )
        }
      }
    }
  }

  /** Volume keys silence the ring instead of changing the volume. Power is handled in the service. */
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
      if (RingingState.session.value != null) {
        RingingService.send(this, RingingService.ACTION_MUTE)
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onResume() {
    super.onResume()
    if (RingingState.session.value != null) RingingService.send(this, RingingService.ACTION_SCREEN_SHOWN)
  }

  override fun onPause() {
    if (RingingState.session.value != null) RingingService.send(this, RingingService.ACTION_SCREEN_HIDDEN)
    super.onPause()
  }

  private fun showOverLockScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }
}
