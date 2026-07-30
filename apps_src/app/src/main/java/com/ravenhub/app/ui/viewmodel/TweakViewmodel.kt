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


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ravenhub.app.R
import com.ravenhub.app.ui.util.BackupManager
import com.ravenhub.app.ui.util.PropertyUtils
import com.ravenhub.app.ui.util.CustomConfigUtil


class TweakViewModel : ViewModel() {
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }
    
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private val viewModelScope = kotlinx.coroutines.CoroutineScope(
        (this as androidx.lifecycle.ViewModel).viewModelScope.coroutineContext + exceptionHandler
    )

    data class ValidationResult(
        val isValid: Boolean, 
        val message: String, 
        val hasTweaks: Boolean, 
        val hasApplist: Boolean,
        val socType: String?,
        val data: Map<String, String>?
    )
    
    var isUiLoaded by mutableStateOf(false)
        private set




    var currentRenderer by mutableStateOf<String?>(null)
    var currentRefreshRate by mutableStateOf<Int?>(null)
    
    var fastChargeState by mutableStateOf<Boolean?>(false)
    var chargeLimitState by mutableStateOf<Float?>(100f)
    var ravencoreUtilityState by mutableStateOf<Boolean?>(false)
    var refreshRateConfigState by mutableStateOf("smart")
    
    var isRendererLoading by mutableStateOf(false)
        private set
    
    var isRefreshRateLoading by mutableStateOf(false)
        private set
    
    private val configKeysToBackup = listOf(
        "persist.sys.ravencore.soctype",
        "persist.sys.ravencoreconf.cpulimit",
        "persist.sys.ravencoreconf.freqoffset",
        "persist.sys.ravencoreconf.APreload",
        "persist.sys.ravencoreconf.clearbg",
        "persist.sys.ravencoreconf.iosched",
        "persist.sys.ravencoreconf.dnd",
        "persist.sys.ravencoreconf.fstrim",
        "persist.sys.ravencoreconf.thermalcore",
        "persist.sys.ravencoreconf.schemeconfig",
        "persist.sys.ravencoreconf.bypasschgthreshold",
        "persist.sys.ravencoreconf.preloadbudget",
        "persist.sys.ravencore.custom_default_cpu_gov",
        "persist.sys.ravencore.custom_powersave_cpu_gov",
        "persist.sys.ravencore.custom_performance_cpu_gov",
        "persist.sys.ravencore.custom_default_balanced_IO",
        "persist.sys.ravencore.custom_performance_IO",
        "persist.sys.ravencore.custom_powersave_IO"
    )
    
    
    private val APPLIST_BACKUP_KEY = "__RAVENCORE_APPLIST_DATA__"
    private val APPLIST_PATH = "/data/adb/.config/ravencore/gamelist/ravencoreApplist.json"
    
    suspend fun createConfigFileBackup(
        context: Context, 
        uri: Uri, 
        backupTweaks: Boolean, 
        backupApplist: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val propsMap = mutableMapOf<String, String>()
                

                propsMap["persist.sys.ravencore.soctype"] = PropertyUtils.get("persist.sys.ravencore.soctype")

                if (backupTweaks) {
                    configKeysToBackup.forEach { key ->
                        if (key != "persist.sys.ravencore.soctype") {
                            propsMap[key] = PropertyUtils.get(key)
                        }
                    }
                }

                if (backupApplist) {
                    val applistContent = Shell.cmd("cat $APPLIST_PATH").exec().out.joinToString("\n")
                    if (applistContent.isNotBlank()) {
                        propsMap[APPLIST_BACKUP_KEY] = applistContent
                    }
                }

                val isSuccess = BackupManager.createBackup(context, uri, propsMap)
                
                delay(1500) 
                
                isSuccess
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun validateAndRestoreFile(context: Context, uri: Uri): ValidationResult {
        return withContext(Dispatchers.IO) {
            val backupData = BackupManager.readBackup(context, uri)
            
            if (backupData == null) {
                return@withContext ValidationResult(false, context.getString(R.string.err_invalid_backup), false, false, null, null)
            }
            val backupSocType = backupData["persist.sys.ravencore.soctype"] 
                ?: backupData["persist.sys.ravencoredebug.soctype"]
                
            val hasApplist = backupData.containsKey(APPLIST_BACKUP_KEY)
            
            val hasTweaks = backupData.keys.any { 
                it.startsWith("persist.sys.ravencore") && 
                it != "persist.sys.ravencore.soctype" &&
                it != "persist.sys.ravencoredebug.soctype" 
            }
    
            ValidationResult(true, "", hasTweaks, hasApplist, backupSocType, backupData)
        }
    }

    suspend fun applyRestoreData(
        context: Context, 
        backupData: Map<String, String>, 
        restoreTweaks: Boolean, 
        restoreApplist: Boolean
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (restoreTweaks) {
                    backupData.forEach { (key, value) ->
                        if (key != "persist.sys.ravencore.soctype" && 
                            key != "persist.sys.ravencoredebug.soctype" && 
                            key != APPLIST_BACKUP_KEY && 
                            value.isNotEmpty()) {
                            
                            PropertyUtils.set(key, value)
                            
                            if (key == "persist.sys.ravencoreconf.freqoffset") {
                                Shell.cmd("echo $value > /data/adb/.config/ravencore/freqoffset").exec()
                            }
                        }
                    }
                }

                if (restoreApplist && backupData.containsKey(APPLIST_BACKUP_KEY)) {
                    val applistContent = backupData[APPLIST_BACKUP_KEY]!!
                    val file = SuFile(APPLIST_PATH)
                    val parent = file.parentFile
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs()
                    }
                    if (file.exists()) {
                        file.delete()
                    }
                    SuFileOutputStream.open(file).use { outputStream ->
                        outputStream.write(applistContent.toByteArray())
                    }
                }
                
                Shell.cmd("touch /data/adb/modules/ravencore/reboot").exec()
                if (restoreTweaks) {
                    loadAllConfiguration(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(1200) 
        }
    }
    
    fun loadAllConfiguration(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            
            launch {
                fastChargeState = CustomConfigUtil.getValue("fast_charge", "0") == "1"
                chargeLimitState = CustomConfigUtil.getValue("charge_limit", "100").toFloatOrNull() ?: 100f
                ravencoreUtilityState = CustomConfigUtil.getValue("ravencore_utility", "0") == "1"
                refreshRateConfigState = CustomConfigUtil.getValue("refresh_rate", "smart")

                val rawRenderer = CustomConfigUtil.getValue("global_renderer", "skiagl")
                currentRenderer = if (rawRenderer.isEmpty() || rawRenderer.equals("default", ignoreCase = true)) {
                    "skiagl"
                } else {
                    rawRenderer
                }

                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                currentRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display.refreshRate.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.refreshRate.toInt()
                }
            }

            withContext(Dispatchers.Main) {
                isUiLoaded = true
            }
        }
    }

    fun executeSetRenderer(reason: String, context: Context) {
        isRendererLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            CustomConfigUtil.setValue("global_renderer", reason)
            delay(1000)
            loadAllConfiguration(context)
            withContext(Dispatchers.Main) {
                isRendererLoading = false
            }
        }
    }
    
    fun executeSetRefreshRates(reason: String, context: Context) {
        isRefreshRateLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            CustomConfigUtil.setValue("refresh_rate", reason)
            
            val fps = when (reason) {
                "smart" -> 120
                "auto" -> 60
                else -> reason.toIntOrNull() ?: 60
            }
            val intent = Intent("com.ravenhub.app.SET_FPS").apply {
                putExtra("fps", fps)
            }
            context.sendBroadcast(intent)
            
            delay(1000)
            loadAllConfiguration(context)
            withContext(Dispatchers.Main) {
                isRefreshRateLoading = false
            }
        }
    }
    
    fun updateFastCharge(checked: Boolean) {
        fastChargeState = checked
        viewModelScope.launch(Dispatchers.IO) {
            CustomConfigUtil.setValue("fast_charge", if (checked) "1" else "0")
        }
    }

    fun updateChargeLimit(value: Float) {
        chargeLimitState = value
        viewModelScope.launch(Dispatchers.IO) {
            CustomConfigUtil.setValue("charge_limit", value.roundToInt().toString())
        }
    }

    fun updateRavencoreUtility(checked: Boolean) {
        ravencoreUtilityState = checked
        viewModelScope.launch(Dispatchers.IO) {
            CustomConfigUtil.setValue("ravencore_utility", if (checked) "1" else "0")
        }
    }


    fun runFstrim(onStart: () -> Unit, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { onStart() }
            Shell.cmd("fstrim -v /data").exec()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    fun runGameCompilation(onStart: () -> Unit, onProgress: (String) -> Unit, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { onStart() }
            
            val activePkgs = mutableListOf<String>()
            try {
                val file = com.topjohnwu.superuser.io.SuFile("/data/adb/.config/ravencore/gamelist/ravencoreApplist.json")
                if (file.exists()) {
                    val content = com.topjohnwu.superuser.io.SuFileInputStream.open(file).bufferedReader().use { it.readText() }
                    if (content.isNotBlank()) {
                        val json = org.json.JSONObject(content)
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
}
