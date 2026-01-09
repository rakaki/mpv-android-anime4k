package com.fam4k007.videoplayer.dandanplay.auth

import okhttp3.Interceptor
import okhttp3.Response
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import android.util.Base64
import java.util.Locale

/**
 * 弹弹play API 签名验证拦截器
 * 文档: https://doc.dandanplay.com/open/#%E4%BA%8C%E3%80%81api-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97
 * 
 * 签名生成规则 (V2):
 * 1. 拼接字符串: AppId + Timestamp + Path + AppSecret (注意: V2 不需要 Query 参数参与签名)
 * 2. 计算 SHA256 哈希
 * 3. 对哈希结果进行 Base64 编码
 */
class DanDanAuthInterceptor(
    private val appId: String,
    private val appSecret: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        
        // 获取当前 Unix 时间戳 (秒)
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        
        // 获取 API 路径 (例如 /api/v2/match)
        // encodedPath 获取的是 URL 中域名之后的部分，不包含 Query
        val path = originalUrl.encodedPath
        
        // 1. 拼接字符串: AppId + Timestamp + Path + AppSecret
        val rawString = appId + timestamp + path + appSecret
        
        // 2. 计算 SHA256 哈希并 Base64 编码
        val signature = sha256Base64(rawString)
        
        // 3. 添加 Header
        // X-AppId: [AppId]
        // X-Signature: [Signature]
        // X-Timestamp: [Timestamp]
        
        val newRequest = originalRequest.newBuilder()
            .header("X-AppId", appId)
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp)
            .build()
            
        return chain.proceed(newRequest)
    }
    
    private fun sha256Base64(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val encodedhash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encodedhash, Base64.NO_WRAP)
    }
}
