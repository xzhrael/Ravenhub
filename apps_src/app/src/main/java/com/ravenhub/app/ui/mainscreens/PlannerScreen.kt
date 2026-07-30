package com.ravenhub.app.ui.mainscreens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravenhub.app.data.planner.HabitFrequency
import com.ravenhub.app.data.planner.HabitItem
import com.ravenhub.app.data.planner.SubTaskItem
import com.ravenhub.app.data.planner.TodoItem
import com.ravenhub.app.notification.TodoScheduler
import com.ravenhub.app.ui.component.CustomBottomSheet
import com.ravenhub.app.ui.component.RootAppDialog
import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveListItem
import com.ravenhub.app.ui.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: PlannerViewModel = viewModel()) {
    val context = LocalContext.current
    val data by viewModel.data.collectAsState()

    LaunchedEffect(Unit) { viewModel.reload() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Todos, 1=Habits
    var showAddTodo by remember { mutableStateOf(false) }
    var showAddHabit by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var editingHabit by remember { mutableStateOf<HabitItem?>(null) }
    var todoToDelete by remember { mutableStateOf<TodoItem?>(null) }
    var habitToDelete by remember { mutableStateOf<HabitItem?>(null) }

    // Todo filter
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = remember(data.todos) {
        data.todos.mapNotNull { it.category.ifBlank { null } }.distinct()
    }

    // Habit filter
    var habitFilter by remember { mutableStateOf<HabitFrequency?>(null) }

    val isBlurEnabled = com.ravenhub.app.ui.component.LocalBlurEnabled.current
    val hazeState = com.ravenhub.app.ui.component.LocalAppHazeState.current

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingTodo = null
                        showAddTodo = true
                    } else {
                        editingHabit = null
                        showAddHabit = true
                    }
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
                Icon(Icons.Rounded.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isBlurEnabled && hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

            // Tab row: Todos / Habits
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Todos (${data.todos.size})") },
                    leadingIcon = { Icon(Icons.Rounded.Checklist, null, Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Habits (${data.habits.size})") },
                    leadingIcon = { Icon(Icons.Rounded.Loop, null, Modifier.size(18.dp)) }
                )
            }

            AnimatedContent(targetState = selectedTab, label = "tab") { tab ->
                when (tab) {
                    0 -> TodoTab(
                        todos = data.todos,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        onToggle = { viewModel.toggleTodo(it) },
                        onToggleSubTask = { todoId, subTaskId -> viewModel.toggleSubTask(todoId, subTaskId) },
                        onEdit = { todo ->
                            editingTodo = todo
                            showAddTodo = true
                        },
                        onDelete = { id ->
                            val todo = data.todos.find { it.id == id }
                            todoToDelete = todo
                        }
                    )
                    1 -> HabitTab(
                        habits = data.habits,
                        filter = habitFilter,
                        onFilterChanged = { habitFilter = it },
                        onToggle = viewModel::toggleHabitToday,
                        onEdit = { habit ->
                            editingHabit = habit
                            showAddHabit = true
                        },
                        onDelete = { id ->
                            val habit = data.habits.find { it.id == id }
                            habitToDelete = habit
                        }
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    todoToDelete?.let { todo ->
        AlertDialog(
            onDismissRequest = { todoToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete '${todo.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTodo(todo.id)
                        TodoScheduler.cancel(context, todo.id)
                        todoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { todoToDelete = null }) { Text("Cancel") }
            }
        )
    }

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete '${habit.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabit(habit.id)
                        habitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { habitToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Add/Edit Todo Bottom Sheet
    RootAppDialog {
        AddTodoSheet(
            visible = showAddTodo,
            todoToEdit = editingTodo,
            onDismiss = { showAddTodo = false },
            onSave = { title, category, dueDateTime, reminderOffset, subTasks ->
                if (editingTodo != null) {
                    val updated = viewModel.updateTodo(editingTodo!!.id, title, category, dueDateTime, reminderOffset, subTasks)
                    if (dueDateTime != null) {
                        TodoScheduler.schedule(context, updated)
                    } else {
                        TodoScheduler.cancel(context, editingTodo!!.id)
                    }
                } else {
                    val newTodo = viewModel.addTodo(title, category, dueDateTime, reminderOffset, subTasks)
                    if (dueDateTime != null) {
                        TodoScheduler.schedule(context, newTodo)
                    }
                }
                showAddTodo = false
            }
        )
    }

    // Add/Edit Habit Bottom Sheet
    RootAppDialog {
        AddHabitSheet(
            visible = showAddHabit,
            habitToEdit = editingHabit,
            onDismiss = { showAddHabit = false },
            onSave = { title, frequency ->
                if (editingHabit != null) {
                    viewModel.updateHabit(editingHabit!!.id, title, frequency)
                } else {
                    viewModel.addHabit(title, frequency)
                }
                showAddHabit = false
            }
        )
    }
}

@Composable
private fun TodoTab(
    todos: List<TodoItem>,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onToggle: (String) -> Unit,
    onToggleSubTask: (String, String) -> Unit,
    onEdit: (TodoItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (categories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategorySelected(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat) }
                    )
                }
            }
        }

        val filtered = if (selectedCategory != null) {
            todos.filter { it.category == selectedCategory }
        } else todos

        if (filtered.isEmpty()) {
            EmptyState("No todos yet", Icons.Rounded.Checklist)
        } else {
            ExpressiveList(
                content = filtered.map { todo ->
                    {
                        var isTreeExpanded by remember { mutableStateOf(false) }

                        Column {
                            ExpressiveListItem(
                                onClick = { onEdit(todo) },
                                onLongClick = { onDelete(todo.id) },
                                headlineContent = {
                                    Text(
                                        text = todo.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (todo.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        if (todo.category.isNotBlank()) {
                                            Text(todo.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (todo.dueDateTime != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Rounded.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                                Text(dateFormat.format(todo.dueDateTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                            }
                                        }
                                        if (todo.subTasks.isNotEmpty()) {
                                            val completedCount = todo.subTasks.count { it.isCompleted }
                                            Text(
                                                text = "Tree Steps: $completedCount/${todo.subTasks.size} completed",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                leadingContent = {
                                    Checkbox(
                                        checked = todo.isCompleted,
                                        onCheckedChange = { onToggle(todo.id) }
                                    )
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (todo.subTasks.isNotEmpty()) {
                                            IconButton(onClick = { isTreeExpanded = !isTreeExpanded }, modifier = Modifier.size(40.dp)) {
                                                Icon(
                                                    if (isTreeExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                    "Toggle Steps",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onEdit(todo) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Edit, "Edit", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { onDelete(todo.id) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Close, "Delete", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            )

                            AnimatedVisibility(visible = isTreeExpanded && todo.subTasks.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp, end = 16.dp, bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    todo.subTasks.forEach { sub ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().clickable { onToggleSubTask(todo.id, sub.id) }
                                        ) {
                                            Checkbox(
                                                checked = sub.isCompleted,
                                                onCheckedChange = { onToggleSubTask(todo.id, sub.id) }
                                            )
                                            Text(
                                                text = sub.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (sub.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HabitTab(
    habits: List<HabitItem>,
    filter: HabitFrequency?,
    onFilterChanged: (HabitFrequency?) -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (HabitItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val today = remember { System.currentTimeMillis() / 86_400_000L }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == null, onClick = { onFilterChanged(null) }, label = { Text("All") })
            FilterChip(selected = filter == HabitFrequency.DAILY, onClick = { onFilterChanged(HabitFrequency.DAILY) }, label = { Text("Daily") })
            FilterChip(selected = filter == HabitFrequency.WEEKLY, onClick = { onFilterChanged(HabitFrequency.WEEKLY) }, label = { Text("Weekly") })
        }

        val filtered = if (filter != null) habits.filter { it.frequency == filter } else habits

        if (filtered.isEmpty()) {
            EmptyState("No habits yet", Icons.Rounded.Loop)
        } else {
            ExpressiveList(
                content = filtered.map { habit ->
                    val isDoneToday = today in habit.completedDates
                    {
                        ExpressiveListItem(
                            onClick = { onEdit(habit) },
                            onLongClick = { onDelete(habit.id) },
                            headlineContent = {
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "${habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} · ${habit.completedDates.size} completions",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Checkbox(
                                    checked = isDoneToday,
                                    onCheckedChange = { onToggle(habit.id) }
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onEdit(habit) }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Rounded.Edit, "Edit", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onDelete(habit.id) }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Rounded.Close, "Delete", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTodoSheet(
    visible: Boolean,
    todoToEdit: TodoItem? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?, Int?, List<SubTaskItem>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedCalendar by remember { mutableStateOf<Calendar?>(null) }
    var reminderOffset by remember { mutableIntStateOf(0) }
    var subTasks by remember { mutableStateOf<List<SubTaskItem>>(emptyList()) }
    var newSubTaskInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(visible, todoToEdit) {
        if (visible) {
            if (todoToEdit != null) {
                title = todoToEdit.title
                category = todoToEdit.category
                selectedCalendar = todoToEdit.dueDateTime?.let { Calendar.getInstance().apply { timeInMillis = it } }
                reminderOffset = todoToEdit.reminderOffsetMinutes ?: 0
                subTasks = todoToEdit.subTasks
            } else {
                title = ""
                category = ""
                selectedCalendar = null
                reminderOffset = 0
                subTasks = emptyList()
            }
            newSubTaskInput = ""
        }
    }

    fun pickDateTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val now = selectedCalendar ?: Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                        }
                        selectedCalendar = cal
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    CustomBottomSheet(visible = visible, onDismiss = onDismiss) {
        Text(if (todoToEdit != null) "Edit Todo" else "Add Todo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Title", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = category, onValueChange = { category = it },
            label = { Text("Category (optional)", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        // Sub-task Tree Items Input
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tree Sub-tasks / Steps (optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newSubTaskInput,
                    onValueChange = { newSubTaskInput = it },
                    label = { Text("Add sub-task step", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newSubTaskInput.isNotBlank()) {
                            subTasks = subTasks + SubTaskItem(title = newSubTaskInput.trim())
                            newSubTaskInput = ""
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.AddCircle, "Add step", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (subTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subTasks.forEachIndexed { index, sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("• ${sub.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { subTasks = subTasks.filterIndexed { i, _ -> i != index } }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCalendar?.let { dateTimeFormat.format(it.time) } ?: "No Reminder Set",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedCalendar != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (selectedCalendar != null) {
                    IconButton(onClick = { selectedCalendar = null }) {
                        Icon(Icons.Rounded.Close, "Clear reminder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = { pickDateTime() }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.Event, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (selectedCalendar != null) "Change" else "Set Time")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onSave(title.trim(), category.trim(), selectedCalendar?.timeInMillis, if (selectedCalendar != null) reminderOffset else null, subTasks)
                }
            },
            enabled = title.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp)
        ) {
            Text(if (todoToEdit != null) "Save Changes" else "Add Todo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AddHabitSheet(
    visible: Boolean,
    habitToEdit: HabitItem? = null,
    onDismiss: () -> Unit,
    onSave: (String, HabitFrequency) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(HabitFrequency.DAILY) }

    LaunchedEffect(visible, habitToEdit) {
        if (visible) {
            if (habitToEdit != null) {
                title = habitToEdit.title
                frequency = habitToEdit.frequency
            } else {
                title = ""
                frequency = HabitFrequency.DAILY
            }
        }
    }

    CustomBottomSheet(visible = visible, onDismiss = onDismiss) {
        Text(if (habitToEdit != null) "Edit Habit" else "Add Habit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Habit Title", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
            FilterChip(
                selected = frequency == HabitFrequency.DAILY,
                onClick = { frequency = HabitFrequency.DAILY },
                label = { Text("Daily") }
            )
            FilterChip(
                selected = frequency == HabitFrequency.WEEKLY,
                onClick = { frequency = HabitFrequency.WEEKLY },
                label = { Text("Weekly") }
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (title.isNotBlank()) onSave(title.trim(), frequency) },
            enabled = title.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp)
        ) {
            Text(if (habitToEdit != null) "Save Changes" else "Add Habit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}
