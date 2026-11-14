package com.example.data.local

import com.example.data.NeceCityDataSource
import com.example.model.AlarmEntry
import com.example.model.CalcHistoryEntry
import com.example.model.CalendarEvent
import com.example.model.Note
import com.example.model.WorldClockEntry
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalNeceCityRepository(
  private val dao: NeceCityLocalDao,
) : NeceCityDataSource {

  companion object {
    const val OFFLINE_USER_ID = "local_offline_user"
  }

  private fun Long.toTimestamp(): Timestamp = Timestamp(Date(this))

  // ---------------- Notes ----------------

  override fun observeNotes(userId: String): Flow<List<Note>> =
    dao.observeNotes().map { list ->
      list.map { e ->
        Note(
          id = e.id,
          userId = OFFLINE_USER_ID,
          title = e.title,
          content = e.content,
          colorTag = e.colorTag,
          pinned = e.pinned,
          createdAt = e.createdAtMillis.toTimestamp(),
          updatedAt = e.updatedAtMillis.toTimestamp(),
        )
      }
    }

  override suspend fun createNote(
    title: String,
    content: String,
    colorTag: String,
    pinned: Boolean,
  ): String {
    val id = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    dao.upsertNote(
      LocalNoteEntity(
        id = id,
        title = title.take(160),
        content = content.take(20000),
        colorTag = colorTag,
        pinned = pinned,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    )
    return id
  }

  override suspend fun updateNote(
    noteId: String,
    title: String,
    content: String,
    colorTag: String,
    pinned: Boolean,
  ) {
    require(noteId.isNotBlank()) { "Note ID must not be blank for update" }
    dao.updateNote(
      id = noteId,
      title = title.take(160),
      content = content.take(20000),
      colorTag = colorTag,
      pinned = pinned,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  override suspend fun toggleNotePin(noteId: String, pinned: Boolean) {
    dao.updateNotePin(
      id = noteId,
      pinned = pinned,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  override suspend fun deleteNote(noteId: String) {
    dao.deleteNoteById(noteId)
  }

  // ---------------- Calendar Events ----------------

  override fun observeEvents(userId: String): Flow<List<CalendarEvent>> =
    dao.observeEvents().map { list ->
      list.map { e ->
        CalendarEvent(
          id = e.id,
          userId = OFFLINE_USER_ID,
          title = e.title,
          description = e.description,
          dateIso = e.dateIso,
          startTimeMinutes = e.startTimeMinutes,
          endTimeMinutes = e.endTimeMinutes,
          category = e.category,
          createdAt = e.createdAtMillis.toTimestamp(),
          updatedAt = e.updatedAtMillis.toTimestamp(),
        )
      }
    }

  override suspend fun createEvent(
    title: String,
    description: String,
    dateIso: String,
    startTimeMinutes: Long,
    endTimeMinutes: Long,
    category: String,
  ): String {
    val id = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    dao.upsertEvent(
      LocalEventEntity(
        id = id,
        title = title.trim().take(140),
        description = description.trim().take(1000),
        dateIso = dateIso,
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        category = category,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    )
    return id
  }

  override suspend fun updateEvent(
    eventId: String,
    title: String,
    description: String,
    dateIso: String,
    startTimeMinutes: Long,
    endTimeMinutes: Long,
    category: String,
  ) {
    require(eventId.isNotBlank()) { "Event ID must not be blank for update" }
    dao.updateEvent(
      id = eventId,
      title = title.trim().take(140),
      description = description.trim().take(1000),
      dateIso = dateIso,
      startTimeMinutes = startTimeMinutes,
      endTimeMinutes = endTimeMinutes,
      category = category,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  override suspend fun deleteEvent(eventId: String) {
    dao.deleteEventById(eventId)
  }

  // ---------------- Alarms ----------------

  override fun observeAlarms(userId: String): Flow<List<AlarmEntry>> =
    dao.observeAlarms().map { list ->
      list.map { e ->
        AlarmEntry(
          id = e.id,
          userId = OFFLINE_USER_ID,
          hour = e.hour,
          minute = e.minute,
          label = e.label,
          enabled = e.enabled,
          repeatDays = decodeRepeatDays(e.repeatDaysCsv),
          vibrate = e.vibrate,
          createdAt = e.createdAtMillis.toTimestamp(),
          updatedAt = e.updatedAtMillis.toTimestamp(),
        )
      }
    }

  override suspend fun createAlarm(
    hour: Long,
    minute: Long,
    label: String,
    enabled: Boolean,
    repeatDays: List<Long>,
    vibrate: Boolean,
  ): String {
    val id = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    dao.upsertAlarm(
      LocalAlarmEntity(
        id = id,
        hour = hour.coerceIn(0L, 23L),
        minute = minute.coerceIn(0L, 59L),
        label = label.trim().take(80),
        enabled = enabled,
        repeatDaysCsv = encodeRepeatDays(repeatDays),
        vibrate = vibrate,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    )
    return id
  }

  override suspend fun updateAlarm(
    alarmId: String,
    hour: Long,
    minute: Long,
    label: String,
    enabled: Boolean,
    repeatDays: List<Long>,
    vibrate: Boolean,
  ) {
    require(alarmId.isNotBlank()) { "Alarm ID must not be blank for update" }
    dao.updateAlarm(
      id = alarmId,
      hour = hour.coerceIn(0L, 23L),
      minute = minute.coerceIn(0L, 59L),
      label = label.trim().take(80),
      enabled = enabled,
      repeatDaysCsv = encodeRepeatDays(repeatDays),
      vibrate = vibrate,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  override suspend fun toggleAlarmEnabled(alarmId: String, enabled: Boolean) {
    dao.updateAlarmEnabled(
      id = alarmId,
      enabled = enabled,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  override suspend fun deleteAlarm(alarmId: String) {
    dao.deleteAlarmById(alarmId)
  }

  // ---------------- World Clocks ----------------

  override fun observeWorldClocks(userId: String): Flow<List<WorldClockEntry>> =
    dao.observeWorldClocks().map { list ->
      list.map { e ->
        WorldClockEntry(
          id = e.id,
          userId = OFFLINE_USER_ID,
          zoneId = e.zoneId,
          cityLabel = e.cityLabel,
          sortOrder = e.sortOrder,
          createdAt = e.createdAtMillis.toTimestamp(),
          updatedAt = e.updatedAtMillis.toTimestamp(),
        )
      }
    }

  override suspend fun addWorldClock(zoneId: String, cityLabel: String, sortOrder: Long): String {
    val id = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    dao.upsertWorldClock(
      LocalWorldClockEntity(
        id = id,
        zoneId = zoneId.take(64),
        cityLabel = cityLabel.take(80),
        sortOrder = sortOrder.coerceIn(0L, 10000L),
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    )
    return id
  }

  override suspend fun deleteWorldClock(clockId: String) {
    dao.deleteWorldClockById(clockId)
  }

  // ---------------- Calculator History ----------------

  override fun observeCalcHistory(userId: String): Flow<List<CalcHistoryEntry>> =
    dao.observeCalcHistory().map { list ->
      list.map { e ->
        CalcHistoryEntry(
          id = e.id,
          userId = OFFLINE_USER_ID,
          expression = e.expression,
          result = e.result,
          createdAt = e.createdAtMillis.toTimestamp(),
          updatedAt = e.updatedAtMillis.toTimestamp(),
        )
      }
    }

  override suspend fun saveCalcHistory(expression: String, result: String): String {
    val id = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    dao.upsertCalcHistory(
      LocalCalcHistoryEntity(
        id = id,
        expression = expression.take(160),
        result = result.take(64),
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    )
    return id
  }

  override suspend fun deleteCalcHistoryEntry(entryId: String) {
    dao.deleteCalcHistoryById(entryId)
  }

  private fun encodeRepeatDays(days: List<Long>): String =
    days.distinct().sorted().take(7).joinToString(",")

  private fun decodeRepeatDays(csv: String): List<Long> =
    if (csv.isBlank()) emptyList()
    else csv.split(",").mapNotNull { it.trim().toLongOrNull() }
}
