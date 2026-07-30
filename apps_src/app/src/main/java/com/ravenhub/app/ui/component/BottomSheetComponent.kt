package com.ravenhub.app.ui.component

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

val LocalBottomSheetActive = compositionLocalOf { mutableStateOf(false) }

@Composable
fun CustomBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalAppHazeState.current

    val sheetActiveState = LocalBottomSheetActive.current
    DisposableEffect(visible) {
        if (visible) {
            sheetActiveState.value = true
        }
        onDispose {
            sheetActiveState.value = false
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(visible) {
        if (visible) {
            dragOffset.snapTo(0f)
        }
    }

    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))
        ) + fadeOut(animationSpec = tween(250)),
        modifier = Modifier.zIndex(101f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = dragOffset.value.roundToInt()
                        )
                    }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffset.value > 250f) {
                                        onDismiss()
                                    } else {
                                        coroutineScope.launch {
                                            dragOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 300f))
                                    }
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    dragOffset.snapTo(maxOf(0f, dragOffset.value + dragAmount))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                    )
                }

                content()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
