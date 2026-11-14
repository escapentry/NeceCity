package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NeceCityRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun notes_crudAndRealtimeObservation_succeedsForOwner() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = NeceCityRepository(firestore, auth)

    val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createNote(
        title = "Architecture",
        content = "Obsidian squircle layout",
        colorTag = "cyan",
        pinned = false,
      )
    }
    assertTrue(noteId.isNotBlank())

    val notes = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeNotes(aliceUid).first { list -> list.any { it.id == noteId } }
    }
    val created = notes.first { it.id == noteId }
    assertEquals("Architecture", created.title)
    assertNotNull(created.createdAt)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.toggleNotePin(noteId, true)
      repo.updateNote(noteId, "Architecture v2", "Updated body", "indigo", true)
    }
    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getNoteById(aliceUid, noteId) }
    assertEquals("Architecture v2", fetched?.title)
    assertTrue(fetched?.pinned == true)

    withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteNote(noteId) }
  }

  @Test
  fun calendarEvents_crudAndRealtimeObservation_succeedsForOwner() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = NeceCityRepository(firestore, auth)

    val eventId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createEvent(
        title = "Release Review",
        description = "Check emulator tests",
        dateIso = "2026-11-15",
        startTimeMinutes = 540L,
        endTimeMinutes = 600L,
        category = "work",
      )
    }

    val events = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeEvents(aliceUid).first { list -> list.any { it.id == eventId } }
    }
    assertEquals("2026-11-15", events.first { it.id == eventId }.dateIso)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateEvent(eventId, "Release Final", "All green", "2026-11-15", 570L, 630L, "personal")
      repo.deleteEvent(eventId)
    }
  }

  @Test
  fun alarmsAndWorldClocksAndCalcHistory_crudSucceedsForOwner() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = NeceCityRepository(firestore, auth)

    val alarmId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createAlarm(
        hour = 7L,
        minute = 15L,
        label = "Standup",
        enabled = true,
        repeatDays = listOf(1L, 3L, 5L),
        vibrate = true,
      )
    }
    val alarms = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAlarms(aliceUid).first { list -> list.any { it.id == alarmId } }
    }
    assertEquals(7L, alarms.first { it.id == alarmId }.hour)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.toggleAlarmEnabled(alarmId, false)
      repo.deleteAlarm(alarmId)
    }

    val clockId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addWorldClock("Asia/Tokyo", "Tokyo", 0L)
    }
    val clocks = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeWorldClocks(aliceUid).first { list -> list.any { it.id == clockId } }
    }
    assertEquals("Tokyo", clocks.first { it.id == clockId }.cityLabel)
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteWorldClock(clockId) }

    val calcId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveCalcHistory("8 * 9", "72")
    }
    val history = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeCalcHistory(aliceUid).first { list -> list.any { it.id == calcId } }
    }
    assertEquals("72", history.first { it.id == calcId }.result)
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteCalcHistoryEntry(calcId) }
  }

  @Test
  fun notes_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = NeceCityRepository(firestore, auth)
    val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createNote("Alice Secret", "Do not leak", "mint", false)
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = NeceCityRepository(firestore, auth)

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getNoteById(aliceUid, noteId) }
      fail("Expected PERMISSION_DENIED for cross-user read")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    } finally {
      signInTestUser(ALICE_EMAIL)
      withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.deleteNote(noteId) }
    }
  }

  @Test
  fun observeNotes_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = NeceCityRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeNotes("some_user_id").first()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated observeNotes")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_nececity@test.com"
    const val BOB_EMAIL = "bob_nececity@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
