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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.util.CustomConfigUtil
import com.ravenhub.app.ui.util.PropertyUtils

@Composable
fun PreferredTweaksScreen(navController: NavController) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = remember { HazeState() }

    val engineMode = remember { PropertyUtils.get("persist.sys.ravencoreconf.engine_mode", "ai") }
    val isAiActive = engineMode == "ai"

    // Display & System Pipeline States
    var sosmedSaver by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.sosmed_saver", "1") == "1") }
    var deepSleepEnforcer by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.deepsleep_enforcer", "1") == "1") }
    var refreshRateConfigState by remember { mutableStateOf(CustomConfigUtil.getValue("refresh_rate", "smart")) }
    var currentRenderer by remember { mutableStateOf(CustomConfigUtil.getValue("global_renderer", "skiagl")) }
    var sfPhaseAlignment by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.sf_phase", "1") == "1") }

    var showRefreshRateDialog by remember { mutableStateOf(false) }
    var showRendererDialog by remember { mutableStateOf(false) }

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

                    // --- TOP CARD: PROFILES NAVIGATION CARD (ALWAYS ACTIVE) ---
                    item { SectionHeaderTitle("Ravencore Profile") }
                    item {
                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    onClick = { navController.navigate("profiles") },
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.AutoAwesome) },
                                    headlineContent = { Text("Ravencore Profile", fontWeight = FontWeight.Bold) },
                                    supportingContent = {
                                        Text(
                                            if (isAiActive) "Active: Ravencore AI Profile (ML Dynamic Tuning)"
                                            else "Configure AI Profile Engine, Auto Engine, or manual profiles"
                                        )
                                    },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    }

                    if (isAiActive) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "AI Profile is active. Machine Learning is dynamically tuning all system parameters. Manual tweaking cards are locked.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // --- SECTION 1: MANUAL PROFILE CONFIGURATIONS ---
                    item { SectionHeaderTitle("Manual Profile Configurations") }
                    item {
                        Box(modifier = Modifier.alpha(if (isAiActive) 0.5f else 1.0f)) {
                            ExpressiveList(
                                content = listOf(
                                    {
                                        ExpressiveListItem(
                                            onClick = { if (!isAiActive) navController.navigate("profile_config/performance") },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.RocketLaunch) },
                                            headlineContent = { Text("Performance Configuration") },
                                            supportingContent = { Text("Configure manual hardware parameters for Performance Profile") },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            onClick = { if (!isAiActive) navController.navigate("profile_config/balanced") },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.Tune) },
                                            headlineContent = { Text("Balanced Configuration") },
                                            supportingContent = { Text("Configure manual hardware parameters for Balanced Profile") },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            onClick = { if (!isAiActive) navController.navigate("profile_config/battery") },
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.BatterySaver) },
                                            headlineContent = { Text("Battery Configuration") },
                                            supportingContent = { Text("Configure manual hardware parameters for Battery Saver Profile") },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    }
                                )
                            )
                        }
                    }

                    // --- SECTION 2: DISPLAY & SYSTEM PIPELINE ---
                    item {
                        SectionHeaderTitle("Display & Graphics Pipeline")
                    }
                    item {
                        Box(modifier = Modifier.alpha(if (isAiActive) 0.5f else 1.0f)) {
                            ExpressiveList(
                                content = listOf(
                                    {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.Videocam,
                                            title = "Sosmed & Video Thermal Saver",
                                            summary = "Cap Gold core frequencies under light video playback to stay below 40°C",
                                            checked = sosmedSaver,
                                            onCheckedChange = {
                                                if (!isAiActive) {
                                                    sosmedSaver = it
                                                    PropertyUtils.set("persist.sys.ravencoreconf.sosmed_saver", if (it) "1" else "0")
                                                }
                                            }
                                        )
                                    },
                                    {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.NightsStay,
                                            title = "Deep Sleep LPM Enforcer",
                                            summary = "Force kernel Low Power Mode when screen is off to maintain >90% deep sleep",
                                            checked = deepSleepEnforcer,
                                            onCheckedChange = {
                                                if (!isAiActive) {
                                                    deepSleepEnforcer = it
                                                    PropertyUtils.set("persist.sys.ravencoreconf.deepsleep_enforcer", if (it) "1" else "0")
                                                }
                                            }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.Tv) },
                                            onClick = { if (!isAiActive) showRefreshRateDialog = true },
                                            headlineContent = { Text(stringResource(R.string.refresh_rate_title)) },
                                            supportingContent = {
                                                val currentText = when (refreshRateConfigState) {
                                                    "smart" -> "Smart Dynamic (Ravencore)"
                                                    "90" -> "90Hz Peak Smoothness"
                                                    "60" -> "60Hz Battery Saver"
                                                    else -> "Smart Dynamic (Ravencore)"
                                                }
                                                Text(currentText)
                                            },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    },
                                    {
                                        ExpressiveListItem(
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.Palette) },
                                            onClick = { if (!isAiActive) showRendererDialog = true },
                                            headlineContent = { Text("Global Current Render Engine") },
                                            supportingContent = {
                                                val desc = when (currentRenderer) {
                                                    "skiagl" -> "SkiaGL (OpenGL ES - Default)"
                                                    "skiavk" -> "SkiaVK (Vulkan - Experimental)"
                                                    else -> "SkiaGL (OpenGL ES - Default)"
                                                }
                                                Text(desc)
                                            },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    },
                                    {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.Sync,
                                            title = "SurfaceFlinger Phase Alignment",
                                            summary = "Align VSYNC early app phase offset to eliminate frame jitter",
                                            checked = sfPhaseAlignment,
                                            onCheckedChange = {
                                                if (!isAiActive) {
                                                    sfPhaseAlignment = it
                                                    PropertyUtils.set("persist.sys.ravencoreconf.sf_phase", if (it) "1" else "0")
                                                }
                                            }
                                        )
                                    }
                                )
                            )
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
                                text = "PREFERRED TWEAKS",
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

                // Dialog Modals
                RefreshRatePickerDialog(
                    show = showRefreshRateDialog,
                    onDismiss = { showRefreshRateDialog = false },
                    onRefreshRatePicker = { mode ->
                        refreshRateConfigState = mode
                        CustomConfigUtil.setValue("refresh_rate", mode)
                        val shellCmd = when (mode) {
                            "90" -> "settings put system peak_refresh_rate 90.0; settings put system min_refresh_rate 90.0"
                            "60" -> "settings put system peak_refresh_rate 60.0; settings put system min_refresh_rate 60.0"
                            else -> "settings put system peak_refresh_rate 90.0; settings put system min_refresh_rate 60.0"
                        }
                        Shell.cmd(shellCmd).exec()
                    }
                )

                RendererDialog(
                    show = showRendererDialog,
                    onDismiss = { showRendererDialog = false },
                    onRenderer = { renderer ->
                        currentRenderer = renderer
                        CustomConfigUtil.setValue("global_renderer", renderer)
                        Shell.cmd("setprop debug.hwui.renderer $renderer").exec()
                    }
                )
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
