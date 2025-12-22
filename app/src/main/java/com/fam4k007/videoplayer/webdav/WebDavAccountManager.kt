package com.fam4k007.videoplayer.webdav

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * WebDAV 账户信息
 */
data class WebDavAccount(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val serverUrl: String,
    val account: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)

/**
 * WebDAV 多账户管理器
 */
class WebDavAccountManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "webdav_accounts"
        private const val KEY_ACCOUNTS = "accounts"
        
        @Volatile
        private var instance: WebDavAccountManager? = null
        
        fun getInstance(context: Context): WebDavAccountManager {
            return instance ?: synchronized(this) {
                instance ?: WebDavAccountManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * 获取所有账户
     */
    fun getAllAccounts(): List<WebDavAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WebDavAccount>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 添加账户
     */
    fun addAccount(account: WebDavAccount): Boolean {
        return try {
            val accounts = getAllAccounts().toMutableList()
            
            // 检查是否已存在相同服务器的账户
            if (accounts.any { it.serverUrl == account.serverUrl && it.account == account.account }) {
                return false
            }
            
            accounts.add(account)
            saveAccounts(accounts)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 删除账户
     */
    fun deleteAccount(accountId: String): Boolean {
        return try {
            val accounts = getAllAccounts().toMutableList()
            accounts.removeAll { it.id == accountId }
            saveAccounts(accounts)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 更新账户
     */
    fun updateAccount(account: WebDavAccount): Boolean {
        return try {
            val accounts = getAllAccounts().toMutableList()
            val index = accounts.indexOfFirst { it.id == account.id }
            if (index >= 0) {
                accounts[index] = account
                saveAccounts(accounts)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 根据 ID 获取账户
     */
    fun getAccountById(accountId: String): WebDavAccount? {
        return getAllAccounts().find { it.id == accountId }
    }
    
    /**
     * 清除所有账户
     */
    fun clearAllAccounts() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 保存账户列表
     */
    private fun saveAccounts(accounts: List<WebDavAccount>) {
        val json = gson.toJson(accounts)
        prefs.edit().putString(KEY_ACCOUNTS, json).apply()
    }
}
