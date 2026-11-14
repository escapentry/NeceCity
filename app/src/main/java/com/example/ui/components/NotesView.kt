package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.display.NeceCityDisplayProfile
import com.example.model.Note
import com.example.model.NoteColorTag
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitIndigo
import com.example.ui.theme.OrbitMint
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotesView(
  notes: List<Note>,
  searchQuery: String,
  selectedColorFilter: String?,
  profile: NeceCityDisplayProfile,
  onSearchQueryChange: (String) -> Unit,
  onSelectColorFilter: (String?) -> Unit,
  onSaveNote: (noteId: String?, title: String, content: String, colorTag: String, pinned: Boolean) -> Unit,
  onTogglePin: (noteId: String, pinned: Boolean) -> Unit,
  onDeleteNote: (String) -> Unit,
  isOfflineMode: Boolean = false,
  modifier: Modifier = Modifier,
) {
  var editingNote by remember { mutableStateOf<Note?>(null) }
  var isCreatingNew by remember { mutableStateOf(false) }

  val filteredNotes = remember(notes, searchQuery, selectedColorFilter) {
    val q = searchQuery.trim().lowercase()
    notes.filter { note ->
      val matchesColor = selectedColorFilter == null || note.colorTag == selectedColorFilter
      val matchesQuery = q.isEmpty() ||
        note.title.lowercase().contains(q) ||
        note.content.lowercase().contains(q)
      matchesColor && matchesQuery
    }.sortedWith(
      compareByDescending<Note> { it.pinned }
        .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
    )
  }

  val showEditor = isCreatingNew || editingNote != null
  BackHandler(enabled = showEditor && !profile.useTwoPaneLayout) {
    isCreatingNew = false
    editingNote = null
  }

  if (profile.useTwoPaneLayout) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      modifier = modifier.fillMaxSize(),
    ) {
      NotesListPane(
        notes = filteredNotes,
        searchQuery = searchQuery,
        selectedColorFilter = selectedColorFilter,
        selectedNoteId = editingNote?.id,
        isOfflineMode = isOfflineMode,
        onSearchQueryChange = onSearchQueryChange,
        onSelectColorFilter = onSelectColorFilter,
        onNewNote = {
          editingNote = null
          isCreatingNew = true
        },
        onSelectNote = { note ->
          isCreatingNew = false
          editingNote = note
        },
        onTogglePin = onTogglePin,
        onDeleteNote = { id ->
          if (editingNote?.id == id) editingNote = null
          onDeleteNote(id)
        },
        modifier = Modifier
          .weight(0.46f)
          .fillMaxHeight(),
      )
      Box(
        modifier = Modifier
          .weight(0.54f)
          .fillMaxHeight(),
      ) {
        if (showEditor) {
          NoteEditorPane(
            existing = editingNote,
            showBackButton = false,
            onBack = {
              isCreatingNew = false
              editingNote = null
            },
            onSave = { title, content, colorTag, pinned ->
              onSaveNote(editingNote?.id, title, content, colorTag, pinned)
              isCreatingNew = false
              editingNote = null
            },
            onDelete = editingNote?.let { existing ->
              {
                onDeleteNote(existing.id)
                editingNote = null
              }
            },
          )
        } else {
          NotesEmptyEditorPlaceholder(
            onNewNote = {
              editingNote = null
              isCreatingNew = true
            },
          )
        }
      }
    }
  } else {
    if (showEditor) {
      NoteEditorPane(
        existing = editingNote,
        showBackButton = true,
        onBack = {
          isCreatingNew = false
          editingNote = null
        },
        onSave = { title, content, colorTag, pinned ->
          onSaveNote(editingNote?.id, title, content, colorTag, pinned)
          isCreatingNew = false
          editingNote = null
        },
        onDelete = editingNote?.let { existing ->
          {
            onDeleteNote(existing.id)
            editingNote = null
          }
        },
        modifier = modifier.fillMaxSize(),
      )
    } else {
      NotesListPane(
        notes = filteredNotes,
        searchQuery = searchQuery,
        selectedColorFilter = selectedColorFilter,
        selectedNoteId = null,
        isOfflineMode = isOfflineMode,
        onSearchQueryChange = onSearchQueryChange,
        onSelectColorFilter = onSelectColorFilter,
        onNewNote = {
          editingNote = null
          isCreatingNew = true
        },
        onSelectNote = { note ->
          isCreatingNew = false
          editingNote = note
        },
        onTogglePin = onTogglePin,
        onDeleteNote = onDeleteNote,
        modifier = modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun NotesListPane(
  notes: List<Note>,
  searchQuery: String,
  selectedColorFilter: String?,
  selectedNoteId: String?,
  isOfflineMode: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onSelectColorFilter: (String?) -> Unit,
  onNewNote: () -> Unit,
  onSelectNote: (Note) -> Unit,
  onTogglePin: (noteId: String, pinned: Boolean) -> Unit,
  onDeleteNote: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pinnedNotes = remember(notes) { notes.filter { it.pinned } }
  val otherNotes = remember(notes) { notes.filterNot { it.pinned } }

  Column(modifier = modifier) {
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
          .height(48.dp)
          .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp),
        ) {
          Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
              Text(
                text = stringResource(id = R.string.search_notes_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = onSearchQueryChange,
              singleLine = true,
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("notes_search_input"),
            )
          }
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(36.dp)) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.clear_search), tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      Button(
        onClick = onNewNote,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("notes_new_button"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("New")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
    ) {
      ColorFilterChip(
        label = "All",
        accent = MaterialTheme.colorScheme.primary,
        selected = selectedColorFilter == null,
        onClick = { onSelectColorFilter(null) },
      )
      NoteColorTag.entries.forEach { tag ->
        ColorFilterChip(
          label = tag.label,
          accent = noteTagColor(tag.id),
          selected = selectedColorFilter == tag.id,
          onClick = {
            onSelectColorFilter(if (selectedColorFilter == tag.id) null else tag.id)
          },
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (notes.isEmpty()) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        Surface(
          color = Color(0x66101624),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(26.dp),
          ) {
            Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("No notes yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isOfflineMode) {
                "Capture ideas, checklists, and snippets stored locally on this device."
              } else {
                "Capture ideas, checklists, and snippets synced to your account."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.65f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        if (pinnedNotes.isNotEmpty()) {
          item {
            Text(
              text = "PINNED",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 4.dp),
            )
          }
          items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
            NoteCardItem(
              note = note,
              selected = note.id == selectedNoteId,
              onClick = { onSelectNote(note) },
              onTogglePin = { onTogglePin(note.id, !note.pinned) },
              onDelete = { onDeleteNote(note.id) },
            )
          }
        }
        if (otherNotes.isNotEmpty()) {
          if (pinnedNotes.isNotEmpty()) {
            item {
              Text(
                text = "NOTES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.58f),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
              )
            }
          }
          items(otherNotes, key = { it.id }) { note ->
            NoteCardItem(
              note = note,
              selected = note.id == selectedNoteId,
              onClick = { onSelectNote(note) },
              onTogglePin = { onTogglePin(note.id, !note.pinned) },
              onDelete = { onDeleteNote(note.id) },
            )
          }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
      }
    }
  }
}

@Composable
private fun ColorFilterChip(
  label: String,
  accent: Color,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    color = if (selected) accent.copy(alpha = 0.24f) else Color(0x66101624),
    shape = CircleShape,
    modifier = Modifier
      .heightIn(min = 38.dp)
      .clip(CircleShape)
      .border(1.dp, if (selected) accent else Color.White.copy(alpha = 0.14f), CircleShape)
      .clickable(onClick = onClick),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
    }
  }
}

@Composable
private fun NoteCardItem(
  note: Note,
  selected: Boolean,
  onClick: () -> Unit,
  onTogglePin: () -> Unit,
  onDelete: () -> Unit,
) {
  val accent = noteTagColor(note.colorTag)
  val ts = note.updatedAt ?: note.createdAt ?: Timestamp.now()
  val formattedDate = remember(ts) {
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate())
  }

  Surface(
    color = if (selected) Color(0xAA172033) else Color(0x88101624),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) accent else Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp),
      )
      .clickable(onClick = onClick)
      .testTag("note_card_${note.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
    ) {
      Box(
        modifier = Modifier
          .padding(top = 4.dp)
          .width(5.dp)
          .height(38.dp)
          .clip(CircleShape)
          .background(accent),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = note.title.ifBlank { "Untitled Note" },
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (note.content.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = note.content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = formattedDate,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.45f),
        )
      }
      IconButton(onClick = onTogglePin, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = if (note.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
          contentDescription = if (note.pinned) "Unpin note" else "Pin note",
          tint = if (note.pinned) accent else Color.White.copy(alpha = 0.55f),
          modifier = Modifier.size(18.dp),
        )
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = "Delete note",
          tint = DangerCoral,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun NoteEditorPane(
  existing: Note?,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onSave: (title: String, content: String, colorTag: String, pinned: Boolean) -> Unit,
  onDelete: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
  var content by remember(existing?.id) { mutableStateOf(existing?.content.orEmpty()) }
  var colorTag by remember(existing?.id) { mutableStateOf(existing?.colorTag ?: NoteColorTag.CYAN.id) }
  var pinned by remember(existing?.id) { mutableStateOf(existing?.pinned ?: false) }

  Surface(
    color = Color(0x94101624),
    shape = RoundedCornerShape(24.dp),
    modifier = modifier
      .fillMaxSize()
      .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
      .testTag("note_editor_pane"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (showBackButton) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to notes", tint = Color.White)
          }
        }
        Text(
          text = if (existing == null) "New Note" else "Edit Note",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { pinned = !pinned }, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = if (pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
            contentDescription = "Toggle pin",
            tint = if (pinned) noteTagColor(colorTag) else Color.White.copy(alpha = 0.65f),
          )
        }
        if (onDelete != null) {
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete note", tint = DangerCoral)
          }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Button(
          onClick = { onSave(title.trim(), content.trim(), colorTag, pinned) },
          enabled = title.trim().isNotEmpty() || content.trim().isNotEmpty(),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("note_save_button"),
        ) {
          Text("Save")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
      ) {
        NoteColorTag.entries.forEach { tag ->
          ColorFilterChip(
            label = tag.label,
            accent = noteTagColor(tag.id),
            selected = colorTag == tag.id,
            onClick = { colorTag = tag.id },
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState()),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("note_title_input"),
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Write your note...") },
          minLines = 8,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("note_content_input"),
        )
      }
    }
  }
}

@Composable
private fun NotesEmptyEditorPlaceholder(onNewNote: () -> Unit) {
  Surface(
    color = Color(0x66101624),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxSize()
      .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(28.dp),
    ) {
      Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
      Spacer(modifier = Modifier.height(10.dp))
      Text("Select or create a note", style = MaterialTheme.typography.titleMedium, color = Color.White)
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Choose any note on the left or tap New Note to start writing.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.65f),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Button(onClick = onNewNote, shape = RoundedCornerShape(16.dp), modifier = Modifier.heightIn(min = 48.dp)) {
        Text("New Note")
      }
    }
  }
}

private fun noteTagColor(id: String): Color = when (id) {
  NoteColorTag.INDIGO.id -> OrbitIndigo
  NoteColorTag.MINT.id -> OrbitMint
  NoteColorTag.CORAL.id -> DangerCoral
  NoteColorTag.OBSIDIAN.id -> ObsidianSurfaceElevated
  else -> OrbitCyan
}
