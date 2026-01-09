package com.fam4k007.videoplayer.dandanplay.model

import com.google.gson.annotations.SerializedName

/**
 * 弹弹play API 响应数据模型
 */

// 通用响应包装
data class DanDanResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("errorCode") val errorCode: Int,
    @SerializedName("errorMessage") val errorMessage: String?,
    @SerializedName("result") val result: T? = null
)

// 弹幕搜索结果
data class DanDanMatchResult(
    @SerializedName("episodeId") val episodeId: Long,
    @SerializedName("animeId") val animeId: Long,
    @SerializedName("animeTitle") val animeTitle: String,
    @SerializedName("episodeTitle") val episodeTitle: String,
    @SerializedName("type") val type: String,
    @SerializedName("shift") val shift: Double
)

// 匹配请求体
data class MatchRequest(
    @SerializedName("fileName") val fileName: String,
    @SerializedName("fileHash") val fileHash: String,
    @SerializedName("fileSize") val fileSize: Long,
    @SerializedName("videoDuration") val videoDuration: Long,
    @SerializedName("matchMode") val matchMode: String
)

// 评论（弹幕）实体
data class DanDanComment(
    @SerializedName("cid") val cid: Long,
    @SerializedName("p") val p: String, // 格式: "时间,类型,颜色,用户ID"
    @SerializedName("m") val m: String  // 弹幕内容
)

data class DanDanCommentsResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("comments") val comments: List<DanDanComment>
)
