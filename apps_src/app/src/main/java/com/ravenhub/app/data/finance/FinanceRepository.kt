package com.ravenhub.app.data.finance

import android.content.Context
import android.util.Log
import com.ravenhub.app.security.MasterKeyManager
import com.ravenhub.app.security.RustSecurityBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

object FinanceRepository {
    private const val TAG = "FinanceRepository"
    private const val FILE_NAME = "finance_data.enc"
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun getFile(context: Context): File = File(context.filesDir, FILE_NAME)

    suspend fun load(context: Context, key: ByteArray? = MasterKeyManager.getUnlockedMasterKey()): FinanceData? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val file = getFile(context)
            if (!file.exists()) return@withContext FinanceData()
            if (file.length() == 0L) return@withContext FinanceData()

            val masterKey = key ?: return@withContext null

            try {
                val encrypted = file.readBytes()
                val decrypted = RustSecurityBridge.decryptBytes(masterKey, encrypted)
                if (decrypted.isEmpty()) {
                    Log.e(TAG, "decrypted bytes empty for finance_data.enc")
                    return@withContext null
                }
                json.decodeFromString<FinanceData>(String(decrypted, Charsets.UTF_8))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load finance_data.enc: ${e.message}", e)
                null
            }
        }
    }

    suspend fun save(context: Context, data: FinanceData, key: ByteArray? = MasterKeyManager.getUnlockedMasterKey()) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val masterKey = key ?: return@withContext

            try {
                val plaintext = json.encodeToString(FinanceData.serializer(), data).toByteArray(Charsets.UTF_8)
                val encrypted = RustSecurityBridge.encryptBytes(masterKey, plaintext)
                val targetFile = getFile(context)
                val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
                tempFile.writeBytes(encrypted)
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save finance_data.enc: ${e.message}", e)
            }
        }
    }
}
