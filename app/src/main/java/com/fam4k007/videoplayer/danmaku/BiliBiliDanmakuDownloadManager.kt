package com.fam4k007.videoplayer.danmaku

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * B站弹幕下载管理器
 * 负责从B站视频链接下载弹幕XML文件
 */
class BiliBiliDanmakuDownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BiliDanmuDownload"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val TIMEOUT_SECONDS = 15L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * 下载结果
     */
    sealed class DownloadResult {
        data class Success(val fileName: String) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }
    
    /**
     * 从B站视频链接下载弹幕到指定目录
     * @param videoUrl 视频链接
     * @param saveDirectoryUri 保存目录的URI
     * @return 下载结果
     */
    suspend fun downloadDanmaku(
        videoUrl: String,
        saveDirectoryUri: Uri
    ): DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始下载弹幕，URL: $videoUrl")
                
                // 1. 从网页HTML中提取CID和标题
                val (cid, title) = extractVideoInfo(videoUrl)
                    ?: return@withContext DownloadResult.Error("无法解析视频信息")
                
                Log.d(TAG, "视频信息 - 标题: $title, CID: $cid")
                
                // 2. 下载弹幕XML
                val xmlContent = downloadDanmakuXml(cid)
                    ?: return@withContext DownloadResult.Error("弹幕下载失败")
                
                // 3. 保存到用户选择的目录
                val fileName = saveToDirectory(saveDirectoryUri, title, xmlContent)
                    ?: return@withContext DownloadResult.Error("保存文件失败")
                
                Log.d(TAG, "弹幕下载完成: $fileName")
                DownloadResult.Success(fileName)
                
            } catch (e: Exception) {
                Log.e(TAG, "下载弹幕失败", e)
                DownloadResult.Error(e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 从HTML中提取视频CID和标题
     */
    private fun extractVideoInfo(url: String): Pair<Long, String>? {
        try {
            // 使用Jsoup获取网页内容
            val html = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_SECONDS.toInt() * 1000)
                .get()
                .toString()
            
            // 从 __INITIAL_STATE__ 中解析
            val patterns = listOf(
                "window.__INITIAL_STATE__=" to "};(function",
                "__INITIAL_STATE__=" to ";(function"
            )
            
            for ((startTag, endTag) in patterns) {
                val startIndex = html.indexOf(startTag)
                if (startIndex == -1) continue
                
                val actualStart = startIndex + startTag.length
                val endIndex = html.indexOf(endTag, actualStart)
                if (endIndex == -1) continue
                
                val jsonStr = html.substring(actualStart, endIndex + 1)
                val jsonObject = JSONObject(jsonStr)
                
                // 提取videoData
                val videoData = jsonObject.optJSONObject("videoData") ?: continue
                
                val cid = videoData.getLong("cid")
                val title = videoData.getString("title")
                
                return Pair(cid, title)
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "解析HTML失败", e)
            return null
        }
    }
    
    /**
     * 下载弹幕XML内容
     * B站API返回的是deflate压缩的XML数据
     */
    private fun downloadDanmakuXml(cid: Long): String? {
        try {
            val url = "https://api.bilibili.com/x/v1/dm/list.so?oid=$cid"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://www.bilibili.com")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "下载失败: HTTP ${response.code}")
                    return null
                }
                
                val responseBody = response.body
                if (responseBody == null) {
                    Log.e(TAG, "响应体为空")
                    return null
                }
                
                // B站API返回的是deflate压缩的二进制数据
                val compressedData = responseBody.bytes()
                Log.d(TAG, "下载成功，压缩数据长度: ${compressedData.size}")
                
                // 手动解压deflate数据
                val content = decompressDeflate(compressedData)
                if (content == null) {
                    Log.e(TAG, "解压失败")
                    return null
                }
                
                Log.d(TAG, "解压后内容长度: ${content.length}")
                Log.d(TAG, "内容预览(前500字符): ${content.take(500)}")
                
                // 检查是否是有效的XML
                if (!content.trimStart().startsWith("<")) {
                    Log.e(TAG, "返回的不是XML格式")
                    return null
                }
                
                return content
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载弹幕XML失败", e)
            return null
        }
    }
    
    /**
     * 解压deflate压缩数据
     */
    private fun decompressDeflate(data: ByteArray): String? {
        return try {
            val inflater = Inflater(true) // true表示使用nowrap模式（原始deflate）
            val inputStream = InflaterInputStream(ByteArrayInputStream(data), inflater)
            val outputStream = ByteArrayOutputStream()
            
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                outputStream.write(buffer, 0, len)
            }
            
            inputStream.close()
            outputStream.close()
            
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            Log.e(TAG, "解压失败", e)
            null
        }
    }
    
    /**
     * 保存XML内容到用户选择的目录
     */
    private fun saveToDirectory(
        directoryUri: Uri,
        videoTitle: String,
        xmlContent: String
    ): String? {
        try {
            val directory = DocumentFile.fromTreeUri(context, directoryUri)
                ?: return null
            
            // 清理文件名中的非法字符
            val safeTitle = videoTitle
                .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s\\-_]"), "_")
                .take(100) // 限制文件名长度
            
            val fileName = "$safeTitle.xml"
            
            // 检查文件是否已存在
            var existingFile = directory.findFile(fileName)
            if (existingFile != null) {
                existingFile.delete()
            }
            
            // 创建新文件
            val newFile = directory.createFile("text/xml", fileName)
                ?: return null
            
            // 写入内容，使用UTF-8编码
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                // 如果XML没有声明，添加UTF-8声明
                val content = if (!xmlContent.trimStart().startsWith("<?xml")) {
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$xmlContent"
                } else if (!xmlContent.contains("encoding", ignoreCase = true)) {
                    // 如果有XML声明但没有encoding，替换掉
                    xmlContent.replaceFirst(
                        "<?xml version=\"1.0\"?>",
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    )
                } else {
                    xmlContent
                }
                
                output.write(content.toByteArray(Charsets.UTF_8))
                Log.d(TAG, "文件写入成功: $fileName")
                Log.d(TAG, "写入内容长度: ${content.length}")
            }
            
            return fileName
        } catch (e: Exception) {
            Log.e(TAG, "保存文件失败", e)
            return null
        }
    }
    
    /**
     * 验证URL是否为有效的B站视频链接
     */
    fun isValidBilibiliUrl(url: String): Boolean {
        return url.contains("bilibili.com/video/", ignoreCase = true) &&
                (url.contains("BV", ignoreCase = true) || url.contains("av", ignoreCase = true))
    }
}
