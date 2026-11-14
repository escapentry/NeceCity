package com.example.display

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class WindowWidthTier {
  COMPACT,
  MEDIUM,
  EXPANDED,
}

@Immutable
data class NeceCityDisplayProfile(
  val widthTier: WindowWidthTier,
  val isCompactHeight: Boolean,
  val isLandscape: Boolean,
  val useNavigationRail: Boolean,
  val useTwoPaneLayout: Boolean,
  val availableWidthDp: Float,
  val availableHeightDp: Float,
  val density: Float,
  val systemFontScale: Float,
  val horizontalPaddingDp: Dp,
  val keypadButtonHeightDp: Dp,
  val keypadDigitFontSp: TextUnit,
  val titleFontSp: TextUnit,
  val bodyFontSp: TextUnit,
  val captionFontSp: TextUnit,
)

object DisplayAutoScaler {

  fun resolveHardwareMetrics(context: Context): HardwareDisplayInfo {
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    val refreshRate =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { context.display?.refreshRate?.roundToInt() }.getOrNull() ?: 60
      } else {
        @Suppress("DEPRECATION")
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.refreshRate?.roundToInt() ?: 60
      }

    return HardwareDisplayInfo(
      screenWidthPx = displayMetrics.widthPixels,
      screenHeightPx = displayMetrics.heightPixels,
      densityDpi = displayMetrics.densityDpi,
      refreshRateHz = refreshRate.coerceIn(60, 240),
    )
  }

  fun computeProfile(
    availableWidthDp: Float,
    availableHeightDp: Float,
    density: Float,
    systemFontScale: Float,
  ): NeceCityDisplayProfile {
    val safeWidth = availableWidthDp.coerceAtLeast(280f)
    val safeHeight = availableHeightDp.coerceAtLeast(320f)
    val isLandscape = safeWidth > safeHeight * 1.08f
    val isCompactHeight = safeHeight < 480f

    val widthTier = when {
      safeWidth < 600f -> WindowWidthTier.COMPACT
      safeWidth < 840f -> WindowWidthTier.MEDIUM
      else -> WindowWidthTier.EXPANDED
    }

    val useNavigationRail = widthTier != WindowWidthTier.COMPACT || (isLandscape && safeWidth >= 560f)
    val useTwoPaneLayout = widthTier == WindowWidthTier.EXPANDED && !isCompactHeight

    val horizontalPaddingDp = when {
      safeWidth < 360f -> 12.dp
      widthTier == WindowWidthTier.COMPACT -> 16.dp
      widthTier == WindowWidthTier.MEDIUM -> 22.dp
      else -> 26.dp
    }

    val keypadButtonHeightDp = when {
      isCompactHeight -> 48.dp
      safeHeight < 680f -> 54.dp
      else -> 64.dp
    }

    val fontGuard = if (systemFontScale > 1.25f) 1.25f / systemFontScale else 1f
    val scaleBoost = (safeWidth / 400f).coerceIn(0.95f, 1.10f) * fontGuard

    return NeceCityDisplayProfile(
      widthTier = widthTier,
      isCompactHeight = isCompactHeight,
      isLandscape = isLandscape,
      useNavigationRail = useNavigationRail,
      useTwoPaneLayout = useTwoPaneLayout,
      availableWidthDp = safeWidth,
      availableHeightDp = safeHeight,
      density = density,
      systemFontScale = systemFontScale,
      horizontalPaddingDp = horizontalPaddingDp,
      keypadButtonHeightDp = keypadButtonHeightDp,
      keypadDigitFontSp = (if (isCompactHeight) 20f else 24f * fontGuard).sp,
      titleFontSp = (15.5f * scaleBoost).coerceIn(14f, 18f).sp,
      bodyFontSp = (13.5f * scaleBoost).coerceIn(12f, 15.5f).sp,
      captionFontSp = (11.5f * scaleBoost).coerceIn(10.5f, 13f).sp,
    )
  }
}

@Immutable
data class HardwareDisplayInfo(
  val screenWidthPx: Int,
  val screenHeightPx: Int,
  val densityDpi: Int,
  val refreshRateHz: Int,
)
