package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_notes")
data class LocalNoteEntity(
  @PrimaryKey val id: String,
  val title: String,
  val content: String,
  val colorTag: String,
  val pinned: Boolean,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(tableName = "local_events")
data class LocalEventEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val dateIso: String,
  val startTimeMinutes: Long,
  val endTimeMinutes: Long,
  val category: String,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(tableName = "local_alarms")
data class LocalAlarmEntity(
  @PrimaryKey val id: String,
  val hour: Long,
  val minute: Long,
  val label: String,
  val enabled: Boolean,
  val repeatDaysCsv: String,
  val vibrate: Boolean,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(tableName = "local_world_clocks")
data class LocalWorldClockEntity(
  @PrimaryKey val id: String,
  val zoneId: String,
  val cityLabel: String,
  val sortOrder: Long,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(tableName = "local_calc_history")
data class LocalCalcHistoryEntity(
  @PrimaryKey val id: String,
  val expression: String,
  val result: String,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Dao
interface NeceCityLocalDao {
  // Notes
  @Query("SELECT * FROM local_notes ORDER BY updatedAtMillis DESC")
  fun observeNotes(): Flow<List<LocalNoteEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNote(note: LocalNoteEntity)

  @Query(
    "UPDATE local_notes SET title = :title, content = :content, colorTag = :colorTag, " +
      "pinned = :pinned, updatedAtMillis = :updatedAtMillis WHERE id = :id"
  )
  suspend fun updateNote(
    id: String,
    title: String,
    content: String,
    colorTag: String,
    pinned: Boolean,
    updatedAtMillis: Long,
  )

  @Query("UPDATE local_notes SET pinned = :pinned, updatedAtMillis = :updatedAtMillis WHERE id = :id")
  suspend fun updateNotePin(id: String, pinned: Boolean, updatedAtMillis: Long)

  @Query("DELETE FROM local_notes WHERE id = :id")
  suspend fun deleteNoteById(id: String)

  // Events
  @Query("SELECT * FROM local_events ORDER BY dateIso ASC, startTimeMinutes ASC")
  fun observeEvents(): Flow<List<LocalEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertEvent(event: LocalEventEntity)

  @Query(
    "UPDATE local_events SET title = :title, description = :description, dateIso = :dateIso, " +
      "startTimeMinutes = :startTimeMinutes, endTimeMinutes = :endTimeMinutes, " +
      "category = :category, updatedAtMillis = :updatedAtMillis WHERE id = :id"
  )
  suspend fun updateEvent(
    id: String,
    title: String,
    description: String,
    dateIso: String,
    startTimeMinutes: Long,
    endTimeMinutes: Long,
    category: String,
    updatedAtMillis: Long,
  )

  @Query("DELETE FROM local_events WHERE id = :id")
  suspend fun deleteEventById(id: String)

  // Alarms
  @Query("SELECT * FROM local_alarms ORDER BY hour ASC, minute ASC")
  fun observeAlarms(): Flow<List<LocalAlarmEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAlarm(alarm: LocalAlarmEntity)

  @Query(
    "UPDATE local_alarms SET hour = :hour, minute = :minute, label = :label, " +
      "enabled = :enabled, repeatDaysCsv = :repeatDaysCsv, vibrate = :vibrate, " +
      "updatedAtMillis = :updatedAtMillis WHERE id = :id"
  )
  suspend fun updateAlarm(
    id: String,
    hour: Long,
    minute: Long,
    label: String,
    enabled: Boolean,
    repeatDaysCsv: String,
    vibrate: Boolean,
    updatedAtMillis: Long,
  )

  @Query("UPDATE local_alarms SET enabled = :enabled, updatedAtMillis = :updatedAtMillis WHERE id = :id")
  suspend fun updateAlarmEnabled(id: String, enabled: Boolean, updatedAtMillis: Long)

  @Query("DELETE FROM local_alarms WHERE id = :id")
  suspend fun deleteAlarmById(id: String)

  // World Clocks
  @Query("SELECT * FROM local_world_clocks ORDER BY sortOrder ASC")
  fun observeWorldClocks(): Flow<List<LocalWorldClockEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertWorldClock(clock: LocalWorldClockEntity)

  @Query("DELETE FROM local_world_clocks WHERE id = :id")
  suspend fun deleteWorldClockById(id: String)

  // Calculator History
  @Query("SELECT * FROM local_calc_history ORDER BY createdAtMillis DESC LIMIT 20")
  fun observeCalcHistory(): Flow<List<LocalCalcHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCalcHistory(entry: LocalCalcHistoryEntity)

  @Query("DELETE FROM local_calc_history WHERE id = :id")
  suspend fun deleteCalcHistoryById(id: String)
}

@Database(
  entities = [
    LocalNoteEntity::class,
    LocalEventEntity::class,
    LocalAlarmEntity::class,
    LocalWorldClockEntity::class,
    LocalCalcHistoryEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class NeceCityLocalDatabase : RoomDatabase() {
  abstract fun localDao(): NeceCityLocalDao

  companion object {
    @Volatile
    private var INSTANCE: NeceCityLocalDatabase? = null

    fun getInstance(context: Context): NeceCityLocalDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          NeceCityLocalDatabase::class.java,
          "nececity_local.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
