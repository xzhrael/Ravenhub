@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ravenhub.app.ui.subscreens

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveListItem
import com.ravenhub.app.ui.component.LocalAppHazeState
import com.ravenhub.app.ui.component.LocalBlurEnabled
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import java.util.Locale

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    navController: NavController,
    isSearching: Boolean = false,
    onSearchingChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalAppHazeState.current

    val languages = remember {
        listOf(
            LanguageItem("en", "English", "English"),
            LanguageItem("id", "Indonesian", "Bahasa Indonesia"),
            LanguageItem("es", "Spanish", "Español"),
            LanguageItem("fr", "French", "Français"),
            LanguageItem("de", "German", "Deutsch"),
            LanguageItem("ja", "Japanese", "日本語"),
            LanguageItem("zh", "Chinese", "中文"),
            LanguageItem("ru", "Russian", "Русский")
        )
    }

    val currentLocale = remember {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) locales.get(0)?.language ?: "en"
        else Locale.getDefault().language
    }

    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) languages
        else languages.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nativeName.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

                AnimatedVisibility(
                    visible = isSearching,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search language...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchQuery = ""
                                onSearchingChange(false)
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close search")
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                // Languages List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ExpressiveList(
                            content = filteredLanguages.map { lang ->
                                val isSelected = currentLocale == lang.code
                                {
                                    ExpressiveListItem(
                                        headlineContent = {
                                            Text(
                                                text = lang.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = lang.nativeName,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingContent = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            val appLocales = LocaleListCompat.forLanguageTags(lang.code)
                                            AppCompatDelegate.setApplicationLocales(appLocales)
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
