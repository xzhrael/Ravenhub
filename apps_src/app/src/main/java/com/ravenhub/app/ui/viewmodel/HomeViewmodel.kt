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


import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ravenhub.app.R
import com.ravenhub.app.ui.util.RootUtils
import com.ravenhub.app.ui.util.isBannerImageEnabled


data class HomeUiState(
    val isBannerEnabled: Boolean = false,
    val moduleInstalled: Boolean = false,
    val autoMode: String? = null,
    val rootStatus: Boolean = false,
    val serviceStatusRes: Int = R.string.status_suspended,
    val servicePid: String = "",
    val currentProfileRes: Int = R.string.status_initializing,
    val runningGamePkg: String? = null,
    val runningGameStartTime: String? = null,
    val daemonStatus: Map<String, String> = emptyMap()
)


class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }
    
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private val viewModelScope = kotlinx.coroutines.CoroutineScope(
        (this as androidx.lifecycle.ViewModel).viewModelScope.coroutineContext + exceptionHandler
    )

    private val context = application.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enable_banner_image") {
            _uiState.value = _uiState.value.copy(isBannerEnabled = context.isBannerImageEnabled())
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        _uiState.value = _uiState.value.copy(isBannerEnabled = context.isBannerImageEnabled())
        
        observeRootUtils()
        fetchInitialSystemData()
    }

    private fun observeRootUtils() {
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeServiceStatusRes().collect { (statusRes, pid) ->
                _uiState.update { it.copy(serviceStatusRes = statusRes, servicePid = pid) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeProfileRes().collect { profileRes ->
                _uiState.update { it.copy(currentProfileRes = profileRes) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeGameInfo().collect { info ->
                _uiState.update {
                    it.copy(
                        runningGamePkg = info.pkg,
                        runningGameStartTime = info.startTime
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeDaemonStatus().collect { daemonStatus ->
                _uiState.update { it.copy(daemonStatus = daemonStatus) }
            }
        }
    }


    private fun fetchInitialSystemData() {
        viewModelScope.launch(Dispatchers.IO) {
            val isRooted = RootUtils.requestRootAccess()
            val isModuleInstalled = RootUtils.isModuleInstalled()
            val mode = com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.AIenabled")

            _uiState.value = _uiState.value.copy(
                rootStatus = isRooted,
                moduleInstalled = isModuleInstalled,
                autoMode = mode
            )

            if (isRooted && isModuleInstalled) {
                val status = RootUtils.getServiceStatusRes()
                _uiState.update { it.copy(serviceStatusRes = status.first, servicePid = status.second) }
            }
        }
    }

    fun refreshServiceStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = RootUtils.getServiceStatusRes()
            _uiState.update { it.copy(serviceStatusRes = status.first, servicePid = status.second) }
        }
    }

    fun applyProfile(profileReason: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val bin = com.ravenhub.app.ui.util.RootUtils.getServiceBinPath()
            try {
                Shell.cmd("$bin -p $profileReason").submit()
                val ctx = getApplication<Application>().applicationContext
                ctx.sendBroadcast(android.content.Intent("ravencore.intent.action.UPDATE_NOTIFICATION"))
            } catch (_: Exception) {}
            viewModelScope.launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun rebootDevice(reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cmd = when (reason) {
                "" -> "svc power reboot"
                "soft_reboot" -> "killall system_server"
                "recovery" -> "/system/bin/input keyevent 26 && svc power reboot $reason || reboot $reason"
                else -> "svc power reboot $reason || reboot $reason"
            }
            try {
                Shell.cmd(cmd).submit()
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
    


    fun refreshAiMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = com.ravenhub.app.ui.util.PropertyUtils.get("persist.sys.ravencoreconf.AIenabled")
            _uiState.value = _uiState.value.copy(autoMode = mode)
        }
    }

}
