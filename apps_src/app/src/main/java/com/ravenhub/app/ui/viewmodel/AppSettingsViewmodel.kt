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

package com.ravenhub.app.ui.viewmodel


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.ravenhub.app.ui.util.AppConfig
import com.ravenhub.app.ui.util.CustomConfigUtil


class AppSettingsViewModel : ViewModel() {
    private val configPath = "/data/adb/.config/ravencore/gamelist/ravencoreApplist.json"
    private val jsonHandler = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    var fullConfig by mutableStateOf<Map<String, AppConfig>>(emptyMap())
        private set

    fun loadConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = SuFile(configPath)
            if (file.exists()) {
                try {
                    val content = SuFileInputStream.open(file).bufferedReader().use { it.readText() }
                    if (content.isNotEmpty()) {
                        val decoded = jsonHandler.decodeFromString<Map<String, AppConfig>>(content)
                        withContext(Dispatchers.Main) { fullConfig = decoded }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveAndRefresh(newMap: Map<String, AppConfig>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = SuFile(configPath)
                val parent = file.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }

                val jsonString = jsonHandler.encodeToString(newMap)
                
                if (file.exists()) {
                    file.delete()
                }
                
                SuFileOutputStream.open(file).use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                withContext(Dispatchers.Main) { fullConfig = newMap }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleMasterSwitch(packageName: String, isEnabled: Boolean) {
        val newMap = fullConfig.toMutableMap()
        if (isEnabled) {
            if (!newMap.containsKey(packageName)) {
                newMap[packageName] = AppConfig()
            }
            viewModelScope.launch(Dispatchers.IO) {
                updateGamelistTxt(packageName, true)
                CustomConfigUtil.setValue("opt_game_mode_$packageName", "1")
            }
        } else {
            newMap.remove(packageName)
            viewModelScope.launch(Dispatchers.IO) {
                updateGamelistTxt(packageName, false)
                val config = CustomConfigUtil.readConfig().toMutableMap()
                val keysToRemove = listOf(
                    "opt_game_mode_$packageName",
                    "opt_lite_mode_$packageName",
                    "opt_preload_$packageName",
                    "opt_renderer_$packageName",
                    "opt_scale_$packageName",
                    "opt_downscale_$packageName",
                    "opt_clean_ram_$packageName",
                    "opt_disable_thermal_$packageName"
                )
                keysToRemove.forEach { config.remove(it.lowercase()) }
                CustomConfigUtil.writeConfig(config)
            }
        }
        saveAndRefresh(newMap)
    }

    fun updateSetting(packageName: String, key: String, value: String) {
        val currentAppConfig = fullConfig[packageName] ?: AppConfig()
        val updated = when (key) {
            "perf_lite_mode" -> currentAppConfig.copy(perf_lite_mode = value)
            "game_preload" -> currentAppConfig.copy(game_preload = value)
            "game_preload_budget" -> currentAppConfig.copy(game_preload_budget = value)
            "renderer" -> currentAppConfig.copy(renderer = value)
            "downscale" -> currentAppConfig.copy(downscale = value)
            "clean_ram" -> currentAppConfig.copy(clean_ram = value)
            "disable_thermal" -> currentAppConfig.copy(disable_thermal = value)
            else -> currentAppConfig
        }
        
        val newMap = fullConfig.toMutableMap()
        newMap[packageName] = updated
        saveAndRefresh(newMap)

        viewModelScope.launch(Dispatchers.IO) {
            syncToCustomConfig(packageName, key, value)
        }
    }

    private fun updateGamelistTxt(packageName: String, add: Boolean) {
        val file = SuFile("/data/adb/modules/ravencore/gamelist.txt")
        val lines = mutableListOf<String>()
        if (file.exists()) {
            try {
                val content = SuFileInputStream.open(file).bufferedReader().use { it.readLines() }
                lines.addAll(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val lowerPkg = packageName.lowercase()
        lines.removeAll { it.trim().lowercase().startsWith(lowerPkg) }
        if (add) {
            lines.add("$packageName:default")
        }
        try {
            val content = lines.joinToString("\n") + "\n"
            if (file.exists()) {
                file.delete()
            }
            SuFileOutputStream.open(file).use { it.write(content.toByteArray()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncToCustomConfig(packageName: String, key: String, value: String) {
        val daemonKey = when (key) {
            "perf_lite_mode" -> "opt_lite_mode_$packageName"
            "game_preload" -> "opt_preload_$packageName"
            "game_preload_budget" -> "opt_preload_budget_$packageName"
            "downscale" -> "opt_scale_$packageName"
            "clean_ram" -> "opt_clean_ram_$packageName"
            "disable_thermal" -> "opt_disable_thermal_$packageName"
            else -> return
        }
        val daemonValue = when (value) {
            "true" -> "1"
            "false" -> "0"
            "default" -> ""
            else -> value
        }
        
        val config = CustomConfigUtil.readConfig().toMutableMap()
        
        if (key == "downscale") {
            val downscaleKey = "opt_downscale_$packageName".lowercase()
            val scaleKey = "opt_scale_$packageName".lowercase()
            if (daemonValue.isEmpty() || daemonValue == "100") {
                config.remove(scaleKey)
                config.remove(downscaleKey)
            } else {
                config[scaleKey] = daemonValue
                config[downscaleKey] = "1"
            }
            CustomConfigUtil.writeConfig(config)
            return
        }

        if (daemonValue.isEmpty()) {
            config.remove(daemonKey.lowercase())
            CustomConfigUtil.writeConfig(config)
        } else {
            CustomConfigUtil.setValue(daemonKey, daemonValue)
        }
    }
}
