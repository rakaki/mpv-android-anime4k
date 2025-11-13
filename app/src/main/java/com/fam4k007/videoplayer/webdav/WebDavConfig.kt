package com.fam4k007.videoplayer.webdav

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * WebDAV 配置管理类
 * 使用加密存储保存服务器地址、账号、密码
 */
data class WebDavConfig(
    var serverUrl: String = "",
    var displayName: String = "WebDAV媒体库",
    var account: String = "",
    var password: String = "",
    var isAnonymous: Boolean = false
) {
    companion object {
        private const val PREFS_NAME = "webdav_config"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ACCOUNT = "account"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_ANONYMOUS = "is_anonymous"

        /**
         * 从加密存储中加载配置
         */
        fun load(context: Context): WebDavConfig {
            val prefs = getEncryptedPrefs(context)
            return WebDavConfig(
                serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: "",
                displayName = prefs.getString(KEY_DISPLAY_NAME, "WebDAV媒体库") ?: "WebDAV媒体库",
                account = prefs.getString(KEY_ACCOUNT, "") ?: "",
                password = prefs.getString(KEY_PASSWORD, "") ?: "",
                isAnonymous = prefs.getBoolean(KEY_IS_ANONYMOUS, false)
            )
        }

        /**
         * 保存配置到加密存储
         */
        fun save(context: Context, config: WebDavConfig) {
            val prefs = getEncryptedPrefs(context)
            prefs.edit().apply {
                putString(KEY_SERVER_URL, config.serverUrl)
                putString(KEY_DISPLAY_NAME, config.displayName)
                putString(KEY_ACCOUNT, config.account)
                putString(KEY_PASSWORD, config.password)
                putBoolean(KEY_IS_ANONYMOUS, config.isAnonymous)
                apply()
            }
        }

        /**
         * 清除配置
         */
        fun clear(context: Context) {
            val prefs = getEncryptedPrefs(context)
            prefs.edit().clear().apply()
        }

        /**
         * 检查是否已配置
         */
        fun isConfigured(context: Context): Boolean {
            val config = load(context)
            return config.serverUrl.isNotEmpty()
        }

        /**
         * 获取加密的 SharedPreferences
         */
        private fun getEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
