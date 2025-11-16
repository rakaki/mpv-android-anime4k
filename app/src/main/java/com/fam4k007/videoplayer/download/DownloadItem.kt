package com.fam4k007.videoplayer.download

data class DownloadItem(
    val id: String,
    val title: String,
    val url: String,
    val status: String, // "pending", "downloading", "paused", "completed", "failed", "merging"
    val progress: Int = 0,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val mediaType: MediaType = MediaType.Video,
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val fragments: List<DownloadFragmentState> = emptyList()
)

data class DownloadFragmentState(
    val type: String, // "video" or "audio"
    val url: String,
    val size: Long,
    val downloadedSize: Long = 0,
    val status: String = "pending" // "pending", "downloading", "completed", "failed"
)

// 番剧集数信息
data class EpisodeInfo(
    val episodeId: String,
    val aid: String,
    val cid: String,
    val title: String,
    val longTitle: String,
    val index: Int,
    val badge: String = "",
    val badgeType: Int = 0 // 0:普通 1:会员 2:限免
)