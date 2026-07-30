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

package com.ravenhub.app.ui.mainscreens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.viewmodel.ApplistViewmodel
import com.ravenhub.app.ui.viewmodel.MemoryCleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(navController: NavController) {
    val context = LocalContext.current
    val appListViewModel: ApplistViewmodel = viewModel()
    val cleanerViewModel: MemoryCleanerViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (ApplistViewmodel.apps.isEmpty()) {
            appListViewModel.loadApps(context)
        }
    }

    // Initialize/sync cleaner selection list once apps are loaded
    LaunchedEffect(ApplistViewmodel.apps) {
        ApplistViewmodel.apps.forEach { app ->
            if (!app.isSystem && !cleanerViewModel.selectedApps.containsKey(app.packageName)) {
                cleanerViewModel.selectedApps[app.packageName] = false
            }
        }
    }

    val userApps = remember(ApplistViewmodel.apps) {
        ApplistViewmodel.apps.filter { !it.isSystem }
    }

    val filteredApps = remember(userApps, cleanerViewModel.appSearchQuery) {
        val q = cleanerViewModel.appSearchQuery.lowercase()
        val list = userApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        list.sortedBy { it.label.lowercase() }
    }

    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalAppHazeState.current

    MaterialExpressiveTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 100.dp)) },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            LazyColumn(
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
                }
                // Section 1: Memory Cleaner
                item {
                    Text(
                        text = "Memory Cleaner",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Search field
                            OutlinedTextField(
                                value = cleanerViewModel.appSearchQuery,
                                onValueChange = { cleanerViewModel.appSearchQuery = it },
                                placeholder = { Text("Search application name...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Selection controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        filteredApps.forEach { cleanerViewModel.selectedApps[it.packageName] = true }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Select All")
                                }

                                Button(
                                    onClick = {
                                        filteredApps.forEach { app ->
                                            val pkg = app.packageName.lowercase()
                                            val isSafe = !(pkg.contains("ksu") || pkg.contains("ksunext") || pkg.contains("whatsapp") || pkg.contains("ravencore") || pkg.contains("hyperbridge"))
                                            cleanerViewModel.selectedApps[app.packageName] = isSafe
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Select Safe")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Scrollable list of apps
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                if (filteredApps.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No applications found",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        filteredApps.forEach { app ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val current = cleanerViewModel.selectedApps[app.packageName] ?: false
                                                        cleanerViewModel.selectedApps[app.packageName] = !current
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {


                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = app.label,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = app.packageName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Checkbox(
                                                    checked = cleanerViewModel.selectedApps[app.packageName] ?: false,
                                                    onCheckedChange = { isChecked ->
                                                        cleanerViewModel.selectedApps[app.packageName] = isChecked
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Clean Now button
                            Button(
                                onClick = {
                                    cleanerViewModel.cleanSelectedApps(context) { msg ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Rounded.CleaningServices, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clean Cache & RAM")
                            }
                        }
                    }
                }

                // Section 2: Game Optimization
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Game Optimization",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }

                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.Tune) },
                                    onClick = { navController.navigate("preferred_tweaks") },
                                    headlineContent = { Text("Preferred Tweaks") },
                                    supportingContent = { Text("Customize global performance parameters and system tuning") },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.RocketLaunch) },
                                    onClick = { navController.navigate("applist") },
                                    headlineContent = { Text("Game Optimizer Suite") },
                                    supportingContent = { Text("Manage game overlays and asset preloading") },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.ShowChart) },
                                    onClick = { navController.navigate("perfhistory") },
                                    headlineContent = { Text("Performance History") },
                                    supportingContent = { Text("Real-time FPS, CPU, and RAM diagnostic graphs") },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    )
                }
            }
        }

        // Overlay cleaning progress dialog
        if (cleanerViewModel.isCleaning) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Memory Cleaner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Clearing cache & RAM...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = cleanerViewModel.currentCleaningApp,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { cleanerViewModel.percent },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cleanerViewModel.progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
        }
    }
}
