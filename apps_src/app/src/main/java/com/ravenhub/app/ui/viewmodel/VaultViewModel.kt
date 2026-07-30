package com.ravenhub.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ravenhub.app.data.vault.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VaultViewModel(app: Application) : AndroidViewModel(app) {

    val data: StateFlow<VaultData> = VaultDataManager.data

    private val _exportResultUri = MutableStateFlow<Uri?>(null)
    val exportResultUri = _exportResultUri.asStateFlow()

    fun reload() {
        VaultDataManager.load(getApplication())
    }

    // --- Credentials ---
    fun addCredential(title: String, username: String, password: String, category: String, notes: String) {
        VaultDataManager.addCredential(getApplication(), title, username, password, category, notes)
    }

    fun updateCredential(id: String, title: String, username: String, password: String, category: String, notes: String) {
        VaultDataManager.updateCredential(getApplication(), id, title, username, password, category, notes)
    }

    fun deleteCredential(id: String) {
        VaultDataManager.deleteCredential(getApplication(), id)
    }

    // --- Files ---
    fun importFile(uri: Uri, deleteOriginal: Boolean) {
        VaultDataManager.importFile(getApplication(), uri, deleteOriginal)
    }

    fun exportToDestinationUri(
        context: Context,
        entries: List<VaultFileEntry>,
        asEncrypted: Boolean,
        destinationUri: Uri
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                if (entries.size == 1) {
                    val entry = entries[0]
                    val file = if (asEncrypted) {
                        VaultFileManager.exportEncryptedFile(context, entry)
                    } else {
                        VaultFileManager.exportFile(context, entry)
                    }
                    if (file != null && file.exists()) {
                        resolver.openOutputStream(destinationUri)?.use { out ->
                            file.inputStream().use { input -> input.copyTo(out) }
                        }
                        _exportResultUri.value = destinationUri
                    }
                } else {
                    resolver.openOutputStream(destinationUri)?.use { out ->
                        ZipOutputStream(BufferedOutputStream(out)).use { zipOut ->
                            entries.forEach { entry ->
                                val file = if (asEncrypted) {
                                    VaultFileManager.exportEncryptedFile(context, entry)
                                } else {
                                    VaultFileManager.exportFile(context, entry)
                                }
                                if (file != null && file.exists()) {
                                    val entryName = if (asEncrypted) "${entry.originalName}.enc" else entry.originalName
                                    zipOut.putNextEntry(ZipEntry(entryName))
                                    file.inputStream().use { input -> input.copyTo(zipOut) }
                                    zipOut.closeEntry()
                                }
                            }
                        }
                    }
                    _exportResultUri.value = destinationUri
                }
            } catch (_: Exception) {
            } finally {
                VaultFileManager.cleanupExportCache(context)
            }
        }
    }

    fun clearExportResult() {
        _exportResultUri.value = null
    }

    fun deleteFile(entry: VaultFileEntry) {
        VaultDataManager.deleteFile(getApplication(), entry)
    }

    fun deleteFiles(entries: List<VaultFileEntry>) {
        VaultDataManager.deleteFiles(getApplication(), entries)
    }
}
