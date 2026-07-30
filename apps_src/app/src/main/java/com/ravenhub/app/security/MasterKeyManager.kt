package com.ravenhub.app.security

import android.content.Context
import android.util.Log
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object MasterKeyManager {
    private const val TAG = "MasterKeyManager"
    private const val PREFS_NAME = "raven_security_prefs"
    private const val PREF_PIN_VERIFIER = "pin_verifier_hash"
    private const val PREF_SAVED_PIN = "saved_app_pin"
    private const val PREF_SALT_HEX = "device_salt_hex"
    private const val PBKDF2_ITERATIONS = 100_000

    @Volatile
    private var unlockedMasterKey: ByteArray? = null

    fun isPinSetup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(PREF_PIN_VERIFIER)
    }

    @Synchronized
    fun getOrCreateMasterKey(context: Context, pin: String): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val salt = getOrCreateSalt(context)
        val masterKey = derivePinKey(pin, salt)
        val pinVerifier = bytesToHex(derivePinKey(pin, salt.reversedArray()))

        prefs.edit()
            .putString(PREF_PIN_VERIFIER, pinVerifier)
            .putString(PREF_SAVED_PIN, pin)
            .commit()

        unlockedMasterKey = masterKey.copyOf()
        return masterKey
    }

    @Synchronized
    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val verifierHex = prefs.getString(PREF_PIN_VERIFIER, null) ?: return false
        val salt = getOrCreateSalt(context)
        val pinVerifier = bytesToHex(derivePinKey(pin, salt.reversedArray()))

        if (verifierHex == pinVerifier) {
            val masterKey = derivePinKey(pin, salt)
            prefs.edit().putString(PREF_SAVED_PIN, pin).commit()
            unlockedMasterKey = masterKey.copyOf()
            return true
        }
        return false
    }

    @Synchronized
    fun unlockWithBiometric(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPin = prefs.getString(PREF_SAVED_PIN, null)
        val salt = getOrCreateSalt(context)

        if (!savedPin.isNullOrEmpty()) {
            val masterKey = derivePinKey(savedPin, salt)
            unlockedMasterKey = masterKey.copyOf()
            return true
        }
        return false
    }

    fun lock() {
        unlockedMasterKey?.fill(0)
        unlockedMasterKey = null
    }

    fun isUnlocked(): Boolean = unlockedMasterKey != null

    fun getUnlockedMasterKey(): ByteArray? = unlockedMasterKey?.copyOf()

    private fun getOrCreateSalt(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltHex = prefs.getString(PREF_SALT_HEX, null)

        if (saltHex != null && saltHex.length == 32) {
            return hexToBytes(saltHex)
        }

        val rawSalt = ByteArray(16)
        SecureRandom().nextBytes(rawSalt)
        val hex = bytesToHex(rawSalt)

        prefs.edit()
            .putString(PREF_SALT_HEX, hex)
            .commit()

        return rawSalt
    }

    private fun derivePinKey(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
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
}
