package com.fws.smarttrace.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's Gemini API keys encrypted with an AES-256-GCM key that lives in the
 * Android Keystore (the key material never leaves secure hardware / the system keystore).
 */
object SecureKeyStore {
    private const val ALIAS = "smarttrace_gemini_keys"
    private const val PREF = "secure_keys"
    private const val FIELD = "blob"

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun load(ctx: Context): List<String> {
        val sp = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val blob = sp.getString(FIELD, null) ?: return emptyList()
        return try {
            val raw = Base64.decode(blob, Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            val json = String(cipher.doFinal(raw, 12, raw.size - 12), Charsets.UTF_8)
            val arr = JSONArray(json)
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            sp.edit().remove(FIELD).apply() // unreadable (e.g. keystore reset) -> start clean
            emptyList()
        }
    }

    fun save(ctx: Context, keys: List<String>) {
        val sp = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (keys.isEmpty()) {
            sp.edit().remove(FIELD).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val enc = cipher.doFinal(JSONArray(keys).toString().toByteArray(Charsets.UTF_8))
        sp.edit().putString(FIELD, Base64.encodeToString(cipher.iv + enc, Base64.NO_WRAP)).apply()
    }

    /** Parses pasted text: one key per line, trimmed, de-duplicated. */
    fun parseLines(text: String): List<String> =
        text.lines().map { it.trim().trim('"', '\'', ',', ';') }.filter { it.length >= 8 && !it.contains(' ') }.distinct()

    /** First 3 characters + "..." + last 3 characters. */
    fun mask(key: String): String =
        if (key.length >= 8) key.take(3) + "..." + key.takeLast(3) else "***"
}
