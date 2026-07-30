/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ravenhub.app.ui.mainscreens


import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import kotlinx.coroutines.launch
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.blurEffect
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.component.AppIconImage
import com.ravenhub.app.ui.viewmodel.ApplistViewmodel


@Composable
fun ApplistScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: ApplistViewmodel = viewModel()
    val colorScheme = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    
    val pullToRefreshState = rememberPullToRefreshState()
    
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val hazeState = remember { HazeState() }
    val isBlurEnabled = LocalBlurEnabled.current
    LaunchedEffect(Unit) {
        viewModel.showSystemApps = prefs.getBoolean("show_system_apps", false)
        if (ApplistViewmodel.apps.isEmpty()) {
            viewModel.loadApps(context)
        }
    }

    var isSearchMode by remember { mutableStateOf(false) }
    var isCompileRunning by remember { mutableStateOf(false) }
    var compileStatusText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
    
    BackHandler(enabled = isSearchMode) {
        viewModel.clearSearch()
        isSearchMode = false
        focusManager.clearFocus()
    }

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            focusRequester.requestFocus()
        } else {
            viewModel.clearSearch()
            focusManager.clearFocus()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (ApplistViewmodel.apps.isNotEmpty()) {
                    viewModel.refreshAppConfigStatus()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    MaterialExpressiveTheme {
        Scaffold(
            topBar = {}
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        state = pullToRefreshState,
                        isRefreshing = viewModel.isRefreshing,
                        onRefresh = { 
                            AppIconCache.clear()
                            viewModel.loadApps(context, forceRefresh = true) 
                        }
                    )
            ) {
                val appsToDisplay = viewModel.filteredApps
                
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 90.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = appsToDisplay,
                        key = { _, app -> app.packageName }
                    ) { _, app ->
                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    onClick = { navController.navigate("app_settings/${app.packageName}") },
                                    headlineContent = {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Column {
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row {
                                                if (app.isEnabledInConfig) {
                                                    LabelText(
                                                        text = stringResource(R.string.label_enabled),
                                                        color = Color(0xFF4CAF50)
                                                    )
                                                } else {
                                                    LabelText(stringResource(R.string.label_disabled), MaterialTheme.colorScheme.error)
                                                }
                                                if (app.isRecommended) {
                                                    LabelText(stringResource(R.string.label_recommended), MaterialTheme.colorScheme.primary)
                                                }
                                                if (app.isSystem) {
                                                    LabelText(stringResource(R.string.label_system), MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                        }
                                    },
                                    leadingContent = {
                                        AppIconImage(
                                            app = app,
                                            size = 48.dp
                                        )
                                    }
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (!isSearchMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left section (Back Pill container)
                            Box(
                                modifier = Modifier.width(96.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { navController.popBackStack() }
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
                                            text = stringResource(R.string.applist_title).uppercase(),
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

                            // Right section (Combined Pill container)
                            Box(
                                modifier = Modifier.width(96.dp),
                                contentAlignment = Alignment.CenterEnd
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        IconButton(onClick = { isSearchMode = true }) {
                                            Icon(Icons.Default.Search, "Search", tint = colorScheme.onSurface)
                                        }
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, "Menu", tint = colorScheme.onSurface)
                                        }
                                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.opt_games_btn)) },
                                                onClick = { 
                                                    menuExpanded = false
                                                    viewModel.autoDetectGames(context)
                                                    viewModel.runGameCompilation(
                                                        onStart = {
                                                            isCompileRunning = true
                                                            compileStatusText = "Preparing compiler..."
                                                        },
                                                        onProgress = { status ->
                                                            compileStatusText = status
                                                        },
                                                        onComplete = { count ->
                                                            isCompileRunning = false
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("Compiled $count games successfully!")
                                                            }
                                                        }
                                                    )
                                                },
                                                leadingIcon = { Icon(Icons.Default.SportsEsports, null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.menu_show_system_apps)) },
                                                trailingIcon = { Checkbox(viewModel.showSystemApps, null) },
                                                onClick = {
                                                    val newVal = !viewModel.showSystemApps
                                                    viewModel.showSystemApps = newVal
                                                    prefs.edit().putBoolean("show_system_apps", newVal).apply()
                                                    viewModel.loadApps(context, forceRefresh = false)
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Left Back Pill (Search mode active)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { isSearchMode = false }
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
                        // Expanded Search Pill (takes full remaining width)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxWidth()
                                .padding(start = 64.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .then(
                                    if (isBlurEnabled) {
                                        Modifier.hazeEffect(state = hazeState) {
                                            blurEffect {
                                                blurRadius = 24.dp
                                            }
                                        }
                                    } else Modifier
                                ),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isBlurEnabled) colorScheme.surfaceContainer.copy(alpha = 0.4f) else colorScheme.surfaceContainer,
                            shadowElevation = if (isBlurEnabled) 0.dp else 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                            ) {
                                TextField(
                                    value = viewModel.searchTextFieldValue,
                                    onValueChange = { viewModel.updateSearch(it) },
                                    placeholder = { Text("Search apps...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                                IconButton(onClick = { isSearchMode = false }) {
                                    Icon(Icons.Default.Clear, "Clear", tint = colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = viewModel.isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = innerPadding.calculateTopPadding())
                )
            }
        }
    }
}

@Composable
fun ApplistTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    isSearchMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    searchQuery: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    showSystemApps: Boolean,
    onToggleSystem: (Boolean) -> Unit,
    onOptimizeGames: () -> Unit,
    focusRequester: FocusRequester
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
        AnimatedContent(
            targetState = isSearchMode,
            transitionSpec = { 
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) 
            },
            label = "search_bar_transition"
        ) { searching ->
            if (searching) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text(stringResource(R.string.search_apps)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onSearchModeChange(false) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                        }
                    },
                    actions = {
                        if (searchQuery.text.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange(TextFieldValue("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            } else {
                LargeFlexibleTopAppBar(
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                     title = {
                        Text(
                            text = stringResource(R.string.applist_title).uppercase(),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { onSearchModeChange(true) }) {
                            Icon(Icons.Default.Search, stringResource(R.string.cd_search))
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.cd_menu))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.opt_games_btn)) },
                                onClick = { 
                                    onOptimizeGames()
                                    menuExpanded = false 
                                },
                                leadingIcon = { Icon(Icons.Default.SportsEsports, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_show_system_apps)) },
                                trailingIcon = { Checkbox(showSystemApps, null) },
                                onClick = { onToggleSystem(!showSystemApps); menuExpanded = false }
                            )
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun LabelText(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}
