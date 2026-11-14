package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AlarmEntry
import com.example.model.CalcHistoryEntry
import com.example.model.CalendarEvent
import com.example.model.Note
import com.example.model.WorldClockEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NeceCityRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) : NeceCityDataSource {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before writing to Firestore.")

  private fun notesCol(uid: String) =
    db.collection("users").document(uid).collection("notes")

  private fun eventsCol(uid: String) =
    db.collection("users").document(uid).collection("events")

  private fun alarmsCol(uid: String) =
    db.collection("users").document(uid).collection("alarms")

  private fun worldClocksCol(uid: String) =
    db.collection("users").document(uid).collection("worldClocks")

  private fun calcHistoryCol(uid: String) =
    db.collection("users").document(uid).collection("calcHistory")

  // ---------------- Notes ----------------

  override fun observeNotes(userId: String): Flow<List<Note>> {
    val path = "users/$userId/notes"
    return notesCol(userId)
      .orderBy("updatedAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap ->
        snap.toObjects(Note::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        logFlowError(error, OperationType.LIST, path, userId)
        throw error
      }
  }

  suspend fun getNoteById(userId: String, noteId: String): Note? {
    val docRef = notesCol(userId).document(noteId)
    return try {
      docRef.get().await()
        .toObject(Note::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  override suspend fun createNote(
    title: String,
    content: String,
    colorTag: String,
    pinned: Boolean,
  ): String {
    val uid = requireUserId()
    val docRef = notesCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "title" to title.take(160),
      "content" to content.take(20000),
      "colorTag" to colorTag,
      "pinned" to pinned,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  override suspend fun updateNote(
    noteId: String,
    title: String,
    content: String,
    colorTag: String,
    pinned: Boolean,
  ) {
    val uid = requireUserId()
    require(noteId.isNotBlank()) { "Note ID must not be blank for update" }
    val docRef = notesCol(uid).document(noteId)
    val updates = mapOf(
      "title" to title.take(160),
      "content" to content.take(20000),
      "colorTag" to colorTag,
      "pinned" to pinned,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  override suspend fun toggleNotePin(noteId: String, pinned: Boolean) {
    val uid = requireUserId()
    val docRef = notesCol(uid).document(noteId)
    try {
      docRef.update(
        mapOf(
          "pinned" to pinned,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  override suspend fun deleteNote(noteId: String) {
    val uid = requireUserId()
    val docRef = notesCol(uid).document(noteId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // ---------------- Calendar Events ----------------

  override fun observeEvents(userId: String): Flow<List<CalendarEvent>> {
    val path = "users/$userId/events"
    return eventsCol(userId)
      .orderBy("dateIso", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap ->
        snap.toObjects(CalendarEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        logFlowError(error, OperationType.LIST, path, userId)
        throw error
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
    val uid = requireUserId()
    val docRef = eventsCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "title" to title.trim().take(140),
      "description" to description.trim().take(1000),
      "dateIso" to dateIso,
      "startTimeMinutes" to startTimeMinutes,
      "endTimeMinutes" to endTimeMinutes,
      "category" to category,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
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
    val uid = requireUserId()
    require(eventId.isNotBlank()) { "Event ID must not be blank for update" }
    val docRef = eventsCol(uid).document(eventId)
    val updates = mapOf(
      "title" to title.trim().take(140),
      "description" to description.trim().take(1000),
      "dateIso" to dateIso,
      "startTimeMinutes" to startTimeMinutes,
      "endTimeMinutes" to endTimeMinutes,
      "category" to category,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  override suspend fun deleteEvent(eventId: String) {
    val uid = requireUserId()
    val docRef = eventsCol(uid).document(eventId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // ---------------- Alarms ----------------

  override fun observeAlarms(userId: String): Flow<List<AlarmEntry>> {
    val path = "users/$userId/alarms"
    return alarmsCol(userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(AlarmEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<AlarmEntry> { it.hour }.thenBy { it.minute })
      }
      .catch { error ->
        logFlowError(error, OperationType.LIST, path, userId)
        throw error
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
    val uid = requireUserId()
    val docRef = alarmsCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "hour" to hour.coerceIn(0L, 23L),
      "minute" to minute.coerceIn(0L, 59L),
      "label" to label.trim().take(80),
      "enabled" to enabled,
      "repeatDays" to repeatDays.distinct().sorted().take(7),
      "vibrate" to vibrate,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
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
    val uid = requireUserId()
    require(alarmId.isNotBlank()) { "Alarm ID must not be blank for update" }
    val docRef = alarmsCol(uid).document(alarmId)
    val updates = mapOf(
      "hour" to hour.coerceIn(0L, 23L),
      "minute" to minute.coerceIn(0L, 59L),
      "label" to label.trim().take(80),
      "enabled" to enabled,
      "repeatDays" to repeatDays.distinct().sorted().take(7),
      "vibrate" to vibrate,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  override suspend fun toggleAlarmEnabled(alarmId: String, enabled: Boolean) {
    val uid = requireUserId()
    val docRef = alarmsCol(uid).document(alarmId)
    try {
      docRef.update(
        mapOf(
          "enabled" to enabled,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  override suspend fun deleteAlarm(alarmId: String) {
    val uid = requireUserId()
    val docRef = alarmsCol(uid).document(alarmId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // ---------------- World Clocks ----------------

  override fun observeWorldClocks(userId: String): Flow<List<WorldClockEntry>> {
    val path = "users/$userId/worldClocks"
    return worldClocksCol(userId)
      .orderBy("sortOrder", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap ->
        snap.toObjects(WorldClockEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        logFlowError(error, OperationType.LIST, path, userId)
        throw error
      }
  }

  override suspend fun addWorldClock(zoneId: String, cityLabel: String, sortOrder: Long): String {
    val uid = requireUserId()
    val docRef = worldClocksCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "zoneId" to zoneId.take(64),
      "cityLabel" to cityLabel.take(80),
      "sortOrder" to sortOrder.coerceIn(0L, 10000L),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  override suspend fun deleteWorldClock(clockId: String) {
    val uid = requireUserId()
    val docRef = worldClocksCol(uid).document(clockId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // ---------------- Calculator History ----------------

  override fun observeCalcHistory(userId: String): Flow<List<CalcHistoryEntry>> {
    val path = "users/$userId/calcHistory"
    return calcHistoryCol(userId)
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(20)
      .snapshots()
      .map { snap ->
        snap.toObjects(CalcHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        logFlowError(error, OperationType.LIST, path, userId)
        throw error
      }
  }

  override suspend fun saveCalcHistory(expression: String, result: String): String {
    val uid = requireUserId()
    val docRef = calcHistoryCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "expression" to expression.take(160),
      "result" to result.take(64),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  override suspend fun deleteCalcHistoryEntry(entryId: String) {
    val uid = requireUserId()
    val docRef = calcHistoryCol(uid).document(entryId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  private fun logFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception) ?: return
    if (auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }
}
