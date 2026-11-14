package com.example

import com.example.display.DisplayAutoScaler
import com.example.display.WindowWidthTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayAutoScalerTest {

  @Test
  fun `compact phone uses bottom navigation and single pane`() {
    val profile = DisplayAutoScaler.computeProfile(
      availableWidthDp = 360f,
      availableHeightDp = 780f,
      density = 3.0f,
      systemFontScale = 1.0f,
    )

    assertEquals(WindowWidthTier.COMPACT, profile.widthTier)
    assertFalse(profile.useNavigationRail)
    assertFalse(profile.useTwoPaneLayout)
    assertEquals(64f, profile.keypadButtonHeightDp.value)
  }

  @Test
  fun `foldable medium screen enables navigation rail`() {
    val profile = DisplayAutoScaler.computeProfile(
      availableWidthDp = 680f,
      availableHeightDp = 840f,
      density = 2.625f,
      systemFontScale = 1.0f,
    )

    assertEquals(WindowWidthTier.MEDIUM, profile.widthTier)
    assertTrue(profile.useNavigationRail)
    assertFalse(profile.useTwoPaneLayout)
  }

  @Test
  fun `expanded tablet enables navigation rail and two pane layout`() {
    val profile = DisplayAutoScaler.computeProfile(
      availableWidthDp = 1140f,
      availableHeightDp = 800f,
      density = 2.0f,
      systemFontScale = 1.0f,
    )

    assertEquals(WindowWidthTier.EXPANDED, profile.widthTier)
    assertTrue(profile.useNavigationRail)
    assertTrue(profile.useTwoPaneLayout)
  }

  @Test
  fun `compact height landscape phone avoids two pane cramping and clamps font scaling`() {
    val normal = DisplayAutoScaler.computeProfile(
      availableWidthDp = 780f,
      availableHeightDp = 380f,
      density = 3.0f,
      systemFontScale = 1.0f,
    )
    val hugeFont = DisplayAutoScaler.computeProfile(
      availableWidthDp = 780f,
      availableHeightDp = 380f,
      density = 3.0f,
      systemFontScale = 1.7f,
    )

    assertTrue(normal.isCompactHeight)
    assertTrue(normal.useNavigationRail)
    assertFalse(normal.useTwoPaneLayout)
    assertEquals(48f, normal.keypadButtonHeightDp.value)
    assertTrue(hugeFont.titleFontSp.value <= normal.titleFontSp.value)
  }
}
