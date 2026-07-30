package com.ravenhub.app.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    suspend fun createBackupZip(
        context: Context,
        includePlanner: Boolean = true,
        includeFinance: Boolean = true,
        includeVault: Boolean = true,
        includeNotes: Boolean = true
    ): File? = withContext(Dispatchers.IO) {
        val backupZip = File(context.cacheDir, "ravenhub_backup.zip")
        if (backupZip.exists()) backupZip.delete()

        val filesToBackup = mutableListOf<File>()

        if (includePlanner) {
            val f = File(context.filesDir, "planner_data.enc")
            if (f.exists()) filesToBackup.add(f)
        }
        if (includeFinance) {
            val f = File(context.filesDir, "finance_data.enc")
            if (f.exists()) filesToBackup.add(f)
        }
        if (includeVault) {
            val f = File(context.filesDir, "vault_data.enc")
            if (f.exists()) filesToBackup.add(f)
            val vaultDir = File(context.filesDir, "vault_files")
            if (vaultDir.exists() && vaultDir.isDirectory) {
                vaultDir.listFiles()?.forEach { filesToBackup.add(it) }
            }
        }
        if (includeNotes) {
            val f = File(context.filesDir, "notes_data.enc")
            if (f.exists()) filesToBackup.add(f)
        }

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupZip))).use { zos ->
                filesToBackup.forEach { file ->
                    val relativePath = if (file.parentFile?.name == "vault_files") {
                        "vault_files/${file.name}"
                    } else {
                        file.name
                    }
                    val entry = ZipEntry(relativePath)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
                zos.flush()
            }
            backupZip
        } catch (_: Exception) {
            backupZip.delete()
            null
        }
    }

    suspend fun restoreBackupZip(context: Context, zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val targetFile = if (entry.name.startsWith("vault_files/")) {
                        val fileName = entry.name.removePrefix("vault_files/")
                        File(File(context.filesDir, "vault_files").also { it.mkdirs() }, fileName)
                    } else {
                        File(context.filesDir, entry.name)
                    }

                    FileOutputStream(targetFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
