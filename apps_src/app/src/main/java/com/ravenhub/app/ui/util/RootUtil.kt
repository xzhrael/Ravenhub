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

package com.ravenhub.app.ui.util


import android.os.FileObserver
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import com.ravenhub.app.R


object AppLifecycleManager {
    val isAppInForeground = kotlinx.coroutines.flow.MutableStateFlow(false)
    var isLaunchingSystemPicker: Boolean = false
}


object RootUtils {
    private const val MODULE_DIR = "/data/adb/modules/ravencore"
    private const val API_DIR_PATH = "/data/data/ravencore.engine/API"
    private const val PROFILE_FILE_NAME = "current_profile"
    private const val PROFILE_PATH = "$API_DIR_PATH/$PROFILE_FILE_NAME"
    private const val DAEMON_PROFILE_PATH = "/data/adb/.config/ravencore/API/current_profile"
    fun getServiceBinPath(): String {
        return if (SuFile("/system/bin/sys.ravencore-service").exists()) {
            "/system/bin/sys.ravencore-service"
        } else {
            "/data/adb/modules/ravencore/system/bin/sys.ravencore-service"
        }
    }

    fun getHelperBinPath(): String {
        return if (SuFile("/system/bin/ravencore_helper").exists()) {
            "/system/bin/ravencore_helper"
        } else {
            "/data/adb/modules/ravencore/system/bin/ravencore_helper"
        }
    }

    private fun readRootFile(path: String): String? {
        return try {
            val file = SuFile(path)
            if (!file.exists()) return null
            file.newInputStream().bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }

    private fun writeRootFile(path: String, content: String) {
        try {
            SuFile(path).newOutputStream().use { it.write(content.toByteArray()) }
        } catch (e: Exception) {
            // no-op
        }
    }

    private fun syncProfileState() {
        val apiDir = SuFile(API_DIR_PATH)
        if (!apiDir.exists()) {
            apiDir.mkdirs()
        }

        val daemonFile = SuFile(DAEMON_PROFILE_PATH)
        if (daemonFile.exists()) {
            val content = daemonFile.newInputStream().bufferedReader().use { it.readText() }
            writeRootFile(PROFILE_PATH, content)
        }
    }

    fun getModuleVersionCode(): Int {
        return try {
            val result = Shell.cmd("grep '^versionCode=' /data/adb/modules/ravencore/module.prop | cut -d= -f2").exec().out
            result.firstOrNull()?.trim()?.toIntOrNull() ?: -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun isBenchmarkRunning(): Boolean {
        return try {
            val file = SuFile("/dev/ravencore.status")
            if (!file.exists()) return false
            val lines = com.topjohnwu.superuser.io.SuFileInputStream.open(file).bufferedReader().use { it.readLines() }
            for (line in lines) {
                if (line.startsWith("FOCUSED_APP=")) {
                    val app = line.substringAfter("=").trim().lowercase()
                    return app.contains("antutu") ||
                           app.contains("geekbench") ||
                           app.contains("ludashi") ||
                           app.contains("3dmark") ||
                           app.contains("cputhrottling")
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    data class GameInfo(val pkg: String?, val startTime: String?)

    fun observeGameInfo(): Flow<GameInfo> = flow {
        var lastInfo: GameInfo? = null
        while (true) {
            if (!AppLifecycleManager.isAppInForeground.value) {
                AppLifecycleManager.isAppInForeground.first { it }
            }
            val raw = try {
                readRootFile("/data/data/ravencore.engine/API/gameinfo")
            } catch (_: Exception) {
                null
            }
            var currentInfo = GameInfo(null, null)

            if (!raw.isNullOrBlank()) {
                val lines = raw.lines()
                val firstLine = lines[0].split(" ")

                val pkg = firstLine.getOrNull(0)?.takeIf { it != "NULL" && it.isNotBlank() }
                val time = lines.find { it.startsWith("Time:") }?.substringAfter("Time:")?.trim()

                currentInfo = GameInfo(pkg, time)
            }

            if (currentInfo != lastInfo) {
                emit(currentInfo)
                lastInfo = currentInfo
            }
            delay(if (isBenchmarkRunning()) 15000L else 2000L)
        }
    }.flowOn(Dispatchers.IO)

    fun observeProfileRes(): Flow<Int> = flow {
        var lastRes = -1
        while (true) {
            if (!AppLifecycleManager.isAppInForeground.value) {
                AppLifecycleManager.isAppInForeground.first { it }
            }
            val currentRes = try {
                getActiveProfileRes()
            } catch (_: Exception) {
                R.string.status_initializing
            }
            if (currentRes != lastRes) {
                emit(currentRes)
                lastRes = currentRes
            }
            delay(if (isBenchmarkRunning()) 15000L else 2000L)
        }
    }.flowOn(Dispatchers.IO)

    fun getActiveProfileName(): String {
        return try {
            val file = SuFile("/dev/ravencore.status")
            if (!file.exists()) return "Stock"
            val lines = com.topjohnwu.superuser.io.SuFileInputStream.open(file).bufferedReader().use { it.readLines() }
            for (line in lines) {
                if (line.startsWith("ACTIVE_PROFILE=")) {
                    return line.substringAfter("=").trim()
                }
            }
            "Stock"
        } catch (_: Exception) {
            "Stock"
        }
    }

    fun getActiveProfileRes(): Int {
        return when (getActiveProfileName()) {
            "Performance" -> R.string.Profile_Performance
            "Lite" -> R.string.profile_perflite
            "Balanced" -> R.string.Profile_Balanced
            "Battery" -> R.string.Profile_ECO_mode
            "Stock" -> R.string.status_initializing
            else -> R.string.status_unknown
        }
    }

    fun requestRootAccess(): Boolean {
        return try {
            val currentShell = Shell.getCachedShell()
            if (currentShell != null && !currentShell.isRoot) {
                currentShell.close()
            }
            Shell.getShell().isRoot
        } catch (_: Exception) {
            false
        }
    }

    fun observeServiceStatusRes(): Flow<Pair<Int, String>> = flow {
        var lastStatus: Pair<Int, String>? = null
        while (true) {
            if (!AppLifecycleManager.isAppInForeground.value) {
                AppLifecycleManager.isAppInForeground.first { it }
            }
            val currentStatus = try {
                getServiceStatusRes()
            } catch (_: Exception) {
                R.string.status_suspended to ""
            }
            if (currentStatus != lastStatus) {
                emit(currentStatus)
                lastStatus = currentStatus
            }
            delay(if (isBenchmarkRunning()) 15000L else 2000L)
        }
    }.flowOn(Dispatchers.IO)

    fun isRootGranted(): Boolean {
        return try {
            val currentShell = Shell.getCachedShell()
            if (currentShell != null && !currentShell.isRoot) {
                currentShell.close()
            }
            Shell.getShell().isRoot
        } catch (_: Exception) {
            false
        }
    }

    fun isModuleInstalled(): Boolean {
        return try {
            SuFile(MODULE_DIR).exists()
        } catch (_: Exception) {
            false
        }
    }

    fun getServiceStatusRes(): Pair<Int, String> {
        return try {
            val result = Shell.cmd("pidof ravencore_helper || pidof sys.ravencore-service").exec()
            if (result.isSuccess) {
                val pid = result.out.firstOrNull() ?: ""
                R.string.status_alive to pid
            } else {
                R.string.status_suspended to ""
            }
        } catch (_: Exception) {
            R.string.status_suspended to ""
        }
    }

    fun observeDaemonStatus(): Flow<Map<String, String>> = flow {
        while (true) {
            if (!AppLifecycleManager.isAppInForeground.value) {
                AppLifecycleManager.isAppInForeground.first { it }
            }
            val statusMap = mutableMapOf<String, String>()
            val file = SuFile("/dev/ravencore.status")
            if (file.exists()) {
                try {
                    val lines = com.topjohnwu.superuser.io.SuFileInputStream.open(file).bufferedReader().use { it.readLines() }
                    for (line in lines) {
                        val parts = line.split("=")
                        if (parts.size == 2) {
                            statusMap[parts[0].trim()] = parts[1].trim()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            emit(statusMap)
            delay(if (isBenchmarkRunning()) 15000L else 2000L)
        }
    }.flowOn(Dispatchers.IO)

    fun isUpdateApkAvailable(): Boolean {
        return SuFile("/data/adb/modules/ravencore/raven_engine.apk").exists()
    }

    fun isModuleUpdatePendingReboot(): Boolean {
        return SuFile("/data/adb/modules/ravencore/update").exists()
    }
}
