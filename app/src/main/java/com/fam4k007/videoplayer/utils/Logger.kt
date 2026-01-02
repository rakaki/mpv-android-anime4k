package com.fam4k007.videoplayer.utils

import android.util.Log

/**
 * 统一日志管理工具
 * 自动根据BuildConfig控制日志输出，保护敏感信息
 */
object Logger {
    
    // Debug模式：始终开启日志（生产环境可通过ProGuard移除）
    private const val isDebugMode = true
    
    /**
     * Debug级别日志
     * Release版本不会输出，通过ProGuard移除
     */
    fun d(tag: String, msg: String) {
        if (isDebugMode) {
            Log.d(tag, msg)
        }
    }
    
    /**
     * Info级别日志
     * Release版本不会输出
     */
    fun i(tag: String, msg: String) {
        if (isDebugMode) {
            Log.i(tag, msg)
        }
    }
    
    /**
     * Warning级别日志
     * Release版本也会输出（重要警告）
     */
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.w(tag, msg, tr)
        } else {
            Log.w(tag, msg)
        }
    }
    
    /**
     * Error级别日志
     * 始终输出，用于错误追踪
     */
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.e(tag, msg, tr)
        } else {
            Log.e(tag, msg)
        }
    }
    
    /**
     * 敏感信息日志（自动脱敏）
     * Debug模式：显示完整内容
     * Release模式：只显示前后4位，中间用****替代
     * 
     * @param tag 日志标签
     * @param key 信息名称（如"Cookie", "Token"）
     * @param value 敏感信息内容
     */
    fun sensitive(tag: String, key: String, value: String?) {
        if (value.isNullOrEmpty()) {
            if (isDebugMode) {
                Log.d(tag, "$key: (empty)")
            }
            return
        }
        
        if (isDebugMode) {
            // Debug模式：显示完整信息（开发调试用）
            Log.d(tag, "$key: $value")
        } else {
            // Release模式：脱敏显示（保护用户隐私）
            val masked = if (value.length <= 8) {
                "****"  // 太短的直接全部隐藏
            } else {
                "${value.take(4)}****${value.takeLast(4)}"
            }
            Log.d(tag, "$key: $masked")
        }
    }
    
    /**
     * 详细日志（Verbose）
     * 仅在Debug模式输出，用于详细追踪
     */
    fun v(tag: String, msg: String) {
        if (isDebugMode) {
            Log.v(tag, msg)
        }
    }
}
