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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.util.PropertyUtils
import java.io.File

private fun fetchAvailableCpuGovs(): List<String> {
    val paths = listOf(
        "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors",
        "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors",
        "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_governors"
    )
    for (p in paths) {
        try {
            val f = File(p)
            if (f.exists()) {
                val txt = f.readText().trim()
                if (txt.isNotBlank()) return txt.split("\\s+".toRegex()).filter { it.isNotBlank() }
            }
        } catch (_: Exception) {}
    }
    return listOf("schedutil", "performance", "powersave")
}

private fun fetchAvailableIoScheds(): Pair<List<String>, String> {
    val devices = listOf("sda", "sdb", "mmcblk0", "dm-0", "nvme0n1")
    for (dev in devices) {
        val p = "/sys/block/$dev/queue/scheduler"
        try {
            val f = File(p)
            if (f.exists()) {
                val txt = f.readText().trim()
                if (txt.isNotBlank()) {
                    val active = txt.split(" ")
                        .find { it.startsWith("[") && it.endsWith("]") }
                        ?.removeSurrounding("[", "]") ?: ""
                    val list = txt.replace("[", "").replace("]", "").split("\\s+".toRegex()).filter { it.isNotBlank() }
                    if (list.isNotEmpty()) return Pair(list, active)
                }
            }
        } catch (_: Exception) {}
    }
    return Pair(listOf("none", "mq-deadline", "bfq", "kyber"), "mq-deadline")
}

private fun fetchAvailableGpuGovs(): List<String> {
    val paths = listOf(
        "/sys/class/devfreq/3d00000.gpu/available_governors",
        "/sys/class/kgsl/kgsl-3d0/devfreq/available_governors",
        "/sys/class/devfreq/1c00000.qcom,kgsl-3d0/available_governors",
        "/sys/class/devfreq/gpufreq/available_governors"
    )
    for (p in paths) {
        try {
            val f = File(p)
            if (f.exists()) {
                val txt = f.readText().trim()
                if (txt.isNotBlank()) return txt.split("\\s+".toRegex()).filter { it.isNotBlank() }
            }
        } catch (_: Exception) {}
    }
    return listOf("msm-adreno-tz", "performance", "powersave", "simple_ondemand")
}

@Composable
fun ProfileConfigScreen(navController: NavController, profileType: String) {
    val colorScheme = MaterialTheme.colorScheme
    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = remember { HazeState() }

    val cleanProfileType = profileType.lowercase().trim()
    val suffix = when (cleanProfileType) {
        "performance" -> "perf"
        "battery" -> "bat"
        else -> "bal"
    }

    val displayTitle = when (cleanProfileType) {
        "performance" -> "PERFORMANCE CONFIG"
        "battery" -> "BATTERY CONFIG"
        else -> "BALANCED CONFIG"
    }

    val availCpuGovs = remember { fetchAvailableCpuGovs() }
    val (availIoScheds, currentActiveIoSched) = remember { fetchAvailableIoScheds() }
    val availGpuGovs = remember { fetchAvailableGpuGovs() }

    var cpuGov by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.cpugov_$suffix", availCpuGovs.firstOrNull() ?: "schedutil")) }
    var gpuGov by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.gpugov_$suffix", availGpuGovs.firstOrNull() ?: "msm-adreno-tz")) }
    var gpuPwrlevel by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.gpu_pwrlevel_$suffix", if (suffix == "perf") "0" else "2")) }
    var ioSched by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.iosched_$suffix", currentActiveIoSched.ifEmpty { availIoScheds.firstOrNull() ?: "mq-deadline" })) }
    var lmkMode by remember { mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.lmk_mode_$suffix", if (suffix == "perf") "1" else "0")) }

    var showCpuGovDialog by remember { mutableStateOf(false) }
    var showGpuGovDialog by remember { mutableStateOf(false) }
    var showGpuPwrLevelDialog by remember { mutableStateOf(false) }
    var showIoSchedDialog by remember { mutableStateOf(false) }
    var showLmkDialog by remember { mutableStateOf(false) }

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
                    item { SectionHeaderTitle("CPU & GPU Hardware Tuning") }
                    item {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Speed) },
                                        headlineContent = { Text("CPU Governor") },
                                        supportingContent = { Text("Current: ${cpuGov.uppercase()}") },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                        onClick = { showCpuGovDialog = true }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Memory) },
                                        headlineContent = { Text("GPU Governor") },
                                        supportingContent = { Text("Current: ${gpuGov.uppercase()}") },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                        onClick = { showGpuGovDialog = true }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.ElectricBolt) },
                                        headlineContent = { Text("GPU Power Level") },
                                        supportingContent = { Text("Current: Level $gpuPwrlevel ${if (gpuPwrlevel == "0") "(Max Performance)" else ""}") },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                        onClick = { showGpuPwrLevelDialog = true }
                                    )
                                }
                            )
                        )
                    }

                    item { SectionHeaderTitle("Memory & I/O Tuning") }
                    item {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.Storage) },
                                        headlineContent = { Text("I/O Scheduler") },
                                        supportingContent = { Text("Current: ${ioSched.uppercase()}") },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                        onClick = { showIoSchedDialog = true }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Rounded.CleaningServices) },
                                        headlineContent = { Text("Low Memory Killer (LMK)") },
                                        supportingContent = { Text("Current: ${if (lmkMode == "1") "Gaming Aggressive" else "Balanced Moderate"}") },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                        onClick = { showLmkDialog = true }
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
                                text = displayTitle,
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

                // Modals
                OptionModal(
                    title = "Select CPU Governor",
                    show = showCpuGovDialog,
                    onDismiss = { showCpuGovDialog = false },
                    options = availCpuGovs.map { gov -> gov.uppercase() to gov },
                    icon = Icons.Rounded.Speed,
                    onSelect = { selectedGov ->
                        cpuGov = selectedGov
                        PropertyUtils.set("persist.sys.ravencoreconf.cpugov_$suffix", selectedGov)
                    }
                )

                OptionModal(
                    title = "Select GPU Governor",
                    show = showGpuGovDialog,
                    onDismiss = { showGpuGovDialog = false },
                    options = availGpuGovs.map { gov -> gov.uppercase() to gov },
                    icon = Icons.Rounded.Memory,
                    onSelect = { selectedGov ->
                        gpuGov = selectedGov
                        PropertyUtils.set("persist.sys.ravencoreconf.gpugov_$suffix", selectedGov)
                    }
                )

                OptionModal(
                    title = "Select GPU Power Level",
                    show = showGpuPwrLevelDialog,
                    onDismiss = { showGpuPwrLevelDialog = false },
                    options = listOf(
                        "Level 0 - Maximum Performance (Uncapped)" to "0",
                        "Level 1 - High Performance" to "1",
                        "Level 2 - Balanced Power" to "2",
                        "Level 3 - Power Saving (Min Frequency)" to "3"
                    ),
                    icon = Icons.Rounded.ElectricBolt,
                    onSelect = { selectedLevel ->
                        gpuPwrlevel = selectedLevel
                        PropertyUtils.set("persist.sys.ravencoreconf.gpu_pwrlevel_$suffix", selectedLevel)
                    }
                )

                OptionModal(
                    title = "Select I/O Scheduler",
                    show = showIoSchedDialog,
                    onDismiss = { showIoSchedDialog = false },
                    options = availIoScheds.map { sched -> sched.uppercase() to sched },
                    icon = Icons.Rounded.Storage,
                    onSelect = { selectedSched ->
                        ioSched = selectedSched
                        PropertyUtils.set("persist.sys.ravencoreconf.iosched_$suffix", selectedSched)
                    }
                )

                OptionModal(
                    title = "Select Low Memory Killer Preset",
                    show = showLmkDialog,
                    onDismiss = { showLmkDialog = false },
                    options = listOf(
                        "Gaming Aggressive (Max RAM reclaim for games)" to "1",
                        "Balanced Moderate (Standard background app retention)" to "0"
                    ),
                    icon = Icons.Rounded.CleaningServices,
                    onSelect = { selectedLmk ->
                        lmkMode = selectedLmk
                        PropertyUtils.set("persist.sys.ravencoreconf.lmk_mode_$suffix", selectedLmk)
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionModal(
    title: String,
    show: Boolean,
    onDismiss: () -> Unit,
    options: List<Pair<String, String>>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: (String) -> Unit
) {
    CustomBottomSheet(
        visible = show,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            ExpressiveList(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = options.map { (label, value) ->
                    {
                        ExpressiveListItem(
                            headlineContent = {
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingContent = {
                                LeadingIcon(icon = icon)
                            },
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            }
                        )
                    }
                }
            )
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
