package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

val DefaultAppleSquircleShape = AppleSquircleShape(exponent = 4.5f)

@Composable
fun NeceCityLoadingSplash(
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "nececity_loading_pulse")
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "nececity_icon_pulse",
  )
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.55f,
    targetValue = 0.92f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "nececity_ring_alpha",
  )
  val sweepRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "nececity_rainbow_rotation",
  )

  val rainbowAuraColors = remember {
    listOf(
      Color(0xFF22D3EE),
      Color(0xFF3B82F6),
      Color(0xFF818CF8),
      Color(0xFFEC4899),
      Color(0xFFF97316),
      Color(0xFF34D399),
      Color(0xFF22D3EE),
    )
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF080D11))
      .testTag("nececity_loading_overlay"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.safeDrawingPadding(),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Canvas(
          modifier = Modifier
            .size(230.dp)
            .graphicsLayer { rotationZ = sweepRotation },
        ) {
          val baseRadius = (size.minDimension * 0.25f) * pulse
          val rainbowBrush = Brush.sweepGradient(colors = rainbowAuraColors, center = center)

          for (i in 1..6) {
            val stepFraction = i / 6f
            val ringAlpha = ((1f - stepFraction) * 0.24f * glowAlpha).coerceIn(0f, 1f)
            drawCircle(
              brush = rainbowBrush,
              radius = baseRadius * (1.02f + stepFraction * 0.78f),
              center = center,
              alpha = ringAlpha,
              style = Stroke(width = (18.dp.toPx() * (0.65f + stepFraction * 0.6f))),
            )
          }

          drawCircle(
            brush = rainbowBrush,
            radius = baseRadius * 1.06f,
            center = center,
            alpha = glowAlpha * 0.85f,
            style = Stroke(width = 3.dp.toPx()),
          )
        }

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
              scaleX = pulse
              scaleY = pulse
            }
            .shadow(
              elevation = 16.dp,
              shape = DefaultAppleSquircleShape,
              clip = false,
              ambientColor = Color(0xFF818CF8).copy(alpha = 0.65f),
              spotColor = Color(0xFF22D3EE).copy(alpha = 0.85f),
            )
            .clip(DefaultAppleSquircleShape),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_nececity_icon_fg),
            contentDescription = stringResource(id = R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
          shadow = Shadow(
            color = Color.Black.copy(alpha = 0.45f),
            offset = Offset(0f, 2f),
            blurRadius = 8f,
          ),
        ),
        color = Color.White.copy(alpha = 0.94f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
  }
}
