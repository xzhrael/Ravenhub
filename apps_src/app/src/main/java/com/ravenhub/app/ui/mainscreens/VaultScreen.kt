package com.ravenhub.app.ui.mainscreens

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.webkit.MimeTypeMap
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravenhub.app.data.vault.CredentialItem
import com.ravenhub.app.data.vault.VaultFileEntry
import com.ravenhub.app.data.vault.VaultFileManager
import com.ravenhub.app.ui.component.CustomBottomSheet
import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveListItem
import com.ravenhub.app.ui.component.RootAppDialog
import com.ravenhub.app.ui.security.LockMode
import com.ravenhub.app.ui.security.LockScreen
import com.ravenhub.app.ui.util.AppLifecycleManager
import com.ravenhub.app.ui.viewmodel.VaultViewModel
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val data by viewModel.data.collectAsState()
    val exportResultUri by viewModel.exportResultUri.collectAsState()

    LaunchedEffect(Unit) { viewModel.reload() }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearExportResult() }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Passwords, 1=Files
    var showAddCredential by remember { mutableStateOf(false) }
    var editingCredential by remember { mutableStateOf<CredentialItem?>(null) }
    var credentialToDelete by remember { mutableStateOf<CredentialItem?>(null) }
    var fileToDelete by remember { mutableStateOf<VaultFileEntry?>(null) }

    var deleteOriginalOnImport by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(data.credentials) {
        data.credentials.mapNotNull { it.category.ifBlank { null } }.distinct()
    }

    // Multi-select files state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedFileIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showExportOptionsModal by remember { mutableStateOf(false) }
    var showPinReauthModal by remember { mutableStateOf(false) }

    // Pending SAF export state
    var pendingExportEntries by remember { mutableStateOf<List<VaultFileEntry>>(emptyList()) }
    var pendingExportAsEncrypted by rememberSaveable { mutableStateOf(true) }

    // SAF CreateDocument Launcher (User selects where to save export file)
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri ->
        destinationUri?.let { uri ->
            if (pendingExportEntries.isNotEmpty()) {
                AppLifecycleManager.isLaunchingSystemPicker = true
                viewModel.exportToDestinationUri(context, pendingExportEntries, pendingExportAsEncrypted, uri)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFile(it, deleteOriginalOnImport) }
    }

    // When export completes, notify user & offer to open file
    LaunchedEffect(exportResultUri) {
        exportResultUri?.let { uri ->
            Toast.makeText(context, "File saved successfully!", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open exported file"))
            } catch (_: Exception) {}
            viewModel.clearExportResult()
        }
    }

    fun startSafExport(asEncrypted: Boolean) {
        val selectedEntries = data.files.filter { it.id in selectedFileIds }
        if (selectedEntries.isEmpty()) return

        pendingExportEntries = selectedEntries
        pendingExportAsEncrypted = asEncrypted
        showExportOptionsModal = false

        AppLifecycleManager.isLaunchingSystemPicker = true
        val suggestedName = if (selectedEntries.size == 1) {
            val name = selectedEntries[0].originalName
            if (asEncrypted) "$name.enc" else name
        } else {
            "vault_export_${System.currentTimeMillis()}.zip"
        }
        createDocLauncher.launch(suggestedName)
    }

    fun triggerDecryptedExportWithAuth() {
        showExportOptionsModal = false
        showPinReauthModal = true
    }

    fun openOriginalFile(entry: VaultFileEntry) {
        coroutineScope.launch(Dispatchers.IO) {
            val file = VaultFileManager.exportFile(context, entry)
            if (file != null && file.exists()) {
                withContext(Dispatchers.Main) {
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                        val mimeType = context.contentResolver.getType(uri)
                            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                            ?: "*/*"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open ${entry.originalName} with..."))
                    } catch (e: Exception) {
                        Toast.makeText(context, "No application found to open file", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to decrypt file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            val isBlurEnabled = com.ravenhub.app.ui.component.LocalBlurEnabled.current
            val hazeState = com.ravenhub.app.ui.component.LocalAppHazeState.current

            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingCredential = null
                        showAddCredential = true
                    } else {
                        AppLifecycleManager.isLaunchingSystemPicker = true
                        filePickerLauncher.launch(arrayOf("*/*"))
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
                Icon(
                    if (selectedTab == 0) Icons.Rounded.Add else Icons.Rounded.FileUpload,
                    contentDescription = "Add",
                    modifier = Modifier.size(32.dp)
                )
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        isMultiSelectMode = false
                        selectedFileIds = emptySet()
                    },
                    label = { Text("Passwords (${data.credentials.size})") },
                    leadingIcon = { Icon(Icons.Rounded.Key, null, Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Files (${data.files.size})") },
                    leadingIcon = { Icon(Icons.Rounded.Folder, null, Modifier.size(18.dp)) }
                )
            }

            AnimatedContent(targetState = selectedTab, label = "vault_tab") { tab ->
                when (tab) {
                    0 -> CredentialsTab(
                        credentials = data.credentials,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        onEdit = { cred ->
                            editingCredential = cred
                            showAddCredential = true
                        },
                        onDelete = { cred -> credentialToDelete = cred },
                        context = context
                    )
                    1 -> FilesTab(
                        files = data.files,
                        deleteOriginal = deleteOriginalOnImport,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedFileIds = selectedFileIds,
                        onToggleDeleteOriginal = { deleteOriginalOnImport = it },
                        onToggleSelectMode = { isMultiSelectMode = it },
                        onToggleSelectFile = { id ->
                            selectedFileIds = if (id in selectedFileIds) selectedFileIds - id else selectedFileIds + id
                        },
                        onSelectAll = {
                            selectedFileIds = if (selectedFileIds.size == data.files.size) emptySet() else data.files.map { it.id }.toSet()
                        },
                        onExportSelected = {
                            if (selectedFileIds.isNotEmpty()) {
                                showExportOptionsModal = true
                            }
                        },
                        onDeleteSelected = {
                            val entries = data.files.filter { it.id in selectedFileIds }
                            viewModel.deleteFiles(entries)
                            selectedFileIds = emptySet()
                            isMultiSelectMode = false
                        },
                        onOpenFile = { entry -> openOriginalFile(entry) },
                        onExportSingle = { entry ->
                            selectedFileIds = setOf(entry.id)
                            showExportOptionsModal = true
                        },
                        onDeleteSingle = { entry -> fileToDelete = entry }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Modals (Item 5)
    credentialToDelete?.let { cred ->
        AlertDialog(
            onDismissRequest = { credentialToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Password") },
            text = { Text("Are you sure you want to delete '${cred.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCredential(cred.id)
                        credentialToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { credentialToDelete = null }) { Text("Cancel") }
            }
        )
    }

    fileToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Encrypted File") },
            text = { Text("Are you sure you want to securely wipe '${entry.originalName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(entry)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Add/Edit Credential Sheet
    RootAppDialog {
        AddCredentialSheet(
            visible = showAddCredential,
            credentialToEdit = editingCredential,
            onDismiss = { showAddCredential = false },
            onSave = { title, username, password, category, notes ->
                if (editingCredential != null) {
                    viewModel.updateCredential(editingCredential!!.id, title, username, password, category, notes)
                } else {
                    viewModel.addCredential(title, username, password, category, notes)
                }
                showAddCredential = false
            }
        )
    }

    // Export Options Sheet (Encrypted vs Decrypted)
    if (showExportOptionsModal) {
        RootAppDialog {
            CustomBottomSheet(
                visible = showExportOptionsModal,
                onDismiss = { showExportOptionsModal = false }
            ) {
                val selectedCount = selectedFileIds.size
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Export $selectedCount Selected File(s)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose export security format. You will select the save destination folder next.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Encrypted (.enc)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().clickable {
                            startSafExport(asEncrypted = true)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Encrypted (.enc)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Keep privacy protection intact. Select save destination.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Option 2: Decrypted (Raw file) with Auth + SAF Picker
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().clickable {
                            triggerDecryptedExportWithAuth()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Rounded.LockOpen, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Decrypted / Original", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Unencrypted raw format. Requires PIN verification then pick save folder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // PIN Re-Auth dialog fallback
    if (showPinReauthModal) {
        RootAppDialog {
            LockScreen(
                mode = LockMode.REAUTH,
                onUnlocked = {
                    showPinReauthModal = false
                    startSafExport(asEncrypted = false)
                },
                onCancel = { showPinReauthModal = false }
            )
        }
    }
}

@Composable
private fun CredentialsTab(
    credentials: List<CredentialItem>,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onEdit: (CredentialItem) -> Unit,
    onDelete: (CredentialItem) -> Unit,
    context: Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (categories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = selectedCategory == null, onClick = { onCategorySelected(null) },
                        label = { Text("All") })
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

        val filtered = if (selectedCategory != null) credentials.filter { it.category == selectedCategory } else credentials

        if (filtered.isEmpty()) {
            EmptyVaultState("No passwords saved", Icons.Rounded.Key)
        } else {
            ExpressiveList(
                content = filtered.map { cred ->
                    { CredentialRow(cred, onEdit, onDelete, context) }
                }
            )
        }
    }
}

@Composable
private fun CredentialRow(cred: CredentialItem, onEdit: (CredentialItem) -> Unit, onDelete: (CredentialItem) -> Unit, context: Context) {
    var showPassword by remember { mutableStateOf(false) }
    var showReauthForPassword by remember { mutableStateOf(false) }

    var showReauthForEdit by remember { mutableStateOf(false) }

    ExpressiveListItem(
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("password", cred.password)
            if (Build.VERSION.SDK_INT >= 33) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
        },
        onLongClick = { onDelete(cred) },
        headlineContent = {
            Text(
                text = cred.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                if (cred.username.isNotBlank()) Text("User: ${cred.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (cred.category.isNotBlank()) Text("Category: ${cred.category}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (showPassword) cred.password else "••••••••",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            if (showPassword) {
                                showPassword = false
                            } else {
                                showReauthForPassword = true
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        leadingContent = {
            Icon(Icons.Rounded.Key, null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showReauthForEdit = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Edit, "Edit", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDelete(cred) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Close, "Delete", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )

    if (showReauthForPassword) {
        RootAppDialog {
            LockScreen(
                mode = LockMode.REAUTH,
                onUnlocked = {
                    showReauthForPassword = false
                    showPassword = true
                },
                onCancel = { showReauthForPassword = false }
            )
        }
    }

    if (showReauthForEdit) {
        RootAppDialog {
            LockScreen(
                mode = LockMode.REAUTH,
                onUnlocked = {
                    showReauthForEdit = false
                    onEdit(cred)
                },
                onCancel = { showReauthForEdit = false }
            )
        }
    }
}

@Composable
private fun FilesTab(
    files: List<VaultFileEntry>,
    deleteOriginal: Boolean,
    isMultiSelectMode: Boolean,
    selectedFileIds: Set<String>,
    onToggleDeleteOriginal: (Boolean) -> Unit,
    onToggleSelectMode: (Boolean) -> Unit,
    onToggleSelectFile: (String) -> Unit,
    onSelectAll: () -> Unit,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenFile: (VaultFileEntry) -> Unit,
    onExportSingle: (VaultFileEntry) -> Unit,
    onDeleteSingle: (VaultFileEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Delete original after import", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = deleteOriginal, onCheckedChange = onToggleDeleteOriginal)
        }

        // Multi-select toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isMultiSelectMode,
                    onClick = { onToggleSelectMode(!isMultiSelectMode) },
                    label = { Text(if (isMultiSelectMode) "Done" else "Select Files") },
                    leadingIcon = { Icon(if (isMultiSelectMode) Icons.Rounded.Check else Icons.Rounded.Checklist, null, Modifier.size(16.dp)) }
                )
                if (isMultiSelectMode) {
                    TextButton(onClick = onSelectAll) {
                        Text(if (selectedFileIds.size == files.size) "Deselect All" else "Select All")
                    }
                }
            }

            if (isMultiSelectMode && selectedFileIds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onExportSelected,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.FileUpload, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export (${selectedFileIds.size})")
                    }
                    IconButton(onClick = onDeleteSelected, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Delete, "Delete selected", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (files.isEmpty()) {
            EmptyVaultState("No files in vault", Icons.Rounded.Folder)
        } else {
            ExpressiveList(
                content = files.sortedByDescending { it.createdAt }.map { entry ->
                    val isSelected = entry.id in selectedFileIds
                    {
                        ExpressiveListItem(
                            onClick = {
                                if (isMultiSelectMode) {
                                    onToggleSelectFile(entry.id)
                                } else {
                                    onOpenFile(entry)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    onToggleSelectMode(true)
                                    onToggleSelectFile(entry.id)
                                } else {
                                    onDeleteSingle(entry)
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = entry.originalName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(formatFileSize(entry.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            leadingContent = {
                                if (isMultiSelectMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSelectFile(entry.id) }
                                    )
                                } else {
                                    Icon(Icons.Rounded.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            trailingContent = {
                                if (!isMultiSelectMode) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { onExportSingle(entry) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.FileDownload, "Export", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onDeleteSingle(entry) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Close, "Delete", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes bytes"
    }
}

@Composable
private fun EmptyVaultState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun AddCredentialSheet(
    visible: Boolean,
    credentialToEdit: CredentialItem? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible, credentialToEdit) {
        if (visible) {
            isPasswordVisible = false
            if (credentialToEdit != null) {
                title = credentialToEdit.title
                username = credentialToEdit.username
                password = credentialToEdit.password
                category = credentialToEdit.category
                notes = credentialToEdit.notes
            } else {
                title = ""
                username = ""
                password = ""
                category = ""
                notes = ""
            }
        }
    }

    CustomBottomSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            text = if (credentialToEdit != null) "Edit Password" else "New Password",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Title", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Username / Email", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
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

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (title.isNotBlank() && password.isNotBlank()) onSave(title.trim(), username.trim(), password, category.trim(), notes.trim()) },
            enabled = title.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp)
        ) {
            Text(if (credentialToEdit != null) "Save Changes" else "Save Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}
