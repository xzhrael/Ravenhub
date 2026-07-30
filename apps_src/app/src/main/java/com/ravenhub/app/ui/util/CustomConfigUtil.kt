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

import com.topjohnwu.superuser.Shell
import java.io.File

object CustomConfigUtil {
    private const val FILE_PATH = "/data/media/0/Android/media/.ravencore/custom"

    fun readConfig(): Map<String, String> {
        val config = mutableMapOf<String, String>()
        val file = File(FILE_PATH)
        
        // Try reading direct first
        var success = false
        if (file.exists() && file.canRead()) {
            try {
                file.readLines().forEach { line ->
                    val sep = line.indexOf('=')
                    if (sep != -1) {
                        val key = line.substring(0, sep).trim().lowercase()
                        val valStr = line.substring(sep + 1).trim()
                        if (key.isNotEmpty()) {
                            config[key] = valStr
                        }
                    }
                }
                success = true
            } catch (e: Exception) {
                success = false
            }
        }
        
        if (!success) {
            try {
                val lines = Shell.cmd("cat $FILE_PATH").exec().out
                lines.forEach { line ->
                    val sep = line.indexOf('=')
                    if (sep != -1) {
                        val key = line.substring(0, sep).trim().lowercase()
                        val valStr = line.substring(sep + 1).trim()
                        if (key.isNotEmpty()) {
                            config[key] = valStr
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return config
    }

    fun writeConfig(config: Map<String, String>) {
        val sb = StringBuilder()
        config.forEach { (k, v) ->
            sb.append("$k=$v\n")
        }
        val content = sb.toString()
        
        var success = false
        try {
            val file = File(FILE_PATH)
            file.parentFile?.mkdirs()
            file.writeText(content)
            success = true
        } catch (e: Exception) {
            success = false
        }
        
        if (!success) {
            try {
                val escapedContent = content.replace("'", "'\\''")
                Shell.cmd(
                    "mkdir -p /data/media/0/Android/media/.ravencore",
                    "echo '$escapedContent' > $FILE_PATH",
                    "chmod 666 $FILE_PATH"
                ).submit()
            } catch (_: Exception) {}
        }
    }

    fun getValue(key: String, defaultValue: String): String {
        return readConfig()[key.lowercase()] ?: defaultValue
    }

    fun setValue(key: String, value: String) {
        val config = readConfig().toMutableMap()
        config[key.lowercase()] = value
        writeConfig(config)
    }
}
