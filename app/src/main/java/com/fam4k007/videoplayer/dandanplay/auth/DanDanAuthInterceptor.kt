package com.fam4k007.videoplayer.dandanplay.auth

import okhttp3.Interceptor
import okhttp3.Response
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.TreeMap

/**
 * 弹弹play API 签名验证拦截器
 * 文档: https://doc.dandanplay.com/open/#_5-%E7%AD%BE%E5%90%8D%E9%AA%8C%E8%AF%81%E6%A8%A1%E5%BC%8F%E6%8C%87%E5%8D%97
 */
class DanDanAuthInterceptor(
    private val appId: String,
    private val appSecret: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        
        // 1. 构建参数 Map
        val params = TreeMap<String, String>()
        
        // 添加原有查询参数
        for (i in 0 until originalUrl.querySize) {
            params[originalUrl.queryParameterName(i)] = originalUrl.queryParameterValue(i) ?: ""
        }
        
        // 2. 拼接字符串: appId + key1value1key2value2... + appSecret
        val sb = StringBuilder()
        sb.append(appId)
        for ((key, value) in params) {
            sb.append(key).append(value)
        }
        sb.append(appSecret)
        
        // 3. 计算 SHA256 哈希作为签名
        val signature = sha256(sb.toString())
        
        // 4. 重建 URL，添加签名头或参数? 
        // 文档说明：API 访问验证均在 HTTP Header 中进行
        // X-AppId: [AppId]
        // X-Signature: [Signature]
        
        val newRequest = originalRequest.newBuilder()
            .header("X-AppId", appId)
            .header("X-Signature", signature)
            .build()
            
        return chain.proceed(newRequest)
    }
    
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val encodedhash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        
        val hexString = StringBuilder(2 * encodedhash.size)
        for (i in encodedhash.indices) {
            val hex = Integer.toHexString(0xff and encodedhash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}
