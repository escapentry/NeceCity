package com.example.data

import com.example.model.AlarmEntry
import com.example.model.CalcHistoryEntry
import com.example.model.CalendarEvent
import com.example.model.Note
import com.example.model.WorldClockEntry
import kotlinx.coroutines.flow.Flow

interface NeceCityDataSource {
  // Notes
  fun observeNotes(userId: String): Flow<List<Note>>
  suspend fun createNote(title: String, content: String, colorTag: String, pinned: Boolean): String
  suspend fun updateNote(noteId: String, title: String, content: String, colorTag: String, pinned: Boolean)
  suspend fun toggleNotePin(noteId: String, pinned: Boolean)
  suspend fun deleteNote(noteId: String)

  // Calendar Events
  fun observeEvents(userId: String): Flow<List<CalendarEvent>>
  suspend fun createEvent(
    title: String,
    description: String,
    dateIso: String,
    startTimeMinutes: Long,
    endTimeMinutes: Long,
    category: String,
  ): String
  suspend fun updateEvent(
    eventId: String,
    title: String,
    description: String,
    dateIso: String,
    startTimeMinutes: Long,
    endTimeMinutes: Long,
    category: String,
  )
  suspend fun deleteEvent(eventId: String)

  // Alarms
  fun observeAlarms(userId: String): Flow<List<AlarmEntry>>
  suspend fun createAlarm(
    hour: Long,
    minute: Long,
    label: String,
    enabled: Boolean,
    repeatDays: List<Long>,
    vibrate: Boolean,
  ): String
  suspend fun updateAlarm(
    alarmId: String,
    hour: Long,
    minute: Long,
    label: String,
    enabled: Boolean,
    repeatDays: List<Long>,
    vibrate: Boolean,
  )
  suspend fun toggleAlarmEnabled(alarmId: String, enabled: Boolean)
  suspend fun deleteAlarm(alarmId: String)

  // World Clocks
  fun observeWorldClocks(userId: String): Flow<List<WorldClockEntry>>
  suspend fun addWorldClock(zoneId: String, cityLabel: String, sortOrder: Long): String
  suspend fun deleteWorldClock(clockId: String)

  // Calculator History
  fun observeCalcHistory(userId: String): Flow<List<CalcHistoryEntry>>
  suspend fun saveCalcHistory(expression: String, result: String): String
  suspend fun deleteCalcHistoryEntry(entryId: String)
}
