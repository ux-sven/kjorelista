package no.svenway.kjorelista.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Lagrer Supabase-sesjonen kryptert med en AES-nøkkel i Android Keystore.
 * Nøkkelen forlater aldri Keystore; bare chiffertekst ligger i SharedPreferences.
 */
class SecureSessionManager(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY, encrypt(json.encodeToString(UserSession.serializer(), session))) }
    }

    /** Kaster [NoSessionFoundException] når ingen (gyldig) sesjon er lagret, slik supabase-kt forventer. */
    override suspend fun loadSession(): UserSession = withContext(Dispatchers.IO) {
        val stored = prefs.getString(KEY, null) ?: throw NoSessionFoundException()
        runCatching { json.decodeFromString(UserSession.serializer(), decrypt(stored)) }
            .onFailure { prefs.edit { remove(KEY) } }
            .getOrNull() ?: throw NoSessionFoundException()
    }

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        prefs.edit { remove(KEY) }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return b64(cipher.iv) + ":" + b64(encrypted)
    }

    private fun decrypt(stored: String): String {
        val (iv, data) = stored.split(":").map { Base64.decode(it, Base64.NO_WRAP) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "kjorelista_session_key"
        const val KEY = "session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
