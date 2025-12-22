package com.fam4k007.videoplayer.webdav

import com.xyoye.sardine.Sardine
import com.xyoye.sardine.impl.OkHttpSardine
import com.xyoye.sardine.DavResource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * WebDAV 配置数据类（用于客户端）
 */
data class WebDavConfig(
    val serverUrl: String,
    val account: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false
)

/**
 * WebDAV 客户端工具类
 * 封装 Sardine 库的操作
 */
class WebDavClient(internal val config: WebDavConfig) {

    private val sardine: Sardine
    
    data class WebDavFile(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val modifiedTime: Long
    )

    init {
        // 创建不验证 SSL 证书的 OkHttpClient（用于自签名证书）
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        sardine = OkHttpSardine(okHttpClient)
        
        // 设置认证信息
        if (!config.isAnonymous && config.account.isNotEmpty()) {
            sardine.setCredentials(config.account, config.password)
        }
    }

    /**
     * 测试连接
     */
    suspend fun testConnection(): Boolean {
        return try {
            val url = normalizeUrl(config.serverUrl)
            sardine.list(url)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 列出指定路径下的文件和文件夹
     */
    suspend fun listFiles(path: String = ""): List<WebDavFile> {
        return try {
            val url = normalizeUrl(config.serverUrl + path)
            android.util.Log.d("WebDavClient", "列出文件: $url")
            val resources = sardine.list(url)
            android.util.Log.d("WebDavClient", "获取到 ${resources.size} 个资源")
            
            // 获取服务器基础路径用于计算相对路径
            val basePath = android.net.Uri.parse(config.serverUrl).path ?: "/"
            
            resources.mapNotNull { resource: DavResource ->
                val fileName = resource.name ?: return@mapNotNull null
                val fullPath = resource.path ?: return@mapNotNull null
                
                // 计算相对于 serverUrl 的路径
                val relativePath = if (fullPath.startsWith(basePath)) {
                    fullPath.substring(basePath.length).trimStart('/')
                } else {
                    fullPath.trimStart('/')
                }
                
                // 跳过当前目录本身（比较相对路径）
                if (relativePath.trimEnd('/') == path.trimEnd('/')) {
                    return@mapNotNull null
                }
                
                android.util.Log.d("WebDavClient", "文件: $fileName, 完整路径: $fullPath, 相对路径: $relativePath, 是否文件夹: ${resource.isDirectory}")
                
                WebDavFile(
                    name = fileName,
                    path = relativePath,
                    isDirectory = resource.isDirectory,
                    size = resource.contentLength ?: 0L,
                    modifiedTime = resource.modified?.time ?: 0L
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取文件的完整 URL（用于播放）
     */
    fun getFileUrl(filePath: String): String {
        val baseUrl = config.serverUrl.trimEnd('/')
        val path = filePath.trimStart('/')
        return "$baseUrl/$path"
    }

    /**
     * 获取认证头信息（用于播放器）
     */
    fun getAuthHeader(): Map<String, String>? {
        if (config.isAnonymous || config.account.isEmpty()) {
            return null
        }
        
        val credentials = okhttp3.Credentials.basic(config.account, config.password)
        return mapOf("Authorization" to credentials)
    }

    /**
     * 规范化 URL（确保以 / 结尾）
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.endsWith("/")) {
            normalized += "/"
        }
        return normalized
    }

    companion object {
        /**
         * 判断文件是否是视频文件
         */
        fun isVideoFile(fileName: String): Boolean {
            val videoExtensions = listOf(
                // 常见视频格式
                "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
                "m4v", "3gp", "3gpp", "ts", "mts", "m2ts",
                // 经典格式
                "rmvb", "rm", "mpg", "mpeg", "vob", "asf",
                // 高清格式
                "m2v", "m4p", "divx", "xvid", "ogv", "ogm",
                // 其他格式
                "f4v", "dat", "tp", "mxf", "dv"
            )
            val extension = fileName.substringAfterLast('.', "").lowercase()
            val result = extension in videoExtensions
            android.util.Log.d("WebDavClient", "检查视频: $fileName, 扩展名: $extension, 是视频: $result")
            return result
        }
    }
}
