package com.jwd.fafa.core.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jwd.fafa.core.network.SessionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * 会话令牌安全存储：Android Keystore AES/GCM 加密后写入 DataStore。
 * 不写 Cookie、不落明文（对应手册 §6.1“刷新凭据存系统安全区”）。
 * 解密失败（换机恢复等）一律视为无会话并清除，避免旧凭据残留。
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext @Suppress("unused") private val context: Context,
    @com.jwd.fafa.core.di.SessionDataStore private val dataStore: DataStore<Preferences>,
) {

    @Volatile
    private var memToken: String? = null

    @Volatile
    private var memLoaded = false

    val tokenFlow: Flow<String?> = dataStore.data.map { prefs -> decryptToken(prefs) }

    suspend fun token(): String? = decryptToken(dataStore.data.first())

    /** 同步取令牌（拦截器用）；首次调用会阻塞至 DataStore 就绪。 */
    fun tokenNow(): String? {
        if (!memLoaded) {
            memToken = runBlocking { this@SessionStore.token() }
            memLoaded = true
        }
        return memToken
    }

    fun userEmailNow(): String? = runBlocking { dataStore.data.first()[KEY_EMAIL] }

    suspend fun save(session: SessionDto) {
        val blob = encryptToBlob(session.tokenValue.toByteArray())
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = blob
            session.user?.userId?.let { prefs[KEY_USER_ID] = it }
            session.user?.email?.let { prefs[KEY_EMAIL] = it }
        }
        memToken = session.tokenValue
        memLoaded = true
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
        memToken = null
        memLoaded = true
    }

    private fun decryptToken(prefs: Preferences): String? {
        val blob = prefs[KEY_TOKEN] ?: return null
        return try {
            String(decryptFromBlob(blob))
        } catch (_: Exception) {
            // 密钥不可用/数据损坏（如换机恢复备份）：清除并视为未登录
            memToken = null
            memLoaded = true
            runBlocking { dataStore.edit { it.remove(KEY_TOKEN) } }
            null
        }
    }

    // ---------- Keystore AES/GCM ----------

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encryptToBlob(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        val blob = ByteArray(1 + iv.size + encrypted.size)
        blob[0] = iv.size.toByte()
        iv.copyInto(blob, 1)
        encrypted.copyInto(blob, 1 + iv.size)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun decryptFromBlob(blobBase64: String): ByteArray {
        val blob = Base64.decode(blobBase64, Base64.NO_WRAP)
        val ivSize = blob[0].toInt()
        val iv = blob.copyOfRange(1, 1 + ivSize)
        val encrypted = blob.copyOfRange(1 + ivSize, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("fafa_token_enc")
        val KEY_USER_ID = longPreferencesKey("fafa_user_id")
        val KEY_EMAIL = stringPreferencesKey("fafa_email")
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "fafa_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
