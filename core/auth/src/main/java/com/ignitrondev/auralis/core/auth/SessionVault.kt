/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ignitrondev.auralis.provider.api.ProviderSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The only place a session is stored (spec sections 11 and 12.1): encrypted with AES-GCM under a
 * key that never leaves the Android Keystore, in no-backup storage so it is not copied to cloud
 * backups or other devices.
 */
internal class SessionVault(context: Context) {
    private val file = File(context.noBackupFilesDir, FILE_NAME)

    @Serializable
    private class Stored(val cookie: String, val visitorData: String?, val dataSyncId: String?, val authUser: String)

    fun read(): ProviderSession? {
        if (!file.exists()) return null
        return try {
            val bytes = file.readBytes()
            val iv = bytes.copyOfRange(0, IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            val stored = Json.decodeFromString(Stored.serializer(), cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE).decodeToString())
            ProviderSession(stored.cookie, stored.visitorData, stored.dataSyncId, stored.authUser)
        } catch (error: Exception) {
            // Unreadable (for example the key was reset): treat as signed out and remove it.
            clear()
            null
        }
    }

    fun write(session: ProviderSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val plain = Json.encodeToString(
            Stored.serializer(),
            Stored(session.cookie, session.visitorData, session.dataSyncId, session.authUser),
        ).encodeToByteArray()
        val encrypted = cipher.iv + cipher.doFinal(plain)
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        temp.writeBytes(encrypted)
        if (!temp.renameTo(file)) {
            temp.delete()
            error("could not store session")
        }
    }

    fun clear() {
        file.delete()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE_NAME = "session.bin"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "auralis.session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
