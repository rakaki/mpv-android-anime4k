package com.fam4k007.videoplayer.dandanplay

import android.util.Log
import com.fam4k007.videoplayer.dandanplay.auth.DanDanAuthInterceptor
import com.fam4k007.videoplayer.dandanplay.model.DanDanCommentsResponse
import com.fam4k007.videoplayer.dandanplay.model.DanDanMatchResult
import com.fam4k007.videoplayer.dandanplay.model.DanDanResponse
import com.fam4k007.videoplayer.dandanplay.model.MatchRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * 弹弹play API 管理器
 */
object DanDanApiManager {
    private const val TAG = "DanDanApiManager"
    private const val BASE_URL = "https://api.dandanplay.net"
    
    // TODO: 这里的 AppId 和 AppSecret 应该替换为真实申请的值，或者放到配置中
    // 临时使用文档示例或空值
    private const val APP_ID = "" 
    private const val APP_SECRET = ""

    private val gson: Gson = GsonBuilder().create()
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(DanDanAuthInterceptor(APP_ID, APP_SECRET))
            .build()
    }

    /**
     * 计算文件前 16MB 的 MD5
     * 弹弹play 匹配规则：计算文件前 16MB (16 * 1024 * 1024 bytes) 内容的 MD5 值
     */
    suspend fun calculateFileHash(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext ""

            val bufferSize = 16 * 1024 * 1024 // 16MB
            val buffer = ByteArray(bufferSize)
            
            FileInputStream(file).use { fis ->
                val bytesRead = fis.read(buffer)
                if (bytesRead <= 0) return@withContext ""
                
                val digest = MessageDigest.getInstance("MD5")
                digest.update(buffer, 0, bytesRead)
                val messageDigest = digest.digest()
                
                val hexString = StringBuilder()
                for (b in messageDigest) {
                    hexString.append(String.format("%02x", b))
                }
                return@withContext hexString.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating file hash", e)
            return@withContext ""
        }
    }

    /**
     * 匹配视频
     * POST /api/v2/match
     */
    suspend fun matchVideo(
        fileName: String,
        filePath: String,
        durationMs: Long
    ): List<DanDanMatchResult> = withContext(Dispatchers.IO) {
        val fileHash = calculateFileHash(filePath)
        val fileSize = File(filePath).length()
        
        val requestBody = MatchRequest(
            fileName = fileName,
            fileHash = fileHash,
            fileSize = fileSize,
            videoDuration = durationMs / 1000, // 转换为秒
            matchMode = "hashAndFileName"
        )
        
        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url("$BASE_URL/api/v2/match")
            .post(body)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Match video failed: ${response.code}")
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<DanDanResponse<List<DanDanMatchResult>>>() {}.type
                val apiResponse = gson.fromJson<DanDanResponse<List<DanDanMatchResult>>>(responseBody, type)
                
                if (apiResponse.success) {
                    return@withContext apiResponse.result ?: emptyList()
                } else {
                    Log.e(TAG, "Match video API error: ${apiResponse.errorCode} ${apiResponse.errorMessage}")
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Match video exception", e)
            return@withContext emptyList()
        }
    }

    /**
     * 获取弹幕
     * GET /api/v2/comment/{episodeId}?withRelated=true&ch_convert=1
     */
    suspend fun getComments(episodeId: Long): List<com.fam4k007.videoplayer.dandanplay.model.DanDanComment> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            // withRelated=true: 获取相关第三方弹幕
            // ch_convert=1: 繁体转简体
            .url("$BASE_URL/api/v2/comment/$episodeId?withRelated=true&ch_convert=1")
            .get()
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Get comments failed: ${response.code}")
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val type = object : TypeToken<DanDanResponse<DanDanCommentsResponse>>() {}.type
                val apiResponse = gson.fromJson<DanDanResponse<DanDanCommentsResponse>>(responseBody, type)
                
                if (apiResponse.success) {
                    return@withContext apiResponse.result?.comments ?: emptyList()
                } else {
                    Log.e(TAG, "Get comments API error: ${apiResponse.errorCode} ${apiResponse.errorMessage}")
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get comments exception", e)
            return@withContext emptyList()
        }
    }
}
