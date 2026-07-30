package com.ravenhub.app.data.vault

import android.content.Context
import android.net.Uri
import com.ravenhub.app.security.MasterKeyManager
import com.ravenhub.app.security.RootSecurityManager
import com.ravenhub.app.security.RustSecurityBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object VaultFileManager {
    private const val VAULT_DIR = "vault_files"

    fun getVaultDir(context: Context): File =
        File(context.filesDir, VAULT_DIR).also { it.mkdirs() }

    suspend fun importFile(
        context: Context,
        uri: Uri,
        deleteOriginal: Boolean
    ): VaultFileEntry? = withContext(Dispatchers.IO) {
        val key = MasterKeyManager.getUnlockedMasterKey() ?: return@withContext null
        val resolver = context.contentResolver

        val cursor = resolver.query(uri, null, null, null, null)
        val displayName = cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) it.getString(idx) else "unknown"
            } else "unknown"
        } ?: "unknown"

        val encFileName = "${UUID.randomUUID()}.enc"
        val outFile = File(getVaultDir(context), encFileName)
        val tempRaw = File(context.cacheDir, "import_raw_${UUID.randomUUID()}")

        try {
            resolver.openInputStream(uri)?.use { input ->
                tempRaw.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val totalSize = tempRaw.length()

            RustSecurityBridge.encryptFileChunked(
                inputPath = tempRaw.absolutePath,
                outputPath = outFile.absolutePath,
                key = key
            )

            if (RootSecurityManager.isRootAvailable) {
                RootSecurityManager.executeSecureWipe(tempRaw.absolutePath)
            } else {
                tempRaw.delete()
            }

            if (deleteOriginal) {
                try {
                    android.provider.DocumentsContract.deleteDocument(resolver, uri)
                } catch (_: Exception) {}
            }

            VaultFileEntry(
                originalName = displayName,
                encryptedFileName = encFileName,
                sizeBytes = totalSize
            )
        } catch (_: Exception) {
            tempRaw.delete()
            outFile.delete()
            null
        }
    }

    suspend fun exportFile(
        context: Context,
        entry: VaultFileEntry
    ): File? = withContext(Dispatchers.IO) {
        val key = MasterKeyManager.getUnlockedMasterKey() ?: return@withContext null
        val encFile = File(getVaultDir(context), entry.encryptedFileName)
        if (!encFile.exists()) return@withContext null

        val tempFile = File(context.cacheDir, "export_${entry.originalName}")

        try {
            RustSecurityBridge.decryptFileChunked(
                inputPath = encFile.absolutePath,
                outputPath = tempFile.absolutePath,
                key = key
            )
            tempFile
        } catch (_: Exception) {
            tempFile.delete()
            null
        }
    }

    suspend fun exportEncryptedFile(
        context: Context,
        entry: VaultFileEntry
    ): File? = withContext(Dispatchers.IO) {
        val encFile = File(getVaultDir(context), entry.encryptedFileName)
        if (!encFile.exists()) return@withContext null

        val tempFile = File(context.cacheDir, "export_${entry.originalName}.enc")
        try {
            encFile.copyTo(tempFile, overwrite = true)
            tempFile
        } catch (_: Exception) {
            tempFile.delete()
            null
        }
    }

    suspend fun exportMultipleFiles(
        context: Context,
        entries: List<VaultFileEntry>,
        asEncrypted: Boolean
    ): List<File> = withContext(Dispatchers.IO) {
        entries.mapNotNull { entry ->
            if (asEncrypted) {
                exportEncryptedFile(context, entry)
            } else {
                exportFile(context, entry)
            }
        }
    }

    suspend fun deleteVaultFile(context: Context, entry: VaultFileEntry) = withContext(Dispatchers.IO) {
        val file = File(getVaultDir(context), entry.encryptedFileName)
        if (file.exists()) {
            if (RootSecurityManager.isRootAvailable) {
                RootSecurityManager.executeSecureWipe(file.absolutePath)
            } else {
                file.delete()
            }
        }
    }

    suspend fun cleanupExportCache(context: Context) = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles()?.filter { it.name.startsWith("export_") || it.name.startsWith("import_raw_") }?.forEach { f ->
            if (RootSecurityManager.isRootAvailable) {
                RootSecurityManager.executeSecureWipe(f.absolutePath)
            } else {
                f.delete()
            }
        }
    }
}
