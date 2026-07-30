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
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.blurEffect
import java.io.File
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.component.ExpressiveDropdownItem
import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveListItem
import com.ravenhub.app.ui.component.ExpressiveSwitchItem
import com.ravenhub.app.ui.util.getSupportedRefreshRates
import com.ravenhub.app.ui.util.CustomConfigUtil
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.ravenhub.app.ui.viewmodel.AppSettingsViewModel
import com.ravenhub.app.ui.viewmodel.ApplistViewmodel
import androidx.compose.foundation.clickable
import kotlin.math.roundToInt


@Composable
fun AppSettingsScreen(
    navController: NavController, 
    packageName: String?,
    viewModel: AppSettingsViewModel = viewModel(),
    appListViewModel: ApplistViewmodel = viewModel() 
) {
    val context = LocalContext.current
    val appDetails = remember(packageName) { getAppDetails(context, packageName) }
    val colorScheme = MaterialTheme.colorScheme
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val hazeState = remember { HazeState() }
    val isBlurEnabled = LocalBlurEnabled.current
    LaunchedEffect(packageName) { 
        viewModel.loadConfig() 
        packageName?.let { pkg ->
            withContext(Dispatchers.IO) {
                CustomConfigUtil.setValue("name_" + pkg.lowercase().replace(".", "_"), appDetails.first)
            }
        }
    }

    val config = viewModel.fullConfig[packageName]
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var localMasterOn by remember(config != null) { mutableStateOf(config != null) }
    

    var userToggled by remember { mutableStateOf(false) }

    val booleanModes = listOf(
        stringResource(R.string.on_label),
        stringResource(R.string.off_label)
    )

    val rendererModes = listOf(
        "SkiaVK",
        "SkiaVK (Threaded)",
        "SkiaGL",
        "SkiaGL (Threaded)",
        "OpenGL ES",
        "OpenGL ES (Threaded)",
        "Vulkan",
        "Smart Logic"
    )

    val rendererValues = listOf(
        "skiavk",
        "skiavkthreaded", 
        "skiagl",
        "skiaglthreaded", 
        "opengl",
        "openglthreaded", 
        "vulkan",
        "smart_logic"
    )

    val defaultLabel = stringResource(R.string.default_label)
    
    val rawRefreshModes = remember { getSupportedRefreshRates(context) }
    
    val dynamicRefreshDisplayModes = remember(rawRefreshModes, defaultLabel) {
        rawRefreshModes.map { mode ->
            if (mode.equals("default", ignoreCase = true)) defaultLabel else mode
        }
    }

    fun getBoolIndex(v: String?): Int = when(v) {
        "true" -> 0
        else -> 1
    }
    
    DisposableEffect(Unit) {
        onDispose {
            appListViewModel.loadApps(context, forceRefresh = true)
        }
    }

    Scaffold(
        topBar = {}
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 90.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ExpressiveList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf( 
                       {
                           ExpressiveInfoCard(
                               supportingContent = { Text(text = stringResource(R.string.str_app_specific_settings_will_ove)) },
                               leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                               containerColor = colorScheme.surfaceContainerLow,
                               onClick = {}
                           )
                       }
                    )
                )
            }
        
        
            item { AppHeader(appDetails, packageName) }
            

            item {
                ExpressiveList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf {
                        ExpressiveSwitchItem(
                            icon = Icons.Rounded.PowerSettingsNew,
                            title = stringResource(R.string.master_switch),
                            summary = stringResource(R.string.master_switch_desc),
                            checked = localMasterOn,
                            onCheckedChange = { isChecked ->
                                userToggled = true
                                localMasterOn = isChecked 
                                packageName?.let { pkg -> viewModel.toggleMasterSwitch(pkg, isChecked) } 
                            }
                        )
                    }
                )
            }
            
            item {
                AnimatedVisibility(
                    visible = localMasterOn,

                    enter = if (userToggled) {
                        expandVertically(animationSpec = tween(400)) + fadeIn()
                    } else {
                        EnterTransition.None
                    },
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()
                ) {
                    val displayConfig = config ?: com.ravenhub.app.ui.util.AppConfig()
                    val currentDownscale = displayConfig.downscale
                    val initialValue = remember(currentDownscale) {
                        if (currentDownscale == "default" || currentDownscale.isEmpty()) 100f else currentDownscale.toFloatOrNull() ?: 100f
                    }
                    var sliderValue by remember { mutableStateOf(initialValue) }
                    
                    LaunchedEffect(initialValue) {
                        sliderValue = initialValue
                    }

                    Column {
                        SectionHeader(stringResource(R.string.preferred_settings))
                        ExpressiveList(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            content = listOf(
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.Speed,
                                        title = stringResource(R.string.perf_lite_mode),
                                        summary = stringResource(R.string.perf_lite_mode_desc_short),
                                        checked = displayConfig.perf_lite_mode == "true",
                                        onCheckedChange = { isChecked ->
                                            packageName?.let { viewModel.updateSetting(it, "perf_lite_mode", isChecked.toString()) }
                                        }
                                    )
                                },
                                {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.RocketLaunch,
                                            title = stringResource(R.string.game_preload),
                                            summary = stringResource(R.string.game_preload_desc),
                                            checked = displayConfig.game_preload == "true",
                                            onCheckedChange = { isChecked ->
                                                packageName?.let { viewModel.updateSetting(it, "game_preload", isChecked.toString()) }
                                            }
                                        )
                                        
                                        if (displayConfig.game_preload == "true") {
                                            val currentBudget = displayConfig.game_preload_budget
                                            val initialBudget = remember(currentBudget) {
                                                if (currentBudget == "default" || currentBudget.isEmpty()) 512f else currentBudget.toFloatOrNull() ?: 512f
                                            }
                                            var budgetValue by remember { mutableStateOf(initialBudget) }
                                            
                                            LaunchedEffect(initialBudget) {
                                                budgetValue = initialBudget
                                            }
                                            
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 56.dp, end = 16.dp, bottom = 16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Preload Memory Budget",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "${budgetValue.toInt()} MB",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colorScheme.primary
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                val animatedBudgetProgress by animateFloatAsState(
                                                    targetValue = (budgetValue - 128f) / (2048f - 128f),
                                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                                    label = "PreloadBudgetProgress"
                                                )
                                                
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(8.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colorScheme.surfaceContainerHighest)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(animatedBudgetProgress.coerceIn(0f, 1f))
                                                            .height(8.dp)
                                                            .align(Alignment.CenterStart)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colorScheme.primary)
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                Slider(
                                                    value = budgetValue,
                                                    onValueChange = { budgetValue = it },
                                                    onValueChangeFinished = {
                                                        packageName?.let { viewModel.updateSetting(it, "game_preload_budget", budgetValue.toInt().toString()) }
                                                    },
                                                    valueRange = 128f..2048f,
                                                    steps = 14,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = colorScheme.primary,
                                                        activeTrackColor = Color.Transparent,
                                                        inactiveTrackColor = Color.Transparent,
                                                        activeTickColor = colorScheme.onPrimary.copy(alpha = 0.6f),
                                                        inactiveTickColor = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.SwapVerticalCircle,
                                        title = "Clear Cache & RAM",
                                        summary = "Clear background apps and free up system cache on game start",
                                        checked = displayConfig.clean_ram == "true",
                                        onCheckedChange = { isChecked ->
                                            packageName?.let { viewModel.updateSetting(it, "clean_ram", isChecked.toString()) }
                                        }
                                    )
                                },
                                {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                                    icon = Icons.Rounded.AspectRatio,
                                                    contentDescription = null
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(
                                                        text = "Downscale Resolution",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Reduce resolution scale to improve performance",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            
                                            Text(
                                                text = if (sliderValue >= 100f) "100% (Off)" else "${sliderValue.roundToInt()}%",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        val animatedProgress by animateFloatAsState(
                                            targetValue = (sliderValue - 50f) / 50f,
                                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                            label = "DownscaleProgress"
                                        )
                                        
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colorScheme.surfaceContainerHighest)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                                    .height(8.dp)
                                                    .align(Alignment.CenterStart)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colorScheme.primary)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = sliderValue,
                                            onValueChange = { newValue ->
                                                sliderValue = newValue
                                            },
                                            onValueChangeFinished = {
                                                packageName?.let { pkg ->
                                                    val intVal = sliderValue.roundToInt()
                                                    viewModel.updateSetting(pkg, "downscale", if (intVal >= 100) "100" else intVal.toString())
                                                }
                                            },
                                            valueRange = 50f..100f,
                                            steps = 9,
                                            colors = SliderDefaults.colors(
                                                thumbColor = colorScheme.primary,
                                                activeTrackColor = Color.Transparent,
                                                inactiveTrackColor = Color.Transparent,
                                                activeTickColor = colorScheme.onPrimary.copy(alpha = 0.6f),
                                                inactiveTickColor = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(32.dp)
                                        )
                                    }
                                },

                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.Thermostat,
                                        title = "Disable Thermal Core",
                                        summary = "Disable thermal core mitigation for maximum performance (Caution: high heat)",
                                        checked = displayConfig.disable_thermal == "true",
                                        onCheckedChange = { isChecked ->
                                            packageName?.let { viewModel.updateSetting(it, "disable_thermal", isChecked.toString()) }
                                        }
                                    )
                                }
                            )
                        )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left section (Back Pill container)
                    Box(
                        modifier = Modifier.width(48.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    appListViewModel.loadApps(context, forceRefresh = true)
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
                    }

                    // Center section (Title Pill container)
                    Box(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
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
                                    text = stringResource(R.string.app_settings_title).uppercase(),
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

                    // Right section (App Info Pill container)
                    Box(
                        modifier = Modifier.width(48.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp)
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                IconButton(onClick = {
                                    packageName?.let { pkg ->
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:$pkg")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }) {
                                    Icon(Icons.Rounded.Info, "App Info", tint = colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 28.dp,
            end = 28.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun AppHeader(appDetails: Triple<String, android.content.pm.ApplicationInfo?, String>, packageName: String?) {
    val context = LocalContext.current
    val pm = context.packageManager
    val density = LocalDensity.current
    

    val iconSize = 68.dp
    val targetSizePx = remember(iconSize, density) {
        with(density) { iconSize.roundToPx() }
    }


    var appBitmap by remember(packageName) {
        mutableStateOf(packageName?.let { AppIconCache.get(it) })
    }


    LaunchedEffect(packageName, targetSizePx) {
        if (appBitmap == null && packageName != null) {
            appDetails.second?.let { appInfo ->
                try {
                    appBitmap = AppIconCache.loadIcon(pm, appInfo, targetSizePx)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(100.dp),
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Crossfade(
                    targetState = appBitmap,
                    animationSpec = tween(durationMillis = 200),
                    label = "HeaderIconFade"
                ) { icon ->
                    if (icon == null) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        )
                    } else {

                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = appDetails.first,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = packageName ?: stringResource(R.string.unknown_package),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.str_v_appdetails_third, appDetails.third),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}


fun getAppDetails(context: android.content.Context, packageName: String?): Triple<String, android.content.pm.ApplicationInfo?, String> {
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName ?: "", 0)
        val packageInfo = pm.getPackageInfo(packageName ?: "", 0)
        val label = pm.getApplicationLabel(info).toString()

        val version = packageInfo.versionName ?: context.getString(R.string.status_unknown)
        Triple(label, info, version)
    } catch (e: Exception) {
        Triple(context.getString(R.string.unknown_app), null, "0.0.0")
    }
}



@Composable
fun AppSettingsTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior, 
    onLaunchApp: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onBack: () -> Unit
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
            title = { 
                Text(
                    text = stringResource(R.string.app_settings_title).uppercase(),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },
            actions = {

                IconButton(onClick = onLaunchApp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Launch, 
                        contentDescription = stringResource(R.string.str_launch_app)
                    )
                }

                IconButton(onClick = onOpenAppInfo) {
                    Icon(
                        imageVector = Icons.Rounded.Info, 
                        contentDescription = stringResource(R.string.str_app_info)
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
