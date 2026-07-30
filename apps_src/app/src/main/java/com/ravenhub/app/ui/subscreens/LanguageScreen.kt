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

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val isRecommended: Boolean = false
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

    val recommendedLanguages = remember {
        listOf(
            LanguageItem("", "System Default", "Default System", true),
            LanguageItem("en-rIN", "English (India)", "English (India)", true),
            LanguageItem("en-rUS", "English (United States)", "English (United States)", true),
            LanguageItem("id", "Indonesian", "Bahasa Indonesia", true),
            LanguageItem("ms", "Malay (Indonesia)", "Bahasa Melayu (Indonesia)", true)
        )
    }

    val allLanguages = remember {
        listOf(
        LanguageItem("af", "Afrikaans", "Afrikaans"),
        LanguageItem("am", "Amharic", "አማርኛ"),
        LanguageItem("ar", "Arabic", "العربية"),
        LanguageItem("as", "Assamese", "অসমীয়া"),
        LanguageItem("az", "Azerbaijani", "Azərbaycan"),
        LanguageItem("be", "Belarusian", "Belarusan"),
        LanguageItem("bg", "Bulgarian", "Български"),
        LanguageItem("bn", "Bengali", "বাংলা"),
        LanguageItem("bs", "Bosnian", "Bosanski"),
        LanguageItem("b+sr+Latn", "Serbian (Latin)", "Srpski"),
        LanguageItem("ca", "Catalan", "Català"),
        LanguageItem("cs", "Czech", "Čeština"),
        LanguageItem("da", "Danish", "Dansk"),
        LanguageItem("de", "German", "Deutsch"),
        LanguageItem("el", "Greek", "Ελληνικά"),
        LanguageItem("en-rAU", "English (Australia)", "English (Australia)"),
        LanguageItem("en-rCA", "English (Canada)", "English (Canada)"),
        LanguageItem("en-rGB", "English (United Kingdom)", "English (United Kingdom)"),
        LanguageItem("en-rIN", "English (India)", "English (India)"),
        LanguageItem("en-rUS", "English (United States)", "English (United States)"),
        LanguageItem("es", "Spanish", "Español"),
        LanguageItem("es-rUS", "Spanish (United States)", "Español (Estados Unidos)"),
        LanguageItem("et", "Estonian", "Eesti"),
        LanguageItem("eu", "Basque", "Euskara"),
        LanguageItem("fa", "Persian", "فارسی"),
        LanguageItem("fi", "Finnish", "Suomi"),
        LanguageItem("fr", "French", "Français"),
        LanguageItem("fr-rCA", "French (Canada)", "Français (Canada)"),
        LanguageItem("gl", "Galician", "Galego"),
        LanguageItem("gu", "Gujarati", "ગુજરાતી"),
        LanguageItem("hi", "Hindi", "हिन्दी"),
        LanguageItem("hr", "Croatian", "Hrvatski"),
        LanguageItem("hu", "Hungarian", "Magyar"),
        LanguageItem("hy", "Armenian", "Հայերեն"),
        LanguageItem("id", "Indonesian", "Bahasa Indonesia"),
        LanguageItem("is", "Icelandic", "Íslenska"),
        LanguageItem("it", "Italian", "Italiano"),
        LanguageItem("iw", "Hebrew", "עברית"),
        LanguageItem("ja", "Japanese", "日本語"),
        LanguageItem("ka", "Georgian", "ქართული"),
        LanguageItem("kk", "Kazakh", "Қазақ тілі"),
        LanguageItem("km", "Khmer", "ភាសាខ្មែរ"),
        LanguageItem("kn", "Kannada", "ಕನ್ನಡ"),
        LanguageItem("ko", "Korean", "한국어"),
        LanguageItem("ky", "Kyrgyz", "Кыргызча"),
        LanguageItem("lo", "Lao", "ລາວ"),
        LanguageItem("lt", "Lithuanian", "Lietuvių"),
        LanguageItem("lv", "Latvian", "Latviešu"),
        LanguageItem("mk", "Macedonian", "Македонски"),
        LanguageItem("ml", "Malayalam", "മലയാളം"),
        LanguageItem("mn", "Mongolian", "Монгол"),
        LanguageItem("mr", "Marathi", "मराठी"),
        LanguageItem("ms", "Malay (Indonesia)", "Bahasa Melayu (Indonesia)"),
        LanguageItem("my", "Burmese", "မြန်မာစာ"),
        LanguageItem("nb", "Norwegian Bokmål", "Norsk bokmål"),
        LanguageItem("ne", "Nepali", "नेपाली"),
        LanguageItem("nl", "Dutch", "Nederlands"),
        LanguageItem("or", "Odia", "ଓଡ଼ିଆ"),
        LanguageItem("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageItem("pl", "Polish", "Polski"),
        LanguageItem("pt", "Portuguese", "Português"),
        LanguageItem("pt-rBR", "Portuguese (Brazil)", "Português (Brasil)"),
        LanguageItem("pt-rPT", "Portuguese (Portugal)", "Português (Portugal)"),
        LanguageItem("ro", "Romanian", "Română"),
        LanguageItem("ru", "Russian", "Русский"),
        LanguageItem("si", "Sinhala", "සිංහල"),
        LanguageItem("sk", "Slovak", "Slovenčina"),
        LanguageItem("sl", "Slovenian", "Slovenščina"),
        LanguageItem("sq", "Albanian", "Shqip"),
        LanguageItem("sr", "Serbian", "Српски"),
        LanguageItem("sv", "Swedish", "Svenska"),
        LanguageItem("sw", "Swahili", "Kiswahili"),
        LanguageItem("ta", "Tamil", "தமிழ்"),
        LanguageItem("te", "Telugu", "తెలుగు"),
        LanguageItem("th", "Thai", "ไทย"),
        LanguageItem("tl", "Tagalog", "Tagalog"),
        LanguageItem("tr", "Turkish", "Türkçe"),
        LanguageItem("uk", "Ukrainian", "Українська"),
        LanguageItem("ur", "Urdu", "اردو"),
        LanguageItem("uz", "Uzbek", "Oʻzbekcha"),
        LanguageItem("vi", "Vietnamese", "Tiếng Việt"),
        LanguageItem("zh-rCN", "Chinese (Simplified)", "简体中文"),
        LanguageItem("zh-rHK", "Chinese (Hong Kong)", "繁體中文 (香港)"),
        LanguageItem("zh-rTW", "Chinese (Traditional)", "繁體中文 (台灣)"),
        LanguageItem("zu", "Zulu", "isiZulu")
        )
    }

    val currentLocaleTag = remember {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) locales.toLanguageTags() ?: ""
        else ""
    }

    val filteredRecommended = remember(searchQuery) {
        if (searchQuery.isBlank()) recommendedLanguages
        else recommendedLanguages.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nativeName.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredAll = remember(searchQuery) {
        if (searchQuery.isBlank()) allLanguages
        else allLanguages.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nativeName.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    fun applyLanguage(code: String) {
        val tag = when (code) {
            "en-rIN" -> "en-IN"
            "en-rUS" -> "en-US"
            "en-rGB" -> "en-GB"
            "en-rAU" -> "en-AU"
            "en-rCA" -> "en-CA"
            "es-rUS" -> "es-US"
            "fr-rCA" -> "fr-CA"
            "pt-rBR" -> "pt-BR"
            "pt-rPT" -> "pt-PT"
            "zh-rCN" -> "zh-CN"
            "zh-rHK" -> "zh-HK"
            "zh-rTW" -> "zh-TW"
            else -> code
        }
        val appLocales = if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(appLocales)
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

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (filteredRecommended.isNotEmpty()) {
                        item {
                            Text(
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        item {
                            ExpressiveList(
                                content = filteredRecommended.map { lang ->
                                    {
                                        val isSelected = if (lang.code.isEmpty()) currentLocaleTag.isEmpty() else currentLocaleTag.replace("-", "").contains(lang.code.replace("-r", "").replace("-", ""), ignoreCase = true)
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
                                            onClick = { applyLanguage(lang.code) }
                                        )
                                    }
                                }
                            )
                        }
                    }

                    if (filteredAll.isNotEmpty()) {
                        item {
                            Text(
                                text = "ALL LANGUAGES",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        item {
                            ExpressiveList(
                                content = filteredAll.map { lang ->
                                    {
                                        val isSelected = if (lang.code.isEmpty()) false else currentLocaleTag.replace("-", "").contains(lang.code.replace("-r", "").replace("-", ""), ignoreCase = true)
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
                                            onClick = { applyLanguage(lang.code) }
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
}
