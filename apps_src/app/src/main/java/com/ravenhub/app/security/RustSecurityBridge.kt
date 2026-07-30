package com.ravenhub.app.security

import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object RustSecurityBridge {
    private const val TAG = "RustSecurityBridge"

    fun encryptBytes(key: ByteArray, plaintext: ByteArray): ByteArray {
        if (key.isEmpty() || plaintext.isEmpty()) return ByteArray(0)
        return try {
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val secretKey = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val cipherText = cipher.doFinal(plaintext)
            iv + cipherText
        } catch (e: Throwable) {
            Log.e(TAG, "encryptBytes error: ${e.message}", e)
            ByteArray(0)
        }
    }

    fun decryptBytes(key: ByteArray, ciphertext: ByteArray): ByteArray {
        if (key.isEmpty() || ciphertext.size <= 12) return ByteArray(0)
        return try {
            val iv = ciphertext.copyOfRange(0, 12)
            val encryptedData = ciphertext.copyOfRange(12, ciphertext.size)
            val secretKey = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher.doFinal(encryptedData)
        } catch (e: Throwable) {
            Log.e(TAG, "decryptBytes error: ${e.message}", e)
            ByteArray(0)
        }
    }

    fun encryptFileChunked(inputPath: String, outputPath: String, key: ByteArray) {
        try {
            val inputFile = java.io.File(inputPath)
            val outputFile = java.io.File(outputPath)
            val encrypted = encryptBytes(key, inputFile.readBytes())
            outputFile.writeBytes(encrypted)
        } catch (e: Throwable) {
            Log.e(TAG, "encryptFileChunked error: ${e.message}", e)
        }
    }

    fun decryptFileChunked(inputPath: String, outputPath: String, key: ByteArray) {
        val inputFile = java.io.File(inputPath)
        val outputFile = java.io.File(outputPath)
        val decrypted = decryptBytes(key, inputFile.readBytes())
        if (decrypted.isEmpty() && inputFile.length() > 0) {
            throw java.security.GeneralSecurityException("Decryption failed or invalid key")
        }
        outputFile.writeBytes(decrypted)
    }
}
