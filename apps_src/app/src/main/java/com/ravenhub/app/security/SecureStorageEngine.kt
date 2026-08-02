package com.ravenhub.app.security

import android.content.Context
import android.util.Log
import com.ravenhub.app.data.finance.FinanceData
import com.ravenhub.app.data.notes.NotesData
import com.ravenhub.app.data.planner.PlannerData
import com.ravenhub.app.data.vault.VaultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

object SecureStorageEngine {
    private const val TAG = "SecureStorageEngine"
    private const val PREFS_NAME = "raven_data_store"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun saveData(context: Context, keyName: String, fileName: String, encrypted: ByteArray, plainJson: String) {
        try {
            // 1. Encrypted Storage File (Atomic rename)
            if (encrypted.isNotEmpty()) {
                val targetFile = File(context.filesDir, fileName)
                val tempFile = File(context.filesDir, "$fileName.tmp")
                tempFile.writeBytes(encrypted)
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                // SharedPreferences Backup (Async apply)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(keyName, bytesToHex(encrypted)).apply()
            }

            // 2. Failsafe Private App Storage (JSON)
            val jsonFileName = fileName.replace(".enc", ".json")
            val targetJsonFile = File(context.filesDir, jsonFileName)
            val tempJsonFile = File(context.filesDir, "$jsonFileName.tmp")
            tempJsonFile.writeText(plainJson, Charsets.UTF_8)
            if (!tempJsonFile.renameTo(targetJsonFile)) {
                tempJsonFile.copyTo(targetJsonFile, overwrite = true)
                tempJsonFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveData error for $fileName: ${e.message}", e)
        }
    }

    private fun loadData(context: Context, keyName: String, fileName: String): ByteArray? {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists() && file.length() > 0L) {
                val bytes = file.readBytes()
                if (bytes.isNotEmpty()) return bytes
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hex = prefs.getString(keyName, null)
            if (!hex.isNullOrEmpty()) {
                val bytes = hexToBytes(hex)
                if (bytes.isNotEmpty()) return bytes
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadData error for $fileName: ${e.message}", e)
        }
        return null
    }

    private fun loadFallbackJson(context: Context, fileName: String): String? {
        try {
            val jsonFileName = fileName.replace(".enc", ".json")
            val file = File(context.filesDir, jsonFileName)
            if (file.exists() && file.length() > 0L) {
                val str = file.readText(Charsets.UTF_8)
                if (str.isNotBlank()) return str
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadFallbackJson error for $fileName: ${e.message}", e)
        }
        return null
    }

    // --- Synchronous Persistence Methods ---

    fun savePlannerSync(context: Context, data: PlannerData) {
        val key = MasterKeyManager.getUnlockedMasterKey()
        try {
            val str = json.encodeToString(PlannerData.serializer(), data)
            val encrypted = if (key != null) RustSecurityBridge.encryptBytes(key, str.toByteArray(Charsets.UTF_8)) else ByteArray(0)
            saveData(context, "pref_planner_data", "planner_data.enc", encrypted, str)
        } catch (e: Exception) {
            Log.e(TAG, "savePlannerSync error: ${e.message}", e)
        }
    }

    fun loadPlannerSync(context: Context): PlannerData? {
        val key = MasterKeyManager.getUnlockedMasterKey()
        if (key != null) {
            try {
                val encrypted = loadData(context, "pref_planner_data", "planner_data.enc")
                if (encrypted != null && encrypted.isNotEmpty()) {
                    val decrypted = RustSecurityBridge.decryptBytes(key, encrypted)
                    if (decrypted.isNotEmpty()) {
                        val parsed = json.decodeFromString<PlannerData>(String(decrypted, Charsets.UTF_8))
                        if (parsed.todos.isNotEmpty() || parsed.habits.isNotEmpty()) return parsed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPlannerSync decryption error: ${e.message}", e)
            }
        }

        // Failsafe Json Fallback
        val rawJson = loadFallbackJson(context, "planner_data.enc")
        if (rawJson != null) {
            return try {
                json.decodeFromString<PlannerData>(rawJson)
            } catch (_: Exception) { PlannerData() }
        }
        return PlannerData()
    }

    fun saveNotesSync(context: Context, data: NotesData) {
        val key = MasterKeyManager.getUnlockedMasterKey()
        try {
            val str = json.encodeToString(NotesData.serializer(), data)
            val encrypted = if (key != null) RustSecurityBridge.encryptBytes(key, str.toByteArray(Charsets.UTF_8)) else ByteArray(0)
            saveData(context, "pref_notes_data", "notes_data.enc", encrypted, str)
        } catch (e: Exception) {
            Log.e(TAG, "saveNotesSync error: ${e.message}", e)
        }
    }

    fun loadNotesSync(context: Context): NotesData? {
        val key = MasterKeyManager.getUnlockedMasterKey()
        if (key != null) {
            try {
                val encrypted = loadData(context, "pref_notes_data", "notes_data.enc")
                if (encrypted != null && encrypted.isNotEmpty()) {
                    val decrypted = RustSecurityBridge.decryptBytes(key, encrypted)
                    if (decrypted.isNotEmpty()) {
                        val parsed = json.decodeFromString<NotesData>(String(decrypted, Charsets.UTF_8))
                        if (parsed.notes.isNotEmpty()) return parsed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadNotesSync decryption error: ${e.message}", e)
            }
        }

        // Failsafe Json Fallback
        val rawJson = loadFallbackJson(context, "notes_data.enc")
        if (rawJson != null) {
            return try {
                json.decodeFromString<NotesData>(rawJson)
            } catch (_: Exception) { NotesData() }
        }
        return NotesData()
    }

    fun saveVaultSync(context: Context, data: VaultData) {
        val key = MasterKeyManager.getUnlockedMasterKey()
        try {
            val str = json.encodeToString(VaultData.serializer(), data)
            val encrypted = if (key != null) RustSecurityBridge.encryptBytes(key, str.toByteArray(Charsets.UTF_8)) else ByteArray(0)
            saveData(context, "pref_vault_data", "vault_data.enc", encrypted, str)
        } catch (e: Exception) {
            Log.e(TAG, "saveVaultSync error: ${e.message}", e)
        }
    }

    fun loadVaultSync(context: Context): VaultData? {
        val key = MasterKeyManager.getUnlockedMasterKey()
        if (key != null) {
            try {
                val encrypted = loadData(context, "pref_vault_data", "vault_data.enc")
                if (encrypted != null && encrypted.isNotEmpty()) {
                    val decrypted = RustSecurityBridge.decryptBytes(key, encrypted)
                    if (decrypted.isNotEmpty()) {
                        val parsed = json.decodeFromString<VaultData>(String(decrypted, Charsets.UTF_8))
                        if (parsed.credentials.isNotEmpty() || parsed.files.isNotEmpty()) return parsed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadVaultSync decryption error: ${e.message}", e)
            }
        }

        // Failsafe Json Fallback
        val rawJson = loadFallbackJson(context, "vault_data.enc")
        if (rawJson != null) {
            return try {
                json.decodeFromString<VaultData>(rawJson)
            } catch (_: Exception) { VaultData() }
        }
        return VaultData()
    }

    fun saveFinanceSync(context: Context, data: FinanceData) {
        val key = MasterKeyManager.getUnlockedMasterKey()
        try {
            val str = json.encodeToString(FinanceData.serializer(), data)
            val encrypted = if (key != null) RustSecurityBridge.encryptBytes(key, str.toByteArray(Charsets.UTF_8)) else ByteArray(0)
            saveData(context, "pref_finance_data", "finance_data.enc", encrypted, str)
        } catch (e: Exception) {
            Log.e(TAG, "saveFinanceSync error: ${e.message}", e)
        }
    }

    fun loadFinanceSync(context: Context): FinanceData? {
        val key = MasterKeyManager.getUnlockedMasterKey()
        if (key != null) {
            try {
                val encrypted = loadData(context, "pref_finance_data", "finance_data.enc")
                if (encrypted != null && encrypted.isNotEmpty()) {
                    val decrypted = RustSecurityBridge.decryptBytes(key, encrypted)
                    if (decrypted.isNotEmpty()) {
                        val parsed = json.decodeFromString<FinanceData>(String(decrypted, Charsets.UTF_8))
                        if (parsed.expenses.isNotEmpty()) return parsed
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadFinanceSync decryption error: ${e.message}", e)
            }
        }

        // Failsafe Json Fallback
        val rawJson = loadFallbackJson(context, "finance_data.enc")
        if (rawJson != null) {
            return try {
                json.decodeFromString<FinanceData>(rawJson)
            } catch (_: Exception) { FinanceData() }
        }
        return FinanceData()
    }

    // --- Coroutine Wrappers ---
    suspend fun savePlanner(context: Context, data: PlannerData) = withContext(Dispatchers.IO) { savePlannerSync(context, data) }
    suspend fun loadPlanner(context: Context): PlannerData? = withContext(Dispatchers.IO) { loadPlannerSync(context) }
    suspend fun saveNotes(context: Context, data: NotesData) = withContext(Dispatchers.IO) { saveNotesSync(context, data) }
    suspend fun loadNotes(context: Context): NotesData? = withContext(Dispatchers.IO) { loadNotesSync(context) }
    suspend fun saveVault(context: Context, data: VaultData) = withContext(Dispatchers.IO) { saveVaultSync(context, data) }
    suspend fun loadVault(context: Context): VaultData? = withContext(Dispatchers.IO) { loadVaultSync(context) }
    suspend fun saveFinance(context: Context, data: FinanceData) = withContext(Dispatchers.IO) { saveFinanceSync(context, data) }
    suspend fun loadFinance(context: Context): FinanceData? = withContext(Dispatchers.IO) { loadFinanceSync(context) }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }
}
