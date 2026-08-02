/*
 * Copyright (C) 2026-2027 RavenHub
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

package com.ravenhub.app

import android.app.Application
import com.topjohnwu.superuser.Shell

// configure libsu shell ONCE at app startup to prevent
// race conditions and ANR from unconfigured on-demand shell creation
class RavenHubApp : Application() {
    companion object {
        init {
            try {
                Shell.setDefaultBuilder(
                    Shell.Builder.create()
                        .setFlags(Shell.FLAG_MOUNT_MASTER)
                )
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
