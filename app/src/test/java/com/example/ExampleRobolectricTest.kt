package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.calculator.CalculatorEngine
import com.example.calendar.CalendarUtils
import com.example.clock.WorldClockCatalog
import com.example.data.local.LocalNeceCityRepository
import com.example.data.local.NeceCityLocalDatabase
import com.example.display.DisplayAutoScaler
import com.example.model.AlarmEntry
import com.example.ui.components.AuthScreen
import com.example.ui.components.CalculatorView
import com.example.ui.theme.NeceCityTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `app name matches NeceCity`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("NeceCity", context.getString(R.string.app_name))
  }

  @Test
  fun `calculator engine evaluates precedence parentheses percent and divide by zero`() {
    val r1 = CalculatorEngine.evaluate("12 + 3 × (4 + 6)")
    assertTrue(r1 is CalculatorEngine.EvalResult.Success)
    assertEquals("42", (r1 as CalculatorEngine.EvalResult.Success).formatted)

    val r2 = CalculatorEngine.evaluate("200 × 15%")
    assertTrue(r2 is CalculatorEngine.EvalResult.Success)
    assertEquals("30", (r2 as CalculatorEngine.EvalResult.Success).formatted)

    val divZero = CalculatorEngine.evaluate("9 ÷ 0")
    assertTrue(divZero is CalculatorEngine.EvalResult.Error)
  }

  @Test
  fun `calendar utils builds valid month grid and formats times`() {
    val cells = CalendarUtils.buildMonthGrid(2026, 10) // November 2026
    assertTrue(cells.size == 35 || cells.size == 42)
    assertEquals(30, cells.count { it.inCurrentMonth })
    assertEquals("All day", CalendarUtils.formatTimeRange(-1L, -1L))
    assertEquals("9:00 AM – 10:30 AM", CalendarUtils.formatTimeRange(540L, 630L))
  }

  @Test
  fun `world clock catalog and alarm scheduler compute valid outputs`() {
    val live = WorldClockCatalog.formatZoneTime("Asia/Tokyo", 1_763_120_000_000L, true)
    assertTrue(live.formattedTime.contains(":"))
    assertTrue(live.formattedDate.isNotEmpty())

    val countdown = AlarmScheduler.formatCountdown(
      alarms = listOf(
        AlarmEntry(id = "a1", hour = 7L, minute = 30L, enabled = true, repeatDays = listOf(1L, 3L, 5L))
      ),
      nowMillis = 1_763_120_000_000L,
    )
    assertNotNull(countdown)
    assertTrue(countdown!!.startsWith("Next alarm") || countdown.startsWith("Alarm ringing"))
  }

  @Test
  fun `calculator view renders keypad keys and triggers callbacks`() {
    val profile = DisplayAutoScaler.computeProfile(390f, 844f, 3f, 1f)
    var tapped = ""
    var evaluated = false

    composeTestRule.setContent {
      NeceCityTheme {
        CalculatorView(
          expression = "7×8",
          livePreview = "56",
          errorText = null,
          history = emptyList(),
          profile = profile,
          onAppendToken = { tapped += it },
          onToggleParenthesis = {},
          onToggleSign = {},
          onBackspace = {},
          onClearAll = {},
          onEvaluate = { evaluated = true },
          onUseHistory = {},
          onDeleteHistory = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("calc_display_expression").assertIsDisplayed()
    composeTestRule.onNodeWithTag("calc_display_preview").assertIsDisplayed()
    composeTestRule.onNodeWithTag("calc_key_9").performClick()
    composeTestRule.onNodeWithTag("calc_key_eq").performClick()

    assertEquals("9", tapped)
    assertTrue(evaluated)
  }

  @Test
  fun `local room repository stores and manages notes events alarms clocks and history offline`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, NeceCityLocalDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = LocalNeceCityRepository(db.localDao())
      val uid = LocalNeceCityRepository.OFFLINE_USER_ID

      val noteId = repo.createNote("Offline Note", "Saved locally without Google", "mint", true)
      var notes = repo.observeNotes(uid).first()
      assertEquals(1, notes.size)
      assertEquals("Offline Note", notes.first().title)
      assertTrue(notes.first().pinned)

      repo.updateNote(noteId, "Updated Offline", "Body", "indigo", false)
      repo.toggleNotePin(noteId, true)
      notes = repo.observeNotes(uid).first()
      assertEquals("Updated Offline", notes.first().title)
      assertEquals("indigo", notes.first().colorTag)
      assertTrue(notes.first().pinned)

      repo.deleteNote(noteId)
      assertTrue(repo.observeNotes(uid).first().isEmpty())

      val eventId = repo.createEvent("Local Event", "Desc", "2026-11-14", 540L, 600L, "work")
      assertEquals(1, repo.observeEvents(uid).first().size)
      repo.deleteEvent(eventId)

      val alarmId = repo.createAlarm(6L, 45L, "Wake up", true, listOf(1L, 3L), true)
      assertEquals(1, repo.observeAlarms(uid).first().size)
      repo.toggleAlarmEnabled(alarmId, false)
      assertFalse(repo.observeAlarms(uid).first().first().enabled)
      repo.deleteAlarm(alarmId)

      val clockId = repo.addWorldClock("Asia/Tokyo", "Tokyo", 0L)
      assertEquals(1, repo.observeWorldClocks(uid).first().size)
      repo.deleteWorldClock(clockId)

      val calcId = repo.saveCalcHistory("6 × 7", "42")
      assertEquals("42", repo.observeCalcHistory(uid).first().first().result)
      repo.deleteCalcHistoryEntry(calcId)
      assertTrue(repo.observeCalcHistory(uid).first().isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun `auth screen shows offline option button and triggers callback`() {
    var offlineClicked = false
    composeTestRule.setContent {
      NeceCityTheme {
        AuthScreen(
          onAuthSuccess = {},
          onContinueOffline = { offlineClicked = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("use_offline_button").assertIsDisplayed().performClick()
    assertTrue(offlineClicked)
  }
}
