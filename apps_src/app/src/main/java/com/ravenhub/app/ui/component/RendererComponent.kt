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

package com.ravenhub.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ravenhub.app.R

private data class RendererOption(
    val titleRes: String,
    val reason: String,
    val icon: ImageVector
)

private fun getRendererOptions(): List<RendererOption> {
    return listOf(
        RendererOption("OpenGL (SkiaGL)", "skiagl", Icons.Rounded.Layers),
        RendererOption("Vulkan (SkiaVK)", "skiavk", Icons.Rounded.Layers),
        RendererOption("ANGLE", "angle", Icons.Rounded.Layers),
        RendererOption("Default (System)", "default", Icons.Rounded.Layers)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RendererDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onRenderer: (String) -> Unit
) {
    val options = getRendererOptions()

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
                text = stringResource(R.string.Renderer_Select),
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
                                    text = option.titleRes,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingContent = {
                                LeadingIcon(icon = option.icon)
                            },
                            onClick = {
                                onRenderer(option.reason)
                                onDismiss()
                            }
                        )
                    }
                }
            )
        }
    }
}
