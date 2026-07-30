/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.ravenhub.app.ui.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch
import com.ravenhub.app.ui.component.LocalAppHazeState
import com.ravenhub.app.ui.component.LocalBlurEnabled

@Composable
fun DevShellScreen(navController: NavController) {
    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalAppHazeState.current
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var commandInput by remember { mutableStateOf("") }
    val consoleLogs = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        consoleLogs.add("Ravencore Root Shell Console")
        consoleLogs.add("Type root commands to diagnostic device status.")
        consoleLogs.add("----------------------------------------")
    }

    fun executeCommand() {
        val cmd = commandInput.trim()
        if (cmd.isEmpty()) return
        
        consoleLogs.add("# $cmd")
        commandInput = ""

        coroutineScope.launch {
            try {
                val result = Shell.cmd(cmd).exec()
                if (result.out.isNotEmpty()) {
                    consoleLogs.addAll(result.out)
                }
                if (result.err.isNotEmpty()) {
                    consoleLogs.addAll(result.err.map { "[ERR] $it" })
                }
                if (result.out.isEmpty() && result.err.isEmpty()) {
                    consoleLogs.add("[Command returned exit code: ${result.code}]")
                }
            } catch (e: Exception) {
                consoleLogs.add("Error: ${e.message}")
            }
            
            // Auto scroll to bottom
            if (consoleLogs.size > 0) {
                listState.animateScrollToItem(consoleLogs.size - 1)
            }
        }
    }

    MaterialExpressiveTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 100.dp, bottom = 16.dp)
            ) {
                // Terminal output area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.9f), MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(consoleLogs) { line ->
                            Text(
                                text = line,
                                color = if (line.startsWith("#")) Color.Green else if (line.startsWith("[ERR]")) Color.Red else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter shell command...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { executeCommand() })
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { executeCommand() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Execute")
                    }
                }
            }

            // History State
            var showHistorySheet by remember { mutableStateOf(false) }
            val historyList = remember { mutableStateListOf<String>() }

            // Track unique command history on command run
            SideEffect {
                consoleLogs.filter { it.startsWith("# ") }.map { it.removePrefix("# ") }.distinct().let { cmds ->
                    historyList.clear()
                    historyList.addAll(cmds.reversed())
                }
            }

            if (showHistorySheet) {
                ModalBottomSheet(
                    onDismissRequest = { showHistorySheet = false },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Command History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (historyList.isEmpty()) {
                            Text(
                                text = "No command history yet.",
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                            ) {
                                items(historyList) { item ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                commandInput = item
                                                showHistorySheet = false
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = item,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Header Bar overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Left Back Pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            navController.popBackStack()
                        }
                        .then(
                            if (isBlurEnabled && hazeState != null) {
                                Modifier.hazeEffect(state = hazeState) {
                                    blurEffect {
                                        blurRadius = 24.dp
                                    }
                                }
                            } else Modifier
                        ),
                    shape = CircleShape,
                    color = if (isBlurEnabled) colorScheme.surfaceContainer.copy(alpha = 0.4f) else colorScheme.surfaceContainer,
                    shadowElevation = if (isBlurEnabled) 0.dp else 4.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onSurface)
                    }
                }

                // Center Title Pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (isBlurEnabled && hazeState != null) {
                                Modifier.hazeEffect(state = hazeState) {
                                    blurEffect {
                                        blurRadius = 24.dp
                                    }
                                }
                            } else Modifier
                        ),
                    shape = CircleShape,
                    color = if (isBlurEnabled) colorScheme.surfaceContainer.copy(alpha = 0.4f) else colorScheme.surfaceContainer,
                    shadowElevation = if (isBlurEnabled) 0.dp else 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEV SHELL",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Combined Pill (History + Clear)
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (isBlurEnabled && hazeState != null) {
                                Modifier.hazeEffect(state = hazeState) {
                                    blurEffect {
                                        blurRadius = 24.dp
                                    }
                                }
                            } else Modifier
                        ),
                    shape = CircleShape,
                    color = if (isBlurEnabled) colorScheme.surfaceContainer.copy(alpha = 0.4f) else colorScheme.surfaceContainer,
                    shadowElevation = if (isBlurEnabled) 0.dp else 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "History",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(horizontal = 2.dp),
                            color = colorScheme.onSurface.copy(alpha = 0.2f)
                        )

                        IconButton(
                            onClick = { consoleLogs.clear() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
