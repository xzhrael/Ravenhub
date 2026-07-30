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


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.edit
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.materialkolor.rememberDynamicColorScheme
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yalantis.ucrop.UCrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.blurEffect
import java.io.File
import kotlin.math.roundToInt
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.theme.ColorMode
import com.ravenhub.app.ui.theme.ThemeController
import com.ravenhub.app.ui.util.PropertyUtils
import com.ravenhub.app.ui.util.clearHeaderImage
import com.ravenhub.app.ui.util.getHeaderImage
import com.ravenhub.app.ui.util.saveHeaderImage


@Composable
fun BypassChargeScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val hazeState = remember { HazeState() }
    val isBlurEnabled = LocalBlurEnabled.current
    
    var bypassPath by remember { mutableStateOf("") }
    var bypassChgState by remember { mutableStateOf<Boolean?>(null) }
    var thresholdValue by remember { mutableStateOf<Float?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val isUnsupported = bypassPath == "UNSUPPORTED"

    LaunchedEffect(Unit) {
        bypassPath = PropertyUtils.get("persist.sys.ravencoreconf.bypasspath", "")
        
        val thresholdProp = PropertyUtils.get("persist.sys.ravencoreconf.bypasschgthreshold", "20")
        thresholdValue = thresholdProp.toFloatOrNull()?.coerceIn(20f, 50f) ?: 20f
        
        bypassChgState = PropertyUtils.get("persist.sys.ravencoreconf.bypasschg", "0") == "1"
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                
                    item {
                        ExpressiveList(
                            content = listOf( 
                                {
                                    ExpressiveInfoCard(
                                        supportingContent = { Text(text = stringResource(R.string.str_pause_battery_charging_when_pl)) },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                                        containerColor = colorScheme.surfaceContainerLow,
                                        onClick = {}
                                    )
                                }
                            )
                        )
                    }

                    item {
                        Image(
                            painter = painterResource(id = R.drawable.bypasschgillust),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .graphicsLayer(alpha = if (isUnsupported) 0.5f else 1f),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(colorScheme.primary)
                        )
                    }

                    item {
                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    onClick = { navController.navigate("bypasschg_check") },
                                    headlineContent = { Text(stringResource(R.string.CompatibilityCheck)) },
                                    supportingContent = { Text(stringResource(R.string.CompatibilityCheck_desc)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.CheckCircle) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    }

                    item {
                        if (bypassChgState != null) { 
                            ExpressiveList(
                                content = listOf {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Filled.BatteryChargingFull,
                                        title = stringResource(R.string.enable_bypass_charge),
                                        summary = if (isUnsupported) stringResource(R.string.bypass_not_supported)
                                                  else stringResource(R.string.enable_bypass_charge_desc),
                                        checked = bypassChgState!!,
                                        enabled = !isUnsupported,
                                        onCheckedChange = { isChecked ->
                                            bypassChgState = isChecked
                                            val value = if (isChecked) "1" else "0"
                                            PropertyUtils.set("persist.sys.ravencoreconf.bypasschg", value)
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    Shell.cmd("echo $value > /data/adb/.config/ravencore/bypasschgconfig/bypasschg").exec()
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }

                    item {
                        var isBatMonEnabled by remember {
                            mutableStateOf(PropertyUtils.get("persist.sys.ravencoreconf.battery_monitor", "0") == "1")
                        }
                        ExpressiveList(
                            content = buildList<@Composable () -> Unit> {
                                add {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.BatteryChargingFull,
                                        title = "Battery Monitor",
                                        summary = "Monitor active screen time, idle drain rate, and deep sleep diagnostics",
                                        checked = isBatMonEnabled,
                                        onCheckedChange = { isChecked ->
                                            isBatMonEnabled = isChecked
                                            PropertyUtils.set("persist.sys.ravencoreconf.battery_monitor", if (isChecked) "1" else "0")
                                        }
                                    )
                                }
                                if (isBatMonEnabled) {
                                    add {
                                        ExpressiveListItem(
                                            leadingContent = { LeadingIcon(icon = Icons.Rounded.RestartAlt) },
                                            onClick = {
                                                context.sendBroadcast(Intent("ravencore.intent.action.RESET_BATTERY_STATS"))
                                                val statsPrefs = context.getSharedPreferences("battery_monitor_stats", Context.MODE_PRIVATE)
                                                statsPrefs.edit().clear().apply()
                                            },
                                            headlineContent = { Text("Reset Battery Monitor Stats") },
                                            supportingContent = { Text("Clear screen-on time, drain rates, and deep sleep stats") }
                                        )
                                    }
                                }
                            }
                        )
                    }

                    thresholdValue?.let { currentVal -> 
                        item {
                            val animatedSliderValue by animateFloatAsState(
                                targetValue = currentVal,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                                label = "SliderAnimation"
                            )
                    
                            val progress = (currentVal - 20f) / 30f 
                            val animatedBarProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                label = "BarProgress"
                            )
                            
                            val cardAlpha = if (isUnsupported) 0.5f else 1f

                            ExpressiveList(
                                modifier = Modifier.graphicsLayer(alpha = cardAlpha),
                                content = listOf {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                LeadingIcon(
                                                    icon = Icons.Rounded.DataThresholding,
                                                    contentDescription = null
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = stringResource(R.string.charging_threshold),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = colorScheme.onSurface
                                                )
                                            }
                                            
                                            Text(
                                                text = stringResource(R.string.str_currentval_toint, currentVal.toInt()),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUnsupported) colorScheme.outline else colorScheme.primary
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = stringResource(R.string.charging_threshold_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.outline
                                        )
            
                                        Spacer(modifier = Modifier.height(24.dp))
            
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colorScheme.surfaceContainerHighest)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedBarProgress)
                                                    .height(8.dp)
                                                    .align(Alignment.CenterStart)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isUnsupported) {
                                                            SolidColor(colorScheme.outline) 
                                                        } else {
                                                            Brush.horizontalGradient(
                                                                listOf(colorScheme.primary.copy(alpha = 0.6f), colorScheme.primary)
                                                            )
                                                        }
                                                    )
                                            )
                                        }
            
                                        Spacer(modifier = Modifier.height(4.dp))
            
                                        Slider(
                                            value = animatedSliderValue,
                                            enabled = !isUnsupported,
                                            onValueChange = { newValue -> 
                                                val step = 5f
                                                val snapped = (newValue / step).roundToInt() * step
                                                val finalValue = snapped.coerceIn(20f, 50f)
                                                thresholdValue = finalValue
                                            },
                                            onValueChangeFinished = {
                                                thresholdValue?.let { finalVal ->
                                                    PropertyUtils.set("persist.sys.ravencoreconf.bypasschgthreshold", finalVal.toInt().toString())
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            Shell.cmd("echo ${finalVal.toInt()} > /data/adb/.config/ravencore/bypasschgconfig/bypasschgthreshold").exec()
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                            },
                                            valueRange = 20f..50f,
                                            steps = 5, 
                                            colors = SliderDefaults.colors(
                                                thumbColor = if (isUnsupported) colorScheme.outline else colorScheme.primary,
                                                disabledThumbColor = colorScheme.outline.copy(alpha = 0.5f),
                                                activeTrackColor = Color.Transparent,
                                                inactiveTrackColor = Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(32.dp)
                                        )
            
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(stringResource(R.string.str_20), style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                            Text(stringResource(R.string.str_50), style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                    }
                                }
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
                                if (isBlurEnabled) {
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
                                if (isBlurEnabled) {
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
                        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.bcharging),
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
fun BypassChgTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
            title = { 
                Text(
                    text = stringResource(R.string.bcharging),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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