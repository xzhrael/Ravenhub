@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ravenhub.app.ui.mainscreens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ravenhub.app.MainActivity
import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveSwitchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun ExpressiveSystemBackground(
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    val targetOffsetA = when (currentPage) {
        0 -> Offset(0.2f, 0.2f)
        1 -> Offset(0.8f, 0.1f)
        2 -> Offset(0.5f, 0.5f)
        else -> Offset(0.5f, 0.5f)
    }

    val targetOffsetB = when (currentPage) {
        0 -> Offset(0.8f, 0.8f)
        1 -> Offset(0.2f, 0.7f)
        2 -> Offset(0.5f, 0.5f)
        else -> Offset(0.5f, 0.5f)
    }

    val colorPrimary = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val colorSecondary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
    val colorSurface = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        drawRect(color = colorSurface)

        val sizePx = size.minDimension
        val posA = Offset(
            x = targetOffsetA.x * size.width,
            y = targetOffsetA.y * size.height
        )
        val posB = Offset(
            x = targetOffsetB.x * size.width,
            y = targetOffsetB.y * size.height
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorPrimary, Color.Transparent),
                center = posA,
                radius = sizePx * 0.6f
            ),
            center = posA,
            radius = sizePx * 0.6f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorSecondary, Color.Transparent),
                center = posB,
                radius = sizePx * 0.5f
            ),
            center = posB,
            radius = sizePx * 0.5f
        )
    }
}

@Composable
fun GetStartedScreen(navController: NavController) {
    var currentPage by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    var isBlurEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("expressive_blur_ui", false)) }
    var enableNotifications by remember { mutableStateOf(settingsPrefs.getBoolean("enable_notifications", true)) }

    var isFinalizing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val totalPages = 3

    val contentAlpha by animateFloatAsState(
        targetValue = if (isFinalizing) 0f else 1f,
        animationSpec = tween(300),
        label = "contentAlpha"
    )

    Scaffold(
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 24.dp)) {
                AnimatedVisibility(
                    visible = !isFinalizing,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(300)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(totalPages) { index ->
                                val isSelected = index == currentPage
                                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "indicatorWidth")

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .height(8.dp)
                                        .width(width)
                                        .background(color, CircleShape)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (currentPage > 0) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (currentPage > 0) currentPage--
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Back", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (currentPage < totalPages - 1) {
                                        currentPage++
                                    } else {
                                        isFinalizing = true
                                        coroutineScope.launch(Dispatchers.IO) {
                                            appPrefs.edit().putBoolean("has_completed_get_started", true).commit()

                                            delay(1000)
                                            withContext(Dispatchers.Main) {
                                                val intent = Intent(context, MainActivity::class.java)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                                (context as? Activity)?.finish()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Text(
                                    text = if (currentPage == totalPages - 1) "Let's Go" else "Next",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ExpressiveSystemBackground(
                currentPage = currentPage,
                modifier = Modifier.fillMaxSize()
            )

            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 20.dp)
                    .alpha(contentAlpha),
                transitionSpec = {
                    val slideSpec = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 300f)
                    val fadeSpec = tween<Float>(500)

                    if (targetState > initialState) {
                        (slideInHorizontally(slideSpec) { fullWidth -> fullWidth } + fadeIn(fadeSpec)) togetherWith
                                (slideOutHorizontally(slideSpec) { fullWidth -> -fullWidth } + fadeOut(fadeSpec))
                    } else {
                        (slideInHorizontally(slideSpec) { fullWidth -> -fullWidth } + fadeIn(fadeSpec)) togetherWith
                                (slideOutHorizontally(slideSpec) { fullWidth -> fullWidth } + fadeOut(fadeSpec))
                    }.using(SizeTransform(clip = false))
                },
                label = "GetStartedSlideAnimation"
            ) { page ->
                val enterTransition = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    enterTransition.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> {
                            Text(
                                text = "WELCOME TO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                modifier = Modifier.graphicsLayer {
                                    alpha = enterTransition.value
                                    translationY = 50f * (1f - enterTransition.value)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "RavenHub",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer {
                                    val titleProgress = (enterTransition.value - 0.2f).coerceAtLeast(0f) / 0.8f
                                    alpha = titleProgress
                                    translationY = 60f * (1f - titleProgress)
                                    scaleX = 0.9f + (0.1f * titleProgress)
                                    scaleY = 0.9f + (0.1f * titleProgress)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Your encrypted personal planner, notes vault & finance manager.",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.graphicsLayer {
                                    val subProgress = (enterTransition.value - 0.3f).coerceAtLeast(0f) / 0.7f
                                    alpha = subProgress
                                    translationY = 40f * (1f - subProgress)
                                }
                            )
                        }
                        1 -> {
                            Text(
                                text = "Customize RavenHub",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.graphicsLayer {
                                    alpha = enterTransition.value
                                    translationY = 40f * (1f - enterTransition.value)
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            ExpressiveList(
                                content = listOf(
                                    {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.BlurOn,
                                            title = "Expressive Blur UI",
                                            checked = isBlurEnabled,
                                            onCheckedChange = { isChecked ->
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                isBlurEnabled = isChecked
                                                settingsPrefs.edit().putBoolean("expressive_blur_ui", isChecked).apply()
                                            }
                                        )
                                    },
                                    {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Filled.Notifications,
                                            title = "Task & Habit Reminders",
                                            checked = enableNotifications,
                                            onCheckedChange = { isChecked ->
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                enableNotifications = isChecked
                                                settingsPrefs.edit().putBoolean("enable_notifications", isChecked).apply()
                                            }
                                        )
                                    }
                                )
                            )
                        }
                        2 -> {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(88.dp)
                                    .graphicsLayer {
                                        alpha = enterTransition.value
                                        scaleX = 0.5f + (0.5f * enterTransition.value)
                                        scaleY = 0.5f + (0.5f * enterTransition.value)
                                    }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "You're All Set!",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "RavenHub is ready to keep your schedule, notes, and finance secure.",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isFinalizing,
                enter = fadeIn(tween(500, delayMillis = 200)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Setting up RavenHub...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
