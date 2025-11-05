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
            false  // 默认关闭
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
    
    // ==================== 字幕偏好设置（针对每个视频）====================
    
    /**
     * 获取视频的外部字幕路径
     */
    fun getExternalSubtitle(videoUri: String): String? {
        return sharedPreferences.getString("${videoUri}_subtitle", null)
    }
    
    /**
     * 保存视频的外部字幕路径
     */
    fun setExternalSubtitle(videoUri: String, subtitlePath: String) {
        sharedPreferences.edit().putString("${videoUri}_subtitle", subtitlePath).apply()
    }
    
    /**
     * 获取视频的字幕大小
     */
    fun getSubtitleScale(videoUri: String): Double {
        return sharedPreferences.getFloat("${videoUri}_sub_scale", 1.0f).toDouble()
    }
    
    /**
     * 保存视频的字幕大小
     */
    fun setSubtitleScale(videoUri: String, scale: Double) {
        sharedPreferences.edit().putFloat("${videoUri}_sub_scale", scale.toFloat()).apply()
    }
    
    /**
     * 获取视频的字幕位置
     */
    fun getSubtitlePosition(videoUri: String): Int {
        return sharedPreferences.getInt("${videoUri}_sub_pos", 100)
    }
    
    /**
     * 保存视频的字幕位置
     */
    fun setSubtitlePosition(videoUri: String, position: Int) {
        sharedPreferences.edit().putInt("${videoUri}_sub_pos", position).apply()
    }
    
    /**
     * 获取视频的字幕延迟
     */
    fun getSubtitleDelay(videoUri: String): Double {
        return sharedPreferences.getFloat("${videoUri}_sub_delay", 0f).toDouble()
    }
    
    /**
     * 保存视频的字幕延迟
     */
    fun setSubtitleDelay(videoUri: String, delay: Double) {
        sharedPreferences.edit().putFloat("${videoUri}_sub_delay", delay.toFloat()).apply()
    }
    
    /**
     * 获取是否启用ASS字幕样式覆盖
     */
    fun isAssOverrideEnabled(videoUri: String): Boolean {
        return sharedPreferences.getBoolean("${videoUri}_ass_override", false)
    }
    
    /**
     * 保存ASS字幕样式覆盖设置
     */
    fun setAssOverrideEnabled(videoUri: String, enabled: Boolean) {
        sharedPreferences.edit().putBoolean("${videoUri}_ass_override", enabled).apply()
    }
    
    /**
     * 获取视频的字幕轨道ID
     */
    fun getSubtitleTrackId(videoUri: String): Int {
        return sharedPreferences.getInt("${videoUri}_sub_track_id", -1)
    }
    
    /**
     * 保存视频的字幕轨道ID
     */
    fun setSubtitleTrackId(videoUri: String, trackId: Int) {
        sharedPreferences.edit().putInt("${videoUri}_sub_track_id", trackId).apply()
    }
    
    // ==================== 字幕样式设置 ====================
    
    /**
     * 获取字幕文本颜色（ARGB格式）
     */
    fun getSubtitleTextColor(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_text_color", "#FFFFFF") ?: "#FFFFFF"
    }
    
    /**
     * 保存字幕文本颜色
     */
    fun setSubtitleTextColor(videoUri: String, color: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_text_color", color).apply()
    }
    
    /**
     * 获取字幕描边粗细
     */
    fun getSubtitleBorderSize(videoUri: String): Int {
        return sharedPreferences.getInt("${videoUri}_sub_border_size", 3)
    }
    
    /**
     * 保存字幕描边粗细
     */
    fun setSubtitleBorderSize(videoUri: String, size: Int) {
        sharedPreferences.edit().putInt("${videoUri}_sub_border_size", size).apply()
    }
    
    /**
     * 获取字幕描边颜色（ARGB格式）
     */
    fun getSubtitleBorderColor(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_border_color", "#000000") ?: "#000000"
    }
    
    /**
     * 保存字幕描边颜色
     */
    fun setSubtitleBorderColor(videoUri: String, color: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_border_color", color).apply()
    }
    
    /**
     * 获取字幕背景颜色（ARGB格式）
     */
    fun getSubtitleBackColor(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_back_color", "#00000000") ?: "#00000000"
    }
    
    /**
     * 保存字幕背景颜色
     */
    fun setSubtitleBackColor(videoUri: String, color: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_back_color", color).apply()
    }
    
    /**
     * 获取字幕描边样式
     */
    fun getSubtitleBorderStyle(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_border_style", "outline-and-shadow") ?: "outline-and-shadow"
    }
    
    /**
     * 保存字幕描边样式
     */
    fun setSubtitleBorderStyle(videoUri: String, style: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_border_style", style).apply()
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
    
    // ==================== 弹幕设置 ====================
    
    fun getDanmakuEnabled(): Boolean {
        return sharedPreferences.getBoolean("danmaku_enabled", true)
    }
    
    fun setDanmakuEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_enabled", enabled).apply()
    }
    
    fun getDanmakuSize(): Int {
        return sharedPreferences.getInt("danmaku_size", 50)
    }
    
    fun setDanmakuSize(size: Int) {
        sharedPreferences.edit().putInt("danmaku_size", size).apply()
    }
    
    fun getDanmakuSpeed(): Int {
        return sharedPreferences.getInt("danmaku_speed", 50)
    }
    
    fun setDanmakuSpeed(speed: Int) {
        sharedPreferences.edit().putInt("danmaku_speed", speed).apply()
    }
    
    fun getDanmakuAlpha(): Int {
        return sharedPreferences.getInt("danmaku_alpha", 100)
    }
    
    fun setDanmakuAlpha(alpha: Int) {
        sharedPreferences.edit().putInt("danmaku_alpha", alpha).apply()
    }
    
    fun getDanmakuStroke(): Int {
        return sharedPreferences.getInt("danmaku_stroke", 50)
    }
    
    fun setDanmakuStroke(stroke: Int) {
        sharedPreferences.edit().putInt("danmaku_stroke", stroke).apply()
    }
    
    fun getDanmakuOffsetTime(): Long {
        return sharedPreferences.getLong("danmaku_offset_time", 0L)
    }
    
    fun setDanmakuOffsetTime(time: Long) {
        sharedPreferences.edit().putLong("danmaku_offset_time", time).apply()
    }
    
    fun getDanmakuShowScroll(): Boolean {
        return sharedPreferences.getBoolean("danmaku_show_scroll", true)
    }
    
    fun setDanmakuShowScroll(show: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_show_scroll", show).apply()
    }
    
    fun getDanmakuShowTop(): Boolean {
        return sharedPreferences.getBoolean("danmaku_show_top", true)
    }
    
    fun setDanmakuShowTop(show: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_show_top", show).apply()
    }
    
    fun getDanmakuShowBottom(): Boolean {
        return sharedPreferences.getBoolean("danmaku_show_bottom", true)
    }
    
    fun setDanmakuShowBottom(show: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_show_bottom", show).apply()
    }
    
    fun getDanmakuMaxScrollLine(): Int {
        return sharedPreferences.getInt("danmaku_max_scroll_line", 0)
    }
    
    fun setDanmakuMaxScrollLine(line: Int) {
        sharedPreferences.edit().putInt("danmaku_max_scroll_line", line).apply()
    }
    
    fun getDanmakuMaxTopLine(): Int {
        return sharedPreferences.getInt("danmaku_max_top_line", 0)
    }
    
    fun setDanmakuMaxTopLine(line: Int) {
        sharedPreferences.edit().putInt("danmaku_max_top_line", line).apply()
    }
    
    fun getDanmakuMaxBottomLine(): Int {
        return sharedPreferences.getInt("danmaku_max_bottom_line", 0)
    }
    
    fun setDanmakuMaxBottomLine(line: Int) {
        sharedPreferences.edit().putInt("danmaku_max_bottom_line", line).apply()
    }
    
    fun getDanmakuMaxScreenNum(): Int {
        return sharedPreferences.getInt("danmaku_max_screen_num", 0)
    }
    
    fun setDanmakuMaxScreenNum(num: Int) {
        sharedPreferences.edit().putInt("danmaku_max_screen_num", num).apply()
    }
    
    fun getDanmakuUseChoreographer(): Boolean {
        return sharedPreferences.getBoolean("danmaku_use_choreographer", false)
    }
    
    fun setDanmakuUseChoreographer(use: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_use_choreographer", use).apply()
    }
    
    fun getDanmakuDebug(): Boolean {
        return sharedPreferences.getBoolean("danmaku_debug", false)
    }
    
    fun setDanmakuDebug(debug: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_debug", debug).apply()
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
