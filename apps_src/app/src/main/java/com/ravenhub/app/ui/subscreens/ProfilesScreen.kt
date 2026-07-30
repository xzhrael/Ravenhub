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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ravenhub.app.ui.subscreens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.util.PropertyUtils

@Composable
fun ProfilesScreen(navController: NavController) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = remember { HazeState() }

    // Engine Mode: "ai", "auto", "manual"
    var engineMode by remember {
        mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.engine_mode", "ai").lowercase())
    }

    var activeProfile by remember {
        mutableStateOf(PropertyUtils.get("persist.sys.ravencore.active_profile", "balanced").lowercase())
    }

    MaterialExpressiveTheme {
        Scaffold(
            containerColor = colorScheme.surface
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 90.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // --- SECTION 1: PROFILE ENGINE SELECTION MODE ---
                    item { SectionHeaderTitle("Profile Engine Mode") }
                    item {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            engineMode = "ai"
                                            PropertyUtils.set("persist.sys.ravencoreconf.engine_mode", "ai")
                                            PropertyUtils.set("persist.sys.ravencoreconf.autoprofile", "1")
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.AutoAwesome) },
                                        headlineContent = { Text("Ravencore AI Profile", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("Predictive ML engine: On-demand scaling (Balanced), QoS process suspension (Battery), and jitter-free performance envelope (Game)") },
                                        trailingContent = {
                                            RadioButton(
                                                selected = engineMode == "ai",
                                                onClick = {
                                                    engineMode = "ai"
                                                    PropertyUtils.set("persist.sys.ravencoreconf.engine_mode", "ai")
                                                    PropertyUtils.set("persist.sys.ravencoreconf.autoprofile", "1")
                                                }
                                            )
                                        }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            engineMode = "auto"
                                            PropertyUtils.set("persist.sys.ravencoreconf.engine_mode", "auto")
                                            PropertyUtils.set("persist.sys.ravencoreconf.autoprofile", "1")
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Psychology) },
                                        headlineContent = { Text("Auto Profile Engine", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("Standard rule-based auto switcher (Performance on Game Master Switch, Battery Saver on Device Saver)") },
                                        trailingContent = {
                                            RadioButton(
                                                selected = engineMode == "auto",
                                                onClick = {
                                                    engineMode = "auto"
                                                    PropertyUtils.set("persist.sys.ravencoreconf.engine_mode", "auto")
                                                    PropertyUtils.set("persist.sys.ravencoreconf.autoprofile", "1")
                                                }
                                            )
                                        }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            engineMode = "manual"
                                            PropertyUtils.set("persist.sys.ravencoreconf.engine_mode", "manual")
                                            PropertyUtils.set("persist.sys.ravencoreconf.autoprofile", "0")
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Tune) },
                                        headlineContent = { Text("Manual Profile Engine", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("Static manual profile selection (Choose fixed profile manually below)") },
                                        trailingContent = {
                                            RadioButton(
                                                selected = engineMode == "manual",
                                                onClick = {
                                                    engineMode = "manual"
                                                    PropertyUtils.set("persist.sys.ravencoreconf.engine_mode", "manual")
                                                    PropertyUtils.set("persist.sys.ravencoreconf.autoprofile", "0")
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        )
                    }

                    // --- SECTION 2: MANUAL PROFILE SELECTION ---
                    item { SectionHeaderTitle("Manual Profile Selection") }
                    item {
                        val isManualEnabled = engineMode == "manual"
                        ExpressiveList(
                            modifier = Modifier.alpha(if (isManualEnabled) 1f else 0.5f),
                            content = listOf(
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            if (isManualEnabled) {
                                                activeProfile = "performance"
                                                PropertyUtils.set("persist.sys.ravencore.active_profile", "performance")
                                            }
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Speed) },
                                        headlineContent = { Text("Performance Profile", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(if (isManualEnabled) "Maximum performance profile for high workload applications" else "Managed automatically by ${if (engineMode == "ai") "AI Profile Engine" else "Auto Profile Engine"}") },
                                        trailingContent = {
                                            RadioButton(
                                                selected = activeProfile == "performance",
                                                enabled = isManualEnabled,
                                                onClick = {
                                                    if (isManualEnabled) {
                                                        activeProfile = "performance"
                                                        PropertyUtils.set("persist.sys.ravencore.active_profile", "performance")
                                                    }
                                                }
                                            )
                                        }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            if (isManualEnabled) {
                                                activeProfile = "balanced"
                                                PropertyUtils.set("persist.sys.ravencore.active_profile", "balanced")
                                            }
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Tune) },
                                        headlineContent = { Text("Balanced Profile", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(if (isManualEnabled) "Default system profile for regular daily usage" else "Managed automatically by ${if (engineMode == "ai") "AI Profile Engine" else "Auto Profile Engine"}") },
                                        trailingContent = {
                                            RadioButton(
                                                selected = activeProfile == "balanced",
                                                enabled = isManualEnabled,
                                                onClick = {
                                                    if (isManualEnabled) {
                                                        activeProfile = "balanced"
                                                        PropertyUtils.set("persist.sys.ravencore.active_profile", "balanced")
                                                    }
                                                }
                                            )
                                        }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            if (isManualEnabled) {
                                                activeProfile = "battery"
                                                PropertyUtils.set("persist.sys.ravencore.active_profile", "battery")
                                            }
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.BatterySaver) },
                                        headlineContent = { Text("Battery Saver Profile", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(if (isManualEnabled) "Power-saving profile to extend battery endurance" else "Managed automatically by ${if (engineMode == "ai") "AI Profile Engine" else "Auto Profile Engine"}") },
                                        trailingContent = {
                                            RadioButton(
                                                selected = activeProfile == "battery" || activeProfile == "powersave",
                                                enabled = isManualEnabled,
                                                onClick = {
                                                    if (isManualEnabled) {
                                                        activeProfile = "battery"
                                                        PropertyUtils.set("persist.sys.ravencore.active_profile", "battery")
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        )
                    }
                }

                // Floating Header Bar overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { navController.popBackStack() }
                            .then(
                                if (isBlurEnabled) {
                                    Modifier.hazeEffect(state = hazeState) {
                                        blurEffect { blurRadius = 24.dp }
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

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .height(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (isBlurEnabled) {
                                    Modifier.hazeEffect(state = hazeState) {
                                        blurEffect { blurRadius = 24.dp }
                                    }
                                } else Modifier
                            ),
                        shape = CircleShape,
                        color = if (isBlurEnabled) colorScheme.surfaceContainer.copy(alpha = 0.4f) else colorScheme.surfaceContainer,
                        shadowElevation = if (isBlurEnabled) 0.dp else 4.dp
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "RAVENCORE PROFILE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
}
