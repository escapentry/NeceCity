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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmScheduler
import com.example.clock.WorldCityOption
import com.example.clock.WorldClockCatalog
import com.example.display.NeceCityDisplayProfile
import com.example.model.AlarmEntry
import com.example.model.ClockSection
import com.example.model.WorldClockEntry
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitIndigo
import com.example.ui.theme.OrbitMint
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun ClockView(
  activeSection: ClockSection,
  use24Hour: Boolean,
  worldClocks: List<WorldClockEntry>,
  alarms: List<AlarmEntry>,
  profile: NeceCityDisplayProfile,
  onSelectSection: (ClockSection) -> Unit,
  onToggle24Hour: () -> Unit,
  onAddWorldClock: (WorldCityOption) -> Unit,
  onDeleteWorldClock: (String) -> Unit,
  onSaveAlarm: (
    alarmId: String?,
    hour: Long,
    minute: Long,
    label: String,
    enabled: Boolean,
    repeatDays: List<Long>,
    vibrate: Boolean,
  ) -> Unit,
  onToggleAlarm: (alarmId: String, enabled: Boolean) -> Unit,
  onDeleteAlarm: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      delay(1000L)
    }
  }

  var isCityPickerOpen by remember { mutableStateOf(false) }
  var isAlarmDialogOpen by remember { mutableStateOf(false) }
  var editingAlarm by remember { mutableStateOf<AlarmEntry?>(null) }

  if (profile.useTwoPaneLayout) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      modifier = modifier.fillMaxSize(),
    ) {
      WorldClockPane(
        nowMillis = nowMillis,
        use24Hour = use24Hour,
        worldClocks = worldClocks,
        onToggle24Hour = onToggle24Hour,
        onOpenCityPicker = { isCityPickerOpen = true },
        onDeleteWorldClock = onDeleteWorldClock,
        modifier = Modifier
          .weight(0.5f)
          .fillMaxHeight(),
      )
      AlarmsPane(
        nowMillis = nowMillis,
        use24Hour = use24Hour,
        alarms = alarms,
        onAddAlarm = {
          editingAlarm = null
          isAlarmDialogOpen = true
        },
        onEditAlarm = { alarm ->
          editingAlarm = alarm
          isAlarmDialogOpen = true
        },
        onToggleAlarm = onToggleAlarm,
        onDeleteAlarm = onDeleteAlarm,
        modifier = Modifier
          .weight(0.5f)
          .fillMaxHeight(),
      )
    }
  } else {
    Column(modifier = modifier.fillMaxSize()) {
      ClockSectionToggle(
        activeSection = activeSection,
        use24Hour = use24Hour,
        onSelectSection = onSelectSection,
        onToggle24Hour = onToggle24Hour,
      )
      Spacer(modifier = Modifier.height(12.dp))
      when (activeSection) {
        ClockSection.WORLD_CLOCK -> WorldClockPane(
          nowMillis = nowMillis,
          use24Hour = use24Hour,
          worldClocks = worldClocks,
          onToggle24Hour = onToggle24Hour,
          onOpenCityPicker = { isCityPickerOpen = true },
          onDeleteWorldClock = onDeleteWorldClock,
          showHeaderActions = false,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        )
        ClockSection.ALARMS -> AlarmsPane(
          nowMillis = nowMillis,
          use24Hour = use24Hour,
          alarms = alarms,
          onAddAlarm = {
            editingAlarm = null
            isAlarmDialogOpen = true
          },
          onEditAlarm = { alarm ->
            editingAlarm = alarm
            isAlarmDialogOpen = true
          },
          onToggleAlarm = onToggleAlarm,
          onDeleteAlarm = onDeleteAlarm,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        )
      }
    }
  }

  if (isCityPickerOpen) {
    AddWorldCityDialog(
      existingZoneIds = worldClocks.map { it.zoneId }.toSet(),
      onDismiss = { isCityPickerOpen = false },
      onSelectCity = { option ->
        onAddWorldClock(option)
        isCityPickerOpen = false
      },
    )
  }

  if (isAlarmDialogOpen) {
    AlarmEditorDialog(
      existing = editingAlarm,
      use24Hour = use24Hour,
      onDismiss = { isAlarmDialogOpen = false },
      onSave = { hour, minute, label, repeatDays, vibrate ->
        onSaveAlarm(
          editingAlarm?.id,
          hour,
          minute,
          label,
          editingAlarm?.enabled ?: true,
          repeatDays,
          vibrate,
        )
        isAlarmDialogOpen = false
      },
    )
  }
}

@Composable
private fun ClockSectionToggle(
  activeSection: ClockSection,
  use24Hour: Boolean,
  onSelectSection: (ClockSection) -> Unit,
  onToggle24Hour: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Surface(
      color = Color(0x88101624),
      shape = CircleShape,
      modifier = Modifier
        .weight(1f)
        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
    ) {
      Row(modifier = Modifier.padding(4.dp)) {
        SegmentPill(
          label = "World Clock",
          selected = activeSection == ClockSection.WORLD_CLOCK,
          onClick = { onSelectSection(ClockSection.WORLD_CLOCK) },
          testTag = "clock_tab_world",
          modifier = Modifier.weight(1f),
        )
        SegmentPill(
          label = "Alarms",
          selected = activeSection == ClockSection.ALARMS,
          onClick = { onSelectSection(ClockSection.ALARMS) },
          testTag = "clock_tab_alarms",
          modifier = Modifier.weight(1f),
        )
      }
    }

    Surface(
      color = Color(0x88101624),
      shape = CircleShape,
      modifier = Modifier
        .heightIn(min = 48.dp)
        .clip(CircleShape)
        .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
        .clickable(onClick = onToggle24Hour)
        .testTag("clock_format_toggle"),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        Text(
          text = if (use24Hour) "24H" else "12H",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun SegmentPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    shape = CircleShape,
    modifier = modifier
      .heightIn(min = 44.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.72f),
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun WorldClockPane(
  nowMillis: Long,
  use24Hour: Boolean,
  worldClocks: List<WorldClockEntry>,
  onToggle24Hour: () -> Unit,
  onOpenCityPicker: () -> Unit,
  onDeleteWorldClock: (String) -> Unit,
  showHeaderActions: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val (heroTime, heroAmPm, heroDate) = remember(nowMillis, use24Hour) {
    WorldClockCatalog.formatHeroTime(nowMillis, use24Hour)
  }
  val localZoneName = remember { TimeZone.getDefault().id.substringAfterLast('/').replace('_', ' ') }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = modifier,
  ) {
    item {
      Surface(
        color = Color(0x8A101624),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
          .testTag("world_clock_hero_card"),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Local • $localZoneName",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (showHeaderActions) {
              TextButton(onClick = onToggle24Hour) {
                Text(if (use24Hour) "24H" else "12H")
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = heroTime,
              style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp, fontWeight = FontWeight.Bold),
              color = Color.White,
              modifier = Modifier.testTag("world_clock_hero_time"),
            )
            if (heroAmPm.isNotEmpty()) {
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = heroAmPm,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp),
              )
            }
          }
          Text(
            text = heroDate,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.70f),
          )
        }
      }
    }

    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      ) {
        Text(
          text = "World Cities",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
        Button(
          onClick = onOpenCityPicker,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("world_clock_add_city_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add City")
        }
      }
    }

    if (worldClocks.isEmpty()) {
      item {
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
            Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No cities pinned yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Add London, Tokyo, New York, or any global timezone to compare live times.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.65f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(worldClocks, key = { it.id }) { clock ->
        val live = remember(clock.zoneId, nowMillis, use24Hour) {
          WorldClockCatalog.formatZoneTime(clock.zoneId, nowMillis, use24Hour)
        }
        Surface(
          color = Color(0x88101624),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .testTag("world_clock_card_${clock.id}"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
          ) {
            Icon(
              imageVector = if (live.isDaytime) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
              contentDescription = if (live.isDaytime) "Daytime" else "Nighttime",
              tint = if (live.isDaytime) Color(0xFFFBBF24) else OrbitIndigo,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = clock.cityLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${live.offsetLabel} • ${live.formattedDate}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = live.formattedTime,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              if (live.amPm.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = live.amPm,
                  style = MaterialTheme.typography.labelSmall,
                  color = OrbitCyan,
                  modifier = Modifier.padding(bottom = 3.dp),
                )
              }
            }
            IconButton(onClick = { onDeleteWorldClock(clock.id) }, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove city", tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun AlarmsPane(
  nowMillis: Long,
  use24Hour: Boolean,
  alarms: List<AlarmEntry>,
  onAddAlarm: () -> Unit,
  onEditAlarm: (AlarmEntry) -> Unit,
  onToggleAlarm: (alarmId: String, enabled: Boolean) -> Unit,
  onDeleteAlarm: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val countdownText = remember(alarms, nowMillis) {
    AlarmScheduler.formatCountdown(alarms, nowMillis)
  }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = modifier,
  ) {
    item {
      Surface(
        color = Color(0x8A101624),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp)),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Alarms",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
            Text(
              text = countdownText ?: "No active alarms armed",
              style = MaterialTheme.typography.bodyMedium,
              color = if (countdownText != null) OrbitMint else Color.White.copy(alpha = 0.6f),
            )
          }
          Button(
            onClick = onAddAlarm,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("alarm_add_button"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Alarm")
          }
        }
      }
    }

    if (alarms.isEmpty()) {
      item {
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
            Icon(Icons.Rounded.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No alarms configured", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Create alarms with repeat days, labels, and system notifications.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.65f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(alarms, key = { it.id }) { alarm ->
        AlarmCardItem(
          alarm = alarm,
          use24Hour = use24Hour,
          onClick = { onEditAlarm(alarm) },
          onToggle = { enabled -> onToggleAlarm(alarm.id, enabled) },
          onDelete = { onDeleteAlarm(alarm.id) },
        )
      }
    }
    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun AlarmCardItem(
  alarm: AlarmEntry,
  use24Hour: Boolean,
  onClick: () -> Unit,
  onToggle: (Boolean) -> Unit,
  onDelete: () -> Unit,
) {
  val h = alarm.hour.toInt()
  val m = alarm.minute.toInt()
  val timeStr = if (use24Hour) {
    String.format(Locale.getDefault(), "%02d:%02d", h, m)
  } else {
    val h12 = if (h % 12 == 0) 12 else h % 12
    String.format(Locale.getDefault(), "%d:%02d", h12, m)
  }
  val amPm = if (use24Hour) "" else if (h < 12) "AM" else "PM"
  val repeatSummary = remember(alarm.repeatDays) { formatRepeatDays(alarm.repeatDays) }

  Surface(
    color = if (alarm.enabled) Color(0x94101624) else Color(0x66101624),
    shape = RoundedCornerShape(22.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .border(
        width = 1.dp,
        color = if (alarm.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(22.dp),
      )
      .clickable(onClick = onClick)
      .testTag("alarm_card_${alarm.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = timeStr,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
            color = if (alarm.enabled) Color.White else Color.White.copy(alpha = 0.45f),
          )
          if (amPm.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = amPm,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = if (alarm.enabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
              modifier = Modifier.padding(bottom = 4.dp),
            )
          }
          if (alarm.vibrate) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Rounded.Vibration,
              contentDescription = "Vibrate enabled",
              tint = Color.White.copy(alpha = 0.5f),
              modifier = Modifier.padding(bottom = 5.dp).size(15.dp),
            )
          }
        }
        val subtitle = buildString {
          if (alarm.label.isNotBlank()) append(alarm.label).append(" • ")
          append(repeatSummary)
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = if (alarm.enabled) 0.76f else 0.42f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Switch(
        checked = alarm.enabled,
        onCheckedChange = onToggle,
        modifier = Modifier.testTag("alarm_switch_${alarm.id}"),
      )
      IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete alarm", tint = DangerCoral, modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
private fun AddWorldCityDialog(
  existingZoneIds: Set<String>,
  onDismiss: () -> Unit,
  onSelectCity: (WorldCityOption) -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered = remember(query, existingZoneIds) {
    val q = query.trim().lowercase()
    WorldClockCatalog.curatedCities.filter { opt ->
      opt.zoneId !in existingZoneIds &&
        (q.isEmpty() || opt.city.lowercase().contains(q) || opt.region.lowercase().contains(q))
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add World Clock") },
    text = {
      Column(modifier = Modifier.fillMaxWidth().height(340.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Search city or region") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("world_clock_search_input"),
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(filtered, key = { it.zoneId }) { option ->
            Surface(
              color = Color.White.copy(alpha = 0.06f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelectCity(option) }
                .testTag("city_option_${option.city.lowercase().replace(' ', '_')}"),
            ) {
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(option.city, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(option.region, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.62f))
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
private fun AlarmEditorDialog(
  existing: AlarmEntry?,
  use24Hour: Boolean,
  onDismiss: () -> Unit,
  onSave: (hour: Long, minute: Long, label: String, repeatDays: List<Long>, vibrate: Boolean) -> Unit,
) {
  var hour by remember(existing) { mutableIntStateOf(existing?.hour?.toInt() ?: 7) }
  var minute by remember(existing) { mutableIntStateOf(existing?.minute?.toInt() ?: 0) }
  var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
  var repeatDays by remember(existing) { mutableStateOf(existing?.repeatDays?.toSet() ?: emptySet()) }
  var vibrate by remember(existing) { mutableStateOf(existing?.vibrate ?: true) }

  val dayChips = remember {
    listOf(1L to "M", 2L to "T", 3L to "W", 4L to "T", 5L to "F", 6L to "S", 7L to "S")
  }

  val formattedPreview = if (use24Hour) {
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
  } else {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
    String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "New Alarm" else "Edit Alarm") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
      ) {
        Text(
          text = formattedPreview,
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedButton(
            onClick = { hour = (hour + 23) % 24 },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) { Text("-1 Hr") }
          OutlinedButton(
            onClick = { hour = (hour + 1) % 24 },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) { Text("+1 Hr") }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedButton(
            onClick = { minute = (minute + 55) % 60 },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) { Text("-5 Min") }
          OutlinedButton(
            onClick = { minute = (minute + 5) % 60 },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) { Text("+5 Min") }
        }

        OutlinedTextField(
          value = label,
          onValueChange = { label = it },
          label = { Text("Label (optional)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_label_input"),
        )

        Text("Repeat days", style = MaterialTheme.typography.labelLarge)
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
          dayChips.forEach { (dayNum, shortName) ->
            val selected = dayNum in repeatDays
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                .clickable {
                  repeatDays = if (selected) repeatDays - dayNum else repeatDays + dayNum
                },
            ) {
              Text(
                text = shortName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Vibrate", style = MaterialTheme.typography.bodyLarge)
          Switch(checked = vibrate, onCheckedChange = { vibrate = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(hour.toLong(), minute.toLong(), label.trim(), repeatDays.sorted(), vibrate)
        },
        modifier = Modifier.testTag("alarm_save_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

private fun formatRepeatDays(days: List<Long>): String {
  if (days.isEmpty()) return "Once"
  val sorted = days.distinct().sorted()
  if (sorted == listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)) return "Every day"
  if (sorted == listOf(1L, 2L, 3L, 4L, 5L)) return "Weekdays"
  if (sorted == listOf(6L, 7L)) return "Weekends"
  val map = mapOf(
    1L to "Mon", 2L to "Tue", 3L to "Wed", 4L to "Thu", 5L to "Fri", 6L to "Sat", 7L to "Sun"
  )
  return sorted.mapNotNull { map[it] }.joinToString(", ")
}
