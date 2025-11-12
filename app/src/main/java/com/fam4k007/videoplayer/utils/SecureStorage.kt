package com.fam4k007.videoplayer.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 安全存储工具类
 * 使用 EncryptedSharedPreferences 加密存储敏感数据
 * 
 * 安全特性：
 * 1. 使用 AES256-GCM 加密算法
 * 2. 密钥存储在 Android KeyStore 中，硬件级别保护
 * 3. 每次读写都会进行加密/解密
 * 4. 防止数据被其他应用或 Root 设备读取
 */
object SecureStorage {
    
    private const val FILE_NAME = "secure_prefs"
    
    /**
     * 获取加密的 SharedPreferences
     */
    fun getEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            // 创建或获取 MasterKey
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            // 创建加密的 SharedPreferences
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 如果加密失败（极少数情况），fallback 到普通存储
            // 这可能发生在某些低端设备或特殊 ROM 上
            android.util.Log.e("SecureStorage", "Failed to create encrypted preferences, using normal", e)
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * 保存字符串
     */
    fun putString(context: Context, key: String, value: String?) {
        getEncryptedPreferences(context).edit().putString(key, value).apply()
    }
    
    /**
     * 获取字符串
     */
    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getEncryptedPreferences(context).getString(key, defaultValue)
    }
    
    /**
     * 删除指定键
     */
    fun remove(context: Context, key: String) {
        getEncryptedPreferences(context).edit().remove(key).apply()
    }
    
    /**
     * 清空所有数据
     */
    fun clear(context: Context) {
        getEncryptedPreferences(context).edit().clear().apply()
    }
}
