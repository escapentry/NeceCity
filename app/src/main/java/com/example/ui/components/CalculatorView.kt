package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.display.NeceCityDisplayProfile
import com.example.model.CalcHistoryEntry

private enum class CalcKeyKind {
  DIGIT,
  FUNCTION,
  OPERATOR,
  EQUALS,
}

private data class CalcKeySpec(
  val label: String,
  val kind: CalcKeyKind,
  val tag: String,
  val onTap: () -> Unit,
)

@Composable
fun CalculatorView(
  expression: String,
  livePreview: String?,
  errorText: String?,
  history: List<CalcHistoryEntry>,
  profile: NeceCityDisplayProfile,
  onAppendToken: (String) -> Unit,
  onToggleParenthesis: () -> Unit,
  onToggleSign: () -> Unit,
  onBackspace: () -> Unit,
  onClearAll: () -> Unit,
  onEvaluate: () -> Unit,
  onUseHistory: (CalcHistoryEntry) -> Unit,
  onDeleteHistory: (String) -> Unit,
  isOfflineMode: Boolean = false,
  modifier: Modifier = Modifier,
) {
  if (profile.useTwoPaneLayout) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      modifier = modifier.fillMaxSize(),
    ) {
      CalculatorPadPane(
        expression = expression,
        livePreview = livePreview,
        errorText = errorText,
        history = emptyList(),
        showInlineHistory = false,
        profile = profile,
        onAppendToken = onAppendToken,
        onToggleParenthesis = onToggleParenthesis,
        onToggleSign = onToggleSign,
        onBackspace = onBackspace,
        onClearAll = onClearAll,
        onEvaluate = onEvaluate,
        onUseHistory = onUseHistory,
        modifier = Modifier
          .weight(0.56f)
          .fillMaxHeight(),
      )
      CalculatorHistoryPane(
        history = history,
        isOfflineMode = isOfflineMode,
        onUseHistory = onUseHistory,
        onDeleteHistory = onDeleteHistory,
        modifier = Modifier
          .weight(0.44f)
          .fillMaxHeight(),
      )
    }
  } else {
    CalculatorPadPane(
      expression = expression,
      livePreview = livePreview,
      errorText = errorText,
      history = history,
      showInlineHistory = true,
      profile = profile,
      onAppendToken = onAppendToken,
      onToggleParenthesis = onToggleParenthesis,
      onToggleSign = onToggleSign,
      onBackspace = onBackspace,
      onClearAll = onClearAll,
      onEvaluate = onEvaluate,
      onUseHistory = onUseHistory,
      modifier = modifier.fillMaxSize(),
    )
  }
}

@Composable
private fun CalculatorPadPane(
  expression: String,
  livePreview: String?,
  errorText: String?,
  history: List<CalcHistoryEntry>,
  showInlineHistory: Boolean,
  profile: NeceCityDisplayProfile,
  onAppendToken: (String) -> Unit,
  onToggleParenthesis: () -> Unit,
  onToggleSign: () -> Unit,
  onBackspace: () -> Unit,
  onClearAll: () -> Unit,
  onEvaluate: () -> Unit,
  onUseHistory: (CalcHistoryEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier) {
    val containerHeight = maxHeight
    val compactSpace = containerHeight < 560.dp || profile.isCompactHeight
    val rowGap = if (compactSpace) 6.dp else 10.dp
    val cardGap = if (compactSpace) 8.dp else 12.dp
    val bottomPad = if (compactSpace) 4.dp else 8.dp
    val minDisplayHeight = if (compactSpace) 118.dp else 136.dp
    val showHistoryChips =
      showInlineHistory && history.isNotEmpty() && !compactSpace && containerHeight >= 600.dp
    val historyReserve = if (showHistoryChips) 42.dp else 0.dp
    val maxKeySpace =
      (containerHeight - minDisplayHeight - cardGap - bottomPad - historyReserve - rowGap * 4) / 5
    val keyHeight = minOf(profile.keypadButtonHeightDp, maxKeySpace).coerceAtLeast(48.dp)

    val rows = listOf(
      listOf(
        CalcKeySpec("AC", CalcKeyKind.FUNCTION, "calc_key_ac", onClearAll),
        CalcKeySpec("( )", CalcKeyKind.FUNCTION, "calc_key_paren", onToggleParenthesis),
        CalcKeySpec("%", CalcKeyKind.FUNCTION, "calc_key_percent") { onAppendToken("%") },
        CalcKeySpec("÷", CalcKeyKind.OPERATOR, "calc_key_div") { onAppendToken("÷") },
      ),
      listOf(
        CalcKeySpec("7", CalcKeyKind.DIGIT, "calc_key_7") { onAppendToken("7") },
        CalcKeySpec("8", CalcKeyKind.DIGIT, "calc_key_8") { onAppendToken("8") },
        CalcKeySpec("9", CalcKeyKind.DIGIT, "calc_key_9") { onAppendToken("9") },
        CalcKeySpec("×", CalcKeyKind.OPERATOR, "calc_key_mul") { onAppendToken("×") },
      ),
      listOf(
        CalcKeySpec("4", CalcKeyKind.DIGIT, "calc_key_4") { onAppendToken("4") },
        CalcKeySpec("5", CalcKeyKind.DIGIT, "calc_key_5") { onAppendToken("5") },
        CalcKeySpec("6", CalcKeyKind.DIGIT, "calc_key_6") { onAppendToken("6") },
        CalcKeySpec("−", CalcKeyKind.OPERATOR, "calc_key_sub") { onAppendToken("−") },
      ),
      listOf(
        CalcKeySpec("1", CalcKeyKind.DIGIT, "calc_key_1") { onAppendToken("1") },
        CalcKeySpec("2", CalcKeyKind.DIGIT, "calc_key_2") { onAppendToken("2") },
        CalcKeySpec("3", CalcKeyKind.DIGIT, "calc_key_3") { onAppendToken("3") },
        CalcKeySpec("+", CalcKeyKind.OPERATOR, "calc_key_add") { onAppendToken("+") },
      ),
      listOf(
        CalcKeySpec("±", CalcKeyKind.DIGIT, "calc_key_sign", onToggleSign),
        CalcKeySpec("0", CalcKeyKind.DIGIT, "calc_key_0") { onAppendToken("0") },
        CalcKeySpec(".", CalcKeyKind.DIGIT, "calc_key_dot") { onAppendToken(".") },
        CalcKeySpec("=", CalcKeyKind.EQUALS, "calc_key_eq", onEvaluate),
      ),
    )

    Column(
      verticalArrangement = Arrangement.Bottom,
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = bottomPad),
    ) {
      if (showHistoryChips) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        ) {
          history.take(6).forEach { entry ->
            Surface(
              color = Color(0x66101624),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                .clickable { onUseHistory(entry) }
                .testTag("calc_history_chip_${entry.id}"),
            ) {
              Text(
                text = "${entry.expression} = ${entry.result}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Display Card
      Surface(
        color = Color(0x88101624),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .heightIn(min = minDisplayHeight)
          .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
      ) {
        Column(
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.End,
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              horizontal = 20.dp,
              vertical = if (compactSpace) 10.dp else 16.dp,
            ),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "EXPR",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            )
            IconButton(
              onClick = onBackspace,
              modifier = Modifier
                .size(48.dp)
                .testTag("calc_backspace_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Backspace",
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(20.dp),
              )
            }
          }

          val displayExpr = expression.ifBlank { "0" }
          val exprFontSize = when {
            compactSpace && displayExpr.length > 10 -> 24.sp
            compactSpace -> 32.sp
            displayExpr.length > 14 -> 28.sp
            else -> 40.sp
          }
          val exprLineHeight = when {
            compactSpace && displayExpr.length > 10 -> 28.sp
            compactSpace -> 36.sp
            displayExpr.length > 14 -> 34.sp
            else -> 46.sp
          }
          Text(
            text = displayExpr,
            style = MaterialTheme.typography.displayLarge.copy(
              fontSize = exprFontSize,
              lineHeight = exprLineHeight,
            ),
            color = Color.White,
            maxLines = if (compactSpace) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("calc_display_expression"),
          )

          Spacer(modifier = Modifier.height(if (compactSpace) 2.dp else 6.dp))

          val secondaryText = errorText ?: livePreview?.let { "= $it" } ?: ""
          Text(
            text = secondaryText,
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = if (compactSpace) 18.sp else 20.sp,
            ),
            color = if (errorText != null) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.primary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("calc_display_preview"),
          )
        }
      }

      Spacer(modifier = Modifier.height(cardGap))

      // Squircle Keypad
      Column(
        verticalArrangement = Arrangement.spacedBy(rowGap),
        modifier = Modifier.fillMaxWidth(),
      ) {
      rows.forEach { row ->
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          row.forEach { key ->
            val bgColor = when (key.kind) {
              CalcKeyKind.DIGIT -> Color(0x94101624)
              CalcKeyKind.FUNCTION -> if (key.label == "AC") {
                MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
              } else {
                Color(0xA6172033)
              }
              CalcKeyKind.OPERATOR -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.26f)
              CalcKeyKind.EQUALS -> MaterialTheme.colorScheme.primary
            }
            val borderColor = when (key.kind) {
              CalcKeyKind.EQUALS -> MaterialTheme.colorScheme.primary
              CalcKeyKind.OPERATOR -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
              CalcKeyKind.FUNCTION -> if (key.label == "AC") {
                MaterialTheme.colorScheme.error.copy(alpha = 0.50f)
              } else {
                Color.White.copy(alpha = 0.14f)
              }
              CalcKeyKind.DIGIT -> Color.White.copy(alpha = 0.10f)
            }
            val textColor = when (key.kind) {
              CalcKeyKind.EQUALS -> MaterialTheme.colorScheme.onPrimary
              CalcKeyKind.OPERATOR -> Color.White
              CalcKeyKind.FUNCTION -> if (key.label == "AC") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
              CalcKeyKind.DIGIT -> Color.White
            }

            val shape = DefaultAppleSquircleShape
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .height(keyHeight)
                .clip(shape)
                .background(bgColor)
                .border(1.dp, borderColor, shape)
                .clickable(onClick = key.onTap)
                .testTag(key.tag),
            ) {
              Text(
                text = key.label,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontSize = if (compactSpace) 20.sp else profile.keypadDigitFontSp,
                  fontWeight = if (key.kind == CalcKeyKind.DIGIT) FontWeight.Medium else FontWeight.Bold,
                ),
                color = textColor,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
  }
}

@Composable
private fun CalculatorHistoryPane(
  history: List<CalcHistoryEntry>,
  isOfflineMode: Boolean,
  onUseHistory: (CalcHistoryEntry) -> Unit,
  onDeleteHistory: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0x88101624),
    shape = RoundedCornerShape(24.dp),
    modifier = modifier
      .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.History,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isOfflineMode) "Local History" else "Synced History",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      if (history.isEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize(),
        ) {
          Text(
            text = "Tap = to evaluate and save calculations here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(history, key = { it.id }) { entry ->
            Surface(
              color = Color.White.copy(alpha = 0.05f),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onUseHistory(entry) },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = entry.expression,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "= ${entry.result}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                IconButton(
                  onClick = { onDeleteHistory(entry.id) },
                  modifier = Modifier.size(48.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete entry",
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
