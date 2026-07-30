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

package com.ravenhub.app.ui.component


import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WebStories
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import com.ravenhub.app.R
import com.ravenhub.app.ui.util.getSupportedRefreshRatesPicker


private data class RefreshRatePickerOption(
    val titleString: String,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getRefreshRatePickerOptions(context: Context): List<RefreshRatePickerOption> {
    val options = mutableListOf<RefreshRatePickerOption>()
    
    options.add(
        RefreshRatePickerOption(
            titleString = "Smart Dynamic (Ravencore)",
            reason = "smart",
            icon = Icons.Rounded.Autorenew
        )
    )
    
    options.add(
        RefreshRatePickerOption(
            titleString = "System Auto (Android)",
            reason = "auto",
            icon = Icons.Rounded.BrightnessAuto
        )
    )
    
    val supported = getSupportedRefreshRatesPicker(context)
    options.addAll(
        supported.map { rate ->
            RefreshRatePickerOption(
                titleString = context.getString(R.string.refresh_rate_format, rate),
                reason = rate,
                icon = Icons.Outlined.WebStories
            )
        }
    )
    return options
}


@Composable
fun RefreshRatePickerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onRefreshRatePicker: (String) -> Unit
) {
    val context = LocalContext.current
    val options = getRefreshRatePickerOptions(context)

    CustomBottomSheet(
        visible = show,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = stringResource(R.string.RefreshRatePicker_Select),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            ExpressiveList(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = options.map { option ->
                    {
                        ExpressiveListItem(
                            headlineContent = { 
                                Text(
                                    text = option.titleString,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            leadingContent = { 
                                SmallLeadingIcon(icon = option.icon) 
                            },
                            onClick = {
                                onDismiss()
                                onRefreshRatePicker(option.reason)
                            }
                        )
                    }
                }
            )
        }
    }
}
