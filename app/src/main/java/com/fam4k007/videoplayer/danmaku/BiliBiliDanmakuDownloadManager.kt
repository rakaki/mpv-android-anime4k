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
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        private const val TIMEOUT_SECONDS = 10L // 降低超时时间到10秒,通常2-3秒就能完成
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true) // 自动跟随重定向
        .followSslRedirects(true)
        .build()
    
    /**
     * 下载结果
     */
    sealed class DownloadResult {
        data class Success(val fileName: String) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }
    
    /**
     * 从B站视频/番剧链接下载弹幕到指定目录
     * @param input 用户输入(可能包含分享文本)
     * @param saveDirectoryUri 保存目录的URI
     * @param downloadWholeSeason 番剧是否下载整季(true=整季,false=单集)
     * @param progressCallback 进度回调(当前集数,总集数,集标题,成功数,失败数)
     * @return 下载结果
     */
    suspend fun downloadDanmaku(
        input: String,
        saveDirectoryUri: Uri,
        downloadWholeSeason: Boolean = true,
        progressCallback: ((Int, Int, String, Int, Int) -> Unit)? = null
    ): DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始下载弹幕，原始输入: $input")
                
                // 1. 从输入中提取纯链接
                val videoUrl = extractUrlFromInput(input)
                    ?: return@withContext DownloadResult.Error("无法从输入中识别有效的B站链接")
                
                Log.d(TAG, "提取到的链接: $videoUrl")
                
                // 如果是 b23.tv 短链接,需要获取真实URL
                val realUrl = if (videoUrl.contains("b23.tv", ignoreCase = true)) {
                    // 对于 b23.tv/ep{id} 格式,直接解析 ep_id
                    val epMatch = Regex("b23\\.tv/ep(\\d+)", RegexOption.IGNORE_CASE).find(videoUrl)
                    if (epMatch != null) {
                        val epId = epMatch.groupValues[1]
                        "https://www.bilibili.com/bangumi/play/ep$epId"
                    } else {
                        // 其他短链接需要请求获取重定向
                        resolveShortUrl(videoUrl) ?: videoUrl
                    }
                } else {
                    videoUrl
                }
                
                Log.d(TAG, "解析后的链接: $realUrl")
                
                // 判断是普通视频还是番剧
                val isBangumi = realUrl.contains("bilibili.com/bangumi/play/", ignoreCase = true)
                
                if (isBangumi && downloadWholeSeason) {
                    // 番剧:下载整季所有集数
                    Log.d(TAG, "识别为番剧链接,准备下载整季弹幕")
                    downloadBangumiSeasonDanmaku(realUrl, saveDirectoryUri, progressCallback)
                } else if (isBangumi) {
                    // 番剧:下载单集
                    Log.d(TAG, "识别为番剧链接,下载单集弹幕")
                    downloadSingleBangumiEpisode(realUrl, saveDirectoryUri, progressCallback)
                } else {
                    // 普通视频:下载单个视频
                    Log.d(TAG, "识别为普通视频链接")
                    downloadSingleVideoDanmaku(realUrl, saveDirectoryUri, progressCallback)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "下载弹幕失败", e)
                DownloadResult.Error(e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 从用户输入中提取B站链接
     * 支持从分享文本中提取
     */
    private fun extractUrlFromInput(input: String): String? {
        val trimmedInput = input.trim()
        
        // 常见的B站链接模式
        val patterns = listOf(
            // 标准链接
            Regex("https?://(?:www\\.)?bilibili\\.com/video/[ABab][Vv][0-9A-Za-z]+[^\\s]*"),
            Regex("https?://(?:www\\.)?bilibili\\.com/video/av\\d+[^\\s]*"),
            Regex("https?://(?:www\\.)?bilibili\\.com/bangumi/play/(?:ss|ep)\\d+[^\\s]*"),
            // b23.tv 短链接
            Regex("https?://b23\\.tv/[0-9A-Za-z]+[^\\s]*"),
            // 移动端链接
            Regex("https?://(?:m|www)\\.bilibili\\.com/bangumi/play/(?:ss|ep)\\d+[^\\s]*")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(trimmedInput)
            if (match != null) {
                var url = match.value
                // 移除末尾可能的标点符号
                url = url.trimEnd('\u3002', '\uFF0C', ',', '.', '!', '\uFF01', '?', '\uFF1F', '"', '\u201D', '\'', '\u2019')
                Log.d(TAG, "从输入中提取到链接: $url")
                return url
            }
        }
        
        // 如果没有匹配到,检查是否本身就是干净的链接
        if (trimmedInput.contains("bilibili.com") || trimmedInput.contains("b23.tv")) {
            return trimmedInput
        }
        
        return null
    }
    
    /**
     * 解析 b23.tv 短链接获取真实 URL
     */
    private fun resolveShortUrl(shortUrl: String): String? {
        try {
            Log.d(TAG, "解析短链接: $shortUrl")
            
            val request = Request.Builder()
                .url(shortUrl)
                .addHeader("User-Agent", USER_AGENT)
                .build()
            
            // 使用 HEAD 请求获取重定向后的 URL,不下载内容
            client.newBuilder()
                .followRedirects(false) // 不自动跟随重定向
                .build()
                .newCall(request)
                .execute()
                .use { response ->
                    val location = response.header("Location")
                    if (location != null) {
                        Log.d(TAG, "短链接重定向到: $location")
                        return location
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "解析短链接失败", e)
        }
        return null
    }
    
    /**
     * 下载单个视频的弹幕
     */
    private suspend fun downloadSingleVideoDanmaku(
        videoUrl: String,
        saveDirectoryUri: Uri,
        progressCallback: ((Int, Int, String, Int, Int) -> Unit)? = null
    ): DownloadResult {
        return try {
            progressCallback?.invoke(1, 1, "正在获取视频信息...", 0, 0)
            
            val (cid, title) = extractVideoInfo(videoUrl)
                ?: return DownloadResult.Error("无法解析视频信息")
            
            Log.d(TAG, "视频信息 - 标题: $title, CID: $cid")
            
            progressCallback?.invoke(1, 1, "正在下载弹幕...", 0, 0)
            
            val xmlContent = downloadDanmakuXml(cid)
                ?: return DownloadResult.Error("弹幕下载失败")
            
            val fileName = saveToDirectory(saveDirectoryUri, title, xmlContent)
                ?: return DownloadResult.Error("保存文件失败")
            
            Log.d(TAG, "弹幕下载完成: $fileName")
            progressCallback?.invoke(1, 1, "下载完成", 1, 0)
            DownloadResult.Success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "下载视频弹幕失败", e)
            DownloadResult.Error(e.message ?: "未知错误")
        }
    }
    
    /**
     * 下载番剧单集的弹幕
     */
    private suspend fun downloadSingleBangumiEpisode(
        bangumiUrl: String,
        saveDirectoryUri: Uri,
        progressCallback: ((Int, Int, String, Int, Int) -> Unit)? = null
    ): DownloadResult {
        return try {
            progressCallback?.invoke(1, 1, "正在获取番剧信息...", 0, 0)
            
            val (cid, title) = extractBangumiInfo(bangumiUrl)
                ?: return DownloadResult.Error("无法解析番剧信息")
            
            Log.d(TAG, "番剧信息 - 标题: $title, CID: $cid")
            
            progressCallback?.invoke(1, 1, "正在下载弹幕...", 0, 0)
            
            val xmlContent = downloadDanmakuXml(cid)
                ?: return DownloadResult.Error("弹幕下载失败")
            
            val fileName = saveToDirectory(saveDirectoryUri, title, xmlContent)
                ?: return DownloadResult.Error("保存文件失败")
            
            Log.d(TAG, "弹幕下载完成: $fileName")
            progressCallback?.invoke(1, 1, "下载完成", 1, 0)
            DownloadResult.Success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "下载番剧弹幕失败", e)
            DownloadResult.Error(e.message ?: "未知错误")
        }
    }
    
    /**
     * 下载番剧整季所有集数的弹幕
     */
    private suspend fun downloadBangumiSeasonDanmaku(
        bangumiUrl: String,
        saveDirectoryUri: Uri,
        progressCallback: ((Int, Int, String, Int, Int) -> Unit)? = null
    ): DownloadResult {
        return try {
            progressCallback?.invoke(0, 0, "正在获取番剧季度信息...", 0, 0)
            
            // 获取番剧季度信息
            val seasonInfo = getBangumiSeasonInfo(bangumiUrl)
                ?: return DownloadResult.Error("无法获取番剧信息")
            
            val (seasonTitle, episodes) = seasonInfo
            Log.d(TAG, "番剧: $seasonTitle, 共${episodes.size}集")
            
            if (episodes.isEmpty()) {
                return DownloadResult.Error("该番剧没有可下载的集数")
            }
            
            var successCount = 0
            var failCount = 0
            
            // 下载每一集的弹幕
            for ((index, episode) in episodes.withIndex()) {
                try {
                    val (cid, epTitle) = episode
                    val fullTitle = "$seasonTitle - $epTitle"
                    
                    progressCallback?.invoke(index + 1, episodes.size, epTitle, successCount, failCount)
                    Log.d(TAG, "下载第${index + 1}/${episodes.size}集: $fullTitle")
                    
                    // 检查文件是否已存在
                    if (isFileExists(saveDirectoryUri, fullTitle)) {
                        Log.d(TAG, "第${index + 1}集已存在,跳过")
                        successCount++
                        continue
                    }
                    
                    val xmlContent = downloadDanmakuXml(cid)
                    if (xmlContent != null) {
                        val saved = saveToDirectory(saveDirectoryUri, fullTitle, xmlContent)
                        if (saved != null) {
                            successCount++
                            Log.d(TAG, "第${index + 1}集下载成功")
                        } else {
                            failCount++
                            Log.e(TAG, "第${index + 1}集保存失败")
                        }
                    } else {
                        failCount++
                        Log.e(TAG, "第${index + 1}集弹幕下载失败")
                    }
                    
                    // 添加延迟,避免请求过快被限流
                    // 现在使用镜像接口,限流较松,可以缩短间隔
                    if (index < episodes.size - 1) {
                        kotlinx.coroutines.delay(300)
                    }
                } catch (e: Exception) {
                    failCount++
                    Log.e(TAG, "下载第${index + 1}集时出错", e)
                }
            }
            
            val message = "番剧《$seasonTitle》下载完成\n成功: $successCount 集\n失败: $failCount 集"
            Log.d(TAG, message)
            
            progressCallback?.invoke(episodes.size, episodes.size, "下载完成", successCount, failCount)
            
            if (successCount > 0) {
                DownloadResult.Success(message)
            } else {
                DownloadResult.Error("所有集数下载失败")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "下载番剧弹幕失败", e)
            DownloadResult.Error(e.message ?: "未知错误")
        }
    }
    
    /**
     * 获取番剧季度的所有集数信息
     * @return Pair<季度标题, List<Pair<cid, 集数标题>>>
     */
    private fun getBangumiSeasonInfo(url: String): Pair<String, List<Pair<Long, String>>>? {
        try {
            // 提取 season_id 或 ep_id
            val seasonIdMatch = Regex("ss(\\d+)", RegexOption.IGNORE_CASE).find(url)
            val epIdMatch = Regex("ep(\\d+)", RegexOption.IGNORE_CASE).find(url)
            
            // 构建API请求
            val apiUrl = when {
                seasonIdMatch != null -> {
                    val seasonId = seasonIdMatch.groupValues[1]
                    "https://api.bilibili.com/pgc/view/web/season?season_id=$seasonId"
                }
                epIdMatch != null -> {
                    val epId = epIdMatch.groupValues[1]
                    "https://api.bilibili.com/pgc/view/web/season?ep_id=$epId"
                }
                else -> return null
            }
            
            Log.d(TAG, "请求番剧季度信息: $apiUrl")
            
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://www.bilibili.com")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "获取番剧信息失败: HTTP ${response.code}")
                    return null
                }
                
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(TAG, "响应体为空")
                    return null
                }
                
                val jsonObject = JSONObject(responseBody)
                val code = jsonObject.getInt("code")
                if (code != 0) {
                    Log.e(TAG, "API返回错误: code=$code, message=${jsonObject.optString("message")}")
                    return null
                }
                
                val result = jsonObject.getJSONObject("result")
                
                // 获取季度标题
                val seasonTitle = result.optString("season_title") 
                    ?: result.optString("title")
                    ?: "未知番剧"
                
                // 获取所有集数
                val episodes = result.optJSONArray("episodes")
                if (episodes == null || episodes.length() == 0) {
                    Log.e(TAG, "未找到番剧集数信息")
                    return null
                }
                
                val episodeList = mutableListOf<Pair<Long, String>>()
                for (i in 0 until episodes.length()) {
                    val ep = episodes.getJSONObject(i)
                    val cid = ep.getLong("cid")
                    val epTitle = ep.optString("long_title")
                        ?: ep.optString("title")
                        ?: "第${i + 1}集"
                    episodeList.add(Pair(cid, epTitle))
                }
                
                Log.d(TAG, "获取到番剧《$seasonTitle》共${episodeList.size}集")
                return Pair(seasonTitle, episodeList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取番剧季度信息失败", e)
            return null
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
     * 从番剧链接中提取CID和标题
     * 支持 ss{season_id} 和 ep{ep_id} 两种格式
     */
    private fun extractBangumiInfo(url: String): Pair<Long, String>? {
        try {
            // 提取 season_id 或 ep_id
            val seasonIdMatch = Regex("ss(\\d+)", RegexOption.IGNORE_CASE).find(url)
            val epIdMatch = Regex("ep(\\d+)", RegexOption.IGNORE_CASE).find(url)
            
            // 构建API请求
            val apiUrl = when {
                seasonIdMatch != null -> {
                    val seasonId = seasonIdMatch.groupValues[1]
                    "https://api.bilibili.com/pgc/view/web/season?season_id=$seasonId"
                }
                epIdMatch != null -> {
                    val epId = epIdMatch.groupValues[1]
                    "https://api.bilibili.com/pgc/view/web/season?ep_id=$epId"
                }
                else -> return null
            }
            
            Log.d(TAG, "请求番剧季度信息: $apiUrl")
            
            // 请求番剧信息
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://www.bilibili.com")
                .build()
            
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "番剧API响应: HTTP ${response.code}")
                if (!response.isSuccessful) {
                    Log.e(TAG, "获取番剧信息失败: HTTP ${response.code}, Headers: ${response.headers}")
                    return null
                }
                
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(TAG, "响应体为空")
                    return null
                }
                
                val jsonObject = JSONObject(responseBody)
                val code = jsonObject.getInt("code")
                if (code != 0) {
                    Log.e(TAG, "API返回错误: code=$code, message=${jsonObject.optString("message")}")
                    return null
                }
                
                val result = jsonObject.getJSONObject("result")
                
                // 获取标题
                val title = result.optString("season_title") 
                    ?: result.optString("title")
                    ?: "未知番剧"
                
                // 获取第一集的cid（作为默认）
                // 优先从episodes数组获取
                val episodes = result.optJSONArray("episodes")
                if (episodes != null && episodes.length() > 0) {
                    // 如果URL包含ep_id，尝试找到对应的集数
                    val targetEpId = epIdMatch?.groupValues?.get(1)?.toLongOrNull()
                    
                    if (targetEpId != null) {
                        // 查找指定的ep
                        for (i in 0 until episodes.length()) {
                            val ep = episodes.getJSONObject(i)
                            if (ep.getLong("id") == targetEpId) {
                                val cid = ep.getLong("cid")
                                val epTitle = ep.optString("long_title") 
                                    ?: ep.optString("title")
                                    ?: "第${i + 1}集"
                                val fullTitle = "$title - $epTitle"
                                Log.d(TAG, "找到指定集数: $fullTitle, CID: $cid")
                                return Pair(cid, fullTitle)
                            }
                        }
                    }
                    
                    // 如果没有指定或没找到，使用第一集
                    val firstEp = episodes.getJSONObject(0)
                    val cid = firstEp.getLong("cid")
                    val epTitle = firstEp.optString("long_title") 
                        ?: firstEp.optString("title")
                        ?: "第1集"
                    val fullTitle = "$title - $epTitle"
                    Log.d(TAG, "使用第一集: $fullTitle, CID: $cid")
                    return Pair(cid, fullTitle)
                }
                
                Log.e(TAG, "未找到番剧集数信息")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取番剧信息失败", e)
            return null
        }
    }
    
    /**
     * 下载弹幕XML内容(带重试)
     * B站API返回的是deflate压缩的XML数据
     */
    private fun downloadDanmakuXml(cid: Long, retryCount: Int = 3): String? {
        var lastException: Exception? = null
        
        repeat(retryCount) { attempt ->
            try {
                // 使用 comment.bilibili.com 镜像接口,避免 WBI 签名
                val url = "https://comment.bilibili.com/$cid.xml"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Referer", "https://www.bilibili.com")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "弹幕API响应: HTTP ${response.code}, Content-Type: ${response.header("Content-Type")}")
                    if (!response.isSuccessful) {
                        Log.e(TAG, "下载失败 (尝试${attempt + 1}/$retryCount): HTTP ${response.code}, Message: ${response.message}")
                        if (attempt < retryCount - 1) {
                            Thread.sleep(500) // 缩短重试间隔到0.5秒
                            return@repeat
                        }
                        return null
                    }
                    
                    val responseBody = response.body
                    if (responseBody == null) {
                        Log.e(TAG, "响应体为空 (尝试${attempt + 1}/$retryCount)")
                        if (attempt < retryCount - 1) {
                            Thread.sleep(500)
                            return@repeat
                        }
                        return null
                    }
                    
                    // B站API返回的是deflate压缩的二进制数据
                    val compressedData = responseBody.bytes()
                    Log.d(TAG, "下载成功，压缩数据长度: ${compressedData.size}")
                    
                    if (compressedData.isEmpty()) {
                        Log.e(TAG, "下载的数据为空 (尝试${attempt + 1}/$retryCount)")
                        if (attempt < retryCount - 1) {
                            Thread.sleep(500)
                            return@repeat
                        }
                        return null
                    }
                    
                    // 手动解压deflate数据
                    val content = decompressDeflate(compressedData)
                    if (content == null) {
                        Log.e(TAG, "解压失败 (尝试${attempt + 1}/$retryCount)")
                        if (attempt < retryCount - 1) {
                            Thread.sleep(500)
                            return@repeat
                        }
                        return null
                    }
                    
                    Log.d(TAG, "解压后内容长度: ${content.length}")
                    
                    if (content.isEmpty()) {
                        Log.e(TAG, "解压后内容为空 (尝试${attempt + 1}/$retryCount)")
                        if (attempt < retryCount - 1) {
                            Thread.sleep(500)
                            return@repeat
                        }
                        return null
                    }
                    
                    // 检查是否是有效的XML
                    if (!content.trimStart().startsWith("<")) {
                        Log.e(TAG, "返回的不是XML格式 (尝试${attempt + 1}/$retryCount)")
                        Log.e(TAG, "内容预览: ${content.take(200)}")
                        if (attempt < retryCount - 1) {
                            Thread.sleep(1000)
                            return@repeat
                        }
                        return null
                    }
                    
                    Log.d(TAG, "弹幕下载成功 CID=$cid")
                    return content
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "下载弹幕XML失败 (尝试${attempt + 1}/$retryCount): ${e.message}", e)
                if (attempt < retryCount - 1) {
                    Thread.sleep(1000)
                }
            }
        }
        
        Log.e(TAG, "所有重试均失败 CID=$cid", lastException)
        return null
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
     * 检查文件是否已存在
     */
    private fun isFileExists(directoryUri: Uri, videoTitle: String): Boolean {
        return try {
            val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return false
            val safeTitle = videoTitle
                .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s\\-_]"), "_")
                .take(100)
            val fileName = "$safeTitle.xml"
            directory.findFile(fileName) != null
        } catch (e: Exception) {
            Log.e(TAG, "检查文件是否存在失败", e)
            false
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
     * 验证URL是否为有效的B站视频/番剧链接
     */
    fun isValidBilibiliUrl(url: String): Boolean {
        val trimmedUrl = url.trim()
        
        // b23.tv 短链接
        val isShortUrl = trimmedUrl.contains("b23.tv/", ignoreCase = true)
        
        // 普通视频链接：bilibili.com/video/BV... 或 av...
        val isVideoUrl = trimmedUrl.contains("bilibili.com/video/", ignoreCase = true) &&
                (trimmedUrl.contains("BV", ignoreCase = true) || trimmedUrl.contains("av", ignoreCase = true))
        
        // 番剧链接：bilibili.com/bangumi/play/ss... 或 ep...
        val isBangumiUrl = trimmedUrl.contains("bilibili.com/bangumi/play/", ignoreCase = true) &&
                (trimmedUrl.contains("ss", ignoreCase = true) || trimmedUrl.contains("ep", ignoreCase = true))
        
        // 移动端链接
        val isMobileUrl = trimmedUrl.contains("m.bilibili.com/", ignoreCase = true)
        
        return isShortUrl || isVideoUrl || isBangumiUrl || isMobileUrl
    }
}
