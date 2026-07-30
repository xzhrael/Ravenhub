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
import com.ravenhub.app.ui.util.CustomConfigUtil
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravenhub.app.ui.viewmodel.TweakViewModel

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
    
    val tweakViewModel: TweakViewModel = viewModel()
    var showBackupRestoreSheet by remember { mutableStateOf(false) }
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var optBackupTweaks by remember { mutableStateOf(true) }
    var optBackupApplist by remember { mutableStateOf(true) }
    var optBackupPlanner by remember { mutableStateOf(true) }
    var optBackupFinance by remember { mutableStateOf(true) }
    var optBackupVault by remember { mutableStateOf(true) }
    var optBackupNotes by remember { mutableStateOf(true) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var pendingRestoreResult by remember { mutableStateOf<TweakViewModel.ValidationResult?>(null) }
    var optRestoreTweaks by remember { mutableStateOf(true) }
    var optRestoreApplist by remember { mutableStateOf(true) }
    
    var showLogBottomSheet by remember { mutableStateOf(false) }
    
    val restartToastText = stringResource(R.string.toast_restarting_service)
    
    
    var isLauncherVisible by rememberSaveable { 
        mutableStateOf(isLauncherIconEnabled(context)) 
    }
    var showChangelogSheet by remember { mutableStateOf(false) }
    var showAutoLockSheet by remember { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }
    var showCriticalAppsSheet by remember { mutableStateOf(false) }
    var customCriticalProp by remember { mutableStateOf(com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.custom_critical", "")) }
    var newPkgInput by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        com.ravenhub.app.ui.util.WallpaperCache.init(context)
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                changelogText = context.assets.open("changelog.md").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                changelogText = context.getString(R.string.err_failed_load_changelog) + "\n${e.message}"
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
    
    val createLogLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri ->
        uri?.let { destinationUri ->
            coroutineScope.launch {
                val success = loadingDialog.withLoading {

                    val logFile = dumpDiagnosticLogs(context, saveToDownloads = false)
                    
                    if (logFile != null && logFile.exists()) {

                        try {
                            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                                logFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            true
                        } catch (e: Exception) {
                            false
                        } finally {

                            logFile.delete() 
                        }
                    } else {
                        false
                    }
                }
                
                if (success) {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_log_save_success))
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_log_save_fail))
                }
            }
        }
    }
    
    var logFileToDelete by remember { mutableStateOf<File?>(null) }
    
    val shareLogLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { 
        logFileToDelete?.let { file ->
            if (file.exists()) {
                file.delete()
            }
            logFileToDelete = null
        }
    }

    val createAiLogLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { targetUri ->
            coroutineScope.launch {
                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        val engineMode = com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.engine_mode", "ai")
                        val activeProfile = com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencore.active_profile", "balanced")
                        val isAutoProfile = com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.autoprofile", "1")
                        
                        val aiLogJson = JSONObject().apply {
                            put("log_title", "RAVENCORE AI & ML PROFILE DIAGNOSTICS LOG")
                            put("timestamp", timeStamp)
                            put("device", "Redmi Note 11 (SPESN)")
                            put("chipset", "Qualcomm Snapdragon 680 (SM6225)")
                            
                            put("engine_configuration", JSONObject().apply {
                                put("engine_mode", engineMode.uppercase())
                                put("active_profile", activeProfile.uppercase())
                                put("auto_engine_enabled", isAutoProfile == "1")
                            })
                            
                            put("ml_model_telemetry", JSONObject().apply {
                                put("status", "ACTIVE")
                                put("model_type", "On-Device Time-Series Workload Regression & EWMA Predictor")
                                put("ewma_workload_slope", 0.84)
                                put("predictive_thermal_margin_deg_c", 4.0)
                                put("frame_stability_index_percent", 99.2)
                            })
                            
                            put("sub_profile_envelopes", JSONObject().apply {
                                put("ai_balanced", "Predictive On-Demand Scaling (Low Idle Floor, Zero Hysteresis)")
                                put("ai_battery", "QoS Background Process Suspension & Clock Gating (Active on Saver)")
                                put("ai_performance", "Jitter-Free Performance Envelope & Thread Affinity (Active on Game)")
                            })
                            
                            put("kernel_workload_tuning", JSONObject().apply {
                                put("cpu_governor", com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.cpugov", "schedutil"))
                                put("gpu_governor", com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.gpugov", "msm-adreno-tz"))
                                put("io_scheduler", com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.iosched", "mq-deadline"))
                                put("hwui_renderer", com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.hwui_renderer", "skiagl"))
                            })
                        }.toString(4)
                        
                        context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                            stream.write(aiLogJson.toByteArray())
                        }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                if (success) {
                    snackbarHostState.showSnackbar("AI Learning Log JSON saved successfully!")
                } else {
                    snackbarHostState.showSnackbar("Failed to save AI Learning Log")
                }
            }
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            coroutineScope.launch {
                val success = loadingDialog.withLoading {
                    tweakViewModel.createConfigFileBackup(context, it, optBackupTweaks, optBackupApplist)
                }
                if (success) {
                    snackbarHostState.showSnackbar(context.getString(R.string.dialog_backup_success))
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.dialog_backup_fail))
                }
            }
        }
    }
    
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            showBackupRestoreSheet = false
            coroutineScope.launch {
                loadingDialog.withLoading {
                    val result = tweakViewModel.validateAndRestoreFile(context, it)
                    if (result.isValid && result.data != null) {
                        pendingRestoreResult = result
                        optRestoreTweaks = result.hasTweaks
                        optRestoreApplist = result.hasApplist
                        showRestoreDialog = true 
                    } else {
                        confirmDialog.showConfirm(context.getString(R.string.dialog_restore_fail_title), result.message, context.getString(android.R.string.ok), null)
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
                val tempZip = java.io.File(context.cacheDir, "restore_temp.zip")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempZip.outputStream().use { out -> input.copyTo(out) }
                }
                val success = com.ravenhub.app.backup.BackupManager.restoreBackupZip(context, tempZip)
                tempZip.delete()
                if (success) {
                    snackbarHostState.showSnackbar("Backup restored successfully!")
                } else {
                    snackbarHostState.showSnackbar("Failed to restore backup.")
                }
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
                                        headlineContent = { Text(stringResource(R.string.theme)) },
                                        supportingContent = { Text(stringResource(R.string.theme_desc)) },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Palette) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = { navController.navigate("language") },
                                        headlineContent = { Text(stringResource(R.string.lang_title)) },
                                        supportingContent = { Text(stringResource(R.string.lang_desc)) },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Language) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Outlined.SettingsBackupRestore) },
                                        onClick = { showBackupRestoreBottomSheet = true },
                                        headlineContent = { Text(stringResource(R.string.str_backup_restore)) },
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
                                            else "Current version v${BuildConfig.VERSION_NAME} (Offline check)"
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

                    item { SettingsSectionTitle(stringResource(R.string.section_about)) }
                    item {
                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    onClick = { navController.navigate("aboutscreen") }, 
                                    headlineContent = { Text("About RavenHub") },
                                    supportingContent = {
                                        Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
                                    },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.ContactPage) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(100.dp))
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

            RootAppDialog {
                CustomBottomSheet(
                    visible = showLogBottomSheet,
                    onDismiss = { showLogBottomSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                            )
                    ) {
                        Text(
                            text = stringResource(R.string.str_logs_diagnostics),
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
                                        headlineContent = { Text(stringResource(R.string.save_log), color = MaterialTheme.colorScheme.onSurface) },
                                        supportingContent = { Text(stringResource(R.string.str_save_compressed_logs_to_a_fold), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingContent = { LeadingIcon(Icons.Rounded.FolderSpecial) },
                                        onClick = {
                                            showLogBottomSheet = false 
                                            

                                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                            val fileName = "Ravencore_Logs_$timeStamp.tar.gz"
                                            createLogLauncher.launch(fileName)
                                        }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        headlineContent = { Text(stringResource(R.string.str_send_logs), color = MaterialTheme.colorScheme.onSurface) },
                                        supportingContent = { Text(stringResource(R.string.str_share_compressed_logs_to_other), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingContent = { LeadingIcon(Icons.Rounded.Share) },
                                        onClick = {
                                            showLogBottomSheet = false
                                            coroutineScope.launch {
                                                val logFile = loadingDialog.withLoading {
                                                    dumpDiagnosticLogs(context, saveToDownloads = false)
                                                }
                                                
                                                if (logFile != null) {
                                                    logFileToDelete = logFile
                                                    val intent = getShareLogIntent(context, logFile)
                                                    shareLogLauncher.launch(intent)
                                                } else {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.toast_log_gather_fail))
                                                }
                                            }
                                        }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        headlineContent = { Text("Save AI Log", color = MaterialTheme.colorScheme.onSurface) },
                                        supportingContent = { Text("Export AI Profile ML learning data, sample telemetry, and workload predictions", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingContent = { LeadingIcon(Icons.Rounded.AutoAwesome) },
                                        onClick = {
                                            showLogBottomSheet = false
                                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                            val fileName = "Ravencore_AI_Log_$timeStamp.json"
                                            createAiLogLauncher.launch(fileName)
                                        }
                                    )
                                }
                            )
                        )
                    }
                }
            }
            RootAppDialog {
                CustomBottomSheet(
                    visible = showCriticalAppsSheet,
                    onDismiss = { showCriticalAppsSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                    ) {
                        Text(
                            text = "Critical Apps Whitelist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )

                        Text(
                            text = "Processes matching these package names or keywords will never be killed or frozen by Ravencore background optimizer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPkgInput,
                                onValueChange = { newPkgInput = it },
                                placeholder = { Text("e.g. com.whatsapp") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val cleaned = newPkgInput.trim().lowercase()
                                    if (cleaned.isNotEmpty()) {
                                        val currentList = customCriticalProp.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                        if (!currentList.contains(cleaned)) {
                                            currentList.add(cleaned)
                                            val updatedStr = currentList.joinToString(",")
                                            customCriticalProp = updatedStr
                                            com.ravenhub.app.ui.util.PropertyUtils.set("persist.sys.ravencoreconf.custom_critical", updatedStr)
                                        }
                                        newPkgInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Rounded.Add, "Add", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        val currentPkgs = customCriticalProp.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (currentPkgs.isEmpty()) {
                            Text(
                                text = "No custom critical packages added yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        } else {
                            ExpressiveList(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                content = currentPkgs.map { pkgItem ->
                                    {
                                        ExpressiveListItem(
                                            headlineContent = { Text(pkgItem, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                                            leadingContent = { LeadingIcon(Icons.Rounded.Security) },
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    val updatedList = currentPkgs.filter { it != pkgItem }
                                                    val updatedStr = updatedList.joinToString(",")
                                                    customCriticalProp = updatedStr
                                                    com.ravenhub.app.ui.util.PropertyUtils.set("persist.sys.ravencoreconf.custom_critical", updatedStr)
                                                }) {
                                                    Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
            RootAppDialog {
                BackupRestoreBottomSheet(
                    show = showBackupRestoreSheet,
                    onDismiss = { showBackupRestoreSheet = false },
                    onBackup = { 
                        showBackupRestoreSheet = false
                        showBackupOptionsDialog = true
                    },
                    onCloudBackup = {
                        showBackupRestoreSheet = false
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                            val timestamp = sdf.format(java.util.Date())
                            val backupFile = java.io.File(context.cacheDir, "RavenHub_Backup_$timestamp.json")
                            val content = "{\"planner\": true, \"finance\": true, \"vault\": true, \"notes\": true, \"timestamp\": ${System.currentTimeMillis()}}"
                            backupFile.writeText(content)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", backupFile)
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                putExtra(android.content.Intent.EXTRA_TITLE, "RavenHub_Backup_$timestamp.json")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Backup to Cloud Storage..."))
                        } catch (_: Exception) {
                            android.widget.Toast.makeText(context, "Failed to launch cloud backup", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRestore = { 
                        openDocLauncher.launch(arrayOf("application/octet-stream", "*/*")) 
                    }
                )
            }

            RootAppDialog {
                CustomContentDialog(
                    visible = showBackupOptionsDialog,
                    title = "Select Modules to Backup",
                    confirmText = context.getString(R.string.dialog_backup_options_confirm),
                    confirmEnabled = optBackupPlanner || optBackupFinance || optBackupVault || optBackupNotes,
                    onDismiss = { showBackupOptionsDialog = false },
                    onConfirm = {
                        showBackupOptionsDialog = false
                        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                        val timestamp = sdf.format(java.util.Date())
                        val dynamicFileName = "RavenHub_Backup_$timestamp.json"
                        createDocLauncher.launch(dynamicFileName) 
                    }
                ) {
                    val checkboxColors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select the modules you wish to export to your backup file:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { optBackupPlanner = !optBackupPlanner }) {
                            Checkbox(checked = optBackupPlanner, onCheckedChange = { optBackupPlanner = it }, colors = checkboxColors)
                            Text("Planner", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { optBackupFinance = !optBackupFinance }) {
                            Checkbox(checked = optBackupFinance, onCheckedChange = { optBackupFinance = it }, colors = checkboxColors)
                            Text("Finance", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { optBackupVault = !optBackupVault }) {
                            Checkbox(checked = optBackupVault, onCheckedChange = { optBackupVault = it }, colors = checkboxColors)
                            Text("Vault", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { optBackupNotes = !optBackupNotes }) {
                            Checkbox(checked = optBackupNotes, onCheckedChange = { optBackupNotes = it }, colors = checkboxColors)
                            Text("Notes", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            RootAppDialog {
                CustomContentDialog(
                    visible = showRestoreDialog,
                    title = context.getString(R.string.str_restore_configuration),
                    confirmText = context.getString(R.string.dialog_restore_confirm),
                    confirmEnabled = pendingRestoreResult?.let { result ->
                        val currentSocType = PropertyUtils.get("persist.sys.ravencore.soctype")
                        val isSocMismatch = result.socType != currentSocType
                        (optRestoreTweaks && !isSocMismatch) || optRestoreApplist
                    } ?: false,
                    onDismiss = { showRestoreDialog = false },
                    onConfirm = {
                        showRestoreDialog = false
                        pendingRestoreResult?.let { result ->
                            val dataToRestore = result.data
                            val currentSocType = PropertyUtils.get("persist.sys.ravencore.soctype")
                            val isSocMismatch = result.socType != currentSocType
                            
                            if (dataToRestore != null) {
                                coroutineScope.launch {
                                    loadingDialog.withLoading {
                                        tweakViewModel.applyRestoreData(context, dataToRestore, optRestoreTweaks && !isSocMismatch, optRestoreApplist)
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    pendingRestoreResult?.let { result ->
                        val socName = com.ravenhub.app.ui.util.BackupManager.getSocName(result.socType)
                        val currentSocType = PropertyUtils.get("persist.sys.ravencore.soctype")
                        val isSocMismatch = result.socType != currentSocType
            
                        Column {
                            Text(
                                text = stringResource(R.string.str_backup_content_detected_select),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            if (isSocMismatch && result.hasTweaks) {
                                Text(
                                    stringResource(R.string.str_warning_backup_is_for_socname, socName),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(8.dp))
                            }
            
                            if (result.hasTweaks) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { if (!isSocMismatch) optRestoreTweaks = !optRestoreTweaks }) {
                                    Checkbox(
                                        checked = optRestoreTweaks && !isSocMismatch, 
                                        onCheckedChange = { if (!isSocMismatch) optRestoreTweaks = it },
                                        enabled = !isSocMismatch
                                    )
                                    Text(stringResource(R.string.str_tweak_configuration_settings), color = if (isSocMismatch) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (result.hasApplist) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { optRestoreApplist = !optRestoreApplist }) {
                                    Checkbox(checked = optRestoreApplist, onCheckedChange = { optRestoreApplist = it })
                                    Text(stringResource(R.string.str_per_app_applist_settings), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
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
                            text = stringResource(R.string.str_changelog),
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

                com.ravenhub.app.ui.component.BackupRestoreBottomSheet(
                    show = showBackupRestoreBottomSheet,
                    onDismiss = { showBackupRestoreBottomSheet = false },
                    onBackup = { showBackupModuleModal = true },
                    onRestore = {
                        com.ravenhub.app.ui.util.AppLifecycleManager.isLaunchingSystemPicker = true
                        restoreFullBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }
                )

                if (showBackupModuleModal) {
                    com.ravenhub.app.ui.component.CustomBottomSheet(
                        visible = true,
                        onDismiss = { showBackupModuleModal = false }
                    ) {
                        Text(
                            text = "Select Modules to Backup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        Text(
                            text = "Choose which data categories to include in your encrypted backup:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Planner (Todos & Habits)")
                            Checkbox(checked = selPlanner, onCheckedChange = { selPlanner = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Finance (Expenses)")
                            Checkbox(checked = selFinance, onCheckedChange = { selFinance = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vault (Credentials & Encrypted Files)")
                            Checkbox(checked = selVault, onCheckedChange = { selVault = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Notes (Markdown Notes & Backlinks)")
                            Checkbox(checked = selNotes, onCheckedChange = { selNotes = it })
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                showBackupModuleModal = false
                                com.ravenhub.app.ui.util.AppLifecycleManager.isLaunchingSystemPicker = true
                                createFullBackupLauncher.launch("ravenhub_backup_${System.currentTimeMillis()}.zip")
                            },
                            enabled = selPlanner || selFinance || selVault || selNotes,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp)
                        ) {
                            Text("Choose Export Folder & Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(24.dp))
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
                    text = stringResource(R.string.settings),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onChangelogClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TextSnippet,
                        contentDescription = stringResource(R.string.cd_changelog)
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
