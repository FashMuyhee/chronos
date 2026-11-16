package expo.modules.chronosalarms.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import expo.modules.kotlin.views.ComposeProps

data class MorphButtonProps(
  val width: Float? = null,
  val height: Float? = null,
  val label: String? = null,
  val icon: String? = null,
  /** Spoken name when the button only shows an icon. */
  val accessibilityLabel: String? = null,
  /** Corner radius at rest as a fraction of the shorter side: 0.5 is a pill or circle. */
  val roundness: Float? = null,
  /** Display digits (number pad) instead of the text cut. */
  val display: Boolean? = null,
  val fontSize: Float? = null,
  val iconSize: Float? = null,
  val enabled: Boolean? = null,
  val containerColor: String? = null,
  val contentColor: String? = null,
  val borderColor: String? = null
) : ComposeProps

/**
 * An expressive button: at rest it is round, and while held its corners squeeze toward a
 * squircle as it shrinks a little, both on springs. Releasing springs it back with a small
 * overshoot. Icons and labels come from props so one view covers the keypad and the controls.
 */
@Composable
fun MorphButton(props: MorphButtonProps, onPress: () -> Unit) {
  val container = parseColor(props.containerColor, Color(0xFFF6E4E4))
  val content = parseColor(props.contentColor, Color(0xFF22191A))
  val border = props.borderColor?.let { parseColor(it, Color.Transparent) }
  val haptics = LocalHapticFeedback.current
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()

  val w = (props.width ?: 64f).dp
  val h = (props.height ?: 64f).dp
  val enabled = props.enabled ?: true
  val rest = min(w, h) * (props.roundness ?: 0.5f).coerceIn(0f, 0.5f)
  val squeezed = min(w, h) * 0.22f
  val corner by animateDpAsState(if (pressed) squeezed else rest, spring(0.45f, 700f), label = "corner")
  val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(0.4f, 700f), label = "scale")
  val shape = RoundedCornerShape(corner)
  val alpha = if (enabled) 1f else 0.38f

  Box(
    modifier = Modifier
      .size(w, h)
      .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
      .clip(shape)
      .background(container)
      .let { if (border != null) it.border(1.5.dp, border, shape) else it }
      .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        onPress()
      }
      .semantics {
        role = Role.Button
        (props.accessibilityLabel ?: props.label)?.let { contentDescription = it }
        if (!enabled) disabled()
      },
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      iconRes(props.icon)?.let {
        Icon(painterResource(it), contentDescription = null, tint = content, modifier = Modifier.size((props.iconSize ?: 24f).dp))
      }
      if (props.icon != null && !props.label.isNullOrEmpty()) Spacer(Modifier.width(8.dp))
      if (!props.label.isNullOrEmpty()) {
        Text(
          text = props.label,
          style = TextStyle(
            fontFamily = if (props.display == true) ChronosDisplayFamily else ChronosTextFamily,
            fontSize = (props.fontSize ?: 18f).sp,
            color = content,
            fontFeatureSettings = "tnum"
          ),
          maxLines = 1
        )
      }
    }
  }
}
