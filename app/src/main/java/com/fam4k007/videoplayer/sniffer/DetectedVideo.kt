package com.fam4k007.videoplayer.sniffer

/**
 * 视频URL检测结果
 */
data class DetectedVideo(
    val url: String,              // 视频URL
    val title: String = "",       // 视频标题
    val pageUrl: String = "",     // 来源页面URL
    val headers: Map<String, String> = emptyMap(),  // HTTP头信息
    val timestamp: Long = System.currentTimeMillis()  // 检测时间戳
) {
    /**
     * 转换为完整的URL字符串（包含HTTP头）
     * 格式：url;{Cookie@xxx&&User-Agent@yyy}
     */
    fun toFullUrlString(): String {
        if (headers.isEmpty()) {
            return url
        }
        
        val headerStr = headers.entries.joinToString("&&") { (key, value) ->
            // 将半角分号转换为全角分号（避免与分隔符冲突）
            "$key@${value.replace("; ", "；；")}"
        }
        
        return "$url;{$headerStr}"
    }
    
    /**
     * 获取格式化的显示文本
     */
    fun getDisplayText(): String {
        val format = when {
            url.contains(".m3u8") -> "M3U8"
            url.contains(".mp4") -> "MP4"
            url.contains(".flv") -> "FLV"
            url.contains(".avi") -> "AVI"
            url.contains(".mkv") -> "MKV"
            url.contains("rtmp://") -> "RTMP"
            url.contains("rtsp://") -> "RTSP"
            else -> "VIDEO"
        }
        
        return if (title.isNotEmpty()) {
            "$title ($format)"
        } else {
            format
        }
    }
}
