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

package com.ravenhub.app.ui.subscreens

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.ravenhub.app.ui.component.LocalAppHazeState
import com.ravenhub.app.ui.component.LocalBlurEnabled
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PerfRecord(
    val packageName: String,
    val dateStr: String,
    val minFps: Float,
    val avgFps: Float,
    val maxFps: Float,
    val minTemp: Float,
    val avgTemp: Float,
    val maxTemp: Float,
    val fpsPoints: List<Float>,
    val tempPoints: List<Float>
)

fun loadPerfRecord(packageName: String): PerfRecord? {
    val file1 = "/data/adb/.config/ravencore/perf_history/perf_history_$packageName.json"
    val file2 = "/data/adb/.config/ravencore/perf_history_$packageName.json"
    
    val res1 = Shell.cmd("cat $file1 2>/dev/null").exec()
    val jsonStr = if (res1.isSuccess && res1.out.isNotEmpty()) {
        res1.out.joinToString("\n")
    } else {
        val res2 = Shell.cmd("cat $file2 2>/dev/null").exec()
        if (res2.isSuccess && res2.out.isNotEmpty()) {
            res2.out.joinToString("\n")
        } else {
            return null
        }
    }
    return try {
        val obj = JSONObject(jsonStr)
        val epoch = obj.optLong("epoch", 0L)
        val dateStr = if (epoch > 0L) {
            val sdf = SimpleDateFormat("dd MMM 2026 • HH:mm", Locale.getDefault())
            sdf.format(Date(epoch * 1000))
        } else {
            "Just now"
        }
        
        val fpsArr = obj.getJSONArray("fps_points")
        val fpsPoints = mutableListOf<Float>()
        for (i in 0 until fpsArr.length()) {
            fpsPoints.add(fpsArr.getDouble(i).toFloat())
        }
        
        val tempArr = obj.getJSONArray("temp_points")
        val tempPoints = mutableListOf<Float>()
        for (i in 0 until tempArr.length()) {
            tempPoints.add(tempArr.getDouble(i).toFloat())
        }
        
        PerfRecord(
            packageName = obj.getString("package_name"),
            dateStr = dateStr,
            minFps = obj.getDouble("min_fps").toFloat(),
            avgFps = obj.getDouble("avg_fps").toFloat(),
            maxFps = obj.getDouble("max_fps").toFloat(),
            minTemp = obj.getDouble("min_temp").toFloat(),
            avgTemp = obj.getDouble("avg_temp").toFloat(),
            maxTemp = obj.getDouble("max_temp").toFloat(),
            fpsPoints = fpsPoints,
            tempPoints = tempPoints
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun PerfRecordDetailScreen(navController: NavController, packageName: String?) {
    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalAppHazeState.current
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var record by remember { mutableStateOf<PerfRecord?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(packageName) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            loadPerfRecord(packageName ?: "")
        }
        record = loaded
        isLoading = false
    }

    val appLabel = remember(packageName) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName ?: "", 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            "Game"
        }
    }

    val appIconDrawable = remember(packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationIcon(packageName ?: "")
        } catch (_: Exception) {
            null
        }
    }

    val deviceModel = Build.MODEL
    val chipset = if (Build.HARDWARE.contains("qcom") || Build.HARDWARE.contains("sm")) "Snapdragon 680" else Build.HARDWARE.uppercase()
    val androidVersion = "Android ${Build.VERSION.RELEASE}"
    val kernelVersion = System.getProperty("os.version")?.split("-")?.firstOrNull() ?: "5.4.147"

    MaterialExpressiveTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else {
                val currentRecord = record
                
                if (currentRecord == null) {
                    // Empty state container with instruction alert
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "No Record",
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Performance Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No recorded performance history found for $appLabel.\n\nPlease launch and play the game first. The background daemon will automatically record your FPS and temperature statistics to draw the charts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { navController.popBackStack() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Go Back")
                                }
                            }
                        }
                    }
                } else {
                    val dateText = currentRecord.dateStr
                    val minFpsText = String.format("%.0f", currentRecord.minFps)
                    val avgFpsText = String.format("%.0f", currentRecord.avgFps)
                    val maxFpsText = String.format("%.0f", currentRecord.maxFps)
                    
                    val minTempText = String.format("%.1f°C", currentRecord.minTemp)
                    val avgTempText = String.format("%.1f°C", currentRecord.avgTemp)
                    val maxTempText = String.format("%.1f°C", currentRecord.maxTemp)

                    val fpsPoints = currentRecord.fpsPoints
                    val tempPoints = currentRecord.tempPoints

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 100.dp, bottom = 32.dp)
                    ) {
                        // Card 1: Game & Summary Metrics
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer.copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Game details row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (appIconDrawable != null) {
                                            Image(
                                                bitmap = appIconDrawable.toBitmap().asImageBitmap(),
                                                contentDescription = "Game Icon",
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Gamepad, 
                                                    "Game", 
                                                    tint = colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                appLabel,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                dateText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "$avgFpsText.0 FPS",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Metrics Grid (FPS & Temp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("MIN FPS", style = MaterialTheme.typography.labelSmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                            Text(minFpsText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("AVG FPS", style = MaterialTheme.typography.labelSmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                            Text(avgFpsText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("MAX FPS", style = MaterialTheme.typography.labelSmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                            Text(maxFpsText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("MIN TEMP", style = MaterialTheme.typography.labelSmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                            Text(minTempText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("AVG TEMP", style = MaterialTheme.typography.labelSmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                            Text(avgTempText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("MAX TEMP", style = MaterialTheme.typography.labelSmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                            Text(maxTempText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }

                        // Card 2: Performance twin Y-axis line chart
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Performance graph",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            val topPadding = 20f
                                            val bottomPadding = 40f
                                            val leftPadding = 50f
                                            val rightPadding = 50f
                                            
                                            val plotW = w - leftPadding - rightPadding
                                            val plotH = h - topPadding - bottomPadding
                                            
                                            // 1. Grid lines (horizontal)
                                            val gridCount = 4
                                            for (i in 0 until gridCount) {
                                                val y = topPadding + (plotH * i / (gridCount - 1))
                                                drawLine(
                                                    color = Color.Gray.copy(alpha = 0.2f),
                                                    start = Offset(leftPadding, y),
                                                    end = Offset(w - rightPadding, y),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }
                                            
                                            // 2. Real FPS Data Path
                                            val fpsPath = Path()
                                            for (i in fpsPoints.indices) {
                                                val x = leftPadding + (plotW * i / (fpsPoints.size - 1).coerceAtLeast(1))
                                                val pct = (fpsPoints[i] / 120f).coerceIn(0f, 1f)
                                                val y = topPadding + plotH - (pct * plotH)
                                                if (i == 0) fpsPath.moveTo(x, y) else fpsPath.lineTo(x, y)
                                            }
                                            drawPath(fpsPath, Color.LightGray, style = Stroke(width = 2.dp.toPx()))
                                            
                                            // 3. Real Temp Data Path
                                            val tempPath = Path()
                                            for (i in tempPoints.indices) {
                                                val x = leftPadding + (plotW * i / (tempPoints.size - 1).coerceAtLeast(1))
                                                val pct = (tempPoints[i] / 46f).coerceIn(0f, 1f)
                                                val y = topPadding + plotH - (pct * plotH)
                                                if (i == 0) tempPath.moveTo(x, y) else tempPath.lineTo(x, y)
                                            }
                                            drawPath(tempPath, Color.Red, style = Stroke(width = 2.dp.toPx()))
                                        }
                                        
                                        // Left Axis Labels (FPS)
                                        Column(
                                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("120", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("80", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("40", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("0", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(28.dp))
                                        }
                                        
                                        // Right Axis Labels (Temp)
                                        Column(
                                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text("46°", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("30°", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("15°", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("0°", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(28.dp))
                                        }
                                        
                                        // Bottom Timeline Labels
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("00:00", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("04:05", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("08:10", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("12:15", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                            Text("16:20", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Graph Legend Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).background(Color.Red))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Temp", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(modifier = Modifier.size(8.dp).background(Color.LightGray))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("FPS", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Eco Mode", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(modifier = Modifier.width(12.dp).height(2.dp).background(Color.Red))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Xtreme Thermal", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }

                        // Card 3: Device System Specs
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Model", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(deviceModel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Chipset", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(chipset, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Android", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(androidVersion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Kernel", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(kernelVersion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        // Delete Data Button
                        item {
                            Button(
                                onClick = { 
                                    Shell.cmd("rm -f /data/adb/.config/ravencore/perf_history/perf_history_$packageName.json /data/adb/.config/ravencore/perf_history_$packageName.json").exec()
                                    record = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .height(54.dp),
                                shape = RoundedCornerShape(27.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, "Delete")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "DELETE DATA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onError
                                )
                            }
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
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RECORD DETAIL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Performance • Temperature",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
