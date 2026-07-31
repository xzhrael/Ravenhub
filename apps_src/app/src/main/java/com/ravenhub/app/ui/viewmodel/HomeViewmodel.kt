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
    val serviceStatusRes: String = "Suspended",
    val servicePid: String = "",
    val currentProfileRes: String = "Initializing",
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
        fetchInitialSystemData()
    }

    private fun fetchInitialSystemData() {
        viewModelScope.launch(Dispatchers.IO) {
            val isRooted = RootUtils.requestRootAccess()
            _uiState.value = _uiState.value.copy(
                rootStatus = isRooted
            )
        }
    }

    fun refreshServiceStatus() {
        // No-op
    }

    fun applyProfile(profileReason: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) { onSuccess() }
    }

    fun rebootDevice(reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cmd = when (reason) {
                "" -> "svc power reboot"
                "soft_reboot" -> "killall system_server"
                "recovery" -> "svc power reboot recovery || reboot recovery"
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
        // No-op
    }
}
