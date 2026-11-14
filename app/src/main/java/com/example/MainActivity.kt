package com.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.NeceCityApp
import com.example.ui.theme.NeceCityTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)
    enableHighestRefreshRate()

    setContent {
      NeceCityTheme {
        NeceCityApp()
      }
    }
  }

  private fun enableHighestRefreshRate() {
    runCatching {
      val currentDisplay: Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          display
        } else {
          @Suppress("DEPRECATION")
          windowManager.defaultDisplay
        }

      if (currentDisplay != null) {
        val currentMode = currentDisplay.mode
        val bestMode = currentDisplay.supportedModes
          .filter {
            it.physicalWidth == currentMode.physicalWidth &&
              it.physicalHeight == currentMode.physicalHeight
          }
          .maxByOrNull { it.refreshRate }

        val lp = window.attributes
        if (bestMode != null) {
          lp.preferredDisplayModeId = bestMode.modeId
          lp.preferredRefreshRate = bestMode.refreshRate
        }
        window.attributes = lp
      }
    }
  }
}
