@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package expo.modules.chronosalarms.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch
import androidx.compose.ui.util.lerp
import expo.modules.kotlin.views.ComposeProps

data class GlassTabScaffoldProps(
  val selected: Int? = null,
  val labels: List<String>? = null,
  val icons: List<String>? = null,
  /** The page that shows the round action button (New alarm) beside the bar; null for none. */
  val actionPage: Int? = null,
  val actionIcon: String? = null,
  val actionLabel: String? = null,
  /** Space under the bar for the gesture navigation area, in dp. */
  val bottomInset: Float? = null,
  val dark: Boolean? = null,
  val accentColor: String? = null,
  val onAccentColor: String? = null,
  val contentColor: String? = null,
  val glassColor: String? = null
) : ComposeProps

/** A tab tap scrolls the pager on this curve: eased in and out, no overshoot. */
private val PageSlide = tween<Float>(450, easing = FastOutSlowInEasing)
private val TabPadding = 18.dp
private val TabIcon = 24.dp
private val LabelGap = 8.dp
private val TabSpacing = 2.dp
private val ActionGap = 12.dp
private val ActionSize = 64.dp

/** How open tab [index] is at pager [position]: 1 when it is the page, 0 a page or more away. */
private fun openness(position: Float, index: Int): Float =
  FastOutSlowInEasing.transform((1f - abs(position - index)).coerceIn(0f, 1f))

/**
 * The home scaffold: its children are the pages of a horizontal pager that fills the screen,
 * and a frosted-glass tab bar floats over the bottom.
 *
 * Everything in the bar follows one value, the pager's scroll position: each tab's width and
 * label, the sliding pill, and the action button growing in or out. A swipe moves them with the
 * finger; a tap scrolls the pager on an eased curve and they ride along. Nothing animates on its
 * own, so nothing can lag or chase.
 *
 * The glass is real backdrop blur on Android 12+: the pager draws into a GraphicsLayer each
 * frame, and the bar redraws the slice of it underneath through a BlurEffect, clipped to the
 * pill. Older versions get a denser tint instead.
 */
@Composable
fun GlassTabScaffold(
  props: GlassTabScaffoldProps,
  pageCount: Int,
  onTabSelect: (Int) -> Unit,
  onPageChange: (Int) -> Unit,
  onAction: () -> Unit,
  page: @Composable (Int) -> Unit
) {
  val accent = parseColor(props.accentColor, Color(0xFF8C1D30))
  val onAccent = parseColor(props.onAccentColor, Color.White)
  val ink = parseColor(props.contentColor, Color(0xFF22191A))
  val glass = parseColor(props.glassColor, Color(0xFFFCEAEA))
  val dark = props.dark == true
  val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

  val contentLayer = rememberGraphicsLayer()
  val blurLayer = rememberGraphicsLayer()
  // Bumped whenever the pager redraws, so the bar redraws its blurred slice too.
  var contentFrame by remember { mutableIntStateOf(0) }
  var contentOrigin by remember { mutableStateOf(Offset.Zero) }
  var barOrigin by remember { mutableStateOf(Offset.Zero) }

  val selected = props.selected ?: 0
  val currentSelected by rememberUpdatedState(selected)
  val currentOnPageChange by rememberUpdatedState(onPageChange)
  val pager = rememberPagerState(initialPage = selected) { pageCount }
  val scope = rememberCoroutineScope()
  // From JS (a deep link): glide there, unless the pager is already heading there.
  LaunchedEffect(selected) {
    if (pager.currentPage != selected && pager.targetPage != selected) {
      pager.animateScrollToPage(selected, animationSpec = PageSlide)
    }
  }
  LaunchedEffect(pager) {
    snapshotFlow { pager.currentPage }.collect { if (it != currentSelected) currentOnPageChange(it) }
  }
  val position by remember { derivedStateOf { pager.currentPage + pager.currentPageOffsetFraction } }
  val select: (Int) -> Unit = { index ->
    scope.launch { pager.animateScrollToPage(index, animationSpec = PageSlide) }
    onTabSelect(index)
  }

  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier
        .fillMaxSize()
        .onGloballyPositioned { contentOrigin = it.positionInRoot() }
        .drawWithContent {
          if (canBlur) {
            contentLayer.record { this@drawWithContent.drawContent() }
            drawLayer(contentLayer)
            contentFrame++
          } else {
            drawContent()
          }
        }
    ) {
      HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index ->
        Box(Modifier.fillMaxSize()) { page(index) }
      }
    }

    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = (props.bottomInset ?: 0f).dp + 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val pill = RoundedCornerShape(percent = 50)
      Box {
        // A soft, blurred shadow under the pill. The glass draws the blurred page over it, so it
        // only shows around the edges and lifts the bar without muddying it.
        Box(
          Modifier
            .matchParentSize()
            .offset(y = 6.dp)
            .blur(18.dp, BlurredEdgeTreatment.Unbounded)
            .background(Color.Black.copy(alpha = if (dark) 0.35f else 0.1f), pill)
        )
        Box(
          modifier = Modifier
            .onGloballyPositioned { barOrigin = it.positionInRoot() }
            .clip(pill)
            .drawBehind {
              if (canBlur) {
                contentFrame // read: redraw with the pager
                blurLayer.renderEffect = BlurEffect(14.dp.toPx(), 14.dp.toPx(), TileMode.Clamp)
                val shift = barOrigin - contentOrigin
                blurLayer.record { translate(-shift.x, -shift.y) { drawLayer(contentLayer) } }
                drawLayer(blurLayer)
              }
              drawRect(glass.copy(alpha = if (canBlur) (if (dark) 0.3f else 0.22f) else 0.9f))
              // A soft sheen across the top half, like light on glass.
              drawRoundRect(
                brush = Brush.verticalGradient(
                  listOf(Color.White.copy(alpha = if (dark) 0.12f else 0.5f), Color.Transparent),
                  endY = size.height * 0.6f
                ),
                cornerRadius = CornerRadius(size.height / 2)
              )
            }
            .border(
              width = 1.dp,
              brush = Brush.verticalGradient(
                listOf(
                  Color.White.copy(alpha = if (dark) 0.22f else 0.75f),
                  Color.White.copy(alpha = if (dark) 0.03f else 0.12f)
                )
              ),
              shape = pill
            )
            .padding(6.dp)
        ) {
          TabRow(props, { position }, accent, onAccent, ink, select)
        }
      }

      props.actionPage?.let { actionPage ->
        // Grows in as the pager nears the action's page and shrinks away as it leaves.
        val grow = openness(position, actionPage)
        Box(
          modifier = Modifier.width((ActionGap + ActionSize) * grow).height(ActionSize),
          contentAlignment = Alignment.CenterEnd
        ) {
          if (grow > 0.01f) ActionButton(props, grow, accent, onAccent, onAction)
        }
      }
    }
  }
}

@Composable
private fun TabRow(
  props: GlassTabScaffoldProps,
  position: () -> Float,
  accent: Color,
  onAccent: Color,
  ink: Color,
  onSelect: (Int) -> Unit
) {
  val haptics = LocalHapticFeedback.current
  val density = LocalDensity.current
  val labels = props.labels ?: emptyList()
  val icons = props.icons ?: emptyList()
  val labelStyle = TextStyle(fontFamily = ChronosTextFamily, fontSize = 15.sp)
  val measurer = rememberTextMeasurer()
  val labelWidths = remember(labels) {
    labels.map { with(density) { measurer.measure(it, labelStyle, maxLines = 1).size.width.toDp() } }
  }
  val p = position()
  val opens = labels.indices.map { openness(p, it) }
  // The page the pager has come to rest on (-1 while between pages). Its icon plays when it
  // arrives and again on each tap; pages passed on the way stay still.
  val settled = p.roundToInt().takeIf { abs(p - it) < 0.01f } ?: -1
  val taps = remember { mutableStateMapOf<Int, Int>() }
  val widths = labels.indices.map { i -> TabPadding * 2 + TabIcon + (LabelGap + labelWidths[i]) * opens[i] }

  val inPill = rememberGraphicsLayer()
  val pillPath = remember { Path() }
  Row(
    modifier = Modifier.drawWithContent {
      if (widths.isEmpty()) return@drawWithContent drawContent()
      // The pill sits between the two tabs the pager is between, in proportion.
      val spacing = TabSpacing.toPx()
      val starts = widths.runningFold(0f) { x, w -> x + w.toPx() + spacing }
      val from = p.toInt().coerceIn(0, widths.lastIndex)
      val to = (from + 1).coerceAtMost(widths.lastIndex)
      val t = (p - from).coerceIn(0f, 1f)
      val pill = RoundRect(
        left = lerp(starts[from], starts[to], t),
        top = 0f,
        right = lerp(starts[from], starts[to], t) + lerp(widths[from].toPx(), widths[to].toPx(), t),
        bottom = size.height,
        cornerRadius = CornerRadius(size.height / 2)
      )
      pillPath.rewind()
      pillPath.addRoundRect(pill)
      drawPath(pillPath, accent)
      // Every tab in ink, then the same tabs again, tinted, through the pill's shape: whatever
      // the pill covers turns onAccent, exactly to its edge, even mid-glide.
      drawContent()
      inPill.colorFilter = ColorFilter.tint(onAccent, BlendMode.SrcIn)
      inPill.record { this@drawWithContent.drawContent() }
      clipPath(pillPath) { drawLayer(inPill) }
    },
    horizontalArrangement = Arrangement.spacedBy(TabSpacing),
    verticalAlignment = Alignment.CenterVertically
  ) {
    labels.forEachIndexed { index, label ->
      val open = opens[index]
      val interaction = remember { MutableInteractionSource() }
      val pressed by interaction.collectIsPressedAsState()
      val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(140, easing = FastOutSlowInEasing), label = "press")
      val tint = ink
      Box(
        modifier = Modifier
          .width(widths[index])
          .height(52.dp)
          .clip(RoundedCornerShape(percent = 50))
          .clickable(interactionSource = interaction, indication = null) {
            if (index != props.selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            if (index == settled) taps[index] = (taps[index] ?: 0) + 1
            onSelect(index)
          }
          .semantics {
            role = Role.Tab
            selected = index == props.selected
            contentDescription = label
          }
          .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.CenterStart
      ) {
        Row(
          modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true).padding(start = TabPadding),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TabGlyph(icons.getOrNull(index), tint, active = settled == index, replay = taps[index] ?: 0)
          Text(
            text = label,
            modifier = Modifier.padding(start = LabelGap).graphicsLayer { alpha = open },
            style = labelStyle.copy(color = tint),
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
private fun ActionButton(props: GlassTabScaffoldProps, grow: Float, accent: Color, onAccent: Color, onAction: () -> Unit) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val turn by animateFloatAsState(if (pressed) 40f else 0f, tween(260, easing = FastOutSlowInEasing), label = "turn")
  val press by animateFloatAsState(if (pressed) 0.94f else 1f, tween(140, easing = FastOutSlowInEasing), label = "scale")
  val shape = MaterialShapes.Cookie9Sided.toShape()
  Box(
    modifier = Modifier
      .size(ActionSize)
      .graphicsLayer {
        val s = press * (0.5f + 0.5f * grow)
        scaleX = s
        scaleY = s
        alpha = grow
        // Rolls in as it grows.
        rotationZ = turn + (1f - grow) * -90f
      }
      .clip(shape)
      .drawBehind { drawRect(accent) }
      .clickable(interactionSource = interaction, indication = null, enabled = grow > 0.5f, onClick = onAction)
      .semantics {
        role = Role.Button
        contentDescription = props.actionLabel ?: ""
      },
    contentAlignment = Alignment.Center
  ) {
    iconRes(props.actionIcon ?: "add")?.let {
      Icon(
        painterResource(it),
        contentDescription = null,
        tint = onAccent,
        modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = -turn }
      )
    }
  }
}

/**
 * A tab icon with a little life in it, played when its tab becomes current and on every tap of
 * the current tab: the alarm bell rings, the hourglass turns over, the stopwatch hand sweeps
 * round once. Other icons stay still.
 */
@Composable
private fun TabGlyph(name: String?, tint: Color, active: Boolean, replay: Int) {
  val play = remember { Animatable(1f) }
  // The hourglass keeps its turns so each flip continues from where the last one ended.
  val turns = remember { Animatable(0f) }
  LaunchedEffect(active, replay) {
    if (!active) return@LaunchedEffect
    when (name) {
      "hourglass" -> turns.animateTo(turns.targetValue + 180f, tween(620, easing = FastOutSlowInEasing))
      else -> {
        play.snapTo(0f)
        play.animateTo(1f, tween(if (name == "alarm") 760 else 900, easing = LinearEasing))
      }
    }
  }
  val v = play.value
  when (name) {
    "alarm" -> iconRes(name)?.let {
      Icon(
        painterResource(it),
        contentDescription = null,
        tint = tint,
        modifier = Modifier
          .size(TabIcon)
          .graphicsLayer {
            // Rings from the top, like a bell: quick swings that settle.
            transformOrigin = TransformOrigin(0.5f, 0.15f)
            rotationZ = sin(v * 5f * 2f * PI.toFloat()) * 16f * (1f - v)
          }
      )
    }
    "hourglass" -> iconRes(name)?.let {
      Icon(painterResource(it), contentDescription = null, tint = tint, modifier = Modifier.size(TabIcon).graphicsLayer { rotationZ = turns.value })
    }
    "stopwatch" -> StopwatchGlyph(tint, handTurn = FastOutSlowInEasing.transform(v) * 360f)
    else -> iconRes(name)?.let { Icon(painterResource(it), contentDescription = null, tint = tint, modifier = Modifier.size(TabIcon)) }
  }
}

/** The stopwatch icon drawn by hand (Material Symbols proportions) so its hand can move. */
@Composable
private fun StopwatchGlyph(tint: Color, handTurn: Float) {
  Canvas(Modifier.size(TabIcon)) {
    val u = size.width / 24f
    val center = Offset(12f * u, 13.5f * u)
    val stroke = 2f * u
    drawCircle(tint, radius = 7.5f * u, center = center, style = Stroke(stroke))
    // Crown and its stem.
    drawLine(tint, Offset(9.5f * u, 2.5f * u), Offset(14.5f * u, 2.5f * u), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(12f * u, 2.5f * u), Offset(12f * u, 5.5f * u), strokeWidth = stroke, cap = StrokeCap.Round)
    // Side button.
    drawLine(tint, Offset(18.2f * u, 6.3f * u), Offset(19.6f * u, 4.9f * u), strokeWidth = stroke, cap = StrokeCap.Round)
    rotate(handTurn, pivot = center) {
      drawLine(tint, center, Offset(center.x, center.y - 4.5f * u), strokeWidth = stroke, cap = StrokeCap.Round)
    }
  }
}
