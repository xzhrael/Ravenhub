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

package com.ravenhub.app.ui.viewmodel


import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import com.ravenhub.app.R
import com.ravenhub.app.ui.util.CustomConfigUtil


class ApplistViewmodel : ViewModel() {

    companion object {
        private const val TAG = "ApplistViewmodel"
        private val appsLock = Any()
        var apps by mutableStateOf<List<AppInfo>>(emptyList())

        @JvmStatic
        fun getAppIconDrawable(context: Context, packageName: String): Drawable? {
            val appList = synchronized(appsLock) { apps }
            val appDetail = appList.find { it.packageName == packageName }
            return appDetail?.packageInfo?.applicationInfo?.loadIcon(context.packageManager)
        }
    }

    @Parcelize
    data class AppInfo(
        val label: String,
        val packageInfo: PackageInfo,
        val isRecommended: Boolean = false,
        var isEnabledInConfig: Boolean = false
    ) : Parcelable {
        val packageName: String get() = packageInfo.packageName
        val isSystem: Boolean get() = (packageInfo.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0)
        val uid: Int get() = packageInfo.applicationInfo?.uid ?: 0
    }

    var isRefreshing by mutableStateOf(false)
    var showSystemApps by mutableStateOf(false)
    
    var searchTextFieldValue by mutableStateOf(TextFieldValue(""))
        private set
    
    private val searchQueryString: String get() = searchTextFieldValue.text
    
    val searchQuery: String get() = searchTextFieldValue.text
    
    fun updateSearch(newValue: TextFieldValue) {
        searchTextFieldValue = newValue
    }
    
    fun clearSearch() {
        searchTextFieldValue = TextFieldValue("")
    }

    private val configPath = "/data/adb/.config/ravencore/gamelist/ravencoreApplist.json"

    val filteredApps by derivedStateOf {
        val query = searchQueryString.lowercase()
        synchronized(appsLock) {
            apps.filter { app ->
                val matchesSearch = app.label.lowercase().contains(query) || 
                                  app.packageName.lowercase().contains(query)
                val matchesSystem = showSystemApps || !app.isSystem
                matchesSearch && matchesSystem
            }.sortedWith(
                compareByDescending<AppInfo> { it.isEnabledInConfig }
                    .thenByDescending { it.isRecommended }
                    .thenBy(Collator.getInstance(Locale.getDefault())) { it.label }
            )
        }
    }

    fun loadApps(context: Context, forceRefresh: Boolean = false) {
        if (!forceRefresh && apps.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            val pm = context.packageManager

            val enabledList = getEnabledPackages()
            val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            val loadedApps = installed.map { pkg ->
                val appInfo = pkg.applicationInfo
                

                @Suppress("DEPRECATION")
                val isGame = appInfo != null && (
                    appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                )

                AppInfo(
                    label = appInfo?.loadLabel(pm)?.toString() ?: context.getString(R.string.status_unknown),
                    packageInfo = pkg,
                    isRecommended = isGame,
                    isEnabledInConfig = enabledList.contains(pkg.packageName)
                )
            }

            // sync app names for preload notifications
            try {
                val configMap = CustomConfigUtil.readConfig().toMutableMap()
                var changed = false
                loadedApps.forEach { app ->
                    if (app.isEnabledInConfig) {
                        val key = "name_" + app.packageName.lowercase().replace(".", "_")
                        if (configMap[key] != app.label) {
                            configMap[key] = app.label
                            changed = true
                        }
                    }
                }
                if (changed) {
                    CustomConfigUtil.writeConfig(configMap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                synchronized(appsLock) {
                    apps = loadedApps
                }
                isRefreshing = false
            }
        }
    }

    fun refreshAppConfigStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabledList = getEnabledPackages()
            synchronized(appsLock) {
                apps = apps.map { it.copy(isEnabledInConfig = enabledList.contains(it.packageName)) }
            }
        }
    }

    fun autoDetectGames(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            val pm = context.packageManager
            val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val configMap = CustomConfigUtil.readConfig().toMutableMap()

            installed.forEach { pkg ->
                val appInfo = pkg.applicationInfo
                @Suppress("DEPRECATION")
                val isGame = appInfo != null && (
                    appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                )
                if (isGame) {
                    val key = "opt_game_" + pkg.packageName.lowercase().replace(".", "_")
                    configMap[key] = "1"
                    val nameKey = "name_" + pkg.packageName.lowercase().replace(".", "_")
                    configMap[nameKey] = appInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                }
            }

            CustomConfigUtil.writeConfig(configMap)
            loadApps(context, forceRefresh = true)
        }
    }

    fun runGameCompilation(onStart: () -> Unit, onProgress: (String) -> Unit, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { onStart() }
            
            val activePkgs = mutableListOf<String>()
            try {
                val file = SuFile("/data/adb/.config/ravencore/gamelist/ravencoreApplist.json")
                if (file.exists()) {
                    val content = SuFileInputStream.open(file).bufferedReader().use { it.readText() }
                    if (content.isNotBlank()) {
                        val json = JSONObject(content)
                        json.keys().forEach { activePkgs.add(it) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (activePkgs.isEmpty()) {
                withContext(Dispatchers.Main) { onComplete(0) }
                return@launch
            }
            
            activePkgs.forEachIndexed { index, pkg ->
                withContext(Dispatchers.Main) {
                    onProgress("${index + 1}/${activePkgs.size}: $pkg")
                }
                Shell.cmd(
                    "am force-stop $pkg",
                    "setprop dalvik.vm.dex2oat-threads 8; setprop dalvik.vm.dex2oat-cpu-set 0,1,2,3,4,5,6,7",
                    "cmd package compile --reset $pkg",
                    "cmd package compile -m speed -f $pkg",
                    "cmd package compile -m speed --secondary-dex $pkg",
                    "setprop dalvik.vm.dex2oat-threads ''; setprop dalvik.vm.dex2oat-cpu-set ''"
                ).exec()
            }
            
            withContext(Dispatchers.Main) { onComplete(activePkgs.size) }
        }
    }

    private fun getEnabledPackages(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val file = SuFile(configPath)
            if (file.exists()) {
                val content = SuFileInputStream.open(file).bufferedReader().use { it.readText() }
                if (content.isNotBlank()) {
                    val json = JSONObject(content)
                    json.keys().forEach { set.add(it) }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return set
    }
}
