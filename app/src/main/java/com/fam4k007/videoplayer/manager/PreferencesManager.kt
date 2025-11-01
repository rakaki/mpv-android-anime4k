package com.fam4k007.videoplayer.manager

import android.content.Context
import android.content.SharedPreferences
import com.fam4k007.videoplayer.AppConstants

/**
 * 统一的设置管理器（单例模式）
 * 集中所有 SharedPreferences 操作，避免重复创建和散落在各处
 */
class PreferencesManager private constructor(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        AppConstants.Preferences.PLAYER_PREFS,
        Context.MODE_PRIVATE
    )
    
    companion object {
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // ==================== 快进时长 ====================
    
    /**
     * 获取快进/快退时长（秒）
     */
    fun getSeekTime(): Int {
        return sharedPreferences.getInt(
            AppConstants.Preferences.SEEK_TIME,
            AppConstants.Defaults.DEFAULT_SEEK_TIME
        )
    }
    
    /**
     * 保存快进/快退时长
     */
    fun setSeekTime(seconds: Int) {
        sharedPreferences.edit().putInt(AppConstants.Preferences.SEEK_TIME, seconds).apply()
    }
    
    // ==================== 长按倍速 ====================
    
    /**
     * 获取长按倍速
     */
    fun getLongPressSpeed(): Float {
        return sharedPreferences.getFloat(
            AppConstants.Preferences.LONG_PRESS_SPEED,
            AppConstants.Defaults.DEFAULT_LONG_PRESS_SPEED
        )
    }
    
    /**
     * 保存长按倍速
     */
    fun setLongPressSpeed(speed: Float) {
        sharedPreferences.edit().putFloat(AppConstants.Preferences.LONG_PRESS_SPEED, speed).apply()
    }
    
    // ==================== 精确进度定位 ====================
    
    /**
     * 获取是否启用精确进度定位
     */
    fun isPreciseSeekingEnabled(): Boolean {
        return sharedPreferences.getBoolean(
            AppConstants.Preferences.PRECISE_SEEKING,
            false
        )
    }
    
    /**
     * 保存精确进度定位设置
     */
    fun setPreciseSeekingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(AppConstants.Preferences.PRECISE_SEEKING, enabled).apply()
    }
    
    // ==================== 音量增强 ====================
    
    /**
     * 获取是否启用音量增强(允许音量超过100%)
     */
    fun isVolumeBoostEnabled(): Boolean {
        return sharedPreferences.getBoolean(
            AppConstants.Preferences.VOLUME_BOOST_ENABLED,
            true  // 默认启用
        )
    }
    
    /**
     * 保存音量增强设置
     */
    fun setVolumeBoostEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(AppConstants.Preferences.VOLUME_BOOST_ENABLED, enabled).apply()
    }
    
    // ==================== 播放位置（用于记忆播放进度）====================
    
    /**
     * 获取视频的保存播放位置
     * @param videoUri 视频URI（使用 uri.toString() 作为键）
     */
    fun getPlaybackPosition(videoUri: String): Double {
        return sharedPreferences.getFloat(videoUri, 0f).toDouble()
    }
    
    /**
     * 保存视频播放位置
     */
    fun setPlaybackPosition(videoUri: String, position: Double) {
        sharedPreferences.edit().putFloat(videoUri, position.toFloat()).apply()
    }
    
    /**
     * 清除视频播放位置记录
     */
    fun clearPlaybackPosition(videoUri: String) {
        sharedPreferences.edit().remove(videoUri).apply()
    }
    
    // ==================== 主题设置 ====================
    
    /**
     * 获取主题模式
     * @return "light" 亮色 | "dark" 深色 | "system" 跟随系统
     */
    fun getThemeMode(): String {
        return sharedPreferences.getString(
            "theme_mode",
            "light"  // 默认亮色
        ) ?: "light"
    }
    
    /**
     * 保存主题模式
     */
    fun setThemeMode(mode: String) {
        sharedPreferences.edit().putString("theme_mode", mode).apply()
    }
    
    // ==================== 批量操作 ====================
    
    /**
     * 清除所有设置（谨慎使用）
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * 获取所有设置
     */
    fun getAll(): Map<String, *> {
        return sharedPreferences.all ?: emptyMap<String, Any>()
    }
}
