package com.example.ui

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.calculator.CalculatorEngine
import com.example.calendar.CalendarUtils
import com.example.clock.WorldCityOption
import com.example.data.NeceCityDataSource
import com.example.data.WallpaperPreset
import com.example.data.WallpaperRepository
import com.example.data.WallpaperState
import com.example.model.AlarmEntry
import com.example.model.CalcHistoryEntry
import com.example.model.CalendarEvent
import com.example.model.ClockSection
import com.example.model.NeceCityTab
import com.example.model.Note
import com.example.model.WorldClockEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class NeceCityUiState(
  val isInitialLoading: Boolean = true,
  val isOfflineMode: Boolean = false,
  val activeTab: NeceCityTab = NeceCityTab.CALCULATOR,
  val clockSection: ClockSection = ClockSection.WORLD_CLOCK,
  val use24HourClock: Boolean = false,
  val calcExpression: String = "",
  val calcPreview: String? = null,
  val calcError: String? = null,
  val calcHistory: List<CalcHistoryEntry> = emptyList(),
  val displayedYear: Int = CalendarUtils.currentYearMonth().first,
  val displayedMonth: Int = CalendarUtils.currentYearMonth().second,
  val selectedDateIso: String = CalendarUtils.todayIso(),
  val events: List<CalendarEvent> = emptyList(),
  val notes: List<Note> = emptyList(),
  val notesSearchQuery: String = "",
  val notesColorFilter: String? = null,
  val worldClocks: List<WorldClockEntry> = emptyList(),
  val alarms: List<AlarmEntry> = emptyList(),
  val wallpaperState: WallpaperState = WallpaperState(),
  val isWallpaperMenuOpen: Boolean = false,
  val statusBanner: String? = null,
)

class NeceCityViewModel(
  private val repository: NeceCityDataSource,
  private val wallpaperRepository: WallpaperRepository,
  private val alarmScheduler: AlarmScheduler,
  private val currentUserId: String,
  private val isOfflineMode: Boolean = false,
) : ViewModel() {

  private val isInitialLoading = MutableStateFlow(true)
  private val activeTab = MutableStateFlow(NeceCityTab.CALCULATOR)
  private val clockSection = MutableStateFlow(ClockSection.WORLD_CLOCK)
  private val use24HourClock = MutableStateFlow(false)
  private val calcExpression = MutableStateFlow("")
  private val calcError = MutableStateFlow<String?>(null)
  private val displayedYearMonth = MutableStateFlow(CalendarUtils.currentYearMonth())
  private val selectedDateIso = MutableStateFlow(CalendarUtils.todayIso())
  private val notesSearchQuery = MutableStateFlow("")
  private val notesColorFilter = MutableStateFlow<String?>(null)
  private val wallpaperState = MutableStateFlow(WallpaperState())
  private val isWallpaperMenuOpen = MutableStateFlow(false)
  private val statusBanner = MutableStateFlow<String?>(null)

  private val notesFlow: Flow<List<Note>> = repository.observeNotes(currentUserId)
    .safeCatch("Failed to load notes")
  private val eventsFlow: Flow<List<CalendarEvent>> = repository.observeEvents(currentUserId)
    .safeCatch("Failed to load events")
  private val alarmsFlow: Flow<List<AlarmEntry>> = repository.observeAlarms(currentUserId)
    .onEach { list -> alarmScheduler.syncAlarms(list) }
    .safeCatch("Failed to load alarms")
  private val worldClocksFlow: Flow<List<WorldClockEntry>> = repository.observeWorldClocks(currentUserId)
    .safeCatch("Failed to load world clocks")
  private val calcHistoryFlow: Flow<List<CalcHistoryEntry>> = repository.observeCalcHistory(currentUserId)
    .safeCatch("Failed to load history")

  private val syncedDataFlow = combine(
    notesFlow,
    eventsFlow,
    alarmsFlow,
    worldClocksFlow,
    calcHistoryFlow,
  ) { notes, events, alarms, clocks, history ->
    SyncedDomainData(notes, events, alarms, clocks, history)
  }

  private val localUiFlow = combine(
    combine(isInitialLoading, activeTab, clockSection, use24HourClock) { loading, tab, sec, use24 ->
      Quad(loading, tab, sec, use24)
    },
    combine(calcExpression, calcError, displayedYearMonth, selectedDateIso) { expr, err, ym, selDate ->
      Quad(expr, err, ym, selDate)
    },
    combine(notesSearchQuery, notesColorFilter, wallpaperState, isWallpaperMenuOpen, statusBanner) { q, color, wp, wpOpen, banner ->
      Quint(q, color, wp, wpOpen, banner)
    },
  ) { a, b, c ->
    Triple(a, b, c)
  }

  val uiState: StateFlow<NeceCityUiState> = combine(syncedDataFlow, localUiFlow) { data, local ->
    val expr = local.second.first
    val preview = if (local.second.second == null) CalculatorEngine.tryPreview(expr) else null
    NeceCityUiState(
      isInitialLoading = local.first.first,
      isOfflineMode = isOfflineMode,
      activeTab = local.first.second,
      clockSection = local.first.third,
      use24HourClock = local.first.fourth,
      calcExpression = expr,
      calcPreview = preview,
      calcError = local.second.second,
      calcHistory = data.calcHistory,
      displayedYear = local.second.third.first,
      displayedMonth = local.second.third.second,
      selectedDateIso = local.second.fourth,
      events = data.events,
      notes = data.notes,
      notesSearchQuery = local.third.first,
      notesColorFilter = local.third.second,
      worldClocks = data.worldClocks,
      alarms = data.alarms,
      wallpaperState = local.third.third,
      isWallpaperMenuOpen = local.third.fourth,
      statusBanner = local.third.fifth,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = NeceCityUiState(),
  )

  init {
    viewModelScope.launch {
      wallpaperState.value = wallpaperRepository.loadWallpaperState()
      delay(420L)
      isInitialLoading.value = false
    }
  }

  fun selectTab(tab: NeceCityTab) {
    activeTab.value = tab
  }

  fun selectClockSection(section: ClockSection) {
    clockSection.value = section
  }

  fun toggle24HourClock() {
    use24HourClock.value = !use24HourClock.value
  }

  fun dismissStatusBanner() {
    statusBanner.value = null
  }

  // ---------------- Calculator ----------------

  fun appendCalcToken(token: String) {
    calcError.value = null
    val current = calcExpression.value
    val operators = setOf("+", "−", "×", "÷")
    if (token in operators) {
      if (current.isEmpty()) {
        if (token == "−") calcExpression.value = "−"
        return
      }
      if (current.last().toString() in operators) {
        calcExpression.value = current.dropLast(1) + token
        return
      }
    }
    if (token == ".") {
      val lastSegment = current.split('+', '−', '×', '÷', '(', ')').lastOrNull().orEmpty()
      if (lastSegment.contains('.')) return
      calcExpression.value = if (lastSegment.isEmpty()) "${current}0." else "$current."
      return
    }
    calcExpression.value = current + token
  }

  fun toggleCalcParenthesis() {
    calcError.value = null
    val current = calcExpression.value
    val openCount = current.count { it == '(' }
    val closeCount = current.count { it == ')' }
    val last = current.lastOrNull()
    val shouldClose = openCount > closeCount && last != null && (last.isDigit() || last == ')' || last == '%')
    calcExpression.value = if (shouldClose) "$current)" else "$current("
  }

  fun toggleCalcSign() {
    calcError.value = null
    calcExpression.value = CalculatorEngine.toggleLastNumberSign(calcExpression.value)
  }

  fun backspaceCalc() {
    calcError.value = null
    if (calcExpression.value.isNotEmpty()) {
      calcExpression.value = calcExpression.value.dropLast(1)
    }
  }

  fun clearCalcAll() {
    calcError.value = null
    calcExpression.value = ""
  }

  fun evaluateCalc() {
    val expr = calcExpression.value
    if (expr.isBlank()) return
    when (val res = CalculatorEngine.evaluate(expr)) {
      is CalculatorEngine.EvalResult.Success -> {
        calcError.value = null
        calcExpression.value = res.formatted
        if (expr != res.formatted) {
          viewModelScope.launch {
            runCatching { repository.saveCalcHistory(expr, res.formatted) }
          }
        }
      }
      is CalculatorEngine.EvalResult.Error -> {
        calcError.value = res.message
      }
    }
  }

  fun useCalcHistory(entry: CalcHistoryEntry) {
    calcError.value = null
    calcExpression.value = entry.result
  }

  fun deleteCalcHistory(entryId: String) {
    viewModelScope.launch {
      runCatching { repository.deleteCalcHistoryEntry(entryId) }
        .onFailure { statusBanner.value = it.localizedMessage ?: "Unable to delete history entry" }
    }
  }

  // ---------------- Calendar ----------------

  fun prevMonth() {
    val (y, m) = displayedYearMonth.value
    displayedYearMonth.value = CalendarUtils.shiftMonth(y, m, -1)
  }

  fun nextMonth() {
    val (y, m) = displayedYearMonth.value
    displayedYearMonth.value = CalendarUtils.shiftMonth(y, m, 1)
  }

  fun jumpToday() {
    displayedYearMonth.value = CalendarUtils.currentYearMonth()
    selectedDateIso.value = CalendarUtils.todayIso()
  }

  fun selectDate(dateIso: String) {
    selectedDateIso.value = dateIso
  }

  fun saveCalendarEvent(
    eventId: String?,
    title: String,
    description: String,
    dateIso: String,
    startTimeMinutes: Long,
    endTimeMinutes: Long,
    category: String,
  ) {
    viewModelScope.launch {
      runCatching {
        if (eventId == null) {
          repository.createEvent(title, description, dateIso, startTimeMinutes, endTimeMinutes, category)
        } else {
          repository.updateEvent(eventId, title, description, dateIso, startTimeMinutes, endTimeMinutes, category)
        }
      }.onFailure {
        statusBanner.value = it.localizedMessage ?: "Failed to save event"
      }
    }
  }

  fun deleteCalendarEvent(eventId: String) {
    viewModelScope.launch {
      runCatching { repository.deleteEvent(eventId) }
        .onFailure { statusBanner.value = it.localizedMessage ?: "Failed to delete event" }
    }
  }

  // ---------------- Notes ----------------

  fun updateNotesSearch(query: String) {
    notesSearchQuery.value = query
  }

  fun selectNotesColorFilter(colorTag: String?) {
    notesColorFilter.value = colorTag
  }

  fun saveNote(
    noteId: String?,
    title: String,
    content: String,
    colorTag: String,
    pinned: Boolean,
  ) {
    viewModelScope.launch {
      runCatching {
        if (noteId == null) {
          repository.createNote(title, content, colorTag, pinned)
        } else {
          repository.updateNote(noteId, title, content, colorTag, pinned)
        }
      }.onFailure {
        statusBanner.value = it.localizedMessage ?: "Failed to save note"
      }
    }
  }

  fun toggleNotePin(noteId: String, pinned: Boolean) {
    viewModelScope.launch {
      runCatching { repository.toggleNotePin(noteId, pinned) }
        .onFailure { statusBanner.value = it.localizedMessage ?: "Failed to update pin" }
    }
  }

  fun deleteNote(noteId: String) {
    viewModelScope.launch {
      runCatching { repository.deleteNote(noteId) }
        .onFailure { statusBanner.value = it.localizedMessage ?: "Failed to delete note" }
    }
  }

  // ---------------- World Clock & Alarms ----------------

  fun addWorldClock(option: WorldCityOption) {
    viewModelScope.launch {
      val nextOrder = (uiState.value.worldClocks.maxOfOrNull { it.sortOrder } ?: -1L) + 1L
      runCatching {
        repository.addWorldClock(option.zoneId, option.city, nextOrder)
      }.onFailure {
        statusBanner.value = it.localizedMessage ?: "Failed to add city"
      }
    }
  }

  fun deleteWorldClock(clockId: String) {
    viewModelScope.launch {
      runCatching { repository.deleteWorldClock(clockId) }
        .onFailure { statusBanner.value = it.localizedMessage ?: "Failed to remove city" }
    }
  }

  fun saveAlarm(
    alarmId: String?,
    hour: Long,
    minute: Long,
    label: String,
    enabled: Boolean,
    repeatDays: List<Long>,
    vibrate: Boolean,
  ) {
    viewModelScope.launch {
      runCatching {
        if (alarmId == null) {
          repository.createAlarm(hour, minute, label, enabled, repeatDays, vibrate)
        } else {
          repository.updateAlarm(alarmId, hour, minute, label, enabled, repeatDays, vibrate)
        }
      }.onFailure {
        statusBanner.value = it.localizedMessage ?: "Failed to save alarm"
      }
    }
  }

  fun toggleAlarm(alarmId: String, enabled: Boolean) {
    viewModelScope.launch {
      runCatching { repository.toggleAlarmEnabled(alarmId, enabled) }
        .onFailure { statusBanner.value = it.localizedMessage ?: "Failed to update alarm" }
    }
  }

  fun deleteAlarm(alarmId: String) {
    alarmScheduler.cancelAlarm(alarmId)
    viewModelScope.launch {
      runCatching { repository.deleteAlarm(alarmId) }
        .onFailure { statusBanner.value = it.localizedMessage ?: "Failed to delete alarm" }
    }
  }

  // ---------------- Wallpaper ----------------

  fun openWallpaperMenu() {
    isWallpaperMenuOpen.value = true
  }

  fun dismissWallpaperMenu() {
    isWallpaperMenuOpen.value = false
  }

  fun selectWallpaperPreset(preset: WallpaperPreset) {
    viewModelScope.launch {
      wallpaperRepository.selectPreset(preset)
      wallpaperState.value = wallpaperState.value.copy(activePreset = preset)
    }
  }

  fun saveCustomWallpaper(uri: Uri) {
    viewModelScope.launch {
      val bmp = wallpaperRepository.saveCustomPhotoFromUri(uri)
      if (bmp != null) {
        wallpaperState.value = WallpaperState(
          activePreset = WallpaperPreset.CUSTOM_PHOTO,
          customBitmap = bmp,
          customVersion = wallpaperState.value.customVersion + 1,
        )
        statusBanner.value = "Custom backdrop applied"
      } else {
        statusBanner.value = "Could not load selected photo"
      }
    }
  }

  private fun <T> Flow<List<T>>.safeCatch(fallbackMsg: String): Flow<List<T>> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      statusBanner.value = rootError.message ?: fallbackMsg
      emit(emptyList())
    }

  private data class SyncedDomainData(
    val notes: List<Note>,
    val events: List<CalendarEvent>,
    val alarms: List<AlarmEntry>,
    val worldClocks: List<WorldClockEntry>,
    val calcHistory: List<CalcHistoryEntry>,
  )

  private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
  private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
