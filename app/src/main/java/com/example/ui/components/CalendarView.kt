package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.calendar.CalendarDayCell
import com.example.calendar.CalendarUtils
import com.example.display.NeceCityDisplayProfile
import com.example.model.CalendarEvent
import com.example.model.EventCategory
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitIndigo
import com.example.ui.theme.OrbitMint

@Composable
fun CalendarView(
  displayedYear: Int,
  displayedMonth: Int,
  selectedDateIso: String,
  allEvents: List<CalendarEvent>,
  profile: NeceCityDisplayProfile,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onJumpToday: () -> Unit,
  onSelectDate: (String) -> Unit,
  onSaveEvent: (
    eventId: String?,
    title: String,
    description: String,
    dateIso: String,
    startTimeMinutes: Long,
    endTimeMinutes: Long,
    category: String,
  ) -> Unit,
  onDeleteEvent: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
  var isEditorOpen by remember { mutableStateOf(false) }

  val monthCells = remember(displayedYear, displayedMonth) {
    CalendarUtils.buildMonthGrid(displayedYear, displayedMonth)
  }
  val eventsByDate = remember(allEvents) { allEvents.groupBy { it.dateIso } }
  val selectedDayEvents = remember(eventsByDate, selectedDateIso) {
    eventsByDate[selectedDateIso].orEmpty()
      .sortedBy { it.startTimeMinutes }
  }

  if (profile.useTwoPaneLayout) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      modifier = modifier.fillMaxSize(),
    ) {
      MonthGridCard(
        monthTitle = CalendarUtils.monthTitle(displayedYear, displayedMonth),
        cells = monthCells,
        selectedDateIso = selectedDateIso,
        eventsByDate = eventsByDate,
        onPrevMonth = onPrevMonth,
        onNextMonth = onNextMonth,
        onJumpToday = onJumpToday,
        onSelectDate = onSelectDate,
        modifier = Modifier
          .weight(0.52f)
          .verticalScroll(rememberScrollState()),
      )
      AgendaPane(
        selectedDateIso = selectedDateIso,
        dayEvents = selectedDayEvents,
        onAddEvent = {
          editingEvent = null
          isEditorOpen = true
        },
        onEditEvent = { evt ->
          editingEvent = evt
          isEditorOpen = true
        },
        onDeleteEvent = onDeleteEvent,
        modifier = Modifier
          .weight(0.48f)
          .fillMaxHeight(),
      )
    }
  } else {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = modifier.fillMaxSize(),
    ) {
      item {
        MonthGridCard(
          monthTitle = CalendarUtils.monthTitle(displayedYear, displayedMonth),
          cells = monthCells,
          selectedDateIso = selectedDateIso,
          eventsByDate = eventsByDate,
          onPrevMonth = onPrevMonth,
          onNextMonth = onNextMonth,
          onJumpToday = onJumpToday,
          onSelectDate = onSelectDate,
        )
      }
      item {
        AgendaHeaderRow(
          selectedDateIso = selectedDateIso,
          eventCount = selectedDayEvents.size,
          onAddEvent = {
            editingEvent = null
            isEditorOpen = true
          },
        )
      }
      if (selectedDayEvents.isEmpty()) {
        item {
          EmptyAgendaCard(
            onAddEvent = {
              editingEvent = null
              isEditorOpen = true
            },
          )
        }
      } else {
        items(selectedDayEvents, key = { it.id }) { evt ->
          CalendarEventCard(
            event = evt,
            onEdit = {
              editingEvent = evt
              isEditorOpen = true
            },
            onDelete = { onDeleteEvent(evt.id) },
          )
        }
      }
      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }

  if (isEditorOpen) {
    CalendarEventDialog(
      initialDateIso = selectedDateIso,
      existing = editingEvent,
      onDismiss = { isEditorOpen = false },
      onSave = { title, desc, startMin, endMin, category ->
        onSaveEvent(
          editingEvent?.id,
          title,
          desc,
          editingEvent?.dateIso ?: selectedDateIso,
          startMin,
          endMin,
          category,
        )
        isEditorOpen = false
      },
    )
  }
}

@Composable
private fun MonthGridCard(
  monthTitle: String,
  cells: List<CalendarDayCell>,
  selectedDateIso: String,
  eventsByDate: Map<String, List<CalendarEvent>>,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onJumpToday: () -> Unit,
  onSelectDate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val weekLabels = remember { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }
  val weeks = remember(cells) { cells.chunked(7) }

  Surface(
    color = Color(0x8A101624),
    shape = RoundedCornerShape(24.dp),
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
      .testTag("calendar_month_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = monthTitle,
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onJumpToday,
            modifier = Modifier
              .size(48.dp)
              .testTag("calendar_today_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.CalendarToday,
              contentDescription = "Jump to today",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(
            onClick = onPrevMonth,
            modifier = Modifier
              .size(48.dp)
              .testTag("calendar_prev_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = "Previous month",
              tint = Color.White,
            )
          }
          IconButton(
            onClick = onNextMonth,
            modifier = Modifier
              .size(48.dp)
              .testTag("calendar_next_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = "Next month",
              tint = Color.White,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(modifier = Modifier.fillMaxWidth()) {
        weekLabels.forEach { label ->
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.52f),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        weeks.forEach { week ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            week.forEach { cell ->
              val isSelected = cell.dateIso == selectedDateIso
              val dayEvents = eventsByDate[cell.dateIso].orEmpty()
              val bgColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                cell.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else -> Color.Transparent
              }
              val borderColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                cell.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                else -> Color.Transparent
              }
              val textColor = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                !cell.inCurrentMonth -> Color.White.copy(alpha = 0.30f)
                cell.isToday -> MaterialTheme.colorScheme.primary
                else -> Color.White
              }

              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .clip(DefaultAppleSquircleShape)
                  .background(bgColor)
                  .border(1.dp, borderColor, DefaultAppleSquircleShape)
                  .clickable { onSelectDate(cell.dateIso) }
                  .testTag("calendar_day_${cell.dateIso}"),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = cell.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (isSelected || cell.isToday) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = textColor,
                  )
                  if (dayEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                      dayEvents.take(3).forEach { evt ->
                        Box(
                          modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                              if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else categoryColor(evt.category)
                            ),
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
    }
  }
}

@Composable
private fun AgendaPane(
  selectedDateIso: String,
  dayEvents: List<CalendarEvent>,
  onAddEvent: () -> Unit,
  onEditEvent: (CalendarEvent) -> Unit,
  onDeleteEvent: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    AgendaHeaderRow(
      selectedDateIso = selectedDateIso,
      eventCount = dayEvents.size,
      onAddEvent = onAddEvent,
    )
    Spacer(modifier = Modifier.height(10.dp))
    if (dayEvents.isEmpty()) {
      EmptyAgendaCard(onAddEvent = onAddEvent)
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(dayEvents, key = { it.id }) { evt ->
          CalendarEventCard(
            event = evt,
            onEdit = { onEditEvent(evt) },
            onDelete = { onDeleteEvent(evt.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun AgendaHeaderRow(
  selectedDateIso: String,
  eventCount: Int,
  onAddEvent: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = CalendarUtils.formatSelectedDateHeading(selectedDateIso),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = if (eventCount == 0) "No scheduled events" else "$eventCount event(s)",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.62f),
      )
    }
    Button(
      onClick = onAddEvent,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag("calendar_add_event_button"),
    ) {
      Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Add Event")
    }
  }
}

@Composable
private fun EmptyAgendaCard(onAddEvent: () -> Unit) {
  Surface(
    color = Color(0x66101624),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.EventNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Clear day ahead",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Tap Add Event to schedule a meeting, reminder, or milestone.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.65f),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedButton(
        onClick = onAddEvent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text("Schedule Event")
      }
    }
  }
}

@Composable
private fun CalendarEventCard(
  event: CalendarEvent,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val accent = categoryColor(event.category)
  Surface(
    color = Color(0x88101624),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
      .clickable(onClick = onEdit)
      .testTag("calendar_event_card_${event.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      Box(
        modifier = Modifier
          .width(5.dp)
          .height(44.dp)
          .clip(CircleShape)
          .background(accent),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = event.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = CalendarUtils.formatTimeRange(event.startTimeMinutes, event.endTimeMinutes),
          style = MaterialTheme.typography.labelSmall,
          color = accent,
        )
        if (event.description.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = event.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.68f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.Edit, contentDescription = "Edit event", tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete event", tint = DangerCoral, modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
private fun CalendarEventDialog(
  initialDateIso: String,
  existing: CalendarEvent?,
  onDismiss: () -> Unit,
  onSave: (title: String, description: String, startMin: Long, endMin: Long, category: String) -> Unit,
) {
  var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
  var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
  var allDay by remember(existing) { mutableStateOf(existing != null && existing.startTimeMinutes < 0L) }
  var startHour by remember(existing) {
    mutableIntStateOf(if (existing != null && existing.startTimeMinutes >= 0L) (existing.startTimeMinutes / 60L).toInt() else 9)
  }
  var startMinute by remember(existing) {
    mutableIntStateOf(if (existing != null && existing.startTimeMinutes >= 0L) (existing.startTimeMinutes % 60L).toInt() else 0)
  }
  var category by remember(existing) { mutableStateOf(existing?.category ?: EventCategory.PERSONAL.id) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (existing == null) "New Event (${existing?.dateIso ?: initialDateIso})" else "Edit Event")
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Event title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("event_title_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Notes (optional)") },
          maxLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("event_description_input"),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { allDay = !allDay },
        ) {
          Checkbox(checked = allDay, onCheckedChange = { allDay = it })
          Text("All-day event")
        }
        if (!allDay) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Start: ${CalendarUtils.formatMinuteOfDay(startHour * 60 + startMinute)}",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(
              onClick = { startHour = (startHour + 23) % 24 },
              modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("-1h") }
            OutlinedButton(
              onClick = { startHour = (startHour + 1) % 24 },
              modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("+1h") }
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
          ) {
            listOf(0, 15, 30, 45).forEach { min ->
              val selected = startMinute == min
              Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { startMinute = min },
              ) {
                Text(
                  text = ":${min.toString().padStart(2, '0')}",
                  color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
            }
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
          EventCategory.entries.forEach { cat ->
            val selected = category == cat.id
            val accent = categoryColor(cat.id)
            Surface(
              color = if (selected) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f),
              shape = CircleShape,
              modifier = Modifier
                .clip(CircleShape)
                .border(1.dp, if (selected) accent else Color.White.copy(alpha = 0.14f), CircleShape)
                .clickable { category = cat.id },
            ) {
              Text(
                text = cat.label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val sMin = if (allDay) -1L else (startHour * 60 + startMinute).toLong()
          val eMin = if (allDay) -1L else ((startHour * 60 + startMinute + 60).coerceAtMost(1439)).toLong()
          onSave(title.trim(), description.trim(), sMin, eMin, category)
        },
        enabled = title.trim().isNotEmpty(),
        modifier = Modifier.testTag("event_save_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

private fun categoryColor(category: String): Color = when (category) {
  EventCategory.WORK.id -> OrbitIndigo
  EventCategory.HEALTH.id -> OrbitMint
  EventCategory.REMINDER.id -> DangerCoral
  else -> OrbitCyan
}
