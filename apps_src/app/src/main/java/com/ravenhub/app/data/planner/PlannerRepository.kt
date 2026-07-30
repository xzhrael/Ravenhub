package com.ravenhub.app.data.planner

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

object PlannerRepository {
    private const val TAG = "PlannerRepository"
    private const val FILE_NAME = "planner_data.enc"
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun getFile(context: Context): File = File(context.filesDir, FILE_NAME)

    suspend fun load(context: Context, key: ByteArray? = MasterKeyManager.getUnlockedMasterKey()): PlannerData? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val file = getFile(context)
            if (!file.exists()) return@withContext PlannerData()
            if (file.length() == 0L) return@withContext PlannerData()

            val masterKey = key ?: return@withContext null

            try {
                val encrypted = file.readBytes()
                val decrypted = RustSecurityBridge.decryptBytes(masterKey, encrypted)
                if (decrypted.isEmpty()) {
                    Log.e(TAG, "decrypted bytes empty for planner_data.enc")
                    return@withContext null
                }
                json.decodeFromString<PlannerData>(String(decrypted, Charsets.UTF_8))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load planner_data.enc: ${e.message}", e)
                null
            }
        }
    }

    suspend fun save(context: Context, data: PlannerData, key: ByteArray? = MasterKeyManager.getUnlockedMasterKey()) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val masterKey = key ?: return@withContext

            try {
                val plaintext = json.encodeToString(PlannerData.serializer(), data).toByteArray(Charsets.UTF_8)
                val encrypted = RustSecurityBridge.encryptBytes(masterKey, plaintext)
                val targetFile = getFile(context)
                val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
                tempFile.writeBytes(encrypted)
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save planner_data.enc: ${e.message}", e)
            }
        }
    }
}
