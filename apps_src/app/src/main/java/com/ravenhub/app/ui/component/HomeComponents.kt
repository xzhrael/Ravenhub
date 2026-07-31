
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

package com.ravenhub.app.ui.component


import android.content.ContentResolver
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.system.Os
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.ravenhub.app.R
import com.ravenhub.app.ui.util.getBannerGradientAlpha
import com.ravenhub.app.ui.util.getChipsetName
import com.ravenhub.app.ui.util.getHeaderImage
import com.ravenhub.app.ui.util.getRealDeviceName


@Composable
fun MediaBannerRenderer(
    uriString: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (uriString == null) {
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }

        AsyncImage(
            model = R.drawable.banner_bg,
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
        return
    }

    val uri = Uri.parse(uriString)
    val mimeType = remember(uriString) {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uriString)
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) 
            ?: context.contentResolver.getType(uri) ?: ""
    }

    val isVideo = mimeType.startsWith("video/")

    if (isVideo) {
        var isVideoReady by remember { mutableStateOf(false) }

        val videoAlpha by animateFloatAsState(
            targetValue = if (isVideoReady) 1f else 0f,
            animationSpec = tween(500),
            label = "videoFade"
        )

        Box(modifier = modifier) {
            if (!isVideoReady) {
                CircularWavyProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AndroidView(
                factory = { ctx ->
                    val textureView = TextureView(ctx)
                    
                    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        var mediaPlayer: MediaPlayer? = null

                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(surfaceTexture)
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(ctx, uri)
                                setSurface(surface)
                                isLooping = true
                                setVolume(0f, 0f)

                                setOnInfoListener { _, what, _ ->
                                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                        isVideoReady = true 
                                        true
                                    } else {
                                        false
                                    }
                                }

                                setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                    val viewWidth = textureView.width.toFloat()
                                    val viewHeight = textureView.height.toFloat()
                                    
                                    if (viewWidth == 0f || viewHeight == 0f || videoWidth == 0 || videoHeight == 0) return@setOnVideoSizeChangedListener

                                    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
                                    val viewRatio = viewWidth / viewHeight

                                    val scaleX: Float
                                    val scaleY: Float

                                    if (videoRatio > viewRatio) {
                                        scaleX = (viewHeight * videoRatio) / viewWidth
                                        scaleY = 1f
                                    } else {
                                        scaleX = 1f
                                        scaleY = (viewWidth / videoRatio) / viewHeight
                                    }

                                    val matrix = Matrix()
                                    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
                                    textureView.setTransform(matrix)
                                }

                                prepareAsync() 
                                setOnPreparedListener { start() } 
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                        
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            return true
                        }
                        
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                    
                    textureView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(videoAlpha) 
            )
        }
    } else {
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }

        AsyncImage(
            model = uri,
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}




@Composable
fun HomeTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onRebootClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val smoothGradient = Brush.verticalGradient(
        0.0f to colorScheme.surface,
        0.4f to colorScheme.surface.copy(alpha = 0.9f),
        1.0f to Color.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(smoothGradient)
            .statusBarsPadding()
    ) {
        LargeFlexibleTopAppBar(
            title = { Text(text = "RavenHub", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            actions = {
                IconButton(onClick = onRebootClick) {
                    Icon(imageVector = Icons.Filled.PowerSettingsNew, contentDescription = "Reboot")
                }
            },
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}


@Composable
fun InfoTile(
    modifier: Modifier, 
    icon: ImageVector, 
    label: String, 
    value: String, 
    highlight: Boolean,
    showArrow: Boolean = false, 
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

            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun DeviceInfoCard() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }
    
    val uname = remember { Os.uname() }
    val kernelVer = remember { uname.release }
    val selinux = remember {
        try {
            val p = java.lang.Runtime.getRuntime().exec("getenforce")
            p.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) { "Enforcing" }
    }
    val appVer = remember { com.ravenhub.app.BuildConfig.VERSION_NAME }
    val chipsetName = remember { getChipsetName(context) }

    var realDeviceName by remember { mutableStateOf("${Build.MANUFACTURER} ${Build.MODEL}") }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            realDeviceName = getRealDeviceName(context)
        }
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expandArrowRotation"
    )

    Surface(
        shape = RoundedCornerShape(26.dp), 
        color = colorScheme.surfaceColorAtElevation(1.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .animateContentSize(animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallLeadingIcon(Icons.Outlined.Info)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Device Info", 
                    modifier = Modifier.weight(1f), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore, 
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { 
                        rotationZ = rotationAngle
                    }
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp) 
                ) {
                    DeviceInfoGridItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Device Name", 
                        value = realDeviceName
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp) 
                    ) {
                        DeviceInfoGridItem(
                            modifier = Modifier.weight(1f),
                            title = "Kernel Version", 
                            value = kernelVer
                        )
                        DeviceInfoGridItem(
                            modifier = Modifier.weight(1f),
                            title = "Chipset", 
                            value = chipsetName
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeviceInfoGridItem(
                            modifier = Modifier.weight(1f),
                            title = "SELinux Status", 
                            value = selinux
                        )
                        DeviceInfoGridItem(
                            modifier = Modifier.weight(1f),
                            title = "App Version", 
                            value = appVer
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DeviceInfoGridItem(modifier: Modifier = Modifier, title: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(


        modifier = modifier.height(86.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.5f), 
        shape = RoundedCornerShape(18.dp) 
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            

            Box(
                modifier = Modifier.weight(1f), 
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = value, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, 
                    color = colorScheme.onSurface,
                    maxLines = 2, 



                    lineHeight = 18.sp
                )
            }
        }
    }
}



@Composable
fun LinkCard(icon: ImageVector, titleRes: Int, descRes: Int, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    
    Surface(
        shape = shape, 
        color = colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            SmallLeadingIcon(icon)
            
            Spacer(modifier = Modifier.width(16.dp))
            

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(titleRes), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold, 
                    color = colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(descRes), 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "Open",
                modifier = Modifier.size(22.dp),
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}
