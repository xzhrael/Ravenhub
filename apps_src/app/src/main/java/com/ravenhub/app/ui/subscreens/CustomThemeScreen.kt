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

package com.ravenhub.app.ui.subscreens


import android.app.Activity
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.blurEffect
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlinx.coroutines.launch
import com.ravenhub.app.R
import com.ravenhub.app.ui.component.*
import com.ravenhub.app.ui.theme.ColorMode
import com.ravenhub.app.ui.theme.ThemeController
import com.ravenhub.app.ui.theme.animateColorSchemeAsState
import com.ravenhub.app.ui.util.clearHeaderImage
import com.ravenhub.app.ui.util.getBannerGradientAlpha
import com.ravenhub.app.ui.util.getHeaderImage
import com.ravenhub.app.ui.util.isBannerImageEnabled
import com.ravenhub.app.ui.util.saveHeaderImage
import com.ravenhub.app.ui.util.saveMediaDirectly
import com.ravenhub.app.ui.util.setBannerGradientAlpha
import com.ravenhub.app.ui.util.setBannerImageEnabled


private val keyColorOptions = listOf(
    Color(0xFF388E3C).toArgb(), // mint
    Color(0xFF7E57C2).toArgb(), // lavender
    Color(0xFF0288D1).toArgb(), // ocean
    Color(0xFF757575).toArgb()  // monochrome
)

@Composable
fun ColorPaletteScreen(navController: NavController) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current 
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var isBannerEnabled by rememberSaveable { 
        mutableStateOf(context.isBannerImageEnabled()) 
    }

    var bannerGradientAlpha by rememberSaveable { 
        mutableFloatStateOf(context.getBannerGradientAlpha()) 
    }

    var customBannerUri by remember { 
        mutableStateOf(context.getHeaderImage()) 
    }
    

    var pendingCropUriPath by rememberSaveable { 
        mutableStateOf<String?>(null) 
    }
    

    var isBlurEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean("expressive_blur_ui", false))
    }
    val hazeState = remember { HazeState() }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    
    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                val uriString = it.toString()
                context.saveHeaderImage(uriString)
                customBannerUri = uriString
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Banner image updated!")
                }
            }
        } else {
            pendingCropUriPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        pendingCropUriPath = null
    }
    
    val colorScheme = MaterialTheme.colorScheme 
    
    
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
            val mimeType = context.contentResolver.getType(sourceUri) ?: ""
            val isVideo = mimeType.startsWith("video/")
            val isGif = mimeType == "image/gif"
            val isVideoOrGif = isVideo || isGif
    

            var sizeBytes = 0L
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
            val maxSize = 50 * 1024 * 1024L
            if (sizeBytes > maxSize) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Maximum video size is 50MB")
                }
                return@let
            }
    

            if (isVideo) {
                var durationMs = 0L
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, sourceUri)
                    val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationMs = timeString?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
    
                if (durationMs > 30000L) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Video duration must be 30s or less")
                    }
                    return@let
                }
            }
    

            if (isVideoOrGif) {

                coroutineScope.launch {
                    val extension = if (isVideo) "mp4" else "gif"
                    val savedUriString = context.saveMediaDirectly(sourceUri, extension)
                    
                    if (savedUriString != null) {
                        context.saveHeaderImage(savedUriString)
                        customBannerUri = savedUriString
                        snackbarHostState.showSnackbar("Media banner saved successfully!")
                    } else {
                        snackbarHostState.showSnackbar("Failed to save media banner")
                    }
                }
            } else {

                val bannerDir = File(context.filesDir, "banners")
                if (!bannerDir.exists()) bannerDir.mkdirs()
                
                val destinationFile = File(bannerDir, "banner_${System.currentTimeMillis()}.jpg")
                pendingCropUriPath = destinationFile.absolutePath 
                val destinationUri = Uri.fromFile(destinationFile)
                
                val options = UCrop.Options().apply {
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(false)
                    setToolbarColor(colorScheme.surface.toArgb()) 
                    setToolbarWidgetColor(colorScheme.onSurface.toArgb()) 
                    setRootViewBackgroundColor(colorScheme.surfaceContainerLowest.toArgb()) 
                    setActiveControlsWidgetColor(colorScheme.primary.toArgb()) 
                    setCropFrameColor(colorScheme.primary.toArgb()) 
                    setCropGridColor(colorScheme.primary.copy(alpha = 0.5f).toArgb()) 
                    setDimmedLayerColor(colorScheme.scrim.copy(alpha = 0.6f).toArgb())
                }
                
                val uCrop = UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(20f, 9f)
                    .withOptions(options)
    
                cropLauncher.launch(uCrop.getIntent(context))
            }
        }
    }
    
    
    var currentColorMode by remember { 
        mutableStateOf(ThemeController.getAppSettings(context).colorMode) 
    }
    var currentKeyColor by remember { 
        mutableIntStateOf(ThemeController.getAppSettings(context).keyColor) 
    }
    var currentColorSpec by remember { 
        mutableStateOf(ThemeController.getAppSettings(context).colorSpec) 
    }

    val isDark = currentColorMode.getDarkThemeValue(isSystemInDarkTheme())
    val amoledMode = currentColorMode == ColorMode.DARKAMOLED

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {},
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding -> 
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .padding(top = innerPadding.calculateTopPadding() + 90.dp), 
                        contentAlignment = Alignment.Center
                    ) {
                        ThemePreviewCard(
                            keyColor = currentKeyColor,
                            colorSpec = currentColorSpec,
                            isDark = isDark, 
                            isAmoled = amoledMode,
                            isLandscape = true
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(0.6f) 
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 90.dp, 
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        settingsItems(
                            currentColorMode = currentColorMode,
                            currentKeyColor = currentKeyColor,
                            currentColorSpec = currentColorSpec,
                            isDark = isDark,
                            isBannerEnabled = isBannerEnabled,
                            bannerGradientAlpha = bannerGradientAlpha,
                            customBannerUri = customBannerUri,
                            isBlurEnabled = isBlurEnabled,
                            prefs = prefs,
                            onColorModeChange = { currentColorMode = it },
                            onKeyColorChange = { currentKeyColor = it },
                            onColorSpecChange = { currentColorSpec = it },
                            onBannerEnabledChange = { 
                                isBannerEnabled = it 
                                context.setBannerImageEnabled(it)
                            },
                            onBannerGradientAlphaChange = {
                                bannerGradientAlpha = it
                                context.setBannerGradientAlpha(it)
                            },
                            onBannerUpdated = { customBannerUri = it },
                            onBlurEnabledChange = {
                                isBlurEnabled = it
                                prefs.edit { putBoolean("expressive_blur_ui", it) }
                            },
                            imagePicker = imagePicker,
                            context = context,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 90.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ThemePreviewCard(
                            keyColor = currentKeyColor, 
                            colorSpec = currentColorSpec,
                            isDark = isDark, 
                            isAmoled = amoledMode,
                            isLandscape = false
                        )
                    }

                    settingsItems(
                        currentColorMode = currentColorMode,
                        currentKeyColor = currentKeyColor,
                        currentColorSpec = currentColorSpec,
                        isDark = isDark,
                        isBannerEnabled = isBannerEnabled,
                        isBlurEnabled = isBlurEnabled,
                        bannerGradientAlpha = bannerGradientAlpha,
                        customBannerUri = customBannerUri,
                        prefs = prefs,
                        onColorModeChange = { currentColorMode = it },
                        onKeyColorChange = { currentKeyColor = it },
                        onColorSpecChange = { currentColorSpec = it },
                        onBannerEnabledChange = { 
                            isBannerEnabled = it 
                            context.setBannerImageEnabled(it)
                        },
                        onBannerGradientAlphaChange = {
                            bannerGradientAlpha = it
                            context.setBannerGradientAlpha(it)
                        },
                        onBlurEnabledChange = {
                            isBlurEnabled = it
                            prefs.edit { putBoolean("expressive_blur_ui", it) }
                        },
                        onBannerUpdated = { customBannerUri = it },
                        imagePicker = imagePicker,
                        context = context,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope
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
                // Left Back Pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
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
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.settingsItems(
    currentColorMode: ColorMode,
    currentKeyColor: Int,
    currentColorSpec: ColorSpec.SpecVersion,
    isDark: Boolean,
    isBannerEnabled: Boolean,
    bannerGradientAlpha: Float,
    customBannerUri: String?,
    isBlurEnabled: Boolean,
    prefs: android.content.SharedPreferences,
    onColorModeChange: (ColorMode) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onColorSpecChange: (ColorSpec.SpecVersion) -> Unit,
    onBannerEnabledChange: (Boolean) -> Unit,
    onBannerGradientAlphaChange: (Float) -> Unit,
    onBannerUpdated: (String?) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    imagePicker: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Accent Color",
                modifier = Modifier.padding(horizontal = 28.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.99f }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                0.08f to Color.Black,
                                0.92f to Color.Black,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    ColorButton(
                        color = Color.Unspecified,
                        isSelected = currentKeyColor == 0,
                        isDark = isDark,
                        colorSpec = currentColorSpec,
                        onClick = {
                            onKeyColorChange(0)
                            prefs.edit { putInt("key_color", 0) }
                        }
                    )
                }

                items(keyColorOptions) { colorInt ->
                    ColorButton(
                        color = Color(colorInt),
                        isSelected = currentKeyColor == colorInt,
                        isDark = isDark,
                        colorSpec = currentColorSpec,
                        onClick = {
                            onKeyColorChange(colorInt)
                            prefs.edit { putInt("key_color", colorInt) }
                        }
                    )
                }
            }
        }
    }

    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Appearance",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            val options = listOf(
                ColorMode.SYSTEM, ColorMode.LIGHT, ColorMode.DARK, ColorMode.DARKAMOLED
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                options.forEachIndexed { index, mode ->
                    ToggleButton(
                        checked = currentColorMode == mode,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onColorModeChange(mode)
                                prefs.edit { putInt("color_mode", mode.value) }
                            }
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                ColorMode.LIGHT -> Icons.Filled.Brightness7
                                ColorMode.DARK -> Icons.Filled.Brightness3
                                ColorMode.DARKAMOLED -> Icons.Filled.Brightness1
                            },
                            contentDescription = mode.name
                        )
                    }
                }
            }
        }
    }
    
    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Hero Banner Customization",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            content = buildList {
                

                add {
                    Column {
                        ExpressiveSwitchItem(
                            icon = Icons.Outlined.Image,
                            title = "Enable Header Banner",
                            checked = isBannerEnabled,
                            onCheckedChange = onBannerEnabledChange
                        )
                        
                        AnimatedVisibility(
                            visible = isBannerEnabled,
                            enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()

                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) 
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                ) {
                                    OutlinedButton(
                                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Pick Media", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            context.clearHeaderImage()
                                            onBannerUpdated(null)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Default banner restored")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 50.dp, bottomEnd = 50.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Default", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
        
        AnimatedVisibility(
            visible = isBannerEnabled,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
        ) {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            content = buildList {

                add {
                    AnimatedVisibility(
                        visible = isBannerEnabled,
                        enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                        exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LeadingIcon(icon = Icons.Outlined.Gradient)
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = "Adjust Gradient Overlay",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            BannerGradientPreview(
                                gradientAlpha = bannerGradientAlpha,
                                customBannerUri = customBannerUri
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gradient Opacity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${(bannerGradientAlpha * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { onBannerGradientAlphaChange(0.5f) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Restore,
                                                contentDescription = "Reset",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Slider(
                                    value = bannerGradientAlpha,
                                    onValueChange = { newValue ->
                                        val snappedValue = if (newValue in 0.47f..0.53f) 0.5f else newValue
                                        onBannerGradientAlphaChange(snappedValue)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        )
    }
    
    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Interface & Effects",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            content = buildList {
                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.BlurOn,
                        title = "Expressive Blur Effects",
                        summary = "Apply real-time blur state to cards and dialogs",
                        checked = isBlurEnabled,
                        onCheckedChange = onBlurEnabledChange
                    )
                }
            }
        )
    }
}

@Composable
fun PaletteTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
                    text = "Theme & Accent",
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

@Composable
private fun BannerGradientPreview(gradientAlpha: Float, customBannerUri: String?) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(20 / 9f)
            .clip(RoundedCornerShape(16.dp)),
        color = colorScheme.surfaceContainerHighest
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = customBannerUri,
                animationSpec = tween(500),
                label = "banner_crossfade"
            ) { uri ->
                MediaBannerRenderer(
                    uriString = uri,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colorScheme.surfaceContainerLow.copy(alpha = gradientAlpha))
                        )
                    )
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 12.dp),
                color = colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = "Preview", 
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(keyColor: Int, colorSpec: ColorSpec.SpecVersion, isDark: Boolean, isAmoled: Boolean, isLandscape: Boolean) { 
    val context = LocalContext.current
    
    val targetColorScheme = if (keyColor == 0) {
        val baseScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = baseScheme.primary,
            isDark = isDark,
            isAmoled = isAmoled,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(keyColor), 
            isDark = isDark, 
            isAmoled = isAmoled,
            specVersion = colorSpec
        )
    }

    val colorScheme = animateColorSchemeAsState(targetColorScheme)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), 
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 0.55f) 
                .aspectRatio(0.48f), 
            color = colorScheme.surface,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "RavenHub", 
                        style = MaterialTheme.typography.labelMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    com.ravenhub.app.ui.component.MediaBannerRenderer(
                        uriString = context.getHeaderImage(),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(55.dp),
                        color = colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {}
                    
                    Surface(
                        modifier = Modifier.weight(1f).height(55.dp),
                        color = colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {}
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    color = colorScheme.surfaceColorAtElevation(1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun ColorButton(color: Color, isSelected: Boolean, isDark: Boolean, colorSpec: ColorSpec.SpecVersion, onClick: () -> Unit) {
    val context = LocalContext.current
    
    val targetColorScheme = if (color == Color.Unspecified) {
        val baseScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = baseScheme.primary,
            isDark = isDark,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        val isMonochrome = color.toArgb() == Color(0xFF757575).toArgb()
        val paletteStyle = if (isMonochrome) PaletteStyle.Monochrome else PaletteStyle.TonalSpot
        rememberDynamicColorScheme(
            seedColor = color, 
            isDark = isDark, 
            style = paletteStyle,
            specVersion = colorSpec
        )
    }

    val colorScheme = animateColorSchemeAsState(targetColorScheme)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer, 
        modifier = Modifier.size(72.dp) 
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f, label = "scale")
            
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
