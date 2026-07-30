package com.ravenhub.app.ui.mainscreens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravenhub.app.data.notes.NoteItem
import com.ravenhub.app.ui.component.CustomBottomSheet
import com.ravenhub.app.ui.component.RootAppDialog
import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveListItem
import com.ravenhub.app.ui.viewmodel.NotesViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NotesViewModel = viewModel()) {
    val data by viewModel.data.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var activeNote by remember { mutableStateOf<NoteItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteItem?>(null) }

    val categories = remember(data.notes) {
        data.notes.mapNotNull { it.category.ifBlank { null } }.distinct()
    }

    val filteredNotes = remember(data.notes, searchQuery, selectedCategory) {
        data.notes.filter { note ->
            val matchesCategory = selectedCategory == null || note.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true) ||
                    note.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }.sortedByDescending { it.updatedAt }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            val isBlurEnabled = com.ravenhub.app.ui.component.LocalBlurEnabled.current
            val hazeState = com.ravenhub.app.ui.component.LocalAppHazeState.current

            FloatingActionButton(
                onClick = {
                    activeNote = null
                    isCreatingNew = true
                    showEditor = true
                },
                shape = CircleShape,
                containerColor = if (isBlurEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(if (isBlurEnabled) 0.dp else 6.dp),
                modifier = Modifier
                    .padding(bottom = 100.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurEffect {
                                    blurRadius = 24.dp
                                }
                            }
                        } else Modifier
                    )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New note", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        val isBlurEnabled = com.ravenhub.app.ui.component.LocalBlurEnabled.current
        val hazeState = com.ravenhub.app.ui.component.LocalAppHazeState.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isBlurEnabled && hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Category Chips
            if (categories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (${data.notes.size})") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Notes List
            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Description, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("No notes found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                ExpressiveList(
                    content = filteredNotes.map { note ->
                        {
                            ExpressiveListItem(
                                onClick = {
                                    activeNote = note
                                    isCreatingNew = false
                                    showEditor = true
                                },
                                onLongClick = { viewModel.deleteNote(note.id) },
                                headlineContent = {
                                    Text(
                                        text = note.title.ifBlank { "Untitled Note" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        if (note.content.isNotBlank()) {
                                            Text(
                                                text = note.content.take(100).replace("\n", " "),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${if (note.category.isNotBlank()) "${note.category} · " else ""}${dateFormat.format(Date(note.updatedAt))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                leadingContent = {
                                    Icon(Icons.Rounded.EditNote, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            activeNote = note
                                            isCreatingNew = false
                                            showEditor = true
                                        }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Edit, "Edit note", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { noteToDelete = note }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Close, "Delete", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(110.dp))
        }
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete '${note.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(note.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }

    RootAppDialog {
        NoteEditorSheet(
            visible = showEditor,
            note = activeNote,
            isNew = isCreatingNew,
            onDismiss = { showEditor = false },
            onSave = { title, content, category ->
                if (isCreatingNew || activeNote == null) {
                    viewModel.addNote(title, content, category)
                } else {
                    viewModel.updateNote(activeNote!!.id, title, content, category)
                }
                showEditor = false
            },
            viewModel = viewModel
        )
    }
}

@Composable
private fun NoteEditorSheet(
    visible: Boolean,
    note: NoteItem?,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    viewModel: NotesViewModel
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isPreviewMode by remember { mutableStateOf(false) }

    LaunchedEffect(visible, note, isNew) {
        if (visible) {
            if (isNew || note == null) {
                title = ""
                content = ""
                category = ""
                isPreviewMode = false
            } else {
                title = note.title
                content = note.content
                category = note.category
                isPreviewMode = true
            }
        }
    }

    val backlinks = remember(title, visible) {
        if (visible && title.isNotBlank()) viewModel.getBacklinksFor(title) else emptyList()
    }

    CustomBottomSheet(visible = visible, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNew) "New Note" else if (isPreviewMode) "Note Preview" else "Edit Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isNew) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isPreviewMode,
                            onClick = { isPreviewMode = true },
                            label = { Text("Preview") }
                        )
                        FilterChip(
                            selected = !isPreviewMode,
                            onClick = { isPreviewMode = false },
                            label = { Text("Edit") }
                        )
                    }
                }
            }

            if (!isPreviewMode) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Markdown supported, use [[Title]] for backlinks)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    minLines = 8,
                    maxLines = 16,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (category.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (content.isBlank()) {
                            Text("Nothing to preview", color = MaterialTheme.colorScheme.outline)
                        } else {
                            MarkdownText(
                                markdown = content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Backlinks Section
            if (backlinks.isNotEmpty()) {
                Text(
                    text = "Backlinks (${backlinks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    backlinks.forEach { backlink ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "← ${backlink.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (!isPreviewMode) {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title.trim(), content, category.trim())
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Save Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { isPreviewMode = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Edit Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
