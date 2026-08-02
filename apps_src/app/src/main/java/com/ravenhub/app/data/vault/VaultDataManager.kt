package com.ravenhub.app.data.vault

import android.content.Context
import android.net.Uri
import com.ravenhub.app.security.SecureStorageEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object VaultDataManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _data = MutableStateFlow(VaultData())
    val data = _data.asStateFlow()

    @Volatile
    private var isLoaded = false

    @Synchronized
    fun load(context: Context) {
        val loaded = SecureStorageEngine.loadVaultSync(context)
        if (loaded != null) {
            _data.value = loaded
            isLoaded = true
        }
    }

    @Synchronized
    private fun persist(context: Context, newData: VaultData) {
        if (!isLoaded) {
            load(context)
        }
        _data.value = newData
        scope.launch {
            SecureStorageEngine.saveVaultSync(context, newData)
        }
    }

    // --- Credentials ---
    fun addCredential(context: Context, title: String, username: String, password: String, category: String, notes: String) {
        val cred = CredentialItem(title = title, username = username, password = password, category = category, notes = notes)
        persist(context, _data.value.copy(credentials = _data.value.credentials + cred))
    }

    fun updateCredential(context: Context, id: String, title: String, username: String, password: String, category: String, notes: String) {
        persist(
            context,
            _data.value.copy(
                credentials = _data.value.credentials.map { cred ->
                    if (cred.id == id) cred.copy(title = title, username = username, password = password, category = category, notes = notes) else cred
                }
            )
        )
    }

    fun deleteCredential(context: Context, id: String) {
        persist(context, _data.value.copy(credentials = _data.value.credentials.filter { it.id != id }))
    }

    // --- Files ---
    fun importFile(context: Context, uri: Uri, deleteOriginal: Boolean) {
        scope.launch {
            val entry = VaultFileManager.importFile(context, uri, deleteOriginal)
            if (entry != null) {
                persist(context, _data.value.copy(files = _data.value.files + entry))
            }
        }
    }

    fun deleteFile(context: Context, entry: VaultFileEntry) {
        scope.launch {
            VaultFileManager.deleteVaultFile(context, entry)
            persist(context, _data.value.copy(files = _data.value.files.filter { it.id != entry.id }))
        }
    }

    fun deleteFiles(context: Context, entries: List<VaultFileEntry>) {
        scope.launch {
            val idsToDelete = entries.map { it.id }.toSet()
            entries.forEach { VaultFileManager.deleteVaultFile(context, it) }
            persist(context, _data.value.copy(files = _data.value.files.filter { it.id !in idsToDelete }))
        }
    }
}
