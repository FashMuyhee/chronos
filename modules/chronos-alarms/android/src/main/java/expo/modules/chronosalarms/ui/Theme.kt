package expo.modules.chronosalarms.ui

import expo.modules.chronosalarms.data.*
import expo.modules.chronosalarms.schedule.*
import expo.modules.chronosalarms.ringing.*
import expo.modules.chronosalarms.ui.*
import expo.modules.chronosalarms.R

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Chronos burnt-burgundy Material 3 scheme. Keep in sync with src/theme.ts. */
val BurgundyLight: ColorScheme = lightColorScheme(
  primary = Color(0xFF8C1D30),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFFFDADB),
  onPrimaryContainer = Color(0xFF3F0010),
  inversePrimary = Color(0xFFFFB2B7),
  secondary = Color(0xFF775656),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFF6DCDB),
  onSecondaryContainer = Color(0xFF2C1516),
  tertiary = Color(0xFF755A2F),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFFFDDB1),
  onTertiaryContainer = Color(0xFF2A1800),
  background = Color(0xFFFFF8F7),
  onBackground = Color(0xFF22191A),
  surface = Color(0xFFFFF8F7),
  onSurface = Color(0xFF22191A),
  surfaceVariant = Color(0xFFF4DDDD),
  onSurfaceVariant = Color(0xFF534343),
  outline = Color(0xFF857373),
  outlineVariant = Color(0xFFD8C2C2),
  inverseSurface = Color(0xFF382E2E),
  inverseOnSurface = Color(0xFFFFEDEC),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFFFF0F0),
  surfaceContainer = Color(0xFFFCEAEA),
  surfaceContainerHigh = Color(0xFFF6E4E4),
  surfaceContainerHighest = Color(0xFFF0DEDE)
)

val BurgundyDark: ColorScheme = darkColorScheme(
  primary = Color(0xFFFFB2B7),
  onPrimary = Color(0xFF5F1121),
  primaryContainer = Color(0xFF7A1E2C),
  onPrimaryContainer = Color(0xFFFFDADB),
  inversePrimary = Color(0xFF8C1D30),
  secondary = Color(0xFFE6BDBC),
  onSecondary = Color(0xFF44292A),
  secondaryContainer = Color(0xFF5D3F40),
  onSecondaryContainer = Color(0xFFFFDAD9),
  tertiary = Color(0xFFE5C18D),
  onTertiary = Color(0xFF422C05),
  tertiaryContainer = Color(0xFF5C4200),
  onTertiaryContainer = Color(0xFFFFDDB1),
  background = Color(0xFF1A1112),
  onBackground = Color(0xFFF0DEDF),
  surface = Color(0xFF1A1112),
  onSurface = Color(0xFFF0DEDF),
  surfaceVariant = Color(0xFF534343),
  onSurfaceVariant = Color(0xFFD8C2C2),
  outline = Color(0xFFA08C8C),
  outlineVariant = Color(0xFF534343),
  inverseSurface = Color(0xFFF0DEDF),
  inverseOnSurface = Color(0xFF382E2E),
  surfaceContainerLowest = Color(0xFF140C0D),
  surfaceContainerLow = Color(0xFF22191A),
  surfaceContainer = Color(0xFF271D1E),
  surfaceContainerHigh = Color(0xFF322828),
  surfaceContainerHighest = Color(0xFF3D3232)
)

@Composable
fun ChronosTheme(palette: String, theme: String, content: @Composable () -> Unit) {
  val dark = when (theme) {
    "light" -> false
    "dark" -> true
    else -> isSystemInDarkTheme()
  }
  val context = LocalContext.current
  val scheme = if (palette == "device" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  } else {
    if (dark) BurgundyDark else BurgundyLight
  }
  MaterialTheme(colorScheme = scheme, typography = ChronosTypography, content = content)
}

/** Google Sans Flex cuts shared with the React Native screens (assets/fonts). */
val ChronosTextFamily = FontFamily(
  Font(R.font.chronos_text, FontWeight.Normal),
  Font(R.font.chronos_text_medium, FontWeight.Medium)
)

/** Google Sans Flex at display optical size, for clock digits. */
val ChronosDisplayFamily = FontFamily(Font(R.font.chronos_display, FontWeight.Normal))

private val ChronosTypography = Typography().let { base ->
  fun TextStyle.flex() = copy(fontFamily = ChronosTextFamily)
  base.copy(
    displayLarge = base.displayLarge.copy(fontFamily = ChronosDisplayFamily),
    displayMedium = base.displayMedium.copy(fontFamily = ChronosDisplayFamily),
    displaySmall = base.displaySmall.copy(fontFamily = ChronosDisplayFamily),
    headlineLarge = base.headlineLarge.flex(),
    headlineMedium = base.headlineMedium.flex(),
    headlineSmall = base.headlineSmall.flex(),
    titleLarge = base.titleLarge.flex(),
    titleMedium = base.titleMedium.flex(),
    titleSmall = base.titleSmall.flex(),
    bodyLarge = base.bodyLarge.flex(),
    bodyMedium = base.bodyMedium.flex(),
    bodySmall = base.bodySmall.flex(),
    labelLarge = base.labelLarge.flex(),
    labelMedium = base.labelMedium.flex(),
    labelSmall = base.labelSmall.flex()
  )
}
