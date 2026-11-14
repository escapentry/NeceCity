package com.example.model

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class NeceCityTab {
  CALCULATOR,
  CALENDAR,
  NOTES,
  CLOCK,
}

enum class ClockSection {
  WORLD_CLOCK,
  ALARMS,
}

enum class NoteColorTag(val id: String, val label: String) {
  CYAN("cyan", "Cyan"),
  INDIGO("indigo", "Indigo"),
  MINT("mint", "Mint"),
  CORAL("coral", "Coral"),
  OBSIDIAN("obsidian", "Obsidian");

  companion object {
    fun fromId(id: String?): NoteColorTag =
      entries.firstOrNull { it.id == id } ?: CYAN
  }
}

enum class EventCategory(val id: String, val label: String) {
  PERSONAL("personal", "Personal"),
  WORK("work", "Work"),
  HEALTH("health", "Health"),
  REMINDER("reminder", "Reminder");

  companion object {
    fun fromId(id: String?): EventCategory =
      entries.firstOrNull { it.id == id } ?: PERSONAL
  }
}

@Immutable
@IgnoreExtraProperties
data class Note(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val content: String = "",
  val colorTag: String = NoteColorTag.CYAN.id,
  val pinned: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Immutable
@IgnoreExtraProperties
data class CalendarEvent(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val description: String = "",
  val dateIso: String = "",
  val startTimeMinutes: Long = 540L,
  val endTimeMinutes: Long = 600L,
  val category: String = EventCategory.PERSONAL.id,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Immutable
@IgnoreExtraProperties
data class AlarmEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val hour: Long = 7L,
  val minute: Long = 0L,
  val label: String = "",
  val enabled: Boolean = true,
  val repeatDays: List<Long> = emptyList(),
  val vibrate: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Immutable
@IgnoreExtraProperties
data class WorldClockEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val zoneId: String = "",
  val cityLabel: String = "",
  val sortOrder: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Immutable
@IgnoreExtraProperties
data class CalcHistoryEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val expression: String = "",
  val result: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
