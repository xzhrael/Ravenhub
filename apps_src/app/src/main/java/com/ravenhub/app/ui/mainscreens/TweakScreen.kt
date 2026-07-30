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


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.util.PropertyUtils
import com.ravenhub.app.ui.util.*
import com.ravenhub.app.ui.viewmodel.TweakViewModel


@Composable
fun TweakScreen(
    navController: NavController,
    viewModel: TweakViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val colorScheme = MaterialTheme.colorScheme
    
    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog(onConfirm = {}, onDismiss = {})
    
    var isFstrimRunning by remember { mutableStateOf(false) }
    var isCompileRunning by remember { mutableStateOf(false) }
    var compileStatusText by remember { mutableStateOf("") }
    
    LoadingDialogHost(handle = loadingDialog)
    ConfirmDialogHost(handle = confirmDialog)

    if (isCompileRunning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.opt_games_btn)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(compileStatusText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {}
        )
    }

    if (isFstrimRunning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.run_fstrim_btn)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Running FSTRIM, please wait...", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {}
        )
    }




    LaunchedEffect(Unit) {
        viewModel.loadAllConfiguration(context)
    }

    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalAppHazeState.current

    MaterialExpressiveTheme {
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
                }

                item {
                    TweaksSectionTitle(text = stringResource(R.string.ravencore_utils))
                }

                item {
                    ExpressiveList(
                        content = buildList<@Composable () -> Unit> {
                            add {
                                ExpressiveSwitchItem(
                                    icon = Icons.Rounded.SettingsSuggest,
                                    title = stringResource(R.string.ravencore_utility_title),
                                    summary = stringResource(R.string.ravencore_utility_desc),
                                    checked = viewModel.ravencoreUtilityState ?: false,
                                    onCheckedChange = { viewModel.updateRavencoreUtility(it) }
                                )
                            }
                            add {
                                ExpressiveSwitchItem(
                                    icon = Icons.Rounded.ElectricBolt,
                                    title = stringResource(R.string.fast_charge_title),
                                    summary = stringResource(R.string.fast_charge_desc),
                                    checked = viewModel.fastChargeState ?: false,
                                    onCheckedChange = { viewModel.updateFastCharge(it) }
                                )
                            }
                            add {
                                BatteryHealthGuardSliderItem(
                                    initialValue = viewModel.chargeLimitState ?: 100f,
                                    onSaved = { viewModel.updateChargeLimit(it) }
                                )
                            }
                            add {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.Power) },
                                    onClick = { navController.navigate("bypasschg") },
                                    headlineContent = { Text(stringResource(R.string.bcharging)) },
                                    supportingContent = { Text(stringResource(R.string.bcharging_desc)) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                             add {
                                 ExpressiveListItem(
                                     leadingContent = { LeadingIcon(icon = Icons.Rounded.Terminal) },
                                     onClick = { navController.navigate("devshell") },
                                     headlineContent = { Text("Developer Shell Terminal") },
                                     supportingContent = { Text("Interactive root shell console for diagnostics") },
                                     trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                 )
                             }
                             add {
                                 ExpressiveListItem(
                                     leadingContent = { LeadingIcon(icon = Icons.Rounded.Storage) },
                                     onClick = {
                                         viewModel.runFstrim(
                                             onStart = {
                                                 isFstrimRunning = true
                                                 loadingDialog.show()
                                             },
                                             onComplete = {
                                                 isFstrimRunning = false
                                                 loadingDialog.hide()
                                                 scope.launch {
                                                     snackbarHostState.showSnackbar("FSTRIM completed successfully!")
                                                 }
                                             }
                                         )
                                     },
                                     headlineContent = { Text(stringResource(R.string.run_fstrim_btn)) },
                                     supportingContent = { Text(if (isFstrimRunning) "Trimming storage blocks..." else "Trim storage partitions to restore read/write speeds") },
                                     trailingContent = {
                                         if (isFstrimRunning) {
                                             CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                         } else {
                                             Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                         }
                                     }
                                 )
                             }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        

        
    }
}

@Composable
fun SectionLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
fun TweaksSectionTitle(text: String) {
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
fun FreqLimitSliderItem(
    icon: ImageVector? = null,
    initialValue: Float,
    labels: List<String>,
    onSaved: (Float) -> Unit
) {
    var sliderValue by remember { mutableStateOf(initialValue) }
    val colorScheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = sliderValue / 6f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "OffsetProgress"
    )

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
                if (icon != null) {
                    LeadingIcon(icon = icon, contentDescription = stringResource(R.string.freq_offset))
                    Spacer(modifier = Modifier.width(16.dp)) 
                }
                Text(
                    text = stringResource(R.string.freq_offset),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            }
            
            Surface(
                color = if (sliderValue.roundToInt() == 0) colorScheme.surfaceVariant else colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (sliderValue.roundToInt() == 0) stringResource(R.string.disabled) else labels[sliderValue.roundToInt()],
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (sliderValue.roundToInt() == 0) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
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
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(colorScheme.primary.copy(alpha = 0.6f), colorScheme.primary)))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onSaved(sliderValue) },
            valueRange = 0f..6f,
            steps = 5,
            colors = SliderDefaults.colors(
                thumbColor = colorScheme.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.disabled), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
            Text(stringResource(R.string.str_40), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
        }
    }
}

@Composable
fun ExpressiveTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean,
    showArrow: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val cardBgColor = colorScheme.surfaceColorAtElevation(1.dp)

    val iconBoxBgColor by animateColorAsState(
        targetValue = if (highlight) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(400), 
        label = "iconBoxBgColorAnim"
    )

    val iconColor by animateColorAsState(
        targetValue = if (highlight) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "iconColorAnim"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        color = cardBgColor,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp) 
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)  
                    .clip(RoundedCornerShape(18.dp)) 
                    .background(iconBoxBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = iconColor,
                    modifier = Modifier.size(36.dp) 
                )

                if (showArrow) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight, 
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp), 
                        tint = iconColor.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = label, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300, delayMillis = 100)) +
                             scaleIn(initialScale = 0.95f, animationSpec = tween(300, delayMillis = 100)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(200)) +
                                    scaleOut(targetScale = 1.05f, animationSpec = tween(200))
                                )
                        },
                        label = "ValueTextAnimation"
                    ) { targetValue ->
                        Text(
                            text = targetValue, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = colorScheme.onSurfaceVariant, 
                            maxLines = 2, 
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                    }
                    

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


@Composable
fun TweakScreenTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onMoreClick: () -> Unit) {
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
                    text = stringResource(R.string.nav_tweaks),
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = stringResource(R.string.cd_menu)
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}

@Composable
fun BatteryHealthGuardSliderItem(
    initialValue: Float,
    onSaved: (Float) -> Unit
) {
    var sliderValue by remember { mutableStateOf(initialValue) }
    
    LaunchedEffect(initialValue) {
        sliderValue = initialValue
    }
    
    val colorScheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = (sliderValue - 50f) / 50f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "ChargeProgress"
    )

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
                    icon = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.charge_limit_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.charge_limit_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "${sliderValue.roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onSaved(sliderValue) },
            valueRange = 50f..100f,
            colors = SliderDefaults.colors(
                thumbColor = colorScheme.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
    }
}

