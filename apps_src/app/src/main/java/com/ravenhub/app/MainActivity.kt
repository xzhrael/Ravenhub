package com.ravenhub.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.security.MasterKeyManager
import com.ravenhub.app.data.planner.PlannerDataManager
import com.ravenhub.app.data.notes.NotesDataManager
import com.ravenhub.app.data.vault.VaultDataManager
import com.ravenhub.app.data.finance.FinanceDataManager
import com.ravenhub.app.ui.component.LocalAppHazeState
import com.ravenhub.app.ui.component.LocalBlurEnabled
import com.ravenhub.app.ui.component.RootDialogsProvider
import com.ravenhub.app.ui.mainscreens.*
import com.ravenhub.app.ui.security.LockMode
import com.ravenhub.app.ui.security.LockScreen
import com.ravenhub.app.ui.subscreens.ColorPaletteScreen
import com.ravenhub.app.ui.subscreens.DevShellScreen
import com.ravenhub.app.ui.subscreens.LanguageScreen
import com.ravenhub.app.ui.theme.RavenHubTheme
import com.ravenhub.app.ui.util.AppLifecycleManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

            var isBlurEnabled by remember {
                mutableStateOf(settingsPrefs.getBoolean("expressive_blur", true))
            }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "expressive_blur") {
                        isBlurEnabled = prefs.getBoolean("expressive_blur", true)
                    }
                }
                settingsPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    settingsPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            RavenHubTheme {
                MainAppContent(
                    isBlurEnabled = isBlurEnabled
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLifecycleManager.isAppInForeground.value = true
    }

    override fun onStop() {
        super.onStop()
        AppLifecycleManager.isAppInForeground.value = false

        if (!AppLifecycleManager.isLaunchingSystemPicker) {
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val mode = prefs.getString("auto_lock_mode", "exit") ?: "exit"

            when (mode) {
                "exit" -> MasterKeyManager.lock()
                "delay_10s" -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!AppLifecycleManager.isAppInForeground.value) {
                            MasterKeyManager.lock()
                        }
                    }, 10_000L)
                }
                "screen_lock" -> {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    if (powerManager != null && !powerManager.isInteractive) {
                        MasterKeyManager.lock()
                    }
                }
                "kill" -> {
                    // Lock key when process terminates
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    isBlurEnabled: Boolean
) {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    val hazeState = remember { HazeState() }
    var isUnlocked by remember { mutableStateOf(MasterKeyManager.isUnlocked()) }
    var isPinSet by remember { mutableStateOf(MasterKeyManager.isPinSetup(context)) }
    val hasCompletedGetStarted by remember { mutableStateOf(appPrefs.getBoolean("has_completed_get_started", false)) }
    var isLanguageSearching by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isUnlocked = MasterKeyManager.isUnlocked()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Guarantee data is always loaded when unlocked
    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            PlannerDataManager.load(context)
            NotesDataManager.load(context)
            VaultDataManager.load(context)
            FinanceDataManager.load(context)
        }
    }

    val navItems = remember {
        listOf(
            NavItem("home", "Home", Icons.Rounded.Home),
            NavItem("planner", "Planner", Icons.Rounded.Checklist),
            NavItem("notes", "Notes", Icons.Rounded.Description),
            NavItem("vault", "Vault", Icons.Rounded.Lock),
            NavItem("finance", "Finance", Icons.Rounded.Payments)
        )
    }

    val bottomBarRoutes = remember { setOf("home", "planner", "notes", "vault", "finance", "settings") }
    val isBottomSheetActive = remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalAppHazeState provides hazeState,
        LocalBlurEnabled provides isBlurEnabled,
        com.ravenhub.app.ui.component.LocalBottomSheetActive provides isBottomSheetActive
    ) {
        RootDialogsProvider {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavHost(
                    navController = navController,
                    startDestination = if (!hasCompletedGetStarted) "get_started" else if (!isPinSet) "set_pin" else if (!isUnlocked) "lock_screen" else "home",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .then(
                            if (isBlurEnabled) Modifier.hazeSource(state = hazeState) else Modifier
                        ),
                    enterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        if ((initialRoute == "get_started" || initialRoute == "lock_screen" || initialRoute == "set_pin") && targetRoute == "home") {
                            fadeIn(animationSpec = tween(600, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(600, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
                        } else {
                            val isSlideToRight = getTransitionDirection(initialRoute, targetRoute)
                            slideInHorizontally(
                                initialOffsetX = { if (isSlideToRight) it else -it },
                                animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                            ) + fadeIn(animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
                        }
                    },
                    exitTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        if ((initialRoute == "get_started" || initialRoute == "lock_screen" || initialRoute == "set_pin") && targetRoute == "home") {
                            fadeOut(animationSpec = tween(500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))) +
                            scaleOut(targetScale = 1.05f, animationSpec = tween(500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
                        } else {
                            val isSlideToRight = getTransitionDirection(initialRoute, targetRoute)
                            slideOutHorizontally(
                                targetOffsetX = { if (isSlideToRight) -it else it },
                                animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                            ) + fadeOut(animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
                        }
                    },
                    popEnterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        val isSlideToRight = getTransitionDirection(initialRoute, targetRoute)
                        slideInHorizontally(
                            initialOffsetX = { if (isSlideToRight) it else -it },
                            animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                        ) + fadeIn(animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
                    },
                    popExitTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route
                        val isSlideToRight = getTransitionDirection(initialRoute, targetRoute)
                        slideOutHorizontally(
                            targetOffsetX = { if (isSlideToRight) -it else it },
                            animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                        ) + fadeOut(animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
                    }
                ) {
                    composable("get_started") {
                        GetStartedScreen(navController = navController)
                    }

                    composable("set_pin") {
                        LockScreen(
                            mode = LockMode.SETUP,
                            onUnlocked = {
                                isUnlocked = true
                                isPinSet = true
                                PlannerDataManager.load(context)
                                NotesDataManager.load(context)
                                VaultDataManager.load(context)
                                FinanceDataManager.load(context)
                                navController.navigate("home") {
                                    popUpTo("set_pin") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("lock_screen") {
                        LockScreen(
                            mode = LockMode.UNLOCK,
                            onUnlocked = {
                                isUnlocked = true
                                PlannerDataManager.load(context)
                                NotesDataManager.load(context)
                                VaultDataManager.load(context)
                                FinanceDataManager.load(context)
                                navController.navigate("home") {
                                    popUpTo("lock_screen") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("home") { HomeScreen(navController) }
                    composable("planner") { PlannerScreen() }
                    composable("notes") { NotesScreen() }
                    composable("vault") { VaultScreen() }
                    composable("finance") { FinanceScreen() }
                    composable("settings") { SettingsScreen(navController) }
                    composable("language") { LanguageScreen(navController, isSearching = isLanguageSearching, onSearchingChange = { isLanguageSearching = it }) }
                    composable("devshell") { DevShellScreen(navController) }
                    composable("color_palette") { ColorPaletteScreen(navController) }
                }

                // Unified Top Floating Pill Bar Across All Screens
                AnimatedVisibility(
                    visible = currentRoute in setOf("home", "planner", "notes", "vault", "finance", "settings", "language", "devshell", "color_palette"),
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ) + fadeIn(
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ) + fadeOut(
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    TopHeaderBar(
                        currentRoute = currentRoute ?: "home",
                        isBlurEnabled = isBlurEnabled,
                        hazeState = hazeState,
                        onBackClick = { navController.popBackStack() },
                        onRightPillClick = {
                            if (currentRoute == "language") {
                                isLanguageSearching = !isLanguageSearching
                            } else if (currentRoute != "settings") {
                                navController.navigate("settings")
                            }
                        }
                    )
                }

                // Bottom Floating Navigation Bar
                AnimatedVisibility(
                    visible = currentRoute in bottomBarRoutes && !isBottomSheetActive.value,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ) + fadeIn(
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ) + fadeOut(
                        animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    BottomNavBar(
                        items = navItems,
                        selectedRoute = currentRoute ?: "home",
                        isBlurEnabled = isBlurEnabled,
                        hazeState = hazeState,
                        onItemSelected = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = false,
    hazeState: HazeState? = null
) {
    Surface(
        modifier = modifier
            .animateContentSize(
                animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
            )
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
        color = if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = if (isBlurEnabled) 0.dp else 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedRoute == item.route
                val itemWeight by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 0.8f,
                    animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
                    label = "itemWeight_${item.route}"
                )
                NavPill(
                    item = item,
                    isSelected = isSelected,
                    isBlurEnabled = isBlurEnabled,
                    onClick = { onItemSelected(item.route) },
                    modifier = Modifier.weight(itemWeight)
                )
            }
        }
    }
}

@Composable
private fun NavPill(
    item: NavItem,
    isSelected: Boolean,
    isBlurEnabled: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(300),
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "contentColor"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(52.dp)
            .scale(scale),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isSelected) 12.dp else 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = expandHorizontally(
                    animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
                    expandFrom = Alignment.Start
                ) + fadeIn(animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))),
                exit = shrinkHorizontally(
                    animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopHeaderBar(
    currentRoute: String,
    isBlurEnabled: Boolean,
    hazeState: HazeState?,
    onBackClick: () -> Unit,
    onRightPillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isSubScreen = currentRoute in setOf("settings", "language", "devshell", "color_palette")

    val pageTitle = when (currentRoute) {
        "home" -> "RAVENHUB"
        "planner" -> "PLANNER"
        "notes" -> "NOTES"
        "vault" -> "VAULT"
        "finance" -> "FINANCE"
        "settings" -> "SETTINGS"
        "language" -> "LANGUAGE"
        "color_palette" -> "THEME"
        else -> currentRoute.uppercase()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSubScreen) {
                // Floating Back Pill
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onBackClick() }
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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Left Page Indicator Pill
            Surface(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (isBlurEnabled && hazeState != null) {
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
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.primary,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        if (currentRoute == "settings") {
            Spacer(modifier = Modifier.size(48.dp))
        } else {
            // Right Floating Pill
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onRightPillClick() }
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentRoute == "language") Icons.Rounded.Search else Icons.Rounded.Settings,
                        contentDescription = if (currentRoute == "language") "Search" else "Settings",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private fun getTransitionDirection(initialRoute: String?, targetRoute: String?): Boolean {
    val routeOrder = listOf(
        "get_started", "set_pin", "lock_screen",
        "home", "planner", "notes", "vault", "finance",
        "settings", "language", "devshell", "color_palette"
    )
    val initialIndex = routeOrder.indexOf(initialRoute ?: "").let { if (it == -1) 0 else it }
    val targetIndex = routeOrder.indexOf(targetRoute ?: "").let { if (it == -1) 0 else it }
    return targetIndex > initialIndex
}
