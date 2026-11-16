package expo.modules.chronosalarms.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import expo.modules.chronosalarms.clock.ClockEngine
import expo.modules.kotlin.views.ComposeProps

data class LapListProps(
  val contentColor: String? = null,
  val mutedColor: String? = null,
  val containerColor: String? = null,
  val fastestColor: String? = null,
  val fastestContainerColor: String? = null,
  val onFastestContainerColor: String? = null,
  val slowestColor: String? = null,
  val slowestContainerColor: String? = null,
  val onSlowestContainerColor: String? = null
) : ComposeProps

private enum class Mark { NONE, FASTEST, SLOWEST }

private data class LapRow(val number: Int, val lapMs: Long, val totalMs: Long, val mark: Mark)

private val Ease = FastOutSlowInEasing

/**
 * The stopwatch laps, newest first, read straight from [ClockEngine]. A new lap grows in at the
 * top (height, fade and a short drop) so the older rows glide down beneath it; the fastest and
 * slowest badges cross-fade as they move between laps.
 */
@Composable
fun LapList(props: LapListProps) {
  val content = parseColor(props.contentColor, Color(0xFF22191A))
  val muted = parseColor(props.mutedColor, Color(0xFF534343))
  val container = parseColor(props.containerColor, Color(0xFFFCEAEA))
  val fastest = parseColor(props.fastestColor, Color(0xFF755A2F))
  val fastestContainer = parseColor(props.fastestContainerColor, Color(0xFFFFDDB1))
  val onFastest = parseColor(props.onFastestContainerColor, Color(0xFF2A1800))
  val slowest = parseColor(props.slowestColor, Color(0xFFBA1A1A))
  val slowestContainer = parseColor(props.slowestContainerColor, Color(0xFFFFDAD6))
  val onSlowest = parseColor(props.onSlowestContainerColor, Color(0xFF410002))

  val state by ClockEngine.stopwatch.collectAsState()
  val rows = remember(state.laps) { rowsOf(state.laps) }
  // Laps already there when the list first appears show at once; only new ones animate in.
  val firstSeen = remember { state.laps.size }

  Column(Modifier.fillMaxWidth()) {
    rows.forEachIndexed { index, lap ->
      key(lap.number) {
        val visible = remember { MutableTransitionState(lap.number <= firstSeen).apply { targetState = true } }
        AnimatedVisibility(
          visibleState = visible,
          enter = expandVertically(tween(380, easing = Ease), expandFrom = Alignment.Top) +
            fadeIn(tween(300, delayMillis = 90)) +
            slideInVertically(tween(380, easing = Ease)) { -it / 3 } +
            scaleIn(tween(380, easing = Ease), initialScale = 0.96f)
        ) {
          // Big outer corners on the group, small inner ones; they ease as rows join.
          val top by animateDpAsState(if (index == 0) 20.dp else 4.dp, tween(300, easing = Ease), label = "top")
          val bottom by animateDpAsState(if (index == rows.lastIndex) 20.dp else 4.dp, tween(300, easing = Ease), label = "bottom")
          val lapColor = when (lap.mark) {
            Mark.FASTEST -> fastest
            Mark.SLOWEST -> slowest
            Mark.NONE -> content
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 1.dp)
              .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
              .background(container)
              .padding(horizontal = 20.dp, vertical = 14.dp)
              .semantics {
                contentDescription = "Lap ${lap.number}, ${spoken(lap.lapMs)}" + when (lap.mark) {
                  Mark.FASTEST -> ", fastest"
                  Mark.SLOWEST -> ", slowest"
                  Mark.NONE -> ""
                }
              },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Lap ${lap.number}",
              modifier = Modifier.width(64.dp),
              style = TextStyle(fontFamily = ChronosTextFamily, fontSize = 16.sp, color = muted, fontFeatureSettings = "tnum")
            )
            Box(Modifier.weight(1f)) {
              AnimatedContent(
                targetState = lap.mark,
                transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(160)) },
                label = "mark"
              ) { mark ->
                if (mark != Mark.NONE) {
                  val fast = mark == Mark.FASTEST
                  Text(
                    if (fast) "Fastest" else "Slowest",
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (fast) fastestContainer else slowestContainer)
                      .padding(horizontal = 8.dp, vertical = 2.dp),
                    style = TextStyle(fontFamily = ChronosTextFamily, fontSize = 12.sp, color = if (fast) onFastest else onSlowest)
                  )
                }
              }
            }
            Text(
              stopwatchTime(lap.lapMs),
              style = TextStyle(fontFamily = ChronosTextFamily, fontSize = 20.sp, color = lapColor, fontFeatureSettings = "tnum")
            )
            Spacer(Modifier.width(16.dp))
            Text(
              stopwatchTime(lap.totalMs),
              style = TextStyle(fontFamily = ChronosTextFamily, fontSize = 15.sp, color = muted, fontFeatureSettings = "tnum")
            )
          }
        }
      }
    }
  }
}

/** Newest first, with the fastest and slowest marked once there are two laps to compare. */
private fun rowsOf(totals: List<Long>): List<LapRow> {
  val laps = totals.mapIndexed { i, total -> i + 1 to total - (totals.getOrNull(i - 1) ?: 0L) }
  val fastest = if (laps.size >= 2) laps.minBy { it.second }.first else -1
  val slowest = if (laps.size >= 2) laps.maxBy { it.second }.first else -1
  return laps.map { (number, lapMs) ->
    val mark = when {
      fastest == slowest -> Mark.NONE
      number == fastest -> Mark.FASTEST
      number == slowest -> Mark.SLOWEST
      else -> Mark.NONE
    }
    LapRow(number, lapMs, totals[number - 1], mark)
  }.reversed()
}

/** 0:07.42, 12:07.42 or 1:12:07.42. */
private fun stopwatchTime(ms: Long): String {
  val total = ms / 1000
  val h = total / 3600
  val m = (total % 3600) / 60
  val s = total % 60
  val cs = (ms / 10) % 100
  return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%d:%02d.%02d".format(m, s, cs)
}

private fun spoken(ms: Long): String {
  val total = ms / 1000
  return "${total / 60} minutes ${total % 60} seconds"
}
