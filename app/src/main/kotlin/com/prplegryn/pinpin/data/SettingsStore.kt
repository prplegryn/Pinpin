package com.prplegryn.pinpin.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ApiSettings(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.7f,
    val timeoutSeconds: Int = 90,
    val contextMessageLimit: Int = 40,
    val activeRoleId: String = RoleProfile.GENERAL_ID,
    val customRoleName: String = "自定义",
    val customRolePrompt: String = ""
)

class SettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val secretCipher = SecretCipher()
    private val mutableSettings = MutableStateFlow(read())

    val settings: StateFlow<ApiSettings> = mutableSettings.asStateFlow()

    fun save(value: ApiSettings) {
        val normalized = value.copy(
            baseUrl = value.baseUrl.trim().trimEnd('/'),
            model = value.model.trim(),
            temperature = value.temperature.coerceIn(0f, 2f),
            timeoutSeconds = value.timeoutSeconds.coerceIn(15, 300),
            contextMessageLimit = value.contextMessageLimit.coerceIn(8, 120),
            customRoleName = value.customRoleName.trim().take(32),
            customRolePrompt = value.customRolePrompt.trim().take(8000)
        )
        preferences.edit()
            .putString(KEY_BASE_URL, normalized.baseUrl)
            .putString(KEY_MODEL, normalized.model)
            .putFloat(KEY_TEMPERATURE, normalized.temperature)
            .putInt(KEY_TIMEOUT, normalized.timeoutSeconds)
            .putInt(KEY_CONTEXT_LIMIT, normalized.contextMessageLimit)
            .putString(KEY_ACTIVE_ROLE, normalized.activeRoleId)
            .putString(KEY_CUSTOM_ROLE_NAME, normalized.customRoleName)
            .putString(KEY_CUSTOM_ROLE_PROMPT, normalized.customRolePrompt)
            .apply {
                if (normalized.apiKey.isBlank()) {
                    remove(KEY_API_SECRET)
                } else {
                    putString(KEY_API_SECRET, secretCipher.encrypt(normalized.apiKey.trim()))
                }
            }
            .apply()
        mutableSettings.value = normalized.copy(apiKey = normalized.apiKey.trim())
    }

    fun updateActiveRole(roleId: String) {
        preferences.edit().putString(KEY_ACTIVE_ROLE, roleId).apply()
        mutableSettings.value = mutableSettings.value.copy(activeRoleId = roleId)
    }

    private fun read(): ApiSettings {
        val encryptedSecret = preferences.getString(KEY_API_SECRET, null)
        return ApiSettings(
            baseUrl = preferences.getString(KEY_BASE_URL, null)
                ?.takeIf { it.isNotBlank() }
                ?: ApiSettings().baseUrl,
            apiKey = encryptedSecret?.let(secretCipher::decrypt).orEmpty(),
            model = preferences.getString(KEY_MODEL, "").orEmpty(),
            temperature = preferences.getFloat(KEY_TEMPERATURE, 0.7f),
            timeoutSeconds = preferences.getInt(KEY_TIMEOUT, 90),
            contextMessageLimit = preferences.getInt(KEY_CONTEXT_LIMIT, 40),
            activeRoleId = preferences.getString(KEY_ACTIVE_ROLE, RoleProfile.GENERAL_ID)
                ?: RoleProfile.GENERAL_ID,
            customRoleName = preferences.getString(KEY_CUSTOM_ROLE_NAME, "自定义")
                ?: "自定义",
            customRolePrompt = preferences.getString(KEY_CUSTOM_ROLE_PROMPT, "").orEmpty()
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "pinpin_settings"
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_SECRET = "api_secret"
        const val KEY_MODEL = "model"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_TIMEOUT = "timeout"
        const val KEY_CONTEXT_LIMIT = "context_limit"
        const val KEY_ACTIVE_ROLE = "active_role"
        const val KEY_CUSTOM_ROLE_NAME = "custom_role_name"
        const val KEY_CUSTOM_ROLE_PROMPT = "custom_role_prompt"
    }
}

private class SecretCipher {
    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(Int.SIZE_BYTES + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String = runCatching {
        val payload = ByteBuffer.wrap(Base64.decode(value, Base64.NO_WRAP))
        val ivSize = payload.int
        require(ivSize in 12..32 && payload.remaining() > ivSize)
        val iv = ByteArray(ivSize)
        payload.get(iv)
        val encrypted = ByteArray(payload.remaining())
        payload.get(encrypted)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrDefault("")

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pinpin_api_secret_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
