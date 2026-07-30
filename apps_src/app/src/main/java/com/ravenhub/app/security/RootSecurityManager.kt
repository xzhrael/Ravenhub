package com.ravenhub.app.security

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootSecurityManager {
    var isRootAvailable: Boolean = false
        private set

    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        val granted = try {
            Shell.getShell().isRoot
        } catch (_: Throwable) {
            false
        }
        isRootAvailable = granted
        granted
    }

    suspend fun executeSecureWipe(filePath: String): Boolean = withContext(Dispatchers.IO) {
        if (!isRootAvailable) return@withContext false
        try {
            val result = Shell.cmd(
                "dd if=/dev/urandom of=\"$filePath\" bs=4k count=10 conv=notrunc 2>/dev/null",
                "dd if=/dev/zero of=\"$filePath\" bs=4k count=10 conv=notrunc 2>/dev/null",
                "rm -f \"$filePath\""
            ).exec()
            result.isSuccess
        } catch (_: Throwable) {
            false
        }
    }
}
