package com.ravenhub.app.backup

import android.content.Context
import com.ravenhub.app.security.MasterKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object BackupManager {

    private fun generatePinVerifier(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10_000, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = skf.generateSecret(spec).encoded
        return key.joinToString("") { "%02x".format(it) }
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

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

        val savedPin = MasterKeyManager.getSavedPin(context) ?: "1234"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val verifierHex = generatePinVerifier(savedPin, salt)

        val metaJson = JSONObject().apply {
            put("verifier", verifierHex)
            put("salt", bytesToHex(salt))
            put("created_at", System.currentTimeMillis())
        }.toString()

        val metaFile = File(context.cacheDir, "backup_pin_meta.json")
        metaFile.writeText(metaJson)

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupZip))).use { zos ->
                val metaEntry = ZipEntry("backup_pin_meta.json")
                zos.putNextEntry(metaEntry)
                FileInputStream(metaFile).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()

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
            metaFile.delete()
            backupZip
        } catch (_: Exception) {
            metaFile.delete()
            backupZip.delete()
            null
        }
    }

    suspend fun verifyBackupZipPin(context: Context, zipFile: File, inputPin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var metaContent: String? = null
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "backup_pin_meta.json") {
                        metaContent = zis.bufferedReader().readText()
                        break
                    }
                    entry = zis.nextEntry
                }
            }

            if (metaContent == null) {
                return@withContext MasterKeyManager.verifyPin(context, inputPin)
            }

            val json = JSONObject(metaContent)
            val expectedVerifier = json.getString("verifier")
            val salt = hexToBytes(json.getString("salt"))

            val inputVerifier = generatePinVerifier(inputPin, salt)
            inputVerifier == expectedVerifier
        } catch (_: Exception) {
            false
        }
    }

    suspend fun restoreBackupZip(context: Context, zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name != "backup_pin_meta.json") {
                        val targetFile = if (entry.name.startsWith("vault_files/")) {
                            val fileName = entry.name.removePrefix("vault_files/")
                            File(File(context.filesDir, "vault_files").also { it.mkdirs() }, fileName)
                        } else {
                            File(context.filesDir, entry.name)
                        }

                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
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
