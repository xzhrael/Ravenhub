/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ravenhub.app.ui.mainscreens


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.ui.platform.LocalUriHandler
import com.ravenhub.app.ui.util.UpdateCheckerUtil
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.json.JSONObject
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.BuildConfig
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.util.*
import androidx.compose.foundation.clickable

fun isLauncherIconEnabled(context: Context): Boolean {
    return try {
        val pkg = context.packageManager
        val component = ComponentName(context.packageName, "${context.packageName}.Launcher")
        val state = pkg.getComponentEnabledSetting(component)
        state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    } catch (_: Exception) {
        true
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    
    var showBackupRestoreSheet by remember { mutableStateOf(false) }
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var isCloudBackup by remember { mutableStateOf(false) }
    var optBackupPlanner by remember { mutableStateOf(true) }
    var optBackupFinance by remember { mutableStateOf(true) }
    var optBackupVault by remember { mutableStateOf(true) }
    var optBackupNotes by remember { mutableStateOf(true) }

    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var showRestorePinPrompt by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }
    var showAutoLockSheet by remember { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        com.ravenhub.app.ui.util.WallpaperCache.init(context)
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                changelogText = context.assets.open("changelog.md").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                changelogText = "Failed to load changelog\n${e.message}"
            }
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog(onConfirm = {}, onDismiss = {})
    val uninstallDialog = rememberConfirmDialog(
        onConfirm = {
            try {
                Shell.cmd("sh /data/adb/modules/ravencore/uninstall.sh").submit()
            } catch (_: Exception) {}
        },
        onDismiss = {}
    )
    
    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            coroutineScope.launch {
                val success = loadingDialog.withLoading {
                    val props = mutableMapOf<String, String>()
                    if (optBackupPlanner) props["planner"] = "true"
                    if (optBackupFinance) props["finance"] = "true"
                    if (optBackupVault) props["vault"] = "true"
                    if (optBackupNotes) props["notes"] = "true"
                    com.ravenhub.app.ui.util.BackupManager.createBackup(context, it, props)
                }
                if (success) {
                    snackbarHostState.showSnackbar("Backup created successfully!")
                } else {
                    snackbarHostState.showSnackbar("Failed to create backup")
                }
            }
        }
    }
    
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            showBackupRestoreSheet = false
            coroutineScope.launch {
                loadingDialog.withLoading {
                    val data = com.ravenhub.app.ui.util.BackupManager.readBackup(context, it)
                    if (data != null) {
                        snackbarHostState.showSnackbar("Backup data restored successfully!")
                    } else {
                        confirmDialog.showConfirm("Restore Failed", "Failed to parse backup archive", "OK", null)
                    }
                }
            }
        }
    }

    var showBackupRestoreBottomSheet by remember { mutableStateOf(false) }
    var showBackupModuleModal by remember { mutableStateOf(false) }
    var selPlanner by remember { mutableStateOf(true) }
    var selFinance by remember { mutableStateOf(true) }
    var selVault by remember { mutableStateOf(true) }
    var selNotes by remember { mutableStateOf(true) }

    val createFullBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { destinationUri ->
            coroutineScope.launch {
                val backupFile = com.ravenhub.app.backup.BackupManager.createBackupZip(
                    context, selPlanner, selFinance, selVault, selNotes
                )
                if (backupFile != null && backupFile.exists()) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                        backupFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    snackbarHostState.showSnackbar("Backup created & exported successfully!")
                } else {
                    snackbarHostState.showSnackbar("Failed to create backup zip.")
                }
            }
        }
    }

    val restoreFullBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { sourceUri ->
            coroutineScope.launch {
                val tempZip = java.io.File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}.zip")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempZip.outputStream().use { out -> input.copyTo(out) }
                }
                pendingRestoreFile = tempZip
                showRestorePinPrompt = true
            }
        }
    }

    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalAppHazeState.current

    MaterialExpressiveTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { 
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(
                            bottom = 100.dp
                        )
                    ) 
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { innerPadding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    item {
                        Spacer(modifier = Modifier.statusBarsPadding().height(52.dp)) 
                        
                        ExpressiveList(
                            content = listOf(
                                { AppInfoHeaderContent() },
                                {
                                    ExpressiveListItem(
                                        onClick = { navController.navigate("color_palette") },
                                        headlineContent = { Text("Theme") },
                                        supportingContent = { Text("Customize dynamic accent colors and dark mode") },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Palette) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Outlined.SettingsBackupRestore) },
                                        onClick = { showBackupRestoreBottomSheet = true },
                                        headlineContent = { Text("Backup & Restore") },
                                        supportingContent = { Text("Backup or restore encrypted RavenHub app data") },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    var autoLockMode by remember {
                                        mutableStateOf(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("auto_lock_mode", "exit") ?: "exit")
                                    }
                                    ExpressiveListItem(
                                        onClick = { showAutoLockSheet = true },
                                        headlineContent = { Text("Auto Lock Security") },
                                        supportingContent = {
                                            Text(
                                                when (autoLockMode) {
                                                    "delay_10s" -> "Auto lock after 10 seconds"
                                                    "kill" -> "Auto lock on app kill only"
                                                    "screen_lock" -> "Auto lock on device screen lock"
                                                    else -> "Auto lock immediately on app exit"
                                                }
                                            )
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Security) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = { showChangePinDialog = true },
                                        headlineContent = { Text("Change Security PIN") },
                                        supportingContent = { Text("Update your 4-8 digit master security PIN") },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Key) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = { showChangelogSheet = true },
                                        headlineContent = { Text("Changelog") },
                                        supportingContent = { Text("View application changelog history") },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.History) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                }
                            )
                        )
                    }
                    item { SettingsSectionTitle("Updates & Version") }
                    item {
                        val updateResult by UpdateCheckerUtil.updateState
                        val uriHandler = LocalUriHandler.current

                        LaunchedEffect(Unit) {
                            UpdateCheckerUtil.checkUpdate(context)
                        }

                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    onClick = {
                                        if (updateResult.isUpdateAvailable) {
                                            uriHandler.openUri(updateResult.downloadUrl)
                                        } else {
                                            coroutineScope.launch {
                                                UpdateCheckerUtil.checkUpdate(context, force = true)
                                                if (updateResult.errorMessage != null) {
                                                    snackbarHostState.showSnackbar(updateResult.errorMessage!!)
                                                } else if (!updateResult.isUpdateAvailable) {
                                                    snackbarHostState.showSnackbar("RavenHub is up to date (v${BuildConfig.VERSION_NAME})")
                                                }
                                            }
                                        }
                                    },
                                    headlineContent = {
                                        Text(
                                            if (updateResult.isUpdateAvailable) "Update Available (${updateResult.latestVersion})"
                                            else "Check for Updates",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            if (updateResult.isChecking) "Checking GitHub for updates..."
                                            else if (updateResult.isUpdateAvailable) "New release found! Tap to download from GitHub"
                                            else if (updateResult.errorMessage != null) "Current version v${BuildConfig.VERSION_NAME} (Offline check)"
                                            else "Current version v${BuildConfig.VERSION_NAME} (Latest)"
                                        )
                                    },
                                    leadingContent = {
                                        LeadingIcon(icon = if (updateResult.isUpdateAvailable) Icons.Filled.SystemUpdate else Icons.Filled.Sync)
                                    },
                                    trailingContent = {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                    }
                                )
                            }
                        )
                    }

                    item { SettingsSectionTitle("About") }
                    item {
                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    onClick = { uriHandler.openUri("https://github.com/xzhrael/Ravenhub") }, 
                                    headlineContent = { Text("About RavenHub") },
                                    supportingContent = {
                                        Text("Version ${BuildConfig.VERSION_NAME}")
                                    },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.ContactPage) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    }
                }
            }
            

            LoadingDialogHost(handle = loadingDialog)
            ConfirmDialogHost(handle = uninstallDialog)
            
            RootAppDialog {
                CustomBottomSheet(
                    visible = showAutoLockSheet,
                    onDismiss = { showAutoLockSheet = false }
                ) {
                    var autoLockMode by remember {
                        mutableStateOf(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("auto_lock_mode", "exit") ?: "exit")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                    ) {
                        Text(
                            text = "Auto Lock Security",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )

                        val options = listOf(
                            Triple("exit", "On App Exit", "Lock master key immediately when app leaves foreground"),
                            Triple("delay_10s", "In 10 Seconds", "Delay locking by 10s (allows SAF file pickers without re-lock)"),
                            Triple("kill", "On App Kill", "Lock key only when app is completely closed/killed"),
                            Triple("screen_lock", "On Device Screen Lock", "Lock key when phone screen turns off")
                        )

                        ExpressiveList(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            content = options.map { (modeKey, label, desc) ->
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                                                .edit().putString("auto_lock_mode", modeKey).apply()
                                            autoLockMode = modeKey
                                            showAutoLockSheet = false
                                        },
                                        headlineContent = { Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                        supportingContent = { Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingContent = {
                                            RadioButton(
                                                selected = autoLockMode == modeKey,
                                                onClick = {
                                                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                                                        .edit().putString("auto_lock_mode", modeKey).apply()
                                                    autoLockMode = modeKey
                                                    showAutoLockSheet = false
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (showChangePinDialog) {
                RootAppDialog {
                    com.ravenhub.app.ui.security.LockScreen(
                        mode = com.ravenhub.app.ui.security.LockMode.CHANGE_PIN,
                        onUnlocked = { showChangePinDialog = false },
                        onCancel = { showChangePinDialog = false }
                    )
                }
            }

            if (showRestorePinPrompt && pendingRestoreFile != null) {
                RootAppDialog {
                    var pinInput by remember { mutableStateOf("") }
                    var pinError by remember { mutableStateOf<String?>(null) }

                    AlertDialog(
                        onDismissRequest = {
                            showRestorePinPrompt = false
                            pendingRestoreFile?.delete()
                            pendingRestoreFile = null
                        },
                        icon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text("Backup Security PIN Required", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Enter the 4-8 digit Security PIN used when this backup archive was created:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { pinInput = it; pinError = null },
                                    label = { Text("Backup Security PIN") },
                                    singleLine = true,
                                    isError = pinError != null,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (pinError != null) {
                                    Text(
                                        text = pinError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val fileToRestore = pendingRestoreFile
                                    if (fileToRestore != null && fileToRestore.exists()) {
                                        coroutineScope.launch {
                                            val isPinValid = com.ravenhub.app.backup.BackupManager.verifyBackupZipPin(context, fileToRestore, pinInput)
                                            if (isPinValid) {
                                                val success = com.ravenhub.app.backup.BackupManager.restoreBackupZip(context, fileToRestore)
                                                fileToRestore.delete()
                                                pendingRestoreFile = null
                                                showRestorePinPrompt = false
                                                if (success) {
                                                     com.ravenhub.app.data.planner.PlannerDataManager.load(context)
                                                     com.ravenhub.app.data.notes.NotesDataManager.load(context)
                                                     com.ravenhub.app.data.vault.VaultDataManager.load(context)
                                                     com.ravenhub.app.data.finance.FinanceDataManager.load(context)
                                                    snackbarHostState.showSnackbar("Backup restored successfully!")
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to unpack backup archive.")
                                                }
                                            } else {
                                                pinError = "Incorrect Backup PIN. Restore aborted."
                                            }
                                        }
                                    }
                                },
                                enabled = pinInput.length in 4..8
                            ) {
                                Text("Verify & Restore")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = {
                                    showRestorePinPrompt = false
                                    pendingRestoreFile?.delete()
                                    pendingRestoreFile = null
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            RootAppDialog {
                CustomContentDialog(
                    visible = showRestoreDialog,
                    title = "Restore Backup Data",
                    confirmText = "Restore Data",
                    confirmEnabled = true,
                    onDismiss = { showRestoreDialog = false },
                    onConfirm = {
                        showRestoreDialog = false
                        Toast.makeText(context, "Backup configuration restored", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Column {
                        Text(
                            text = "Restore all encrypted RavenHub modules and settings?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            RootAppDialog {
                CustomBottomSheet(
                    visible = showChangelogSheet,
                    onDismiss = { showChangelogSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f) 
                    ) {
                        Text(
                            text = "Changelog",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)
                        )
            
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
            
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))                            
                            MarkdownText(
                                markdown = changelogText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth() 
                            )
                            Spacer(
                                modifier = Modifier.height(
                                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 64.dp
                                )
                            )
                        }
                    }
                }
            }

            RootAppDialog {
                if (showBackupRestoreBottomSheet) {
                    com.ravenhub.app.ui.component.CustomBottomSheet(
                        visible = showBackupRestoreBottomSheet,
                        onDismiss = { showBackupRestoreBottomSheet = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                        ) {
                            Text(
                                text = "Backup & Restore Data",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )

                            ExpressiveList(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                content = listOf(
                                    {
                                        ExpressiveListItem(
                                            onClick = {
                                                isCloudBackup = false
                                                showBackupRestoreBottomSheet = false
                                                showBackupModuleModal = true
                                            },
                                            headlineContent = { Text("Backup Local Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                            supportingContent = { Text("Export encrypted backup file to local device storage", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.FileUpload) },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            onClick = {
                                                isCloudBackup = true
                                                showBackupRestoreBottomSheet = false
                                                showBackupModuleModal = true
                                            },
                                            headlineContent = { Text("Backup to Cloud Storage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                            supportingContent = { Text("Export encrypted backup to Google Drive, OneDrive, or Cloud SAF", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.CloudUpload) },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            onClick = {
                                                showBackupRestoreBottomSheet = false
                                                com.ravenhub.app.ui.util.AppLifecycleManager.isLaunchingSystemPicker = true
                                                restoreFullBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                            },
                                            headlineContent = { Text("Restore Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                            supportingContent = { Text("Import backup archive from Local or Cloud Storage into RavenHub", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.FileDownload) },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
            }

            RootAppDialog {
                if (showBackupModuleModal) {
                    com.ravenhub.app.ui.component.CustomBottomSheet(
                        visible = showBackupModuleModal,
                        onDismiss = { showBackupModuleModal = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                        ) {
                            Text(
                                text = if (isCloudBackup) "Select Modules for Cloud Backup" else "Select Modules for Local Backup",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )

                            ExpressiveList(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                content = listOf(
                                    {
                                        ExpressiveListItem(
                                            onClick = { selPlanner = !selPlanner },
                                            headlineContent = { Text("Planner", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                            supportingContent = { Text("Export todos, sub-task checklist trees, and habits", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.Checklist) },
                                            trailingContent = {
                                                Checkbox(
                                                    checked = selPlanner,
                                                    onCheckedChange = { selPlanner = it }
                                                )
                                            }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            onClick = { selFinance = !selFinance },
                                            headlineContent = { Text("Finance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                            supportingContent = { Text("Export income, expense records, and analytics history", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.AccountBalanceWallet) },
                                            trailingContent = {
                                                Checkbox(
                                                    checked = selFinance,
                                                    onCheckedChange = { selFinance = it }
                                                )
                                            }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            onClick = { selVault = !selVault },
                                            headlineContent = { Text("Vault", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                            supportingContent = { Text("Export passwords, credentials, and encrypted file vault", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.Lock) },
                                            trailingContent = {
                                                Checkbox(
                                                    checked = selVault,
                                                    onCheckedChange = { selVault = it }
                                                )
                                            }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            onClick = { selNotes = !selNotes },
                                            headlineContent = { Text("Notes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                            supportingContent = { Text("Export categorized markdown notes & documentation", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.Description) },
                                            trailingContent = {
                                                Checkbox(
                                                    checked = selNotes,
                                                    onCheckedChange = { selNotes = it }
                                                )
                                            }
                                        )
                                    }
                                )
                            )

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    showBackupModuleModal = false
                                    if (isCloudBackup) {
                                        coroutineScope.launch {
                                            loadingDialog.withLoading {
                                                val backupFile = com.ravenhub.app.backup.BackupManager.createBackupZip(
                                                    context, selPlanner, selFinance, selVault, selNotes
                                                )
                                                if (backupFile != null && backupFile.exists()) {
                                                    try {
                                                        com.ravenhub.app.ui.util.AppLifecycleManager.isLaunchingSystemPicker = true
                                                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.provider",
                                                            backupFile
                                                        )
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "application/zip"
                                                            putExtra(Intent.EXTRA_STREAM, fileUri)
                                                            putExtra(Intent.EXTRA_SUBJECT, "RavenHub Encrypted Cloud Backup")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        val chooserIntent = Intent.createChooser(shareIntent, "Share / Backup to Cloud Storage")
                                                        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(chooserIntent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        snackbarHostState.showSnackbar("Failed to launch cloud share: ${e.message}")
                                                    }
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to create backup zip.")
                                                }
                                            }
                                        }
                                    } else {
                                        com.ravenhub.app.ui.util.AppLifecycleManager.isLaunchingSystemPicker = true
                                        createFullBackupLauncher.launch("ravenhub_local_backup_${System.currentTimeMillis()}.zip")
                                    }
                                },
                                enabled = selPlanner || selFinance || selVault || selNotes,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(56.dp)
                            ) {
                                Icon(if (isCloudBackup) Icons.Rounded.CloudUpload else Icons.Rounded.FileUpload, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isCloudBackup) "Export & Share to Cloud App" else "Save to Local Storage",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun SettingsScreenTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onChangelogClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val smoothGradient = Brush.verticalGradient(
        0.0f to colorScheme.surface,
        0.4f to colorScheme.surface.copy(alpha = 0.9f),
        0.5f to colorScheme.surface.copy(alpha = 0.8f),
        0.6f to colorScheme.surface.copy(alpha = 0.7f),
        0.7f to colorScheme.surface.copy(alpha = 0.5f),
        0.8f to colorScheme.surface.copy(alpha = 0.4f),
        0.9f to colorScheme.surface.copy(alpha = 0.3f),
        1.0f to Color.Transparent 
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(smoothGradient)
            .statusBarsPadding()
    ) {
        LargeFlexibleTopAppBar(
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            title = {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onChangelogClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TextSnippet,
                        contentDescription = "Changelog"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
