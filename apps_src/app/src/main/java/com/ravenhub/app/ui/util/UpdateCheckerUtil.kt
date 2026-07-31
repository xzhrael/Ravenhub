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

package com.ravenhub.app.ui.util

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ravenhub.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class UpdateResult(
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = BuildConfig.VERSION_NAME,
    val downloadUrl: String = "https://github.com/xzhrael/Ravenhub/releases",
    val releaseNotes: String = "",
    val isChecking: Boolean = false,
    val errorMessage: String? = null
)

object UpdateCheckerUtil {
    private val _updateState = mutableStateOf(UpdateResult())
    val updateState: State<UpdateResult> = _updateState

    suspend fun checkUpdate(context: Context, force: Boolean = false) {
        if (_updateState.value.isChecking) return
        if (!force && _updateState.value.isUpdateAvailable) return

        _updateState.value = _updateState.value.copy(isChecking = true, errorMessage = null)

        withContext(Dispatchers.IO) {
            try {
                var rawTag = ""
                var htmlUrl = "https://github.com/xzhrael/Ravenhub/releases"
                var notes = ""

                // Strategy 1: GitHub REST API
                var apiSuccess = false
                try {
                    val url = URL("https://api.github.com/repos/xzhrael/Ravenhub/releases/latest")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    conn.setRequestProperty("User-Agent", "RavenHub-App")

                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        rawTag = json.optString("tag_name", "").trim()
                        htmlUrl = json.optString("html_url", "https://github.com/xzhrael/Ravenhub/releases")
                        notes = json.optString("body", "")
                        apiSuccess = true
                    }
                } catch (_: Exception) {
                    apiSuccess = false
                }

                // Strategy 2: Web HTML Redirect Fallback (Rate-limit bypass)
                if (!apiSuccess || rawTag.isBlank()) {
                    val url = URL("https://github.com/xzhrael/Ravenhub/releases/latest")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")

                    val finalUrl = conn.url.toString()
                    val matcher = Pattern.compile("/releases/tag/([^/]+)").matcher(finalUrl)
                    if (matcher.find()) {
                        rawTag = matcher.group(1) ?: ""
                        htmlUrl = finalUrl
                    } else {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val bodyMatcher = Pattern.compile("/releases/tag/([^\"'?]+)").matcher(body)
                        if (bodyMatcher.find()) {
                            rawTag = bodyMatcher.group(1) ?: ""
                            htmlUrl = "https://github.com/xzhrael/Ravenhub/releases/tag/$rawTag"
                        }
                    }
                }

                if (rawTag.isBlank()) {
                    _updateState.value = _updateState.value.copy(
                        isChecking = false,
                        errorMessage = "Could not fetch release tag"
                    )
                    return@withContext
                }

                val cleanLatest = rawTag.removePrefix("v").removePrefix("V")
                val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V")
                val hasNewVersion = isVersionGreater(cleanLatest, cleanCurrent)

                _updateState.value = UpdateResult(
                    isUpdateAvailable = hasNewVersion,
                    latestVersion = if (rawTag.startsWith("v", ignoreCase = true)) rawTag else "v$rawTag",
                    downloadUrl = htmlUrl,
                    releaseNotes = notes,
                    isChecking = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    errorMessage = "No internet connection"
                )
            }
        }
    }

    private fun isVersionGreater(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        val latestParts = latest.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
