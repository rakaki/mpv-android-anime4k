package com.fam4k007.videoplayer.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences工具类
 */
object PreferenceUtils {
    private const val PREF_NAME = "video_player_prefs"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存字符串列表
     */
    fun saveStringList(context: Context, key: String, list: List<String>) {
        val prefs = getPreferences(context)
        val editor = prefs.edit()
        
        // 先清空旧数据
        prefs.all.keys.filter { it.startsWith("${key}_") }.forEach { oldKey ->
            editor.remove(oldKey)
        }
        
        // 保存列表大小
        editor.putInt("${key}_size", list.size)
        
        // 保存每个元素
        list.forEachIndexed { index, value ->
            editor.putString("${key}_$index", value)
        }
        
        editor.apply()
    }

    /**
     * 获取字符串列表
     */
    fun getStringList(context: Context, key: String): List<String> {
        val prefs = getPreferences(context)
        val size = prefs.getInt("${key}_size", 0)
        
        return (0 until size).mapNotNull { index ->
            prefs.getString("${key}_$index", null)
        }
    }

    /**
     * 保存字符串
     */
    fun saveString(context: Context, key: String, value: String) {
        getPreferences(context).edit().putString(key, value).apply()
    }

    /**
     * 获取字符串
     */
    fun getString(context: Context, key: String, defaultValue: String = ""): String {
        return getPreferences(context).getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 保存布尔值
     */
    fun saveBoolean(context: Context, key: String, value: Boolean) {
        getPreferences(context).edit().putBoolean(key, value).apply()
    }

    /**
     * 获取布尔值
     */
    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getPreferences(context).getBoolean(key, defaultValue)
    }

    /**
     * 保存整数
     */
    fun saveInt(context: Context, key: String, value: Int) {
        getPreferences(context).edit().putInt(key, value).apply()
    }

    /**
     * 获取整数
     */
    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return getPreferences(context).getInt(key, defaultValue)
    }

    /**
     * 删除指定key
     */
    fun remove(context: Context, key: String) {
        getPreferences(context).edit().remove(key).apply()
    }

    /**
     * 清空所有数据
     */
    fun clear(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}
