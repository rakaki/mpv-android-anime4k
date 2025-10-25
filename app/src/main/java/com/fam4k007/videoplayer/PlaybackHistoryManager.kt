package com.fam4k007.videoplayer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.fam4k007.videoplayer.utils.FormatUtils
import org.json.JSONArray
import org.json.JSONObject

/**
 * 播放历史记录管理器
 */
class PlaybackHistoryManager(context: Context) {

    companion object {
        private const val TAG = "PlaybackHistoryManager"
    }

    // 内存缓存相关
    private var cachedHistory: List<HistoryItem>? = null
    private var lastSavedJsonString: String? = null
    private val cacheLock = Any()

    data class HistoryItem(
        val uri: String,
        val fileName: String,
        val position: Long,      // 播放位置（毫秒）
        val duration: Long,      // 总时长（毫秒）
        val lastPlayed: Long,    // 最后播放时间戳
        val folderName: String   // 所属文件夹
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("uri", uri)
                put("fileName", fileName)
                put("position", position)
                put("duration", duration)
                put("lastPlayed", lastPlayed)
                put("folderName", folderName)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): HistoryItem {
                return HistoryItem(
                    uri = json.getString("uri"),
                    fileName = json.getString("fileName"),
                    position = json.getLong("position"),
                    duration = json.getLong("duration"),
                    lastPlayed = json.getLong("lastPlayed"),
                    folderName = json.optString("folderName", "未知文件夹")
                )
            }
        }

        fun getFormattedDate(): String {
            return FormatUtils.formatDateShort(lastPlayed)
        }

        fun getProgressPercentage(): Int {
            return if (duration > 0) {
                ((position.toDouble() / duration) * 100).toInt()
            } else {
                0
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(AppConstants.Preferences.PLAYBACK_HISTORY, Context.MODE_PRIVATE)

    /**
     * 添加或更新历史记录
     */
    fun addHistory(
        uri: Uri,
        fileName: String,
        position: Long,
        duration: Long,
        folderName: String
    ) {
        try {
            val history = getHistory().toMutableList()
            
            // 移除已存在的相同视频
            history.removeAll { it.uri == uri.toString() }
            
            // 添加新记录到开头
            history.add(
                0, HistoryItem(
                    uri = uri.toString(),
                    fileName = fileName,
                    position = position,
                    duration = duration,
                    lastPlayed = System.currentTimeMillis(),
                    folderName = folderName
                )
            )
            
            // 限制历史记录数量
            if (history.size > AppConstants.Defaults.MAX_HISTORY_SIZE) {
                history.subList(AppConstants.Defaults.MAX_HISTORY_SIZE, history.size).clear()
            }
            
            saveHistory(history)
            Log.d(TAG, "History added successfully: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add history", e)
            // 降级处理：不中断主流程
        }
    }

    /**
     * 获取所有历史记录
     * 优化：使用内存缓存避免频繁 JSON 解析
     */
    fun getHistory(): List<HistoryItem> {
        return try {
            val jsonString = sharedPreferences.getString(AppConstants.Preferences.HISTORY_LIST, null) 
                ?: return emptyList()
            
            // 验证 JSON 格式
            if (jsonString.isEmpty()) {
                Log.w(TAG, "History JSON string is empty")
                return emptyList()
            }
            
            // 缓存命中：如果 JSON 字符串没变化，直接返回缓存
            synchronized(cacheLock) {
                if (lastSavedJsonString == jsonString && cachedHistory != null) {
                    Log.d(TAG, "History cache hit: ${cachedHistory!!.size} items")
                    return cachedHistory!!
                }
            }
            
            // 缓存未命中：解析 JSON
            val jsonArray = JSONArray(jsonString)
            val history = mutableListOf<HistoryItem>()
            
            // 逐个解析，遇到错误的数据项时跳过而不是中断
            for (i in 0 until jsonArray.length()) {
                try {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val item = HistoryItem.fromJson(jsonObject)
                    
                    // 数据验证
                    if (item.uri.isNotEmpty() && item.fileName.isNotEmpty()) {
                        history.add(item)
                    } else {
                        Log.w(TAG, "Skipping invalid history item at index $i: missing uri or fileName")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse history item at index $i", e)
                    // 继续处理下一项
                    continue
                }
            }
            
            // 更新缓存
            synchronized(cacheLock) {
                cachedHistory = history
                lastSavedJsonString = jsonString
            }
            
            Log.d(TAG, "Successfully loaded ${history.size} history items (cache updated)")
            history
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse history", e)
            // 降级处理：返回空列表而不是崩溃
            emptyList()
        }
    }

    /**
     * 删除单条历史记录
     */
    fun removeHistory(uri: String) {
        try {
            val history = getHistory().toMutableList()
            val sizeBefore = history.size
            history.removeAll { it.uri == uri }
            
            if (history.size < sizeBefore) {
                saveHistory(history)
                Log.d(TAG, "History removed successfully: $uri")
            } else {
                Log.w(TAG, "No history found to remove: $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove history", e)
            // 降级处理：不中断主流程
        }
    }

    /**
     * 清空所有历史记录
     */
    fun clearHistory() {
        try {
            sharedPreferences.edit().remove(AppConstants.Preferences.HISTORY_LIST).apply()
            // 清空缓存
            synchronized(cacheLock) {
                cachedHistory = null
                lastSavedJsonString = null
            }
            Log.d(TAG, "History cleared successfully (cache invalidated)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history", e)
            // 降级处理：不中断主流程
        }
    }

    /**
     * 保存历史记录
     * 优化：保存新字符串后更新缓存
     */
    private fun saveHistory(history: List<HistoryItem>) {
        try {
            val jsonArray = JSONArray()
            
            // 逐个转换为 JSON，遇到错误时跳过而不是中断
            for (item in history) {
                try {
                    jsonArray.put(item.toJson())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to convert history item to JSON: ${item.fileName}", e)
                    // 继续处理下一项
                    continue
                }
            }
            
            // 数据验证：不保存空数组给系统，直接删除
            val jsonString = jsonArray.toString()
            if (jsonString.isEmpty() || jsonString == "[]") {
                sharedPreferences.edit().remove(AppConstants.Preferences.HISTORY_LIST).apply()
                // 清空缓存
                synchronized(cacheLock) {
                    cachedHistory = null
                    lastSavedJsonString = null
                }
                Log.d(TAG, "Cleared empty history (cache invalidated)")
                return
            }
            
            sharedPreferences.edit()
                .putString(AppConstants.Preferences.HISTORY_LIST, jsonString)
                .apply()
            
            // 更新缓存
            synchronized(cacheLock) {
                cachedHistory = history
                lastSavedJsonString = jsonString
            }
            
            Log.d(TAG, "History saved successfully: ${history.size} items (cache updated)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save history", e)
            // 降级处理：不中断主流程
        }
    }
}
